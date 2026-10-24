package df.digitalfix_ms_workorders.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de mensajería del productor. Toda la infraestructura de RabbitMQ de ms-workorders
 * está aquí; los servicios de negocio no la conocen (publican eventos de dominio y
 * {@code RabbitPublicadorEventosOrden} los traduce a mensajes).
 * <ul>
 *   <li><b>Exchanges</b>: se declaran los dos donde publica. La declaración es idempotente y usa los
 *       mismos parámetros que ms-notify, así el productor no depende de que el consumidor haya
 *       arrancado primero (publicar en un exchange inexistente cierra el canal y pierde el mensaje).
 *       Las colas y bindings son de ms-notify, el dueño de la topología.</li>
 *   <li><b>Mensajes no entregados</b>: con {@code mandatory} el broker devuelve lo que ninguna cola
 *       recibió (ReturnsCallback) y con publisher confirms avisa si no pudo aceptarlo
 *       (ConfirmCallback). Ambos casos quedan en el log con su eventId.</li>
 * </ul>
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(ProductorRabbitProperties.class)
public class RabbitProducerConfig {

    private final ProductorRabbitProperties propiedades;

    public RabbitProducerConfig(ProductorRabbitProperties propiedades) {
        this.propiedades = propiedades;
    }

    /** cmd.direct: comandos por routing key exacta (email.send, dispatch.ticket, report.gen). */
    @Bean
    public DirectExchange cmdDirectExchange() {
        return ExchangeBuilder.directExchange(propiedades.exchanges().direct()).durable(true).build();
    }

    /** cmd.topic: los mismos comandos por patrón (email.*, dispatch.#, report.*). */
    @Bean
    public TopicExchange cmdTopicExchange() {
        return ExchangeBuilder.topicExchange(propiedades.exchanges().topic()).durable(true).build();
    }

    /** JSON con fechas ISO-8601 (no timestamps numéricos), que es lo que espera ms-notify. */
    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter);

        // Sin cola de destino para la routing key: el broker devuelve el mensaje en vez de descartarlo.
        rabbitTemplate.setMandatory(true);
        rabbitTemplate.setReturnsCallback(devuelto -> log.error(
                "Mensaje NO ENTREGADO: ninguna cola recibe exchange='{}' routingKey='{}' ({} {}). eventId={}",
                devuelto.getExchange(), devuelto.getRoutingKey(), devuelto.getReplyCode(), devuelto.getReplyText(),
                devuelto.getMessage().getMessageProperties().getMessageId()));

        // Confirmación del broker por mensaje (requiere spring.rabbitmq.publisher-confirm-type=correlated).
        rabbitTemplate.setConfirmCallback((correlacion, aceptado, causa) -> {
            String eventId = correlacion != null ? correlacion.getId() : "sin-id";
            if (aceptado) {
                log.debug("Broker confirmó el evento {}", eventId);
            } else {
                log.error("Broker RECHAZÓ el evento {}: {}", eventId, causa);
            }
        });
        return rabbitTemplate;
    }
}
