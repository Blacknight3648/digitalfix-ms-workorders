package df.digitalfix_ms_workorders.dto.Cliente;

import df.digitalfix_ms_workorders.dto.Ubicacion.DireccionResponse;
import df.digitalfix_ms_workorders.model.clientes.Cliente;

import java.time.LocalDateTime;

public record ClienteResponse(
        Long idCliente,
        String rut,
        String nombreCompleto,
        String telefono,
        String telefonoSecundario,
        String email,
        DireccionResponse direccion,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion
) {
    public static ClienteResponse from(Cliente c) {
        return new ClienteResponse(
                c.getIdCliente(),
                c.getRut().formateado(),
                c.getNombreCompleto(),
                c.getTelefono(),
                c.getTelefonoSecundario(),
                c.getEmail(),
                DireccionResponse.from(c.getDireccion()),
                c.getFechaCreacion(),
                c.getFechaActualizacion()
        );
    }
}
