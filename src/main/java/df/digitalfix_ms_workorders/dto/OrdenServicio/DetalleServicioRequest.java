package df.digitalfix_ms_workorders.dto.OrdenServicio;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record DetalleServicioRequest(

        @NotNull(message = "El servicio es obligatorio")
        @Positive(message = "El ID del servicio no es válido")
        Long idServicio,

        @Positive(message = "El ID del técnico no es válido")
        Long idTecnico,

        @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
        String descripcionManoObra,

        @NotNull(message = "El precio de mano de obra es obligatorio")
        @DecimalMin(value = "0.00", message = "El precio no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "El precio admite hasta 10 enteros y 2 decimales")
        BigDecimal precioManoObra
) {
}
