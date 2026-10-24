package df.digitalfix_ms_workorders.dto.event;

import df.digitalfix_ms_workorders.config.RabbitProducerConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConverter;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EventEnvelopeDTOTest {

    private final MessageConverter messageConverter = new RabbitProducerConfig().jsonMessageConverter();

    @Test
    @DisplayName("EventEnvelopeDTO debe generar eventId y timestamp por defecto")
    void testDefaultValues() {
        EventEnvelopeDTO<String> envelope = EventEnvelopeDTO.<String>builder()
                .eventType("test.event")
                .traceId("trace-123")
                .payload("test-payload")
                .build();

        assertNotNull(envelope.getEventId(), "eventId no debe ser nulo");
        assertFalse(envelope.getEventId().isBlank(), "eventId no debe estar vacío");
        assertNotNull(envelope.getTimestamp(), "timestamp no debe ser nulo");
        assertEquals("test.event", envelope.getEventType());
        assertEquals("trace-123", envelope.getTraceId());
        assertEquals("test-payload", envelope.getPayload());
    }

    @Test
    @DisplayName("Serialización JSON de WorkOrderNotificationEvent dentro de EventEnvelopeDTO")
    void testWorkOrderNotificationEventEnvelopeSerialization() {
        WorkOrderNotificationEvent notification = WorkOrderNotificationEvent.builder()
                .workOrderId(101L)
                .clientEmail("cliente@digitalfix.com")
                .clientName("Juan Pérez")
                .orderStatus("ASIGNADA")
                .description("Reparación tablero eléctrico")
                .totalAmount(new BigDecimal("125000.50"))
                .build();

        LocalDateTime fixedTime = LocalDateTime.of(2026, 10, 18, 20, 0, 0);

        EventEnvelopeDTO<WorkOrderNotificationEvent> envelope = EventEnvelopeDTO.<WorkOrderNotificationEvent>builder()
                .eventId("evt-test-01")
                .eventType("email.send")
                .timestamp(fixedTime)
                .traceId("trace-notif-01")
                .payload(notification)
                .build();

        Message message = messageConverter.toMessage(envelope, new MessageProperties());
        assertNotNull(message);

        String json = new String(message.getBody(), StandardCharsets.UTF_8);

        assertTrue(json.contains("\"eventId\":\"evt-test-01\""));
        assertTrue(json.contains("\"eventType\":\"email.send\""));
        assertTrue(json.contains("\"timestamp\":\"2026-10-18T20:00:00\""));
        assertTrue(json.contains("\"traceId\":\"trace-notif-01\""));
        assertTrue(json.contains("\"workOrderId\":101"));
        // ms-notify (EmailNotificationDTO) lo recibe como "emailTo"
        assertTrue(json.contains("\"emailTo\":\"cliente@digitalfix.com\""));
        assertTrue(json.contains("\"clientName\":\"Juan Pérez\""));
        assertTrue(json.contains("\"orderStatus\":\"ASIGNADA\""));
        assertTrue(json.contains("\"totalAmount\":125000.50"));
    }

    @Test
    @DisplayName("Serialización JSON de DispatchTicketEvent dentro de EventEnvelopeDTO")
    void testDispatchTicketEventEnvelopeSerialization() {
        LocalDateTime scheduledDate = LocalDateTime.of(2026, 10, 19, 9, 30, 0);

        DispatchTicketEvent dispatch = DispatchTicketEvent.builder()
                .workOrderId(202L)
                .technicianId("TEC-07")
                .crewId("CREW-Alpha")
                .serviceAddress("Av. Providencia 1234, Santiago")
                .clientPhone("+56912345678")
                .scheduledDate(scheduledDate)
                .serviceDescription("Mantenimiento transformador")
                .build();

        EventEnvelopeDTO<DispatchTicketEvent> envelope = EventEnvelopeDTO.<DispatchTicketEvent>builder()
                .eventId("evt-disp-01")
                .eventType("dispatch.ticket")
                .timestamp(scheduledDate)
                .traceId("trace-disp-01")
                .payload(dispatch)
                .build();

        Message message = messageConverter.toMessage(envelope, new MessageProperties());
        String json = new String(message.getBody(), StandardCharsets.UTF_8);

        assertTrue(json.contains("\"workOrderId\":202"));
        assertTrue(json.contains("\"technicianId\":\"TEC-07\""));
        assertTrue(json.contains("\"crewId\":\"CREW-Alpha\""));
        assertTrue(json.contains("\"scheduledDate\":\"2026-10-19T09:30:00\""));
        assertTrue(json.contains("\"clientPhone\":\"+56912345678\""));
    }

    @Test
    @DisplayName("Serialización JSON de ReportRequestEvent dentro de EventEnvelopeDTO")
    void testReportRequestEventEnvelopeSerialization() {
        ReportRequestEvent report = ReportRequestEvent.builder()
                .workOrderId(303L)
                .reportType("CERTIFICADO_TECNICO")
                .recipientEmail("auditoria@digitalfix.com")
                .requestedBy("supervisor1")
                .build();

        EventEnvelopeDTO<ReportRequestEvent> envelope = EventEnvelopeDTO.<ReportRequestEvent>builder()
                .eventId("evt-rep-01")
                .eventType("report.gen")
                .traceId("trace-rep-01")
                .payload(report)
                .build();

        Message message = messageConverter.toMessage(envelope, new MessageProperties());
        String json = new String(message.getBody(), StandardCharsets.UTF_8);

        assertTrue(json.contains("\"workOrderId\":303"));
        assertTrue(json.contains("\"reportType\":\"CERTIFICADO_TECNICO\""));
        assertTrue(json.contains("\"recipientEmail\":\"auditoria@digitalfix.com\""));
        assertTrue(json.contains("\"requestedBy\":\"supervisor1\""));
    }

    @Test
    @DisplayName("Compatibilidad de com.digitalfix.workorders.dto.event")
    void testComPackageCompatibility() {
        com.digitalfix.workorders.dto.event.WorkOrderNotificationEvent notif =
                com.digitalfix.workorders.dto.event.WorkOrderNotificationEvent.builder()
                        .workOrderId(404L)
                        .clientEmail("test@com.digitalfix")
                        .build();

        com.digitalfix.workorders.dto.event.DispatchTicketEvent disp =
                com.digitalfix.workorders.dto.event.DispatchTicketEvent.builder()
                        .workOrderId(404L)
                        .technicianId("T-1")
                        .build();

        com.digitalfix.workorders.dto.event.ReportRequestEvent rep =
                com.digitalfix.workorders.dto.event.ReportRequestEvent.builder()
                        .workOrderId(404L)
                        .reportType("INFORME_CIERRE")
                        .build();

        com.digitalfix.workorders.dto.event.EventEnvelopeDTO<com.digitalfix.workorders.dto.event.WorkOrderNotificationEvent> envelope =
                com.digitalfix.workorders.dto.event.EventEnvelopeDTO.<com.digitalfix.workorders.dto.event.WorkOrderNotificationEvent>builder()
                        .eventType("email.send")
                        .payload(notif)
                        .build();

        assertNotNull(disp);
        assertNotNull(rep);
        assertNotNull(envelope.getEventId());
        assertEquals(404L, envelope.getPayload().getWorkOrderId());
    }
}
