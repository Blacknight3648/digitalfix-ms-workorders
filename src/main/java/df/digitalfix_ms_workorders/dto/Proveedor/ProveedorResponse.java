package df.digitalfix_ms_workorders.dto.Proveedor;

import df.digitalfix_ms_workorders.dto.Ubicacion.DireccionResponse;
import df.digitalfix_ms_workorders.enums.TipoProveedor;
import df.digitalfix_ms_workorders.model.proveedores.Proveedor;

import java.time.LocalDateTime;

public record ProveedorResponse(
        Long idProveedor,
        String rut,
        String razonSocial,
        TipoProveedor tipo,
        String giro,
        String telefono,
        String email,
        DireccionResponse direccion,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion
) {
    public static ProveedorResponse from(Proveedor p) {
        return new ProveedorResponse(
                p.getIdProveedor(),
                p.getRut().formateado(),
                p.getRazonSocial(),
                p.getTipo(),
                p.getGiro(),
                p.getTelefono(),
                p.getEmail(),
                DireccionResponse.from(p.getDireccion()),
                p.getFechaCreacion(),
                p.getFechaActualizacion()
        );
    }
}
