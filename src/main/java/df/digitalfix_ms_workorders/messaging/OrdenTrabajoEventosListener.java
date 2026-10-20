package df.digitalfix_ms_workorders.messaging;

import com.digitalfix.workorders.dto.event.DispatchTicketEvent;
import com.digitalfix.workorders.dto.event.ReportRequestEvent;
import com.digitalfix.workorders.dto.event.WorkOrderNotificationEvent;
import com.digitalfix.workorders.service.WorkOrderService;
import df.digitalfix_ms_workorders.enums.EstadoOrden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Traduce los eventos de dominio de la OT a mensajes de RabbitMQ para ms-notify:
 * <ul>
 *   <li>OT creada o cambio de estado → correo al cliente (email.send → q.cmd.email).</li>
 *   <li>Cuadrilla asignada → correo + ticket de despacho (dispatch.ticket → q.cmd.dispatch).</li>
 *   <li>OT cerrada → informe de cierre en PDF (report.gen → q.cmd.report).</li>
 * </ul>
 * Corre después del commit: si el broker no responde, la orden ya quedó guardada y solo se
 * registra el error (el RabbitTemplate ya reintentó según spring.rabbitmq.template.retry).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrdenTrabajoEventosListener {

    private static final String SIN_DIRECCION = "Sin dirección registrada";

    private final WorkOrderService publicador;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alConfirmar(OrdenTrabajoEvento evento) {
        try {
            publicador.notifyOrderStatusChange(correo(evento));
            if (evento.tipo() == OrdenTrabajoEvento.Tipo.CUADRILLA_ASIGNADA) {
                publicador.dispatchCrewTicket(despacho(evento));
            }
            if (evento.estado() == EstadoOrden.CERRADA) {
                publicador.requestWorkOrderReport(informeCierre(evento));
            }
        } catch (AmqpException e) {
            log.error("No se pudo publicar el evento {} de la OT #{} en RabbitMQ: {}",
                    evento.tipo(), evento.idOrden(), e.getMessage());
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
