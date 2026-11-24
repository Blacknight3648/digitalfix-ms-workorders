package df.digitalfix_ms_workorders.controller;

import df.digitalfix_ms_workorders.dto.Portal.OrdenPortalRequest;
import df.digitalfix_ms_workorders.dto.Portal.SeguimientoOrdenResponse;
import df.digitalfix_ms_workorders.dto.Portal.ServicioPortalResponse;
import df.digitalfix_ms_workorders.dto.Ubicacion.ComunaResponse;
import df.digitalfix_ms_workorders.dto.Ubicacion.RegionResponse;
import df.digitalfix_ms_workorders.service.PortalExternoService;
import df.digitalfix_ms_workorders.service.UbicacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// API para el backend del Portal Cliente (servicio a servicio, header X-Portal-Api-Key).
// Las órdenes creadas aquí son OT normales: aparecen en el portal de empleados en estado CREADA
// y el supervisor las opera igual que cualquier otra.
@Validated
@RestController
@RequestMapping("/api/externo")
@RequiredArgsConstructor
@Tag(name = "Portal Cliente (externo)", description = "Integración servicio a servicio con el backend del Portal Cliente")
public class PortalExternoController {

    private final PortalExternoService portalExternoService;
    private final UbicacionService ubicacionService;

    @GetMapping("/servicios")
    @Operation(summary = "Servicios activos que el cliente puede contratar")
    public ResponseEntity<List<ServicioPortalResponse>> servicios() {
        return ResponseEntity.ok(portalExternoService.serviciosActivos());
    }

    @GetMapping("/regiones")
    @Operation(summary = "Regiones de Chile")
    public ResponseEntity<List<RegionResponse>> regiones() {
        return ResponseEntity.ok(ubicacionService.listarRegiones());
    }

    @GetMapping("/regiones/{idRegion}/comunas")
    @Operation(summary = "Comunas de una región")
    public ResponseEntity<List<ComunaResponse>> comunas(@PathVariable @Positive Long idRegion) {
        return ResponseEntity.ok(ubicacionService.listarComunas(idRegion));
    }

    @PostMapping("/ordenes-trabajo")
    @Operation(summary = "Crear una OT desde un pedido del portal",
            description = "Reutiliza al cliente por RUT o lo crea con la dirección indicada. La OT parte en CREADA.")
    public ResponseEntity<SeguimientoOrdenResponse> crearOrden(@Valid @RequestBody OrdenPortalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(portalExternoService.crearOrden(request));
    }

    @GetMapping("/ordenes-trabajo")
    @Operation(summary = "Órdenes de un cliente (por RUT) con su seguimiento")
    public ResponseEntity<List<SeguimientoOrdenResponse>> ordenesDelCliente(@RequestParam @NotBlank String rut) {
        return ResponseEntity.ok(portalExternoService.ordenesDelCliente(rut));
    }

    @GetMapping("/ordenes-trabajo/{id}/seguimiento")
    @Operation(summary = "Seguimiento de una OT para el cliente",
            description = "Solo hitos visibles para el cliente. 404 si la orden no pertenece al RUT indicado.")
    public ResponseEntity<SeguimientoOrdenResponse> seguimiento(@PathVariable @Positive Long id,
                                                                @RequestParam @NotBlank String rut) {
        return ResponseEntity.ok(portalExternoService.seguimiento(id, rut));
    }
}
