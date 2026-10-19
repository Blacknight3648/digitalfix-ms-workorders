package df.digitalfix_ms_workorders.config;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;

/**
 * Adaptador de compatibilidad hacia {@link com.digitalfix.workorders.config.RabbitProducerConfig}.
 */
public class RabbitProducerConfig {

    private final com.digitalfix.workorders.config.RabbitProducerConfig delegate =
            new com.digitalfix.workorders.config.RabbitProducerConfig();

    public MessageConverter jsonMessageConverter() {
        return delegate.jsonMessageConverter();
    }

    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        return delegate.rabbitTemplate(connectionFactory, jsonMessageConverter);
    }
}
