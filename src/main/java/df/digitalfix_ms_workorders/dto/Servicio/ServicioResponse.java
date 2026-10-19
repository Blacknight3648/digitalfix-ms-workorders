package df.digitalfix_ms_workorders.dto.Servicio;

import df.digitalfix_ms_workorders.model.servicios.Servicio;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ServicioResponse(
        Long idServicio,
        String codigo,
        String nombre,
        String descripcion,
        BigDecimal tarifa,
        boolean activo,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion
) {
    public static ServicioResponse from(Servicio s) {
        return new ServicioResponse(
                s.getIdServicio(),
                s.getCodigo(),
                s.getNombre(),
                s.getDescripcion(),
                s.getTarifa(),
                s.isActivo(),
                s.getFechaCreacion(),
                s.getFechaActualizacion()
        );
    }
}
