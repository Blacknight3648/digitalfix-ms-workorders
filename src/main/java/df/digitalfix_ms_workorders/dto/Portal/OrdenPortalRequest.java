package df.digitalfix_ms_workorders.dto.Portal;

import df.digitalfix_ms_workorders.dto.Ubicacion.DireccionRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

// Pedido confirmado en el Portal Cliente que se convierte en una Orden de Trabajo
// (POST /api/externo/ordenes-trabajo).
//
// - El cliente se identifica por RUT: si ya existe en el ERP se reutiliza (sus datos no se
//   modifican desde el portal); si no existe, se crea con estos datos y la dirección.
// - Los servicios se registran en la OT con la tarifa vigente del catálogo (el precio no se recibe).
// - Los repuestos NO se registran como detalle: el ERP exige el proveedor que abastece cada uno y lo
//   elige el supervisor al operar la orden. Quedan listados en las observaciones de la OT, junto al
//   token de reserva de stock que el portal obtuvo en ms-catalog.
public record OrdenPortalRequest(

        @NotBlank(message = "El código del pedido del portal es obligatorio")
        @Size(max = 30, message = "El código del pedido no puede exceder 30 caracteres")
        String codigoPedido,

        @NotNull(message = "Los datos del cliente son obligatorios")
        @Valid
        ClientePortal cliente,

        // Obligatoria si el cliente no existe todavía en el ERP.
        @Valid
        DireccionRequest direccion,

        @Size(max = 20, message = "No se pueden solicitar más de 20 servicios por pedido")
        List<@NotNull @Positive(message = "El ID del servicio no es válido") Long> servicios,

        @Size(max = 30, message = "No se pueden solicitar más de 30 repuestos por pedido")
        List<@Valid @NotNull RepuestoSolicitado> repuestos,

        @Size(max = 120, message = "El horario preferido no puede exceder 120 caracteres")
        String horarioPreferido,

        @Size(max = 400, message = "Las notas no pueden exceder 400 caracteres")
        String notas,

        @Size(max = 40, message = "El token de reserva no puede exceder 40 caracteres")
        String tokenReservaStock
) {

    public record ClientePortal(
            @NotBlank(message = "El RUT es obligatorio")
            String rut,

            @NotBlank(message = "El nombre es obligatorio")
            @Size(max = 150, message = "El nombre no puede exceder 150 caracteres")
            String nombre,

            @NotBlank(message = "El teléfono es obligatorio")
            String telefono,

            @NotBlank(message = "El correo es obligatorio")
            @Email(message = "El correo no tiene un formato válido")
            String email
    ) {
    }

    public record RepuestoSolicitado(
            @NotNull(message = "El repuesto es obligatorio")
            @Positive(message = "El ID del repuesto no es válido")
            Long idRepuesto,

            @Size(max = 30, message = "El código del repuesto no puede exceder 30 caracteres")
            String codigo,

            @Size(max = 120, message = "El nombre del repuesto no puede exceder 120 caracteres")
            String nombre,

            @NotNull(message = "La cantidad es obligatoria")
            @Positive(message = "La cantidad debe ser mayor a cero")
            @Max(value = 9_999, message = "La cantidad excede el máximo permitido")
            Integer cantidad
    ) {
    }
}
