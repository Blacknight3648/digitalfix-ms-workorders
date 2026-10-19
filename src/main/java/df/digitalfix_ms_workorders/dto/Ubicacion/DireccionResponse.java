package df.digitalfix_ms_workorders.dto.Ubicacion;

import df.digitalfix_ms_workorders.model.ubicacion.Direccion;

// Incluye comuna y región resueltas para mostrarlas sin otra consulta, y el texto completo
// que el frontend usa para abrir la ubicación en Google Maps.
public record DireccionResponse(
        Long idDireccion,
        Long idComuna,
        String comuna,
        Long idRegion,
        String region,
        String calle,
        String numero,
        String detalle,
        String referencia,
        String codigoPostal,
        String textoCompleto
) {
    public static DireccionResponse from(Direccion d) {
        if (d == null) {
            return null;
        }
        return new DireccionResponse(
                d.getIdDireccion(),
                d.getComuna().getIdComuna(),
                d.getComuna().getNombre(),
                d.getComuna().getRegion().getIdRegion(),
                d.getComuna().getRegion().getNombre(),
                d.getCalle(),
                d.getNumero(),
                d.getDetalle(),
                d.getReferencia(),
                d.getCodigoPostal(),
                d.textoCompleto()
        );
    }
}
