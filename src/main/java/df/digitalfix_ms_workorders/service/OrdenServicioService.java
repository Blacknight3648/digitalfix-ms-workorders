package df.digitalfix_ms_workorders.service;

import df.digitalfix_ms_workorders.dto.CambioEstadoRequest;
import df.digitalfix_ms_workorders.dto.OrdenServicio.ActualizarOrdenServicioRequest;
import df.digitalfix_ms_workorders.dto.OrdenServicio.DetalleOrdenServicioRequest;
import df.digitalfix_ms_workorders.dto.OrdenServicio.OrdenServicioRequest;
import df.digitalfix_ms_workorders.dto.OrdenServicio.OrdenServicioResponse;
import df.digitalfix_ms_workorders.dto.Tecnico.AsignarTecnicoRequest;
import df.digitalfix_ms_workorders.enums.EstadoOrden;

import java.util.List;

/**
 * Contrato de servicio para la gestión de solicitudes y Órdenes de Servicio comercial.
 */
public interface OrdenServicioService {

    OrdenServicioResponse crear(OrdenServicioRequest request);

    List<OrdenServicioResponse> listar(EstadoOrden estado, Long idCliente);

    OrdenServicioResponse buscarPorId(Long id);

    OrdenServicioResponse actualizar(Long id, ActualizarOrdenServicioRequest request);

    OrdenServicioResponse cambiarEstado(Long id, CambioEstadoRequest request);

    OrdenServicioResponse asignarTecnico(Long id, AsignarTecnicoRequest request);

    OrdenServicioResponse agregarDetalle(Long id, DetalleOrdenServicioRequest request);

    OrdenServicioResponse quitarDetalle(Long id, Long idDetalle);

    void eliminar(Long id);
}
