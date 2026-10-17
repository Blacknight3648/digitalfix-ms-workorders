package df.digitalfix_ms_workorders.controller;

import df.digitalfix_ms_workorders.dto.Cliente.ActualizarClienteRequest;
import df.digitalfix_ms_workorders.dto.Cliente.ClienteRequest;
import df.digitalfix_ms_workorders.dto.Cliente.ClienteResponse;
import df.digitalfix_ms_workorders.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "Operaciones para la administración de clientes y datos de contacto")
public class ClienteController {

    private final ClienteService clienteService;

    @PostMapping
    @Operation(summary = "Crear un nuevo cliente", description = "Registra un cliente validando el formato y dígito verificador del RUT (módulo 11).")
    public ResponseEntity<ClienteResponse> crear(@Valid @RequestBody ClienteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteService.crear(request));
    }

    @GetMapping
    @Operation(summary = "Listar clientes", description = "Retorna todos los clientes registrados en el sistema.")
    public ResponseEntity<List<ClienteResponse>> listar() {
        return ResponseEntity.ok(clienteService.listar());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar cliente por ID", description = "Retorna el detalle completo de un cliente específico.")
    public ResponseEntity<ClienteResponse> buscarPorId(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(clienteService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar cliente", description = "Actualiza nombres, apellidos, teléfonos, email o dirección del cliente.")
    public ResponseEntity<ClienteResponse> actualizar(
            @PathVariable @Positive Long id,
            @Valid @RequestBody ActualizarClienteRequest request) {
        return ResponseEntity.ok(clienteService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar cliente", description = "Elimina un cliente si no posee registros históricos asociados.")
    public ResponseEntity<Void> eliminar(@PathVariable @Positive Long id) {
        clienteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
