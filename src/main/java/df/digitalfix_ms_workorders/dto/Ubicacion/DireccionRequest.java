package df.digitalfix_ms_workorders.dto.Ubicacion;

import df.digitalfix_ms_workorders.model.common.PatronesValidacion;
import jakarta.validation.constraints.*;

// Dirección con el formato habitual en Chile. La región no se recibe: se deduce de la comuna (3FN).
public record DireccionRequest(
        @NotNull(message = "La comuna es obligatoria")
        @Positive(message = "La comuna no es válida")
        Long idComuna,

        @NotBlank(message = "La calle es obligatoria")
        @Size(max = 120, message = "La calle no puede exceder 120 caracteres")
        @Pattern(regexp = PatronesValidacion.CALLE, message = "La calle contiene caracteres no permitidos")
        String calle,

        @NotBlank(message = "El número es obligatorio")
        @Pattern(regexp = "(?i)^\\s*(\\d{1,6}\\s*[A-Z]?|S\\s*/?\\s*N)\\s*$",
                message = "El número debe ser numérico (ej. 1234 o 1234B) o S/N")
        String numero,

        @Size(max = 60, message = "El detalle no puede exceder 60 caracteres")
        @Pattern(regexp = PatronesValidacion.CALLE, message = "El detalle contiene caracteres no permitidos")
        String detalle,

        @Size(max = 200, message = "La referencia no puede exceder 200 caracteres")
        String referencia,

        @Pattern(regexp = "^\\s*\\d{3}-?\\d{4}\\s*$", message = "El código postal debe tener 7 dígitos")
        String codigoPostal
) {
}
