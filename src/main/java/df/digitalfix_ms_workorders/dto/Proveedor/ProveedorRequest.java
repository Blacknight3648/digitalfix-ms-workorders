package df.digitalfix_ms_workorders.dto.Proveedor;

import df.digitalfix_ms_workorders.dto.Ubicacion.DireccionRequest;
import df.digitalfix_ms_workorders.enums.TipoProveedor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProveedorRequest(
        @NotBlank(message = "El RUT del proveedor es obligatorio")
        String rut,

        @NotBlank(message = "La razón social es obligatoria")
        @Size(max = 150, message = "La razón social no puede exceder 150 caracteres")
        String razonSocial,

        @NotNull(message = "El tipo de proveedor es obligatorio (SERVICIOS o PRODUCTOS)")
        TipoProveedor tipo,

        @Size(max = 150, message = "El giro no puede exceder 150 caracteres")
        String giro,

        @NotBlank(message = "El teléfono es obligatorio")
        String telefono,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 254, message = "El correo no puede exceder 254 caracteres")
        String email,

        @NotNull(message = "La dirección es obligatoria")
        @Valid
        DireccionRequest direccion
) {
}
