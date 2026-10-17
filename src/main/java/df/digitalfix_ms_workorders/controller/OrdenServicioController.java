package df.digitalfix_ms_workorders.controller;

import df.digitalfix_ms_workorders.dto.CambioEstadoRequest;
import df.digitalfix_ms_workorders.dto.OrdenServicio.ActualizarOrdenServicioRequest;
import df.digitalfix_ms_workorders.dto.OrdenServicio.DetalleOrdenServicioRequest;
import df.digitalfix_ms_workorders.dto.OrdenServicio.OrdenServicioRequest;
import df.digitalfix_ms_workorders.dto.OrdenServicio.OrdenServicioResponse;
import df.digitalfix_ms_workorders.dto.Tecnico.AsignarTecnicoRequest;
import df.digitalfix_ms_workorders.enums.EstadoOrden;
import df.digitalfix_ms_workorders.service.OrdenServicioService;
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
@RequestMapping("/api/ordenes-servicio")
@RequiredArgsConstructor
@Tag(name = "Órdenes de Servicio", description = "Gestión de requerimientos y órdenes comerciales de servicio de mantención")
public class OrdenServicioController {

    private final OrdenServicioService service;

    @PostMapping
    @Operation(summary = "Crear orden de servicio", description = "Registra una nueva orden de servicio para un cliente con sus servicios iniciales opcionales. Inicia en estado CREADA.")
    public ResponseEntity<OrdenServicioResponse> crear(@Valid @RequestBody OrdenServicioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request));
    }

    @GetMapping
    @Operation(summary = "Listar órdenes de servicio", description = "Retorna las órdenes de servicio, permitiendo filtrar opcionalmente por estado y/o cliente.")
    public ResponseEntity<List<OrdenServicioResponse>> listar(
            @RequestParam(required = false) EstadoOrden estado,
            @RequestParam(required = false) Long idCliente) {
        return ResponseEntity.ok(service.listar(estado, idCliente));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar orden de servicio por ID", description = "Retorna el detalle completo de la orden de servicio con sus servicios contratados y total.")
    public ResponseEntity<OrdenServicioResponse> buscarPorId(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar observaciones", description = "Actualiza las observaciones de una orden de servicio mientras no se encuentre en un estado terminal.")
    public ResponseEntity<OrdenServicioResponse> actualizar(
            @PathVariable @Positive Long id,
            @Valid @RequestBody ActualizarOrdenServicioRequest request) {
        return ResponseEntity.ok(service.actualizar(id, request));
    }

    @PutMapping("/{id}/estado")
    @Operation(summary = "Cambiar estado de la orden de servicio", description = "Avanza el estado de la orden según la máquina de estados permitida (CREADA -> ASIGNADA -> EN_DESPLAZAMIENTO -> EN_EJECUCION -> CERRADA / CANCELADA).")
    public ResponseEntity<OrdenServicioResponse> cambiarEstado(
            @PathVariable @Positive Long id,
            @Valid @RequestBody CambioEstadoRequest request) {
        return ResponseEntity.ok(service.cambiarEstado(id, request));
    }

    @PutMapping("/{id}/tecnico")
    @Operation(summary = "Asignar técnico a la orden de servicio", description = "Asigna el técnico responsable a la orden y transiciona automáticamente el estado a ASIGNADA.")
    public ResponseEntity<OrdenServicioResponse> asignarTecnico(
            @PathVariable @Positive Long id,
            @Valid @RequestBody AsignarTecnicoRequest request) {
        return ResponseEntity.ok(service.asignarTecnico(id, request));
    }

    @PostMapping("/{id}/detalles")
    @Operation(summary = "Agregar servicio a la orden", description = "Agrega una nueva línea de servicio con cantidad y precio unitario a una orden de servicio editable.")
    public ResponseEntity<OrdenServicioResponse> agregarDetalle(
            @PathVariable @Positive Long id,
            @Valid @RequestBody DetalleOrdenServicioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.agregarDetalle(id, request));
    }

    @DeleteMapping("/{id}/detalles/{idDetalle}")
    @Operation(summary = "Quitar servicio de la orden", description = "Elimina una línea de servicio de una orden de servicio editable.")
    public ResponseEntity<OrdenServicioResponse> quitarDetalle(
            @PathVariable @Positive Long id,
            @PathVariable @Positive Long idDetalle) {
        return ResponseEntity.ok(service.quitarDetalle(id, idDetalle));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar orden de servicio", description = "Elimina físicamente una orden de servicio únicamente si está en estado CREADA.")
    public ResponseEntity<Void> eliminar(@PathVariable @Positive Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
