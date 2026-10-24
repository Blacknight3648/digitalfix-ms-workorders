package df.digitalfix_ms_workorders.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class RabbitProducerConfigTest {

    public static final ProductorRabbitProperties PROPIEDADES = new ProductorRabbitProperties(
            new ProductorRabbitProperties.Exchanges("cmd.direct", "cmd.topic"),
            new ProductorRabbitProperties.RoutingKeys("email", "dispatch.ticket", "report.gen"));

    private final RabbitProducerConfig config = new RabbitProducerConfig(PROPIEDADES);

    static class SampleEvent {
        private String eventId;
        private LocalDateTime timestamp;

        public SampleEvent() {
        }

        public SampleEvent(String eventId, LocalDateTime timestamp) {
            this.eventId = eventId;
            this.timestamp = timestamp;
        }

        public String getEventId() {
            return eventId;
        }

        public void setEventId(String eventId) {
            this.eventId = eventId;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
        }
    }

    @Test
    @DisplayName("jsonMessageConverter debe crear una instancia de Jackson2JsonMessageConverter y serializar fechas ISO-8601")
    void testJsonMessageConverterSerializesIsoDates() {
        MessageConverter converter = config.jsonMessageConverter();
        assertNotNull(converter, "El convertidor de mensajes no debe ser nulo");
        assertInstanceOf(Jackson2JsonMessageConverter.class, converter, "Debe ser de tipo Jackson2JsonMessageConverter");

        LocalDateTime now = LocalDateTime.of(2026, 10, 18, 22, 30, 0);
        SampleEvent event = new SampleEvent("EVT-100", now);

        MessageProperties messageProperties = new MessageProperties();
        Message message = converter.toMessage(event, messageProperties);

        assertNotNull(message, "El mensaje AMQP generado no debe ser nulo");
        String json = new String(message.getBody(), StandardCharsets.UTF_8);

        assertTrue(json.contains("\"eventId\":\"EVT-100\""), "El JSON debe contener el ID del evento");
        assertTrue(json.contains("\"timestamp\":\"2026-10-18T22:30:00\""),
                "La fecha debe estar serializada como cadena ISO-8601, no como timestamp numérico: " + json);
    }

    @Test
    @DisplayName("rabbitTemplate debe configurar la ConnectionFactory y el MessageConverter")
    void testRabbitTemplateConfiguration() {
        ConnectionFactory connectionFactory = Mockito.mock(ConnectionFactory.class);
        MessageConverter converter = config.jsonMessageConverter();

        RabbitTemplate template = config.rabbitTemplate(connectionFactory, converter);

        assertNotNull(template, "El RabbitTemplate no debe ser nulo");
        assertSame(connectionFactory, template.getConnectionFactory(), "La ConnectionFactory debe ser la provista");
        assertSame(converter, template.getMessageConverter(), "El MessageConverter debe ser el jsonMessageConverter");
    }

    @Test
    @DisplayName("Declara como beans los exchanges donde publica, durables y del tipo del caso")
    void testExchangesDelProductor() {
        assertEquals("cmd.direct", config.cmdDirectExchange().getName());
        assertEquals(ExchangeTypes.DIRECT, config.cmdDirectExchange().getType());
        assertTrue(config.cmdDirectExchange().isDurable());
        assertEquals("cmd.topic", config.cmdTopicExchange().getName());
        assertEquals(ExchangeTypes.TOPIC, config.cmdTopicExchange().getType());
        assertTrue(config.cmdTopicExchange().isDurable());
    }

    @Test
    @DisplayName("rabbitTemplate publica con mandatory para que lo no enrutable vuelva y quede en el log")
    void testRabbitTemplateMandatory() {
        RabbitTemplate template = config.rabbitTemplate(Mockito.mock(ConnectionFactory.class), config.jsonMessageConverter());

        assertTrue(template.isMandatoryFor(new Message(new byte[0], new MessageProperties())));
    }
}
