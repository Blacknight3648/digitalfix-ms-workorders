package df.digitalfix_ms_workorders.service;

import df.digitalfix_ms_workorders.dto.Ubicacion.ComunaResponse;
import df.digitalfix_ms_workorders.dto.Ubicacion.DireccionRequest;
import df.digitalfix_ms_workorders.dto.Ubicacion.RegionResponse;
import df.digitalfix_ms_workorders.model.common.DatoInvalidoException;
import df.digitalfix_ms_workorders.model.ubicacion.Comuna;
import df.digitalfix_ms_workorders.model.ubicacion.Direccion;
import df.digitalfix_ms_workorders.repository.ComunaRepository;
import df.digitalfix_ms_workorders.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** Catálogo de regiones y comunas, y armado de las direcciones de clientes y proveedores. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UbicacionService {

    private final RegionRepository regionRepository;
    private final ComunaRepository comunaRepository;

    public List<RegionResponse> listarRegiones() {
        return regionRepository.findAllByOrderByOrdenGeograficoAsc().stream().map(RegionResponse::from).toList();
    }

    public List<ComunaResponse> listarComunas(Long idRegion) {
        if (!regionRepository.existsById(idRegion)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe una región con id " + idRegion);
        }
        return comunaRepository.findByRegion_IdRegionOrderByNombreAsc(idRegion).stream()
                .map(ComunaResponse::from)
                .toList();
    }

    /** Crea la dirección a partir del request, o actualiza la existente (la entidad dueña la persiste). */
    public Direccion aplicar(Direccion actual, DireccionRequest request) {
        Comuna comuna = comunaRepository.findById(request.idComuna())
                .orElseThrow(() -> new DatoInvalidoException("No existe una comuna con id " + request.idComuna()));
        if (actual == null) {
            return new Direccion(comuna, request.calle(), request.numero(), request.detalle(),
                    request.referencia(), request.codigoPostal());
        }
        actual.actualizar(comuna, request.calle(), request.numero(), request.detalle(),
                request.referencia(), request.codigoPostal());
        return actual;
    }
}
