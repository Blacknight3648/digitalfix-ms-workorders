package df.digitalfix_ms_workorders.dto.Ubicacion;

import df.digitalfix_ms_workorders.model.ubicacion.Region;

public record RegionResponse(Long idRegion, String nombre, String numeroRomano) {

    public static RegionResponse from(Region r) {
        return new RegionResponse(r.getIdRegion(), r.getNombre(), r.getNumeroRomano());
    }
}
