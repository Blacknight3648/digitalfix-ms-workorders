package df.digitalfix_ms_workorders.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de mensajería del productor puro (sin declarar exchanges, colas ni bindings;
 * la topología AMQP es responsabilidad exclusiva de {@code ms-admin-queues}).
 * <ul>
 *   <li><b>Mensajes no entregados</b>: con {@code mandatory} el broker devuelve lo que ninguna cola
 *       recibió (ReturnsCallback) y con publisher confirms avisa si no pudo aceptarlo
 *       (ConfirmCallback). Ambos casos quedan en el log con su eventId.</li>
 * </ul>
 */
@Slf4j
@Configuration
public class RabbitProducerConfig {

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
