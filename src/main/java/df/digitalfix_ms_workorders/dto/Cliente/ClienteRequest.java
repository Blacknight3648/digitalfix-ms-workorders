package df.digitalfix_ms_workorders.dto.Cliente;

import df.digitalfix_ms_workorders.dto.Ubicacion.DireccionRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Nombre en un solo campo (nombre completo o razón social) para que el registro sea simple.
public record ClienteRequest(
        @NotBlank(message = "El RUT es obligatorio")
        String rut,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede exceder 150 caracteres")
        String nombre,

        @NotBlank(message = "El teléfono es obligatorio")
        String telefono,

        String telefonoSecundario,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 254, message = "El correo no puede exceder 254 caracteres")
        String email,

        @NotNull(message = "La dirección es obligatoria")
        @Valid
        DireccionRequest direccion
) {
}
