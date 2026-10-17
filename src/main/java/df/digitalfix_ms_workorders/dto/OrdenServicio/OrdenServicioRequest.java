package df.digitalfix_ms_workorders.dto.OrdenServicio;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record OrdenServicioRequest(
        @NotNull(message = "El cliente es obligatorio")
        @Positive(message = "El ID del cliente no es válido")
        Long idCliente,

        @Size(max = 500, message = "Las observaciones no pueden exceder los 500 caracteres")
        String observaciones,

        @Size(max = 50, message = "No se pueden registrar más de 50 detalles de servicio por solicitud")
        List<@Valid @NotNull DetalleOrdenServicioRequest> detalles
) {
}
