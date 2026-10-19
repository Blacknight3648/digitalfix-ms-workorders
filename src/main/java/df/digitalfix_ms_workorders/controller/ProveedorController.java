package df.digitalfix_ms_workorders.controller;

import df.digitalfix_ms_workorders.dto.Proveedor.ActualizarProveedorRequest;
import df.digitalfix_ms_workorders.dto.Proveedor.ProveedorRequest;
import df.digitalfix_ms_workorders.dto.Proveedor.ProveedorResponse;
import df.digitalfix_ms_workorders.enums.TipoProveedor;
import df.digitalfix_ms_workorders.service.ProveedorService;
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
@RequestMapping("/api/proveedores")
@RequiredArgsConstructor
@Tag(name = "Proveedores", description = "Operaciones para la administración de proveedores de repuestos e insumos")
public class ProveedorController {

    private final ProveedorService proveedorService;

    @PostMapping
    @Operation(summary = "Crear un nuevo proveedor", description = "Registra un proveedor comercial validando su RUT con dígito verificador.")
    public ResponseEntity<ProveedorResponse> crear(@Valid @RequestBody ProveedorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(proveedorService.crear(request));
    }

    @GetMapping
    @Operation(summary = "Listar proveedores",
            description = "Retorna los proveedores ordenados por razón social; con tipo=SERVICIOS o tipo=PRODUCTOS filtra por tipo.")
    public ResponseEntity<List<ProveedorResponse>> listar(@RequestParam(required = false) TipoProveedor tipo) {
        return ResponseEntity.ok(proveedorService.listar(tipo));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar proveedor por ID", description = "Retorna los datos comerciales y de contacto de un proveedor.")
    public ResponseEntity<ProveedorResponse> buscarPorId(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(proveedorService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar proveedor", description = "Modifica la razón social, giro, teléfono, email o dirección del proveedor.")
    public ResponseEntity<ProveedorResponse> actualizar(
            @PathVariable @Positive Long id,
            @Valid @RequestBody ActualizarProveedorRequest request) {
        return ResponseEntity.ok(proveedorService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar proveedor", description = "Elimina un proveedor si no tiene repuestos asignados en órdenes de trabajo.")
    public ResponseEntity<Void> eliminar(@PathVariable @Positive Long id) {
        proveedorService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
