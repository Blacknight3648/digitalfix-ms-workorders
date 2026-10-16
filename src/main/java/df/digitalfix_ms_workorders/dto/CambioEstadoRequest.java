package df.digitalfix_ms_workorders.dto;

import df.digitalfix_ms_workorders.enums.EstadoOrden;
import jakarta.validation.constraints.NotNull;

public record CambioEstadoRequest(

        @NotNull(message = "El estado es obligatorio")
        EstadoOrden estado
) {
}
