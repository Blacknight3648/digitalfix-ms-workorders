package df.digitalfix_ms_workorders.enums;

// Mismos valores que WorkOrderStatus en el frontend
// (digitalfix-frontend/src/app/model/work-order.model.ts). El flujo es lineal
// salvo CANCELADA, que puede ocurrir desde CREADA.
public enum EstadoOrden {
    CREADA,
    ASIGNADA,
    EN_DESPLAZAMIENTO,
    EN_EJECUCION,
    CERRADA,
    CANCELADA
}
