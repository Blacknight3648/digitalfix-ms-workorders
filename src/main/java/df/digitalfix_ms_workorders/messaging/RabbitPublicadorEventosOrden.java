package df.digitalfix_ms_workorders.messaging;

import df.digitalfix_ms_workorders.config.ProductorRabbitProperties;
import df.digitalfix_ms_workorders.dto.event.DispatchTicketEvent;
import df.digitalfix_ms_workorders.dto.event.EventEnvelopeDTO;
import df.digitalfix_ms_workorders.dto.event.ReportRequestEvent;
import df.digitalfix_ms_workorders.dto.event.WorkOrderNotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Adaptador RabbitMQ de {@link PublicadorEventosOrden}. Cada evento viaja en el sobre común del
 * caso ({@code eventId}, {@code eventType}, {@code timestamp}, {@code traceId}, {@code correlationId})
 * por el exchange de su flujo: notificaciones al cliente por {@code cmd.topic}, despacho e informe
 * por {@code cmd.direct}. El eventId va también como messageId y como dato de correlación, para que
 * una devolución o un rechazo del broker se pueda rastrear en el log (ver {@code RabbitProducerConfig}).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitPublicadorEventosOrden implements PublicadorEventosOrden {

    static final String TIPO_CAMBIO_ESTADO = "WORK_ORDER_STATUS_CHANGED";
    static final String TIPO_DESPACHO = "CREW_DISPATCH_ASSIGNED";
    static final String TIPO_INFORME = "WORK_ORDER_REPORT_REQUESTED";

    private final RabbitTemplate rabbitTemplate;
    private final ProductorRabbitProperties propiedades;

    /**
     * Por {@code cmd.topic} con {@code email.<estado>} (email.creada, email.asignada, email.cerrada...):
     * el estado viaja en la clave, así otro consumidor podría suscribirse solo a algunos estados
     * (ej. email.cerrada) sin cambiar este productor. Hoy el binding email.* lleva todos a q.cmd.email.
     */
    @Override
    public void publicarNotificacionCliente(WorkOrderNotificationEvent evento) {
        String estado = Objects.requireNonNull(evento.getOrderStatus(), "La notificación necesita el estado de la orden");
        String routingKey = propiedades.routingKeys().notificacionCliente() + "." + estado.toLowerCase(Locale.ROOT);
        publicar(propiedades.exchanges().topic(), routingKey, TIPO_CAMBIO_ESTADO, evento.getWorkOrderId(), evento);
    }

    /** Comando punto a punto por {@code cmd.direct} con {@code dispatch.ticket}. */
    @Override
    public void publicarTicketDespacho(DispatchTicketEvent evento) {
        publicar(propiedades.exchanges().direct(), propiedades.routingKeys().dispatch(), TIPO_DESPACHO,
                evento.getWorkOrderId(), evento);
    }

    /** Comando punto a punto por {@code cmd.direct} con {@code report.gen}. */
    @Override
    public void publicarSolicitudInforme(ReportRequestEvent evento) {
        publicar(propiedades.exchanges().direct(), propiedades.routingKeys().report(), TIPO_INFORME,
                evento.getWorkOrderId(), evento);
    }

    private <T> void publicar(String exchange, String routingKey, String tipo, Long idOrden, T data) {
        EventEnvelopeDTO<T> sobre = EventEnvelopeDTO.<T>builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(tipo)
                .timestamp(LocalDateTime.now())
                .traceId("TRACE-" + UUID.randomUUID().toString().substring(0, 8))
                .correlationId("OT-" + idOrden)
                .payload(data)
                .build();

        rabbitTemplate.convertAndSend(exchange, routingKey, sobre, mensaje -> {
            mensaje.getMessageProperties().setMessageId(sobre.getEventId());
            mensaje.getMessageProperties().setCorrelationId(sobre.getCorrelationId());
            return mensaje;
        }, new CorrelationData(sobre.getEventId()));

        log.info("Evento {} de {} publicado en {} con routingKey {} (eventId {})",
                tipo, sobre.getCorrelationId(), exchange, routingKey, sobre.getEventId());
    }
}
