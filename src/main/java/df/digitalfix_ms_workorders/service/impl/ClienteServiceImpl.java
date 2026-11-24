package df.digitalfix_ms_workorders.service.impl;

import df.digitalfix_ms_workorders.dto.Cliente.ActualizarClienteRequest;
import df.digitalfix_ms_workorders.dto.Cliente.ClienteRequest;
import df.digitalfix_ms_workorders.dto.Cliente.ClienteResponse;
import df.digitalfix_ms_workorders.model.clientes.Cliente;
import df.digitalfix_ms_workorders.model.common.DatoInvalidoException;
import df.digitalfix_ms_workorders.model.common.Rut;
import df.digitalfix_ms_workorders.repository.ClienteRepository;
import df.digitalfix_ms_workorders.service.ClienteService;
import df.digitalfix_ms_workorders.service.UbicacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Implementación de la gestión de clientes.
 * Valida unicidad de RUT y asocia la dirección georreferenciada.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clienteRepository;
    private final UbicacionService ubicacionService;

    @Override
    @Transactional
    public ClienteResponse crear(ClienteRequest request) {
        Rut rut = Rut.of(request.rut());
        if (clienteRepository.existsByRut_Numero(rut.getNumero())) {
            throw new DatoInvalidoException("Ya existe un cliente registrado con el RUT " + rut.formateado());
        }

        Cliente cliente = new Cliente(
                rut,
                request.nombre(),
                request.telefono(),
                request.telefonoSecundario(),
                request.email(),
                ubicacionService.aplicar(null, request.direccion())
        );

        return ClienteResponse.from(clienteRepository.saveAndFlush(cliente));
    }

    @Override
    public List<ClienteResponse> listar() {
        return clienteRepository.findAllByOrderByNombreAsc()
                .stream()
                .map(ClienteResponse::from)
                .toList();
    }

    @Override
    public ClienteResponse buscarPorId(Long id) {
        return ClienteResponse.from(obtener(id));
    }

    @Override
    @Transactional
    public ClienteResponse actualizar(Long id, ActualizarClienteRequest request) {
        Cliente cliente = obtener(id);
        cliente.actualizarNombre(request.nombre());
        cliente.actualizarContacto(request.telefono(), request.telefonoSecundario(), request.email());
        cliente.asignarDireccion(ubicacionService.aplicar(cliente.getDireccion(), request.direccion()));
        return ClienteResponse.from(clienteRepository.saveAndFlush(cliente));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Cliente cliente = obtener(id);
        clienteRepository.delete(cliente);
    }

    private Cliente obtener(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe un cliente con id " + id));
    }
}

