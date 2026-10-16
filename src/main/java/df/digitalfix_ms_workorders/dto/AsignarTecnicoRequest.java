package df.digitalfix_ms_workorders.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AsignarTecnicoRequest(

        @NotNull(message = "El técnico es obligatorio")
        @Positive(message = "El ID del técnico no es válido")
        Long idTecnico
) {
}
