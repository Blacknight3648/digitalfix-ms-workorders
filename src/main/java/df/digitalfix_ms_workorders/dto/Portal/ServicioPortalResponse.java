package df.digitalfix_ms_workorders.dto.Portal;

import df.digitalfix_ms_workorders.dto.Servicio.ServicioResponse;

import java.math.BigDecimal;

// Servicio del catálogo tal como lo ve el Portal Cliente (sin fechas ni datos de mantención).
public record ServicioPortalResponse(Long idServicio, String codigo, String nombre, String descripcion, BigDecimal tarifa) {

    public static ServicioPortalResponse from(ServicioResponse s) {
        return new ServicioPortalResponse(s.idServicio(), s.codigo(), s.nombre(), s.descripcion(), s.tarifa());
    }
}
