package df.digitalfix_ms_workorders.controller;

import df.digitalfix_ms_workorders.dto.Cuadrilla.ActualizarCuadrillaRequest;
import df.digitalfix_ms_workorders.dto.Cuadrilla.CuadrillaRequest;
import df.digitalfix_ms_workorders.dto.Cuadrilla.CuadrillaResponse;
import df.digitalfix_ms_workorders.enums.EstadoCuadrilla;
import df.digitalfix_ms_workorders.service.CuadrillaService;
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
@RequestMapping("/api/cuadrillas")
@RequiredArgsConstructor
@Tag(name = "Cuadrillas", description = "Coordinación y gestión de cuadrillas de trabajadores para faenas de mantención en terreno")
public class CuadrillaController {

    private final CuadrillaService cuadrillaService;

    @PostMapping
    @Operation(summary = "Crear cuadrilla de terreno",
            description = "Registra una nueva cuadrilla con su técnico líder y miembros iniciales. Inicia en estado DISPONIBLE.")
    public ResponseEntity<CuadrillaResponse> crear(@Valid @RequestBody CuadrillaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cuadrillaService.crear(request));
    }

    @GetMapping
    @Operation(summary = "Listar cuadrillas",
            description = "Retorna el listado de cuadrillas, permitiendo filtrar por su estado operativo (DISPONIBLE, EN_TERRENO, INACTIVA).")
    public ResponseEntity<List<CuadrillaResponse>> listar(@RequestParam(required = false) EstadoCuadrilla estado) {
        return ResponseEntity.ok(cuadrillaService.listar(estado));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar cuadrilla por ID",
            description = "Retorna el detalle de una cuadrilla específica con todos sus integrantes y líder técnico.")
    public ResponseEntity<CuadrillaResponse> buscarPorId(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(cuadrillaService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar cuadrilla",
            description = "Modifica el nombre, técnico líder o estado de la cuadrilla (no permite desactivar si está en terreno).")
    public ResponseEntity<CuadrillaResponse> actualizar(
            @PathVariable @Positive Long id,
            @Valid @RequestBody ActualizarCuadrillaRequest request) {
        return ResponseEntity.ok(cuadrillaService.actualizar(id, request));
    }

    @PostMapping("/{id}/miembros/{idTecnico}")
    @Operation(summary = "Agregar técnico a la cuadrilla",
            description = "Incorpora un nuevo trabajador o técnico de apoyo a la cuadrilla.")
    public ResponseEntity<CuadrillaResponse> agregarMiembro(
            @PathVariable @Positive Long id,
            @PathVariable @Positive Long idTecnico) {
        return ResponseEntity.ok(cuadrillaService.agregarMiembro(id, idTecnico));
    }

    @DeleteMapping("/{id}/miembros/{idTecnico}")
    @Operation(summary = "Quitar técnico de la cuadrilla",
            description = "Remueve un integrante de la cuadrilla (no permite remover al líder sin reasignarlo).")
    public ResponseEntity<CuadrillaResponse> quitarMiembro(
            @PathVariable @Positive Long id,
            @PathVariable @Positive Long idTecnico) {
        return ResponseEntity.ok(cuadrillaService.quitarMiembro(id, idTecnico));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar cuadrilla",
            description = "Elimina la cuadrilla únicamente si no se encuentra actualmente en terreno.")
    public ResponseEntity<Void> eliminar(@PathVariable @Positive Long id) {
        cuadrillaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
