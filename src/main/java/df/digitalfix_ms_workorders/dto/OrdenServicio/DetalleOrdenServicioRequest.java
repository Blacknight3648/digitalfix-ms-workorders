package df.digitalfix_ms_workorders.dto.OrdenServicio;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record DetalleOrdenServicioRequest(
        @NotNull(message = "El ID del servicio es obligatorio")
        @Positive(message = "El ID del servicio no es válido")
        Long idServicio,

        @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "La cantidad debe ser mayor a cero")
        @Max(value = 999, message = "La cantidad excede el máximo permitido")
        Integer cantidad,

        @NotNull(message = "El precio unitario es obligatorio")
        @DecimalMin(value = "0.00", message = "El precio no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "El precio admite hasta 10 enteros y 2 decimales")
        BigDecimal precioUnitario
) {
}
