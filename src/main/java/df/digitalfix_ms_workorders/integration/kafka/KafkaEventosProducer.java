package df.digitalfix_ms_workorders.integration.kafka;

import df.digitalfix_ms_workorders.dto.event.WorkOrderKafkaEvent;
import df.digitalfix_ms_workorders.messaging.PublicacionEventoException;
import df.digitalfix_ms_workorders.messaging.PublicadorEventosKafka;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Adaptador Kafka de {@link PublicadorEventosKafka}: publica cada evento en el tópico
 * {@code workorders.events} usando el {@code orderId} como clave de partición para preservar el
 * orden de transiciones por orden de trabajo.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "digitalfix.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaEventosProducer implements PublicadorEventosKafka {

    private static final long SEND_TIMEOUT_SECONDS = 5L;

    private final KafkaTemplate<String, WorkOrderKafkaEvent> kafkaTemplate;
    private final String topic;

    public KafkaEventosProducer(
            KafkaTemplate<String, WorkOrderKafkaEvent> kafkaTemplate,
            @Value("${kafka.topics.workorders-events:workorders.events}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Override
    public void publicarEventoOrden(WorkOrderKafkaEvent evento) {
        Objects.requireNonNull(evento, "El evento Kafka no puede ser nulo");
        Objects.requireNonNull(evento.getOrderId(), "El evento Kafka requiere orderId");
        String key = String.valueOf(evento.getOrderId());
        try {
            SendResult<String, WorkOrderKafkaEvent> resultado = kafkaTemplate.send(topic, key, evento)
                    .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            var metadata = resultado.getRecordMetadata();
            log.info("Evento Kafka {} ({}) de OT-{} publicado en {}-{}@{}",
                    evento.getEventId(), evento.getNewStatus(), evento.getOrderId(),
                    metadata.topic(), metadata.partition(), metadata.offset());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PublicacionEventoException("Publicación en Kafka interrumpida para OT-" + evento.getOrderId(), e);
        } catch (ExecutionException | TimeoutException | KafkaException e) {
            throw new PublicacionEventoException(
                    "No se pudo publicar en Kafka (" + topic + ") para OT-" + evento.getOrderId() + ": " + e.getMessage(),
                    e);
        }
    }
}
