package df.digitalfix_ms_workorders.integration.kafka;

import df.digitalfix_ms_workorders.dto.event.WorkOrderKafkaEvent;
import df.digitalfix_ms_workorders.messaging.PublicacionEventoException;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class KafkaEventosProducerTest {

    private KafkaTemplate<String, WorkOrderKafkaEvent> kafkaTemplate;
    private KafkaEventosProducer producer;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        kafkaTemplate = mock(KafkaTemplate.class);
        producer = new KafkaEventosProducer(kafkaTemplate, "workorders.events");
    }

    @Test
    @DisplayName("Publica evento en workorders.events usando orderId como clave")
    void publicaEventoEnTopicoEsperado() {
        WorkOrderKafkaEvent evento = WorkOrderKafkaEvent.builder()
                .eventId("evt-42")
                .eventType("WORK_ORDER_STATUS_CHANGED")
                .orderId(42L)
                .previousStatus("CREADA")
                .newStatus("ASIGNADA")
                .assignedTechnicianId(70L)
                .clientId(1L)
                .timestamp("2026-11-24T19:15:00Z")
                .traceId("TRACE-12345678")
                .build();

        RecordMetadata metadata = new RecordMetadata(new TopicPartition("workorders.events", 0), 0L, 0, 0L, 0, 0);
        SendResult<String, WorkOrderKafkaEvent> sendResult = new SendResult<>(null, metadata);
        when(kafkaTemplate.send("workorders.events", "42", evento))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        producer.publicarEventoOrden(evento);

        verify(kafkaTemplate).send("workorders.events", "42", evento);
    }

    @Test
    @DisplayName("Serializa JSON compatible con WorkOrderEventDTO de ms-report")
    void serializaJsonCompatibleConMsReport() {
        WorkOrderKafkaEvent evento = WorkOrderKafkaEvent.builder()
                .eventId("evt-100")
                .eventType("WORK_ORDER_CREATED")
                .orderId(100L)
                .previousStatus(null)
                .newStatus("CREADA")
                .assignedTechnicianId(null)
                .clientId(5L)
                .timestamp("2026-11-24T19:15:00Z")
                .traceId("TRACE-abcd1234")
                .build();

        try (JacksonJsonSerializer<WorkOrderKafkaEvent> serializer = new JacksonJsonSerializer<>()) {
            serializer.configure(Map.of(JacksonJsonSerializer.ADD_TYPE_INFO_HEADERS, false), false);
            byte[] bytes = serializer.serialize("workorders.events", evento);
            String json = new String(bytes, StandardCharsets.UTF_8);

            assertThat(json)
                    .contains("\"eventId\":\"evt-100\"")
                    .contains("\"eventType\":\"WORK_ORDER_CREATED\"")
                    .contains("\"orderId\":100")
                    .contains("\"newStatus\":\"CREADA\"")
                    .contains("\"clientId\":5")
                    .contains("\"timestamp\":\"2026-11-24T19:15:00Z\"")
                    .contains("\"traceId\":\"TRACE-abcd1234\"");
        }
    }

    @Test
    @DisplayName("Si Kafka falla se envuelve en PublicacionEventoException")
    void fallaKafkaLanzaPublicacionEventoException() {
        WorkOrderKafkaEvent evento = WorkOrderKafkaEvent.builder()
                .eventId("evt-fail")
                .orderId(99L)
                .newStatus("CREADA")
                .build();

        CompletableFuture<SendResult<String, WorkOrderKafkaEvent>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("Broker unavailable"));
        when(kafkaTemplate.send("workorders.events", "99", evento)).thenReturn(failed);

        assertThrows(PublicacionEventoException.class, () -> producer.publicarEventoOrden(evento));
    }
}
