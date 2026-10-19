package df.digitalfix_ms_workorders.dto.Servicio;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

// El código no se recibe: es el identificador estable del servicio y no cambia.
public record ActualizarServicioRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
        String nombre,

        @Size(max = 255, message = "La descripción no puede exceder 255 caracteres")
        String descripcion,

        @NotNull(message = "La tarifa es obligatoria")
        @DecimalMin(value = "0.00", message = "La tarifa no puede ser negativa")
        @Digits(integer = 10, fraction = 2, message = "La tarifa admite hasta 10 enteros y 2 decimales")
        BigDecimal tarifa,

        @NotNull(message = "Indique si el servicio está activo")
        Boolean activo
) {
}
