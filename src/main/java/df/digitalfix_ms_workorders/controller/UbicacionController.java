package df.digitalfix_ms_workorders.controller;

import df.digitalfix_ms_workorders.dto.Ubicacion.ComunaResponse;
import df.digitalfix_ms_workorders.dto.Ubicacion.RegionResponse;
import df.digitalfix_ms_workorders.service.UbicacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/regiones")
@RequiredArgsConstructor
@Tag(name = "Ubicaciones", description = "Regiones y comunas de Chile para las direcciones de clientes y proveedores")
public class UbicacionController {

    private final UbicacionService ubicacionService;

    @GetMapping
    @Operation(summary = "Listar regiones", description = "Las 16 regiones de Chile, de norte a sur.")
    public ResponseEntity<List<RegionResponse>> listarRegiones() {
        return ResponseEntity.ok(ubicacionService.listarRegiones());
    }

    @GetMapping("/{idRegion}/comunas")
    @Operation(summary = "Listar comunas de una región", description = "Comunas de la región, en orden alfabético.")
    public ResponseEntity<List<ComunaResponse>> listarComunas(@PathVariable @Positive Long idRegion) {
        return ResponseEntity.ok(ubicacionService.listarComunas(idRegion));
    }
}
