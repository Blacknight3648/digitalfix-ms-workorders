package df.digitalfix_ms_workorders.service;

import df.digitalfix_ms_workorders.dto.Ubicacion.ComunaResponse;
import df.digitalfix_ms_workorders.dto.Ubicacion.DireccionRequest;
import df.digitalfix_ms_workorders.dto.Ubicacion.RegionResponse;
import df.digitalfix_ms_workorders.model.ubicacion.Direccion;

import java.util.List;

/**
 * Contrato de servicio para el manejo de ubicaciones geográficas y armado de direcciones.
 */
public interface UbicacionService {

    List<RegionResponse> listarRegiones();

    List<ComunaResponse> listarComunas(Long idRegion);

    Direccion aplicar(Direccion actual, DireccionRequest request);
}
