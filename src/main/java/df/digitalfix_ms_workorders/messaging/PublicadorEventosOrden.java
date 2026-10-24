package df.digitalfix_ms_workorders.messaging;

import df.digitalfix_ms_workorders.dto.event.DispatchTicketEvent;
import df.digitalfix_ms_workorders.dto.event.ReportRequestEvent;
import df.digitalfix_ms_workorders.dto.event.WorkOrderNotificationEvent;

/**
 * Puerto de salida de la mensajería: lo que una orden de trabajo necesita comunicar a ms-notify.
 * Quien lo usa ({@link OrdenTrabajoEventosListener}) no conoce exchanges, routing keys ni
 * RabbitTemplate; eso queda en {@link RabbitPublicadorEventosOrden}.
 */
public interface PublicadorEventosOrden {

    /** Correo al cliente: orden creada o cambio de estado (flujo email → q.cmd.email). */
    void publicarNotificacionCliente(WorkOrderNotificationEvent evento);

    /** Ticket de despacho para la cuadrilla asignada (flujo dispatch → q.cmd.dispatch). */
    void publicarTicketDespacho(DispatchTicketEvent evento);

    /** Informe o certificado PDF de la orden (flujo report → q.cmd.report). */
    void publicarSolicitudInforme(ReportRequestEvent evento);
}
