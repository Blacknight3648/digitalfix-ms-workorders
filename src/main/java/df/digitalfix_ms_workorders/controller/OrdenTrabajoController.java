package df.digitalfix_ms_workorders.controller;

import df.digitalfix_ms_workorders.dto.*;
import df.digitalfix_ms_workorders.enums.EstadoOrden;
import df.digitalfix_ms_workorders.service.OrdenTrabajoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Gestión de órdenes de trabajo (sección 3 del enunciado del caso). Sin autenticación propia:
// en la arquitectura objetivo este microservicio sólo lo llama ms-digitalfix-bff, que valida el
// JWT de Azure AD antes de reenviar la petición.
@RestController
@RequestMapping("/api/ordenes-trabajo")
@RequiredArgsConstructor
public class OrdenTrabajoController {

    private final OrdenTrabajoService service;

    @PostMapping
    public ResponseEntity<OrdenTrabajoResponse> crear(@Valid @RequestBody OrdenTrabajoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request));
    }

    @GetMapping
    public ResponseEntity<List<OrdenTrabajoResponse>> listar(@RequestParam(required = false) EstadoOrden estado) {
        return ResponseEntity.ok(service.listar(estado));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrdenTrabajoResponse> buscarPorId(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrdenTrabajoResponse> actualizar(
            @PathVariable @Positive Long id, @Valid @RequestBody ActualizarOrdenTrabajoRequest request) {
        return ResponseEntity.ok(service.actualizar(id, request));
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<OrdenTrabajoResponse> cambiarEstado(
            @PathVariable @Positive Long id, @Valid @RequestBody CambioEstadoRequest request) {
        return ResponseEntity.ok(service.cambiarEstado(id, request));
    }

    @PostMapping("/{id}/servicios")
    public ResponseEntity<OrdenTrabajoResponse> agregarServicio(
            @PathVariable @Positive Long id, @Valid @RequestBody DetalleServicioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.agregarServicio(id, request));
    }

    @PutMapping("/{id}/servicios/{idDetalle}/tecnico")
    public ResponseEntity<OrdenTrabajoResponse> asignarTecnico(
            @PathVariable @Positive Long id, @PathVariable @Positive Long idDetalle,
            @Valid @RequestBody AsignarTecnicoRequest request) {
        return ResponseEntity.ok(service.asignarTecnico(id, idDetalle, request));
    }

    @DeleteMapping("/{id}/servicios/{idDetalle}")
    public ResponseEntity<OrdenTrabajoResponse> quitarServicio(
            @PathVariable @Positive Long id, @PathVariable @Positive Long idDetalle) {
        return ResponseEntity.ok(service.quitarServicio(id, idDetalle));
    }

    @PostMapping("/{id}/repuestos")
    public ResponseEntity<OrdenTrabajoResponse> agregarRepuesto(
            @PathVariable @Positive Long id, @Valid @RequestBody DetalleRepuestoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.agregarRepuesto(id, request));
    }

    @DeleteMapping("/{id}/repuestos/{idDetalle}")
    public ResponseEntity<OrdenTrabajoResponse> quitarRepuesto(
            @PathVariable @Positive Long id, @PathVariable @Positive Long idDetalle) {
        return ResponseEntity.ok(service.quitarRepuesto(id, idDetalle));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable @Positive Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
