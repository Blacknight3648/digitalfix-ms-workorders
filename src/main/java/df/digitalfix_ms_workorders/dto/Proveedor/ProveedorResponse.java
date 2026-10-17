package df.digitalfix_ms_workorders.dto.Proveedor;

import df.digitalfix_ms_workorders.model.proveedores.Proveedor;

import java.time.LocalDateTime;

public record ProveedorResponse(
        Long idProveedor,
        String rut,
        String razonSocial,
        String giro,
        String telefono,
        String email,
        Long idDireccion,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion
) {
    public static ProveedorResponse from(Proveedor p) {
        return new ProveedorResponse(
                p.getIdProveedor(),
                p.getRut().formateado(),
                p.getRazonSocial(),
                p.getGiro(),
                p.getTelefono(),
                p.getEmail(),
                p.getIdDireccion(),
                p.getFechaCreacion(),
                p.getFechaActualizacion()
        );
    }
}
