package df.digitalfix_ms_workorders.messaging;

import df.digitalfix_ms_workorders.dto.event.DispatchTicketEvent;
import df.digitalfix_ms_workorders.dto.event.ReportRequestEvent;
import df.digitalfix_ms_workorders.dto.event.WorkOrderKafkaEvent;
import df.digitalfix_ms_workorders.dto.event.WorkOrderNotificationEvent;
import df.digitalfix_ms_workorders.enums.EstadoOrden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Regla de negocio de las comunicaciones de una OT: qué se avisa y a quién en cada cambio.
 * <ul>
 *   <li>OT creada o cambio de estado → correo al cliente (flujo email) + evento en Kafka {@code workorders.events}.</li>
 *   <li>Cuadrilla asignada → correo + ticket de despacho (flujo dispatch) + evento en Kafka {@code workorders.events}.</li>
 *   <li>OT cerrada → informe de cierre en PDF (flujo report) + evento en Kafka {@code workorders.events}.</li>
 * </ul>
 * <p>
 * Corre después del commit: si la mensajería o Kafka no responden, la orden ya quedó guardada y solo
 * se registra el error. Cada aviso se publica por separado, así una falla en un canal no impide los demás.
 */
@Slf4j
@Component
public class OrdenTrabajoEventosListener {

    private static final String SIN_DIRECCION = "Sin dirección registrada";

    private final PublicadorEventosOrden publicador;
    private final PublicadorEventosKafka publicadorKafka;

    public OrdenTrabajoEventosListener(PublicadorEventosOrden publicador) {
        this(publicador, null);
    }

    @Autowired
    public OrdenTrabajoEventosListener(
            PublicadorEventosOrden publicador,
            @Autowired(required = false) PublicadorEventosKafka publicadorKafka) {
        this.publicador = publicador;
        this.publicadorKafka = publicadorKafka;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alConfirmar(OrdenTrabajoEvento evento) {
        publicarSinInterrumpir(evento, "correo al cliente", () -> publicador.publicarNotificacionCliente(correo(evento)));
        if (evento.tipo() == OrdenTrabajoEvento.Tipo.CUADRILLA_ASIGNADA) {
            publicarSinInterrumpir(evento, "ticket de despacho", () -> publicador.publicarTicketDespacho(despacho(evento)));
        }
        if (evento.estado() == EstadoOrden.CERRADA) {
            publicarSinInterrumpir(evento, "informe de cierre", () -> publicador.publicarSolicitudInforme(informeCierre(evento)));
        }
        if (publicadorKafka != null) {
            publicarSinInterrumpir(evento, "evento Kafka workorders.events", () -> publicadorKafka.publicarEventoOrden(eventoKafka(evento)));
        }
    }

    private void publicarSinInterrumpir(OrdenTrabajoEvento evento, String aviso, Runnable publicacion) {
        try {
            publicacion.run();
        } catch (PublicacionEventoException e) {
            log.error("No se pudo publicar el {} del evento {} de la OT #{}: {}",
                    aviso, evento.tipo(), evento.idOrden(), e.getMessage());
        }
    }

    private WorkOrderKafkaEvent eventoKafka(OrdenTrabajoEvento evento) {
        String eventType = switch (evento.tipo()) {
            case CREADA -> "WORK_ORDER_CREATED";
            case CUADRILLA_ASIGNADA -> "CREW_DISPATCH_ASSIGNED";
            case ESTADO_CAMBIADO -> "WORK_ORDER_STATUS_CHANGED";
        };
        return WorkOrderKafkaEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .orderId(evento.idOrden())
                .previousStatus(evento.estadoAnterior() != null ? evento.estadoAnterior().name() : null)
                .newStatus(evento.estado().name())
                .assignedTechnicianId(evento.idTecnicoLider())
                .clientId(evento.idCliente())
                .timestamp(Instant.now().toString())
                .traceId("TRACE-" + UUID.randomUUID().toString().substring(0, 8))
                .build();
    }

    private WorkOrderNotificationEvent correo(OrdenTrabajoEvento evento) {
        Map<String, Object> atributos = new HashMap<>();
        atributos.put("status", evento.estado().name());
        atributos.put("serviceAddress", evento.direccion() != null ? evento.direccion() : SIN_DIRECCION);
        if (evento.nombreCuadrilla() != null) {
            atributos.put("technicianName", "Cuadrilla " + evento.nombreCuadrilla());
        }
        return WorkOrderNotificationEvent.builder()
                .workOrderId(evento.idOrden())
                .clientEmail(evento.clienteEmail())
                .clientName(evento.clienteNombre())
                .orderStatus(evento.estado().name())
                .description(evento.observaciones())
                .totalAmount(evento.total())
                .subject(asunto(evento))
                .templateAttributes(atributos)
                .build();
    }

    private String asunto(OrdenTrabajoEvento evento) {
        return switch (evento.tipo()) {
            case CREADA -> "DigitalFix: registramos tu orden de trabajo #" + evento.idOrden();
            case CUADRILLA_ASIGNADA -> "DigitalFix: tu orden #" + evento.idOrden() + " ya tiene cuadrilla asignada";
            case ESTADO_CAMBIADO -> "DigitalFix: tu orden #" + evento.idOrden() + " cambió a " + evento.estado();
        };
    }

    private DispatchTicketEvent despacho(OrdenTrabajoEvento evento) {
        return DispatchTicketEvent.builder()
                .workOrderId(evento.idOrden())
                .technicianId(String.valueOf(evento.idTecnicoLider()))
                .crewId(String.valueOf(evento.idCuadrilla()))
                .serviceAddress(evento.direccion() != null ? evento.direccion() : SIN_DIRECCION)
                .clientPhone(evento.clienteTelefono())
                .scheduledDate(LocalDateTime.now())
                .serviceDescription(evento.observaciones() != null ? evento.observaciones() : "Orden de trabajo #" + evento.idOrden())
                .build();
    }

    private ReportRequestEvent informeCierre(OrdenTrabajoEvento evento) {
        return ReportRequestEvent.builder()
                .workOrderId(evento.idOrden())
                .reportType("INFORME_CIERRE")
                .recipientEmail(evento.clienteEmail())
                .requestedBy("ms-workorders")
                .build();
    }
}
