package df.digitalfix_ms_workorders.dto.OrdenServicio;

import jakarta.validation.constraints.Size;

public record ActualizarOrdenServicioRequest(
        @Size(max = 500, message = "Las observaciones no pueden exceder los 500 caracteres")
        String observaciones
) {
}
