package com.digitalfix.workorders.controller;

import com.digitalfix.workorders.dto.event.DispatchTicketEvent;
import com.digitalfix.workorders.dto.event.ReportRequestEvent;
import com.digitalfix.workorders.dto.event.WorkOrderNotificationEvent;
import com.digitalfix.workorders.service.WorkOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/work-orders")
@RequiredArgsConstructor
@Tag(name = "Work Orders Events", description = "Emisión de eventos asíncronos vía RabbitMQ para órdenes, despachos y reportes")
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    @PostMapping("/notify")
    @Operation(summary = "Emitir evento de notificación de orden", description = "Publica evento WORK_ORDER_STATUS_CHANGED en RabbitMQ para ms-notify")
    public ResponseEntity<Map<String, String>> triggerNotification(@RequestBody WorkOrderNotificationEvent event) {
        workOrderService.notifyOrderStatusChange(event);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Evento de notificación enviado a RabbitMQ correctamente.");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/dispatch")
    @Operation(summary = "Emitir evento de despacho de cuadrilla", description = "Publica evento CREW_DISPATCH_ASSIGNED en RabbitMQ para asignación en terreno")
    public ResponseEntity<Map<String, String>> triggerDispatch(@RequestBody DispatchTicketEvent event) {
        workOrderService.dispatchCrewTicket(event);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Evento de asignación de cuadrilla enviado a RabbitMQ correctamente.");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/report")
    @Operation(summary = "Emitir solicitud de reporte PDF", description = "Publica evento WORK_ORDER_REPORT_REQUESTED en RabbitMQ para ms-report")
    public ResponseEntity<Map<String, String>> triggerReport(@RequestBody ReportRequestEvent event) {
        workOrderService.requestWorkOrderReport(event);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Solicitud de reporte PDF enviada a RabbitMQ correctamente.");
        return ResponseEntity.ok(response);
    }
}
