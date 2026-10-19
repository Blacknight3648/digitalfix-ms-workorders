package df.digitalfix_ms_workorders.dto.Ubicacion;

import df.digitalfix_ms_workorders.model.ubicacion.Comuna;

public record ComunaResponse(Long idComuna, String nombre) {

    public static ComunaResponse from(Comuna c) {
        return new ComunaResponse(c.getIdComuna(), c.getNombre());
    }
}
