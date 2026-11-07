package df.digitalfix_ms_workorders.service;

import df.digitalfix_ms_workorders.dto.Cliente.ActualizarClienteRequest;
import df.digitalfix_ms_workorders.dto.Cliente.ClienteRequest;
import df.digitalfix_ms_workorders.dto.Cliente.ClienteResponse;

import java.util.List;

/**
 * Contrato de servicio para la gestión de clientes en DigitalFix.
 */
public interface ClienteService {

    ClienteResponse crear(ClienteRequest request);

    List<ClienteResponse> listar();

    ClienteResponse buscarPorId(Long id);

    ClienteResponse actualizar(Long id, ActualizarClienteRequest request);

    void eliminar(Long id);
}
