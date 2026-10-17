package df.digitalfix_ms_workorders.dto.Cliente;

import df.digitalfix_ms_workorders.model.clientes.Cliente;

import java.time.LocalDateTime;

public record ClienteResponse(
        Long idCliente,
        String rut,
        String nombres,
        String apellidoPaterno,
        String apellidoMaterno,
        String nombreCompleto,
        String telefono,
        String telefonoSecundario,
        String email,
        Long idDireccion,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion
) {
    public static ClienteResponse from(Cliente c) {
        return new ClienteResponse(
                c.getIdCliente(),
                c.getRut().formateado(),
                c.getNombres(),
                c.getApellidoPaterno(),
                c.getApellidoMaterno(),
                c.getNombreCompleto(),
                c.getTelefono(),
                c.getTelefonoSecundario(),
                c.getEmail(),
                c.getIdDireccion(),
                c.getFechaCreacion(),
                c.getFechaActualizacion()
        );
    }
}
