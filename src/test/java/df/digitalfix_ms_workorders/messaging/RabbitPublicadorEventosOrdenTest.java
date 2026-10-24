package df.digitalfix_ms_workorders.messaging;

import df.digitalfix_ms_workorders.config.RabbitProducerConfigTest;
import df.digitalfix_ms_workorders.dto.event.DispatchTicketEvent;
import df.digitalfix_ms_workorders.dto.event.EventEnvelopeDTO;
import df.digitalfix_ms_workorders.dto.event.ReportRequestEvent;
import df.digitalfix_ms_workorders.dto.event.WorkOrderNotificationEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class RabbitPublicadorEventosOrdenTest {

    private RabbitTemplate rabbitTemplate;
    private RabbitPublicadorEventosOrden publicador;

    private final ArgumentCaptor<Object> sobre = ArgumentCaptor.forClass(Object.class);
    private final ArgumentCaptor<MessagePostProcessor> postProcesador = ArgumentCaptor.forClass(MessagePostProcessor.class);
    private final ArgumentCaptor<CorrelationData> correlacion = ArgumentCaptor.forClass(CorrelationData.class);

    @BeforeEach
    void setUp() {
        rabbitTemplate = mock(RabbitTemplate.class);
        publicador = new RabbitPublicadorEventosOrden(rabbitTemplate, RabbitProducerConfigTest.PROPIEDADES);
    }

    @SuppressWarnings("unchecked")
    private <T> EventEnvelopeDTO<T> verificarPublicado(String exchange, String routingKey) {
        verify(rabbitTemplate).convertAndSend(eq(exchange), eq(routingKey), sobre.capture(),
                postProcesador.capture(), correlacion.capture());
        return (EventEnvelopeDTO<T>) sobre.getValue();
    }

    private static void assertSobreComun(EventEnvelopeDTO<?> envelope, String tipo, String correlationId) {
        assertNotNull(envelope.getEventId());
        assertEquals(tipo, envelope.getEventType());
        assertNotNull(envelope.getTimestamp());
        assertTrue(envelope.getTraceId().startsWith("TRACE-"));
        assertEquals(correlationId, envelope.getCorrelationId());
    }

    @Test
    @DisplayName("Notificación al cliente: por cmd.topic con email.<estado> (calza con email.*)")
    void publicaNotificacionCliente() {
        WorkOrderNotificationEvent evento = WorkOrderNotificationEvent.builder()
                .workOrderId(55L)
                .clientEmail("cliente@digitalfix.com")
                .clientName("Carlos Soto")
                .orderStatus("ASIGNADA")
                .totalAmount(new BigDecimal("350000.00"))
                .build();

        publicador.publicarNotificacionCliente(evento);

        EventEnvelopeDTO<WorkOrderNotificationEvent> envelope = verificarPublicado("cmd.topic", "email.asignada");
        assertSobreComun(envelope, RabbitPublicadorEventosOrden.TIPO_CAMBIO_ESTADO, "OT-55");
        assertEquals(evento, envelope.getPayload());
    }

    @Test
    @DisplayName("Ticket de despacho: sobre común por cmd.direct con dispatch.ticket")
    void publicaTicketDespacho() {
        DispatchTicketEvent evento = DispatchTicketEvent.builder()
                .workOrderId(77L)
                .technicianId("15")
                .crewId("3")
                .serviceAddress("Calle Los Alerces 456")
                .clientPhone("+56987654321")
                .scheduledDate(LocalDateTime.now().plusDays(1))
                .serviceDescription("Revisión de líneas de media tensión")
                .build();

        publicador.publicarTicketDespacho(evento);

        EventEnvelopeDTO<DispatchTicketEvent> envelope = verificarPublicado("cmd.direct", "dispatch.ticket");
        assertSobreComun(envelope, RabbitPublicadorEventosOrden.TIPO_DESPACHO, "OT-77");
        assertEquals(evento, envelope.getPayload());
    }

    @Test
    @DisplayName("Informe PDF: sobre común por cmd.direct con report.gen")
    void publicaSolicitudInforme() {
        ReportRequestEvent evento = ReportRequestEvent.builder()
                .workOrderId(88L)
                .reportType("INFORME_CIERRE")
                .recipientEmail("cliente@digitalfix.com")
                .requestedBy("ms-workorders")
                .build();

        publicador.publicarSolicitudInforme(evento);

        EventEnvelopeDTO<ReportRequestEvent> envelope = verificarPublicado("cmd.direct", "report.gen");
        assertSobreComun(envelope, RabbitPublicadorEventosOrden.TIPO_INFORME, "OT-88");
        assertEquals(evento, envelope.getPayload());
    }

    @Test
    @DisplayName("El eventId viaja como messageId y como correlación del publisher confirm")
    void eventIdTrazableEnElBroker() {
        publicador.publicarSolicitudInforme(ReportRequestEvent.builder().workOrderId(9L).build());

        EventEnvelopeDTO<ReportRequestEvent> envelope = verificarPublicado("cmd.direct", "report.gen");
        Message mensaje = postProcesador.getValue().postProcessMessage(new Message(new byte[0], new MessageProperties()));
        assertEquals(envelope.getEventId(), mensaje.getMessageProperties().getMessageId());
        assertEquals("OT-9", mensaje.getMessageProperties().getCorrelationId());
        assertEquals(envelope.getEventId(), correlacion.getValue().getId());
    }

    @Test
    @DisplayName("Cada estado de la orden genera su routing key en el topic")
    void routingKeyPorEstado() {
        for (String estado : new String[]{"CREADA", "EN_DESPLAZAMIENTO", "CERRADA"}) {
            publicador.publicarNotificacionCliente(WorkOrderNotificationEvent.builder()
                    .workOrderId(1L).orderStatus(estado).build());
        }

        verify(rabbitTemplate).convertAndSend(eq("cmd.topic"), eq("email.creada"), any(Object.class), any(MessagePostProcessor.class), any(CorrelationData.class));
        verify(rabbitTemplate).convertAndSend(eq("cmd.topic"), eq("email.en_desplazamiento"), any(Object.class), any(MessagePostProcessor.class), any(CorrelationData.class));
        verify(rabbitTemplate).convertAndSend(eq("cmd.topic"), eq("email.cerrada"), any(Object.class), any(MessagePostProcessor.class), any(CorrelationData.class));
    }

    @Test
    @DisplayName("Sin estado no se puede armar la routing key: falla antes de publicar")
    void notificacionSinEstadoFalla() {
        assertThrows(NullPointerException.class, () -> publicador.publicarNotificacionCliente(
                WorkOrderNotificationEvent.builder().workOrderId(1L).build()));
        verifyNoInteractions(rabbitTemplate);
    }
}
