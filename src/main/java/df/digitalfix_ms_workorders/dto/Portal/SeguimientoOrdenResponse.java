package df.digitalfix_ms_workorders.dto.Portal;

import df.digitalfix_ms_workorders.enums.EstadoOrden;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// Vista de una OT para el cliente final: solo hitos comprensibles para él, sin observaciones
// internas, técnicos, proveedores ni IDs operativos.
public record SeguimientoOrdenResponse(
        Long idOrdenTrabajo,
        String codigoSeguimiento,
        EstadoOrden estado,
        String estadoEtiqueta,
        LocalDateTime fechaIngreso,
        LocalDateTime fechaActualizacionEstado,
        LocalDateTime fechaCierre,
        String cuadrilla,
        List<Hito> hitos,
        List<ServicioContratado> servicios,
        BigDecimal totalManoObra
) {

    public record Hito(EstadoOrden estado, String etiqueta, String descripcion, boolean completado, boolean actual) {
    }

    public record ServicioContratado(Long idServicio, String nombre, BigDecimal precio) {
    }
}
