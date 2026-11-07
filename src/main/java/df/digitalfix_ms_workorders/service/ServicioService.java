package df.digitalfix_ms_workorders.service;

import df.digitalfix_ms_workorders.dto.Servicio.ActualizarServicioRequest;
import df.digitalfix_ms_workorders.dto.Servicio.ServicioRequest;
import df.digitalfix_ms_workorders.dto.Servicio.ServicioResponse;
import df.digitalfix_ms_workorders.model.servicios.Servicio;

import java.util.List;

/**
 * Contrato de servicio para el catálogo de prestaciones técnicas y servicios eléctricos.
 */
public interface ServicioService {

    ServicioResponse crear(ServicioRequest request);

    List<ServicioResponse> listar(boolean soloActivos);

    ServicioResponse buscarPorId(Long id);

    ServicioResponse actualizar(Long id, ActualizarServicioRequest request);

    void eliminar(Long id);

    Servicio obtenerActivo(Long id);
}
