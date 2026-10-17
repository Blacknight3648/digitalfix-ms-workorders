package df.digitalfix_ms_workorders.dto.Cuadrilla;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AsignarCuadrillaRequest(
        @NotNull(message = "El ID de la cuadrilla es obligatorio")
        @Positive(message = "El ID de la cuadrilla no es válido")
        Long idCuadrilla
) {
}
