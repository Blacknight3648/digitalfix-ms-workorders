package df.digitalfix_ms_workorders.dto.OrdenTrabajo;

import jakarta.validation.constraints.Size;

// Body de PUT /api/ordenes-trabajo/{id}. El cliente no se cambia: una OT pertenece siempre al mismo.
public record ActualizarOrdenTrabajoRequest(

        @Size(max = 1000, message = "Las observaciones no pueden exceder 1000 caracteres")
        String observaciones
) {
}
