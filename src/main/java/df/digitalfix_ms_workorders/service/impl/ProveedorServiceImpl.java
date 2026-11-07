package df.digitalfix_ms_workorders.service.impl;

import df.digitalfix_ms_workorders.dto.Proveedor.ActualizarProveedorRequest;
import df.digitalfix_ms_workorders.dto.Proveedor.ProveedorRequest;
import df.digitalfix_ms_workorders.dto.Proveedor.ProveedorResponse;
import df.digitalfix_ms_workorders.enums.TipoProveedor;
import df.digitalfix_ms_workorders.model.common.DatoInvalidoException;
import df.digitalfix_ms_workorders.model.common.Rut;
import df.digitalfix_ms_workorders.model.proveedores.Proveedor;
import df.digitalfix_ms_workorders.repository.ProveedorRepository;
import df.digitalfix_ms_workorders.service.ProveedorService;
import df.digitalfix_ms_workorders.service.UbicacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Implementación de la gestión de proveedores en el módulo de órdenes de trabajo.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProveedorServiceImpl implements ProveedorService {

    private final ProveedorRepository proveedorRepository;
    private final UbicacionService ubicacionService;

    @Override
    @Transactional
    public ProveedorResponse crear(ProveedorRequest request) {
        Rut rut = Rut.of(request.rut());
        if (proveedorRepository.existsByRut_Numero(rut.getNumero())) {
            throw new DatoInvalidoException("Ya existe un proveedor registrado con el RUT " + rut.formateado());
        }

        Proveedor proveedor = new Proveedor(
                rut,
                request.razonSocial(),
                request.tipo(),
                request.giro(),
                request.telefono(),
                request.email(),
                ubicacionService.aplicar(null, request.direccion())
        );

        return ProveedorResponse.from(proveedorRepository.saveAndFlush(proveedor));
    }

    @Override
    public List<ProveedorResponse> listar(TipoProveedor tipo) {
        List<Proveedor> proveedores = tipo == null
                ? proveedorRepository.findAllByOrderByRazonSocialAsc()
                : proveedorRepository.findByTipoOrderByRazonSocialAsc(tipo);
        return proveedores.stream().map(ProveedorResponse::from).toList();
    }

    @Override
    public ProveedorResponse buscarPorId(Long id) {
        return ProveedorResponse.from(obtener(id));
    }

    @Override
    @Transactional
    public ProveedorResponse actualizar(Long id, ActualizarProveedorRequest request) {
        Proveedor proveedor = obtener(id);
        proveedor.actualizarDatosComerciales(request.razonSocial(), request.tipo(), request.giro());
        proveedor.actualizarContacto(request.telefono(), request.email());
        proveedor.asignarDireccion(ubicacionService.aplicar(proveedor.getDireccion(), request.direccion()));
        return ProveedorResponse.from(proveedorRepository.saveAndFlush(proveedor));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Proveedor proveedor = obtener(id);
        proveedorRepository.delete(proveedor);
    }

    private Proveedor obtener(Long id) {
        return proveedorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe un proveedor con id " + id));
    }
}

