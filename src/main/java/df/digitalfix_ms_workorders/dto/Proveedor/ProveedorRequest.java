package df.digitalfix_ms_workorders.dto.Proveedor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ProveedorRequest(
        @NotBlank(message = "El RUT del proveedor es obligatorio")
        String rut,

        @NotBlank(message = "La razón social es obligatoria")
        @Size(max = 150, message = "La razón social no puede exceder 150 caracteres")
        String razonSocial,

        @Size(max = 150, message = "El giro no puede exceder 150 caracteres")
        String giro,

        @NotBlank(message = "El teléfono es obligatorio")
        String telefono,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 254, message = "El correo no puede exceder 254 caracteres")
        String email,

        @Positive(message = "El ID de dirección no es válido")
        Long idDireccion
) {
}
