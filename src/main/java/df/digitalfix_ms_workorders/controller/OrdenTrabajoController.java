package df.digitalfix_ms_workorders.controller;

import df.digitalfix_ms_workorders.dto.CambioEstadoRequest;
import df.digitalfix_ms_workorders.dto.OrdenServicio.DetalleServicioRequest;
import df.digitalfix_ms_workorders.dto.OrdenTrabajo.ActualizarOrdenTrabajoRequest;
import df.digitalfix_ms_workorders.dto.OrdenTrabajo.OrdenTrabajoRequest;
import df.digitalfix_ms_workorders.dto.OrdenTrabajo.OrdenTrabajoResponse;
import df.digitalfix_ms_workorders.dto.Repuesto.DetalleRepuestoRequest;
import df.digitalfix_ms_workorders.dto.Tecnico.AsignarTecnicoRequest;
import df.digitalfix_ms_workorders.enums.EstadoOrden;
import df.digitalfix_ms_workorders.service.OrdenTrabajoService;
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
@RequestMapping("/api/ordenes-trabajo")
@RequiredArgsConstructor
@Tag(name = "Órdenes de Trabajo", description = "Gestión del ciclo de vida técnico de cuadrillas en terreno, asignación de mano de obra y consumo de repuestos")
public class OrdenTrabajoController {

    private final OrdenTrabajoService service;

    @PostMapping
    @Operation(summary = "Crear orden de trabajo", description = "Crea una nueva orden de trabajo en terreno asociada a un cliente. Inicia en estado CREADA.")
    public ResponseEntity<OrdenTrabajoResponse> crear(@Valid @RequestBody OrdenTrabajoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request));
    }

    @GetMapping
    @Operation(summary = "Listar órdenes de trabajo", description = "Consulta órdenes de trabajo, con soporte para filtrar por estado y/o por cliente.")
    public ResponseEntity<List<OrdenTrabajoResponse>> listar(
            @RequestParam(required = false) EstadoOrden estado,
            @RequestParam(required = false) Long idCliente) {
        return ResponseEntity.ok(service.listar(estado, idCliente));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar orden de trabajo por ID", description = "Retorna el detalle completo de la OT con sus servicios, técnicos asignados y repuestos vinculados a proveedores.")
    public ResponseEntity<OrdenTrabajoResponse> buscarPorId(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar observaciones", description = "Modifica las observaciones de la orden de trabajo si no está en estado terminal.")
    public ResponseEntity<OrdenTrabajoResponse> actualizar(
            @PathVariable @Positive Long id, @Valid @RequestBody ActualizarOrdenTrabajoRequest request) {
        return ResponseEntity.ok(service.actualizar(id, request));
    }

    @PutMapping("/{id}/estado")
    @Operation(summary = "Cambiar estado de la orden de trabajo", description = "Avanza el estado de la orden (CREADA -> ASIGNADA -> EN_DESPLAZAMIENTO -> EN_EJECUCION -> CERRADA / CANCELADA). Requiere que exista al menos un técnico asignado en los servicios para poder avanzar a ASIGNADA.")
    public ResponseEntity<OrdenTrabajoResponse> cambiarEstado(
            @PathVariable @Positive Long id, @Valid @RequestBody CambioEstadoRequest request) {
        return ResponseEntity.ok(service.cambiarEstado(id, request));
    }

    @PostMapping("/{id}/servicios")
    @Operation(summary = "Agregar servicio a la orden de trabajo", description = "Registra una labor técnica con técnico asignado, descripción y precio de mano de obra.")
    public ResponseEntity<OrdenTrabajoResponse> agregarServicio(
            @PathVariable @Positive Long id, @Valid @RequestBody DetalleServicioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.agregarServicio(id, request));
    }

    @PutMapping("/{id}/servicios/{idDetalle}/tecnico")
    @Operation(summary = "Asignar técnico a un servicio", description = "Asigna o reasigna un técnico especialista al servicio técnico de la orden de trabajo.")
    public ResponseEntity<OrdenTrabajoResponse> asignarTecnico(
            @PathVariable @Positive Long id, @PathVariable @Positive Long idDetalle,
            @Valid @RequestBody AsignarTecnicoRequest request) {
        return ResponseEntity.ok(service.asignarTecnico(id, idDetalle, request));
    }

    @DeleteMapping("/{id}/servicios/{idDetalle}")
    @Operation(summary = "Quitar servicio", description = "Elimina un servicio técnico de una orden de trabajo editable.")
    public ResponseEntity<OrdenTrabajoResponse> quitarServicio(
            @PathVariable @Positive Long id, @PathVariable @Positive Long idDetalle) {
        return ResponseEntity.ok(service.quitarServicio(id, idDetalle));
    }

    @PostMapping("/{id}/repuestos")
    @Operation(summary = "Agregar repuesto de proveedor", description = "Registra repuestos utilizados en terreno vinculados a un proveedor específico.")
    public ResponseEntity<OrdenTrabajoResponse> agregarRepuesto(
            @PathVariable @Positive Long id, @Valid @RequestBody DetalleRepuestoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.agregarRepuesto(id, request));
    }

    @DeleteMapping("/{id}/repuestos/{idDetalle}")
    @Operation(summary = "Quitar repuesto", description = "Remueve un repuesto registrado de una orden de trabajo editable.")
    public ResponseEntity<OrdenTrabajoResponse> quitarRepuesto(
            @PathVariable @Positive Long id, @PathVariable @Positive Long idDetalle) {
        return ResponseEntity.ok(service.quitarRepuesto(id, idDetalle));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar orden de trabajo", description = "Elimina una orden de trabajo únicamente si está en estado inicial CREADA.")
    public ResponseEntity<Void> eliminar(@PathVariable @Positive Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
