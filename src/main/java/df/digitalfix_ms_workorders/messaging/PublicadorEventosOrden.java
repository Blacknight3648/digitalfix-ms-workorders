package df.digitalfix_ms_workorders.messaging;

import df.digitalfix_ms_workorders.dto.event.DispatchTicketEvent;
import df.digitalfix_ms_workorders.dto.event.ReportRequestEvent;
import df.digitalfix_ms_workorders.dto.event.WorkOrderNotificationEvent;

/**
 * Puerto de salida de la mensajería: lo que una orden de trabajo necesita comunicar. Quien lo usa
 * ({@link OrdenTrabajoEventosListener}) no conoce colas, exchanges ni routing keys; la
 * implementación ({@code ColasEventosClient}) entrega los eventos a ms-admin-queues, que es el
 * dueño de toda la mensajería.
 */
public interface PublicadorEventosOrden {

    /** Correo al cliente: orden creada o cambio de estado (flujo email). */
    void publicarNotificacionCliente(WorkOrderNotificationEvent evento);

    /** Ticket de despacho para la cuadrilla asignada (flujo dispatch). */
    void publicarTicketDespacho(DispatchTicketEvent evento);

    /** Informe o certificado PDF de la orden (flujo report). */
    void publicarSolicitudInforme(ReportRequestEvent evento);
}
