package com.digitalfix.workorders.service;

import com.digitalfix.workorders.dto.event.DispatchTicketEvent;
import com.digitalfix.workorders.dto.event.ReportRequestEvent;
import com.digitalfix.workorders.dto.event.WorkOrderNotificationEvent;

public interface WorkOrderService {

    // Emisión de evento al crear o actualizar el estado de una orden
    void notifyOrderStatusChange(WorkOrderNotificationEvent notificationEvent);

    // Emisión de evento al asignar una cuadrilla/técnico en terreno
    void dispatchCrewTicket(DispatchTicketEvent dispatchEvent);

    // Emisión de evento al solicitar la emisión de certificado PDF
    void requestWorkOrderReport(ReportRequestEvent reportEvent);
}
