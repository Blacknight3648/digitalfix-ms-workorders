package df.digitalfix_ms_workorders.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DetalleRepuestoRequest(

        @NotNull(message = "El repuesto es obligatorio")
        @Positive(message = "El ID del repuesto no es válido")
        Long idRepuesto,

        @NotNull(message = "El proveedor es obligatorio")
        @Positive(message = "El ID del proveedor no es válido")
        Long idProveedor,

        @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "La cantidad debe ser mayor a cero")
        @Max(value = 9_999, message = "La cantidad excede el máximo permitido")
        Integer cantidad
) {
}
