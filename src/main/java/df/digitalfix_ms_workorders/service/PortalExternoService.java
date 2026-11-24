package df.digitalfix_ms_workorders.service;

import df.digitalfix_ms_workorders.dto.Portal.OrdenPortalRequest;
import df.digitalfix_ms_workorders.dto.Portal.SeguimientoOrdenResponse;
import df.digitalfix_ms_workorders.dto.Portal.ServicioPortalResponse;

import java.util.List;

/**
 * Operaciones que el ERP expone al Portal Cliente (backend monolito) por /api/externo/**.
 * Reutiliza los servicios internos: las reglas de clientes, servicios y órdenes son las mismas
 * que aplica el portal de empleados.
 */
public interface PortalExternoService {

    List<ServicioPortalResponse> serviciosActivos();

    /** Crea (o reutiliza por RUT) al cliente y registra la OT en estado CREADA. */
    SeguimientoOrdenResponse crearOrden(OrdenPortalRequest request);

    /** Órdenes del cliente con ese RUT, más recientes primero. Lista vacía si el RUT no es cliente. */
    List<SeguimientoOrdenResponse> ordenesDelCliente(String rut);

    /** Seguimiento de una OT; 404 si no existe o no pertenece al RUT indicado. */
    SeguimientoOrdenResponse seguimiento(Long idOrdenTrabajo, String rut);
}
