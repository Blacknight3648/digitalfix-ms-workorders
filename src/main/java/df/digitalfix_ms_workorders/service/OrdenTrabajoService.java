package df.digitalfix_ms_workorders.service;

import df.digitalfix_ms_workorders.dto.*;
import df.digitalfix_ms_workorders.enums.EstadoOrden;
import df.digitalfix_ms_workorders.model.clientes.Cliente;
import df.digitalfix_ms_workorders.model.common.OperacionNoPermitidaException;
import df.digitalfix_ms_workorders.model.ordenTrabajo.OrdenTrabajo;
import df.digitalfix_ms_workorders.model.proveedores.Proveedor;
import df.digitalfix_ms_workorders.repository.ClienteRepository;
import df.digitalfix_ms_workorders.repository.OrdenTrabajoRepository;
import df.digitalfix_ms_workorders.repository.ProveedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

// Orquesta los casos de uso de la OT. Las reglas de negocio (estados, duplicados, edición) viven
// en el modelo OrdenTrabajo; aquí sólo se cargan las entidades y se delimita la transacción.
// Se usa saveAndFlush para que los errores de validación/constraints salgan dentro del request.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrdenTrabajoService {

    private final OrdenTrabajoRepository ordenTrabajoRepository;
    private final ClienteRepository clienteRepository;
    private final ProveedorRepository proveedorRepository;

    @Transactional
    public OrdenTrabajoResponse crear(OrdenTrabajoRequest request) {
        Cliente cliente = clienteRepository.findById(request.idCliente())
                .orElseThrow(() -> noEncontrado("No existe un cliente con id " + request.idCliente()));
        OrdenTrabajo orden = new OrdenTrabajo(cliente, request.observaciones());
        if (request.servicios() != null) {
            request.servicios().forEach(s -> orden.agregarServicio(
                    s.idServicio(), s.idTecnico(), s.descripcionManoObra(), s.precioManoObra()));
        }
        return OrdenTrabajoResponse.from(ordenTrabajoRepository.saveAndFlush(orden));
    }

    public List<OrdenTrabajoResponse> listar(EstadoOrden estado) {
        List<OrdenTrabajo> ordenes = estado != null
                ? ordenTrabajoRepository.findByEstado(estado)
                : ordenTrabajoRepository.findAll();
        return ordenes.stream().map(OrdenTrabajoResponse::from).toList();
    }

    public OrdenTrabajoResponse buscarPorId(Long id) {
        return OrdenTrabajoResponse.from(obtener(id));
    }

    @Transactional
    public OrdenTrabajoResponse actualizar(Long id, ActualizarOrdenTrabajoRequest request) {
        OrdenTrabajo orden = obtener(id);
        orden.actualizarObservaciones(request.observaciones());
        return guardar(orden);
    }

    @Transactional
    public OrdenTrabajoResponse cambiarEstado(Long id, CambioEstadoRequest request) {
        OrdenTrabajo orden = obtener(id);
        orden.cambiarEstado(request.estado());
        return guardar(orden);
    }

    @Transactional
    public OrdenTrabajoResponse agregarServicio(Long id, DetalleServicioRequest request) {
        OrdenTrabajo orden = obtener(id);
        orden.agregarServicio(request.idServicio(), request.idTecnico(),
                request.descripcionManoObra(), request.precioManoObra());
        return guardar(orden);
    }

    @Transactional
    public OrdenTrabajoResponse asignarTecnico(Long id, Long idDetalleServicio, AsignarTecnicoRequest request) {
        OrdenTrabajo orden = obtener(id);
        orden.asignarTecnico(idDetalleServicio, request.idTecnico());
        return guardar(orden);
    }

    @Transactional
    public OrdenTrabajoResponse quitarServicio(Long id, Long idDetalleServicio) {
        OrdenTrabajo orden = obtener(id);
        orden.quitarServicio(idDetalleServicio);
        return guardar(orden);
    }

    @Transactional
    public OrdenTrabajoResponse agregarRepuesto(Long id, DetalleRepuestoRequest request) {
        OrdenTrabajo orden = obtener(id);
        Proveedor proveedor = proveedorRepository.findById(request.idProveedor())
                .orElseThrow(() -> noEncontrado("No existe un proveedor con id " + request.idProveedor()));
        orden.agregarRepuesto(request.idRepuesto(), proveedor, request.cantidad());
        return guardar(orden);
    }

    @Transactional
    public OrdenTrabajoResponse quitarRepuesto(Long id, Long idDetalleRepuesto) {
        OrdenTrabajo orden = obtener(id);
        orden.quitarRepuesto(idDetalleRepuesto);
        return guardar(orden);
    }

    @Transactional
    public void eliminar(Long id) {
        OrdenTrabajo orden = obtener(id);
        if (!orden.esEliminable()) {
            throw new OperacionNoPermitidaException(
                    "Sólo se puede eliminar una orden en estado CREADA; en otro caso debe cancelarse o cerrarse");
        }
        ordenTrabajoRepository.delete(orden);
    }

    private OrdenTrabajoResponse guardar(OrdenTrabajo orden) {
        return OrdenTrabajoResponse.from(ordenTrabajoRepository.saveAndFlush(orden));
    }

    private OrdenTrabajo obtener(Long id) {
        return ordenTrabajoRepository.findById(id)
                .orElseThrow(() -> noEncontrado("No existe una orden de trabajo con id " + id));
    }

    private ResponseStatusException noEncontrado(String mensaje) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, mensaje);
    }
}
