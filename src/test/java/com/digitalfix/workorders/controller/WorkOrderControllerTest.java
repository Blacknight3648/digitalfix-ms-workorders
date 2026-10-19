package com.digitalfix.workorders.controller;

import com.digitalfix.workorders.dto.event.DispatchTicketEvent;
import com.digitalfix.workorders.dto.event.ReportRequestEvent;
import com.digitalfix.workorders.dto.event.WorkOrderNotificationEvent;
import com.digitalfix.workorders.service.WorkOrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WorkOrderControllerTest {

    @Mock
    private WorkOrderService workOrderService;

    @InjectMocks
    private WorkOrderController controller;

    @Test
    @DisplayName("triggerNotification debe llamar al servicio y retornar 200 OK")
    void testTriggerNotification() {
        WorkOrderNotificationEvent event = WorkOrderNotificationEvent.builder()
                .workOrderId(10L)
                .clientEmail("cliente@correo.com")
                .clientName("Maria")
                .orderStatus("CREADA")
                .totalAmount(new BigDecimal("50000"))
                .build();

        ResponseEntity<Map<String, String>> response = controller.triggerNotification(event);

        verify(workOrderService).notifyOrderStatusChange(event);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("message"));
    }

    @Test
    @DisplayName("triggerDispatch debe llamar al servicio y retornar 200 OK")
    void testTriggerDispatch() {
        DispatchTicketEvent event = DispatchTicketEvent.builder()
                .workOrderId(20L)
                .technicianId("TEC-01")
                .crewId("CREW-1")
                .scheduledDate(LocalDateTime.now())
                .build();

        ResponseEntity<Map<String, String>> response = controller.triggerDispatch(event);

        verify(workOrderService).dispatchCrewTicket(event);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("message"));
    }

    @Test
    @DisplayName("triggerReport debe llamar al servicio y retornar 200 OK")
    void testTriggerReport() {
        ReportRequestEvent event = ReportRequestEvent.builder()
                .workOrderId(30L)
                .reportType("CERTIFICADO_TECNICO")
                .recipientEmail("admin@correo.com")
                .requestedBy("supervisor")
                .build();

        ResponseEntity<Map<String, String>> response = controller.triggerReport(event);

        verify(workOrderService).requestWorkOrderReport(event);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("message"));
    }
}
