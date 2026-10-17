package df.digitalfix_ms_workorders.service;

import df.digitalfix_ms_workorders.dto.CambioEstadoRequest;
import df.digitalfix_ms_workorders.dto.OrdenServicio.ActualizarOrdenServicioRequest;
import df.digitalfix_ms_workorders.dto.OrdenServicio.DetalleOrdenServicioRequest;
import df.digitalfix_ms_workorders.dto.OrdenServicio.OrdenServicioRequest;
import df.digitalfix_ms_workorders.dto.OrdenServicio.OrdenServicioResponse;
import df.digitalfix_ms_workorders.dto.Tecnico.AsignarTecnicoRequest;
import df.digitalfix_ms_workorders.enums.EstadoOrden;
import df.digitalfix_ms_workorders.model.clientes.Cliente;
import df.digitalfix_ms_workorders.model.common.DatoInvalidoException;
import df.digitalfix_ms_workorders.model.common.OperacionNoPermitidaException;
import df.digitalfix_ms_workorders.model.ordenServicio.DetalleOrdenServicio;
import df.digitalfix_ms_workorders.model.ordenServicio.OrdenServicio;
import df.digitalfix_ms_workorders.repository.ClienteRepository;
import df.digitalfix_ms_workorders.repository.OrdenServicioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrdenServicioService {

    private final OrdenServicioRepository ordenServicioRepository;
    private final ClienteRepository clienteRepository;

    @Transactional
    public OrdenServicioResponse crear(OrdenServicioRequest request) {
        Cliente cliente = clienteRepository.findById(request.idCliente())
                .orElseThrow(() -> noEncontrado("No existe un cliente con id " + request.idCliente()));

        OrdenServicio orden = new OrdenServicio(cliente, request.observaciones());
        if (request.detalles() != null) {
            request.detalles().forEach(d -> orden.agregarDetalle(
                    d.idServicio(), d.cantidad(), d.precioUnitario()));
        }

        return guardar(orden);
    }

    public List<OrdenServicioResponse> listar(EstadoOrden estado, Long idCliente) {
        List<OrdenServicio> ordenes;
        if (estado != null && idCliente != null) {
            ordenes = ordenServicioRepository.findByEstadoAndCliente_IdCliente(estado, idCliente);
        } else if (estado != null) {
            ordenes = ordenServicioRepository.findByEstado(estado);
        } else if (idCliente != null) {
            ordenes = ordenServicioRepository.findByCliente_IdCliente(idCliente);
        } else {
            ordenes = ordenServicioRepository.findAll();
        }
        return ordenes.stream().map(OrdenServicioResponse::from).toList();
    }

    public OrdenServicioResponse buscarPorId(Long id) {
        return OrdenServicioResponse.from(obtener(id));
    }

    @Transactional
    public OrdenServicioResponse actualizar(Long id, ActualizarOrdenServicioRequest request) {
        OrdenServicio orden = obtener(id);
        orden.actualizarObservaciones(request.observaciones());
        return guardar(orden);
    }

    @Transactional
    public OrdenServicioResponse cambiarEstado(Long id, CambioEstadoRequest request) {
        OrdenServicio orden = obtener(id);
        orden.cambiarEstado(request.estado());
        return guardar(orden);
    }

    @Transactional
    public OrdenServicioResponse asignarTecnico(Long id, AsignarTecnicoRequest request) {
        OrdenServicio orden = obtener(id);
        orden.asignarTecnico(request.idTecnico());
        return guardar(orden);
    }

    @Transactional
    public OrdenServicioResponse agregarDetalle(Long id, DetalleOrdenServicioRequest request) {
        OrdenServicio orden = obtener(id);
        orden.agregarDetalle(request.idServicio(), request.cantidad(), request.precioUnitario());
        return guardar(orden);
    }

    @Transactional
    public OrdenServicioResponse quitarDetalle(Long id, Long idDetalle) {
        OrdenServicio orden = obtener(id);
        DetalleOrdenServicio detalle = orden.getDetalles().stream()
                .filter(d -> Objects.equals(d.getIdDetalleServicio(), idDetalle))
                .findFirst()
                .orElseThrow(() -> new DatoInvalidoException("El detalle indicado no pertenece a esta orden de servicio"));
        orden.quitarDetalle(detalle);
        return guardar(orden);
    }

    @Transactional
    public void eliminar(Long id) {
        OrdenServicio orden = obtener(id);
        if (orden.getEstado() != EstadoOrden.CREADA) {
            throw new OperacionNoPermitidaException(
                    "Sólo se puede eliminar una orden de servicio en estado CREADA; en otro caso debe cancelarse o cerrarse");
        }
        ordenServicioRepository.delete(orden);
    }

    private OrdenServicioResponse guardar(OrdenServicio orden) {
        return OrdenServicioResponse.from(ordenServicioRepository.saveAndFlush(orden));
    }

    private OrdenServicio obtener(Long id) {
        return ordenServicioRepository.findById(id)
                .orElseThrow(() -> noEncontrado("No existe una orden de servicio con id " + id));
    }

    private ResponseStatusException noEncontrado(String mensaje) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, mensaje);
    }
}
