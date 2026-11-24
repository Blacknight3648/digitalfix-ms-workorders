package df.digitalfix_ms_workorders.messaging;

import df.digitalfix_ms_workorders.dto.event.DispatchTicketEvent;
import df.digitalfix_ms_workorders.dto.event.ReportRequestEvent;
import df.digitalfix_ms_workorders.dto.event.WorkOrderNotificationEvent;
import df.digitalfix_ms_workorders.enums.EstadoOrden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Regla de negocio de las comunicaciones de una OT: qué se avisa y a quién en cada cambio.
 * <ul>
 *   <li>OT creada o cambio de estado → correo al cliente (flujo email).</li>
 *   <li>Cuadrilla asignada → correo + ticket de despacho (flujo dispatch).</li>
 *   <li>OT cerrada → informe de cierre en PDF (flujo report).</li>
 * </ul>
 * Cómo viajan los mensajes no es asunto de este servicio: {@link PublicadorEventosOrden} los
 * entrega a ms-admin-queues, dueño de la mensajería.
 * <p>
 * Corre después del commit: si la mensajería no responde, la orden ya quedó guardada y solo se
 * registra el error. Cada aviso se publica por separado, así una falla en el correo no impide el
 * ticket de despacho ni el informe.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrdenTrabajoEventosListener {

    private static final String SIN_DIRECCION = "Sin dirección registrada";

    private final PublicadorEventosOrden publicador;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alConfirmar(OrdenTrabajoEvento evento) {
        publicarSinInterrumpir(evento, "correo al cliente", () -> publicador.publicarNotificacionCliente(correo(evento)));
        if (evento.tipo() == OrdenTrabajoEvento.Tipo.CUADRILLA_ASIGNADA) {
            publicarSinInterrumpir(evento, "ticket de despacho", () -> publicador.publicarTicketDespacho(despacho(evento)));
        }
        if (evento.estado() == EstadoOrden.CERRADA) {
            publicarSinInterrumpir(evento, "informe de cierre", () -> publicador.publicarSolicitudInforme(informeCierre(evento)));
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
