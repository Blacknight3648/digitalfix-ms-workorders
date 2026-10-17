package df.digitalfix_ms_workorders.dto.Cuadrilla;

import df.digitalfix_ms_workorders.enums.EstadoCuadrilla;
import df.digitalfix_ms_workorders.model.cuadrilla.Cuadrilla;

import java.time.LocalDateTime;
import java.util.Set;

public record CuadrillaResponse(
        Long idCuadrilla,
        String nombre,
        Long idTecnicoLider,
        EstadoCuadrilla estado,
        Set<Long> miembros,
        int cantidadMiembros,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion
) {
    public static CuadrillaResponse from(Cuadrilla c) {
        return new CuadrillaResponse(
                c.getIdCuadrilla(),
                c.getNombre(),
                c.getIdTecnicoLider(),
                c.getEstado(),
                c.getMiembros(),
                c.getMiembros().size(),
                c.getFechaCreacion(),
                c.getFechaActualizacion()
        );
    }
}
