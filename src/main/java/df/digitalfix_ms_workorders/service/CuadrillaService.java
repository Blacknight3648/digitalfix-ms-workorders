package df.digitalfix_ms_workorders.service;

import df.digitalfix_ms_workorders.dto.Cuadrilla.ActualizarCuadrillaRequest;
import df.digitalfix_ms_workorders.dto.Cuadrilla.CuadrillaRequest;
import df.digitalfix_ms_workorders.dto.Cuadrilla.CuadrillaResponse;
import df.digitalfix_ms_workorders.enums.EstadoCuadrilla;

import java.util.List;

/**
 * Contrato de servicio para la administración y coordinación de cuadrillas en terreno.
 */
public interface CuadrillaService {

    CuadrillaResponse crear(CuadrillaRequest request);

    List<CuadrillaResponse> listar(EstadoCuadrilla estado);

    CuadrillaResponse buscarPorId(Long id);

    CuadrillaResponse actualizar(Long id, ActualizarCuadrillaRequest request);

    CuadrillaResponse agregarMiembro(Long idCuadrilla, Long idTecnico);

    CuadrillaResponse quitarMiembro(Long idCuadrilla, Long idTecnico);

    void eliminar(Long id);
}
