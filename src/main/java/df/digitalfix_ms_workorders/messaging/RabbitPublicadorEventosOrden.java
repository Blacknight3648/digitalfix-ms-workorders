package df.digitalfix_ms_workorders.messaging;

import df.digitalfix_ms_workorders.dto.event.DispatchTicketEvent;
import df.digitalfix_ms_workorders.dto.event.EventEnvelopeDTO;
import df.digitalfix_ms_workorders.dto.event.ReportRequestEvent;
import df.digitalfix_ms_workorders.dto.event.WorkOrderNotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Adaptador RabbitMQ de {@link PublicadorEventosOrden} (activo cuando
 * {@code digitalfix.messaging.transport=rabbitmq}). Publica sobres {@link EventEnvelopeDTO}
 * hacia el exchange {@code cmd.direct} sin declarar topología (propiedad de ms-admin-queues).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "digitalfix.messaging.transport", havingValue = "rabbitmq")
public class RabbitPublicadorEventosOrden implements PublicadorEventosOrden {

    static final String EXCHANGE_DIRECT = "cmd.direct";
    static final String ROUTING_EMAIL = "email.send";
    static final String ROUTING_DISPATCH = "dispatch.ticket";
    static final String ROUTING_REPORT = "report.gen";

    static final String TIPO_CAMBIO_ESTADO = "WORK_ORDER_STATUS_CHANGED";
    static final String TIPO_DESPACHO = "CREW_DISPATCH_ASSIGNED";
    static final String TIPO_INFORME = "WORK_ORDER_REPORT_REQUESTED";

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publicarNotificacionCliente(WorkOrderNotificationEvent evento) {
        enviar(ROUTING_EMAIL, TIPO_CAMBIO_ESTADO, evento.getWorkOrderId(), evento);
    }

    @Override
    public void publicarTicketDespacho(DispatchTicketEvent evento) {
        enviar(ROUTING_DISPATCH, TIPO_DESPACHO, evento.getWorkOrderId(), evento);
    }

    @Override
    public void publicarSolicitudInforme(ReportRequestEvent evento) {
        enviar(ROUTING_REPORT, TIPO_INFORME, evento.getWorkOrderId(), evento);
    }

    private <T> void enviar(String routingKey, String eventType, Long idOrden, T payload) {
        String correlationId = "OT-" + idOrden;
        EventEnvelopeDTO<T> envelope = EventEnvelopeDTO.<T>builder()
                .eventType(eventType)
                .traceId("TRACE-" + UUID.randomUUID().toString().substring(0, 8))
                .correlationId(correlationId)
                .payload(payload)
                .build();
        try {
            rabbitTemplate.convertAndSend(
                    EXCHANGE_DIRECT,
                    routingKey,
                    envelope,
                    message -> {
                        message.getMessageProperties().setHeader("x-correlation-id", correlationId);
                        return message;
                    },
                    new CorrelationData(envelope.getEventId()));
            log.info("Evento {} ({}) enviado a RabbitMQ {} -> {}", eventType, correlationId, EXCHANGE_DIRECT, routingKey);
        } catch (AmqpException e) {
            throw new PublicacionEventoException(
                    "No se pudo publicar el evento " + eventType + " de " + correlationId + " en RabbitMQ: " + e.getMessage(), e);
        }
    }
}
