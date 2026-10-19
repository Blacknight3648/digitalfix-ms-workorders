package df.digitalfix_ms_workorders.controller;

import df.digitalfix_ms_workorders.dto.Servicio.ActualizarServicioRequest;
import df.digitalfix_ms_workorders.dto.Servicio.ServicioRequest;
import df.digitalfix_ms_workorders.dto.Servicio.ServicioResponse;
import df.digitalfix_ms_workorders.service.ServicioService;
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
@RequestMapping("/api/servicios")
@RequiredArgsConstructor
@Tag(name = "Servicios", description = "Catálogo de servicios técnicos que se ofrecen en las órdenes")
public class ServicioController {

    private final ServicioService servicioService;

    @PostMapping
    @Operation(summary = "Crear un servicio", description = "Registra un servicio del catálogo; el código (SRV-0001...) lo genera el sistema.")
    public ResponseEntity<ServicioResponse> crear(@Valid @RequestBody ServicioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicioService.crear(request));
    }

    @GetMapping
    @Operation(summary = "Listar servicios",
            description = "Retorna el catálogo ordenado por nombre. Con soloActivos=true, solo los que se pueden elegir en una orden.")
    public ResponseEntity<List<ServicioResponse>> listar(@RequestParam(defaultValue = "false") boolean soloActivos) {
        return ResponseEntity.ok(servicioService.listar(soloActivos));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar servicio por ID", description = "Retorna el detalle de un servicio del catálogo.")
    public ResponseEntity<ServicioResponse> buscarPorId(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(servicioService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar servicio", description = "Modifica nombre, descripción, tarifa o si está activo. El código no cambia.")
    public ResponseEntity<ServicioResponse> actualizar(
            @PathVariable @Positive Long id,
            @Valid @RequestBody ActualizarServicioRequest request) {
        return ResponseEntity.ok(servicioService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar servicio", description = "Elimina un servicio que nunca se usó en órdenes; si ya se usó, responde 409.")
    public ResponseEntity<Void> eliminar(@PathVariable @Positive Long id) {
        servicioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
