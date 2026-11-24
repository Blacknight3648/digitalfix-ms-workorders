package df.digitalfix_ms_workorders.service.impl;

import df.digitalfix_ms_workorders.dto.Servicio.ActualizarServicioRequest;
import df.digitalfix_ms_workorders.dto.Servicio.ServicioRequest;
import df.digitalfix_ms_workorders.dto.Servicio.ServicioResponse;
import df.digitalfix_ms_workorders.model.common.DatoInvalidoException;
import df.digitalfix_ms_workorders.model.common.OperacionNoPermitidaException;
import df.digitalfix_ms_workorders.model.servicios.Servicio;
import df.digitalfix_ms_workorders.repository.ServicioRepository;
import df.digitalfix_ms_workorders.service.ServicioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Implementación del catálogo de servicios técnicos.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServicioServiceImpl implements ServicioService {

    private final ServicioRepository servicioRepository;

    private static final String PREFIJO_CODIGO = "SRV-";

    @Override
    @Transactional
    public ServicioResponse crear(ServicioRequest request) {
        Servicio servicio = new Servicio(generarCodigo(), request.nombre(), request.descripcion(), request.tarifa());
        return ServicioResponse.from(servicioRepository.saveAndFlush(servicio));
    }

    @Override
    public List<ServicioResponse> listar(boolean soloActivos) {
        List<Servicio> servicios = soloActivos
                ? servicioRepository.findByActivoTrueOrderByNombreAsc()
                : servicioRepository.findAllByOrderByNombreAsc();
        return servicios.stream().map(ServicioResponse::from).toList();
    }

    @Override
    public ServicioResponse buscarPorId(Long id) {
        return ServicioResponse.from(obtener(id));
    }

    @Override
    @Transactional
    public ServicioResponse actualizar(Long id, ActualizarServicioRequest request) {
        Servicio servicio = obtener(id);
        servicio.actualizarDatos(request.nombre(), request.descripcion(), request.tarifa());
        if (request.activo()) {
            servicio.activar();
        } else {
            servicio.desactivar();
        }
        return ServicioResponse.from(servicioRepository.saveAndFlush(servicio));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Servicio servicio = obtener(id);
        if (servicioRepository.usadoEnOrdenesTrabajo(id) || servicioRepository.usadoEnOrdenesServicio(id)) {
            throw new OperacionNoPermitidaException("El servicio " + servicio.getCodigo()
                    + " ya se usó en órdenes; desactívelo en vez de eliminarlo");
        }
        servicioRepository.delete(servicio);
    }

    @Override
    public Servicio obtenerActivo(Long id) {
        Servicio servicio = servicioRepository.findById(id)
                .orElseThrow(() -> new DatoInvalidoException("No existe un servicio con id " + id));
        if (!servicio.isActivo()) {
            throw new DatoInvalidoException("El servicio " + servicio.getNombre() + " no está disponible");
        }
        return servicio;
    }

    private String generarCodigo() {
        String codigo;
        do {
            codigo = PREFIJO_CODIGO + String.format("%04d", servicioRepository.siguienteCorrelativoCodigo());
        } while (servicioRepository.existsByCodigo(codigo));
        return codigo;
    }

    private Servicio obtener(Long id) {
        return servicioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe un servicio con id " + id));
    }
}

