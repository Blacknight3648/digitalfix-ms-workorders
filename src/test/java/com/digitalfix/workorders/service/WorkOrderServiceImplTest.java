package com.digitalfix.workorders.service;

import com.digitalfix.workorders.dto.event.DispatchTicketEvent;
import com.digitalfix.workorders.dto.event.EventEnvelopeDTO;
import com.digitalfix.workorders.dto.event.ReportRequestEvent;
import com.digitalfix.workorders.dto.event.WorkOrderNotificationEvent;
import com.digitalfix.workorders.service.impl.WorkOrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WorkOrderServiceImplTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private WorkOrderServiceImpl workOrderService;

    @Captor
    private ArgumentCaptor<EventEnvelopeDTO<WorkOrderNotificationEvent>> notificationCaptor;

    @Captor
    private ArgumentCaptor<EventEnvelopeDTO<DispatchTicketEvent>> dispatchCaptor;

    @Captor
    private ArgumentCaptor<EventEnvelopeDTO<ReportRequestEvent>> reportCaptor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(workOrderService, "directExchange", "cmd.direct");
        ReflectionTestUtils.setField(workOrderService, "emailRoutingKey", "email.send");
        ReflectionTestUtils.setField(workOrderService, "dispatchRoutingKey", "dispatch.ticket");
        ReflectionTestUtils.setField(workOrderService, "reportRoutingKey", "report.gen");
    }

    @Test
    @DisplayName("notifyOrderStatusChange debe encapsular en envelope y publicar en RabbitMQ")
    void testNotifyOrderStatusChange() {
        WorkOrderNotificationEvent event = WorkOrderNotificationEvent.builder()
                .workOrderId(55L)
                .clientEmail("cliente@digitalfix.com")
                .clientName("Carlos Soto")
                .orderStatus("EN_PROCESO")
                .description("Instalación de empalme trifásico")
                .totalAmount(new BigDecimal("350000.00"))
                .build();

        workOrderService.notifyOrderStatusChange(event);

        verify(rabbitTemplate).convertAndSend(eq("cmd.direct"), eq("email.send"), notificationCaptor.capture());

        EventEnvelopeDTO<WorkOrderNotificationEvent> captured = notificationCaptor.getValue();
        assertNotNull(captured.getEventId());
        assertEquals("WORK_ORDER_STATUS_CHANGED", captured.getEventType());
        assertNotNull(captured.getTimestamp());
        assertTrue(captured.getTraceId().startsWith("TRACE-"));
        assertEquals(event, captured.getPayload());
    }

    @Test
    @DisplayName("dispatchCrewTicket debe encapsular en envelope y publicar en RabbitMQ")
    void testDispatchCrewTicket() {
        DispatchTicketEvent event = DispatchTicketEvent.builder()
                .workOrderId(77L)
                .technicianId("TEC-15")
                .crewId("CREW-BETA")
                .serviceAddress("Calle Los Alerces 456")
                .clientPhone("+56987654321")
                .scheduledDate(LocalDateTime.now().plusDays(1))
                .serviceDescription("Revisión de líneas de media tensión")
                .build();

        workOrderService.dispatchCrewTicket(event);

        verify(rabbitTemplate).convertAndSend(eq("cmd.direct"), eq("dispatch.ticket"), dispatchCaptor.capture());

        EventEnvelopeDTO<DispatchTicketEvent> captured = dispatchCaptor.getValue();
        assertNotNull(captured.getEventId());
        assertEquals("CREW_DISPATCH_ASSIGNED", captured.getEventType());
        assertNotNull(captured.getTimestamp());
        assertTrue(captured.getTraceId().startsWith("TRACE-"));
        assertEquals(event, captured.getPayload());
    }

    @Test
    @DisplayName("requestWorkOrderReport debe encapsular en envelope y publicar en RabbitMQ")
    void testRequestWorkOrderReport() {
        ReportRequestEvent event = ReportRequestEvent.builder()
                .workOrderId(88L)
                .reportType("CERTIFICADO_TECNICO")
                .recipientEmail("admin@digitalfix.com")
                .requestedBy("supervisor2")
                .build();

        workOrderService.requestWorkOrderReport(event);

        verify(rabbitTemplate).convertAndSend(eq("cmd.direct"), eq("report.gen"), reportCaptor.capture());

        EventEnvelopeDTO<ReportRequestEvent> captured = reportCaptor.getValue();
        assertNotNull(captured.getEventId());
        assertEquals("WORK_ORDER_REPORT_REQUESTED", captured.getEventType());
        assertNotNull(captured.getTimestamp());
        assertTrue(captured.getTraceId().startsWith("TRACE-"));
        assertEquals(event, captured.getPayload());
    }
}
