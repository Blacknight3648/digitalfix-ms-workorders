package df.digitalfix_ms_workorders.service;

import df.digitalfix_ms_workorders.dto.Proveedor.ActualizarProveedorRequest;
import df.digitalfix_ms_workorders.dto.Proveedor.ProveedorRequest;
import df.digitalfix_ms_workorders.dto.Proveedor.ProveedorResponse;
import df.digitalfix_ms_workorders.enums.TipoProveedor;

import java.util.List;

/**
 * Contrato de servicio para la gestión de proveedores asociados a órdenes de trabajo.
 */
public interface ProveedorService {

    ProveedorResponse crear(ProveedorRequest request);

    List<ProveedorResponse> listar(TipoProveedor tipo);

    ProveedorResponse buscarPorId(Long id);

    ProveedorResponse actualizar(Long id, ActualizarProveedorRequest request);

    void eliminar(Long id);
}
