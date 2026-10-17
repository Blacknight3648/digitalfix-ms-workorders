package df.digitalfix_ms_workorders.service;

import df.digitalfix_ms_workorders.dto.Cuadrilla.ActualizarCuadrillaRequest;
import df.digitalfix_ms_workorders.dto.Cuadrilla.CuadrillaRequest;
import df.digitalfix_ms_workorders.dto.Cuadrilla.CuadrillaResponse;
import df.digitalfix_ms_workorders.enums.EstadoCuadrilla;
import df.digitalfix_ms_workorders.model.common.DatoInvalidoException;
import df.digitalfix_ms_workorders.model.common.OperacionNoPermitidaException;
import df.digitalfix_ms_workorders.model.cuadrilla.Cuadrilla;
import df.digitalfix_ms_workorders.repository.CuadrillaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CuadrillaService {

    private final CuadrillaRepository cuadrillaRepository;

    @Transactional
    public CuadrillaResponse crear(CuadrillaRequest request) {
        if (cuadrillaRepository.existsByNombreIgnoreCase(request.nombre())) {
            throw new DatoInvalidoException("Ya existe una cuadrilla registrada con el nombre " + request.nombre());
        }

        Cuadrilla cuadrilla = new Cuadrilla(request.nombre(), request.idTecnicoLider(), request.miembros());
        return CuadrillaResponse.from(cuadrillaRepository.saveAndFlush(cuadrilla));
    }

    public List<CuadrillaResponse> listar(EstadoCuadrilla estado) {
        List<Cuadrilla> cuadrillas = estado != null
                ? cuadrillaRepository.findByEstado(estado)
                : cuadrillaRepository.findAll();
        return cuadrillas.stream().map(CuadrillaResponse::from).toList();
    }

    public CuadrillaResponse buscarPorId(Long id) {
        return CuadrillaResponse.from(obtener(id));
    }

    @Transactional
    public CuadrillaResponse actualizar(Long id, ActualizarCuadrillaRequest request) {
        Cuadrilla cuadrilla = obtener(id);
        cuadrilla.actualizarDatos(request.nombre(), request.idTecnicoLider(), request.estado());
        return CuadrillaResponse.from(cuadrillaRepository.saveAndFlush(cuadrilla));
    }

    @Transactional
    public CuadrillaResponse agregarMiembro(Long idCuadrilla, Long idTecnico) {
        Cuadrilla cuadrilla = obtener(idCuadrilla);
        cuadrilla.agregarMiembro(idTecnico);
        return CuadrillaResponse.from(cuadrillaRepository.saveAndFlush(cuadrilla));
    }

    @Transactional
    public CuadrillaResponse quitarMiembro(Long idCuadrilla, Long idTecnico) {
        Cuadrilla cuadrilla = obtener(idCuadrilla);
        cuadrilla.quitarMiembro(idTecnico);
        return CuadrillaResponse.from(cuadrillaRepository.saveAndFlush(cuadrilla));
    }

    @Transactional
    public void eliminar(Long id) {
        Cuadrilla cuadrilla = obtener(id);
        if (cuadrilla.getEstado() == EstadoCuadrilla.EN_TERRENO) {
            throw new OperacionNoPermitidaException("No se puede eliminar una cuadrilla que se encuentra actualmente en terreno");
        }
        cuadrillaRepository.delete(cuadrilla);
    }

    private Cuadrilla obtener(Long id) {
        return cuadrillaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe una cuadrilla con id " + id));
    }
}
