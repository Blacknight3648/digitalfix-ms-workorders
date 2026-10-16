package df.digitalfix_ms_workorders.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

// Body para crear una OT (POST /api/ordenes-trabajo). El estado no se recibe: siempre parte en
// CREADA y se cambia con PUT /{id}/estado. Los servicios iniciales son opcionales.
public record OrdenTrabajoRequest(

        @NotNull(message = "El cliente es obligatorio")
        @Positive(message = "El ID del cliente no es válido")
        Long idCliente,

        @Size(max = 1000, message = "Las observaciones no pueden exceder 1000 caracteres")
        String observaciones,

        @Size(max = 50, message = "No se pueden registrar más de 50 servicios por solicitud")
        List<@Valid @NotNull DetalleServicioRequest> servicios
) {
}
