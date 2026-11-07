package df.digitalfix_ms_workorders.service;

import df.digitalfix_ms_workorders.dto.CambioEstadoRequest;
import df.digitalfix_ms_workorders.dto.Cuadrilla.AsignarCuadrillaRequest;
import df.digitalfix_ms_workorders.dto.OrdenServicio.DetalleServicioRequest;
import df.digitalfix_ms_workorders.dto.OrdenTrabajo.ActualizarOrdenTrabajoRequest;
import df.digitalfix_ms_workorders.dto.OrdenTrabajo.OrdenTrabajoRequest;
import df.digitalfix_ms_workorders.dto.OrdenTrabajo.OrdenTrabajoResponse;
import df.digitalfix_ms_workorders.dto.Repuesto.DetalleRepuestoRequest;
import df.digitalfix_ms_workorders.dto.Tecnico.AsignarTecnicoRequest;
import df.digitalfix_ms_workorders.enums.EstadoOrden;

import java.util.List;

/**
 * Contrato de servicio para la orquestacion y gestion del ciclo de vida de Ordenes de Trabajo (OT).
 */
public interface OrdenTrabajoService {

    OrdenTrabajoResponse crear(OrdenTrabajoRequest request);

    List<OrdenTrabajoResponse> listar(EstadoOrden estado, Long idCliente);

    OrdenTrabajoResponse buscarPorId(Long id);

    OrdenTrabajoResponse actualizar(Long id, ActualizarOrdenTrabajoRequest request);

    OrdenTrabajoResponse cambiarEstado(Long id, CambioEstadoRequest request);

    OrdenTrabajoResponse agregarServicio(Long id, DetalleServicioRequest request);

    OrdenTrabajoResponse asignarTecnico(Long id, Long idDetalleServicio, AsignarTecnicoRequest request);

    OrdenTrabajoResponse asignarCuadrilla(Long id, AsignarCuadrillaRequest request);

    OrdenTrabajoResponse quitarServicio(Long id, Long idDetalleServicio);

    OrdenTrabajoResponse agregarRepuesto(Long id, DetalleRepuestoRequest request);

    OrdenTrabajoResponse quitarRepuesto(Long id, Long idDetalleRepuesto);

    void eliminar(Long id);
}