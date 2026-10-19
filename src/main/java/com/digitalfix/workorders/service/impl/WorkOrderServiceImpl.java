package com.digitalfix.workorders.service.impl;

import com.digitalfix.workorders.dto.event.DispatchTicketEvent;
import com.digitalfix.workorders.dto.event.EventEnvelopeDTO;
import com.digitalfix.workorders.dto.event.ReportRequestEvent;
import com.digitalfix.workorders.dto.event.WorkOrderNotificationEvent;
import com.digitalfix.workorders.service.WorkOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkOrderServiceImpl implements WorkOrderService {

    private final RabbitTemplate rabbitTemplate;

    // Lectura de Exchanges desde application.yaml
    @Value("${digitalfix.rabbitmq.exchanges.direct:cmd.direct}")
    private String directExchange;

    // Lectura de Routing Keys desde application.yaml
    @Value("${digitalfix.rabbitmq.routing-keys.email:email.send}")
    private String emailRoutingKey;

    @Value("${digitalfix.rabbitmq.routing-keys.dispatch:dispatch.ticket}")
    private String dispatchRoutingKey;

    @Value("${digitalfix.rabbitmq.routing-keys.report:report.gen}")
    private String reportRoutingKey;

    @Override
    public void notifyOrderStatusChange(WorkOrderNotificationEvent notificationEvent) {
        log.info("Procesando evento de notificación para la orden #{}", notificationEvent.getWorkOrderId());

        EventEnvelopeDTO<WorkOrderNotificationEvent> envelope = buildEnvelope(
                "WORK_ORDER_STATUS_CHANGED", 
                notificationEvent
        );

        rabbitTemplate.convertAndSend(directExchange, emailRoutingKey, envelope);

        log.info("Evento [WORK_ORDER_STATUS_CHANGED] publicado exitosamente. eventId: {}, routingKey: {}", 
                envelope.getEventId(), emailRoutingKey);
    }

    @Override
    public void dispatchCrewTicket(DispatchTicketEvent dispatchEvent) {
        log.info("Procesando asignación de cuadrilla/técnico {} para la orden #{}", 
                dispatchEvent.getTechnicianId(), dispatchEvent.getWorkOrderId());

        EventEnvelopeDTO<DispatchTicketEvent> envelope = buildEnvelope(
                "CREW_DISPATCH_ASSIGNED", 
                dispatchEvent
        );

        rabbitTemplate.convertAndSend(directExchange, dispatchRoutingKey, envelope);

        log.info("Evento [CREW_DISPATCH_ASSIGNED] publicado exitosamente. eventId: {}, routingKey: {}", 
                envelope.getEventId(), dispatchRoutingKey);
    }

    @Override
    public void requestWorkOrderReport(ReportRequestEvent reportEvent) {
        log.info("Solicitando generación de reporte PDF tipo '{}' para la orden #{}", 
                reportEvent.getReportType(), reportEvent.getWorkOrderId());

        EventEnvelopeDTO<ReportRequestEvent> envelope = buildEnvelope(
                "WORK_ORDER_REPORT_REQUESTED", 
                reportEvent
        );

        rabbitTemplate.convertAndSend(directExchange, reportRoutingKey, envelope);

        log.info("Evento [WORK_ORDER_REPORT_REQUESTED] publicado exitosamente. eventId: {}, routingKey: {}", 
                envelope.getEventId(), reportRoutingKey);
    }

    /**
     * Método helper privado para encapsular la carga útil dentro del EventEnvelopeDTO estándar.
     */
    private <T> EventEnvelopeDTO<T> buildEnvelope(String eventType, T payload) {
        return EventEnvelopeDTO.<T>builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .timestamp(LocalDateTime.now())
                .traceId("TRACE-" + UUID.randomUUID().toString().substring(0, 8))
                .payload(payload)
                .build();
    }
}
