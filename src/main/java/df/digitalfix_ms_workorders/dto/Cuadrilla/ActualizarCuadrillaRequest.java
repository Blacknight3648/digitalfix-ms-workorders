package df.digitalfix_ms_workorders.dto.Cuadrilla;

import df.digitalfix_ms_workorders.enums.EstadoCuadrilla;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ActualizarCuadrillaRequest(
        @NotBlank(message = "El nombre de la cuadrilla es obligatorio")
        @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
        String nombre,

        @NotNull(message = "El técnico líder es obligatorio")
        @Positive(message = "El ID del técnico líder no es válido")
        Long idTecnicoLider,

        @NotNull(message = "El estado de la cuadrilla es obligatorio")
        EstadoCuadrilla estado
) {
}
