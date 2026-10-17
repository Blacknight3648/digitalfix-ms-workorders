package df.digitalfix_ms_workorders.dto.Cliente;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ActualizarClienteRequest(
        @NotBlank(message = "Los nombres son obligatorios")
        @Size(max = 100, message = "Los nombres no pueden exceder 100 caracteres")
        String nombres,

        @NotBlank(message = "El apellido paterno es obligatorio")
        @Size(max = 60, message = "El apellido paterno no puede exceder 60 caracteres")
        String apellidoPaterno,

        @Size(max = 60, message = "El apellido materno no puede exceder 60 caracteres")
        String apellidoMaterno,

        @NotBlank(message = "El teléfono es obligatorio")
        String telefono,

        String telefonoSecundario,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 254, message = "El correo no puede exceder 254 caracteres")
        String email,

        @Positive(message = "El ID de dirección no es válido")
        Long idDireccion
) {
}
