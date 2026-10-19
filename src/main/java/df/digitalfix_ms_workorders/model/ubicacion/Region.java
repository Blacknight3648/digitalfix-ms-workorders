package df.digitalfix_ms_workorders.model.ubicacion;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Región de Chile (catálogo fijo, sembrado por la migración V3). El ID es el código CUT de la
 * región (ej. 13 = Metropolitana), por eso no es autogenerado. Solo lectura desde la aplicación.
 */
@Entity
@Table(name = "REGION")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Region {

    @Id
    @Column(name = "ID_REGION", nullable = false, updatable = false)
    private Long idRegion;

    @Column(name = "NOMBRE_REGION", nullable = false, updatable = false, length = 100)
    private String nombre;

    @Column(name = "NUMERO_ROMANO", nullable = false, updatable = false, length = 5)
    private String numeroRomano;

    /** Posición de norte a sur, para listar las regiones en el orden habitual. */
    @Column(name = "ORDEN_GEOGRAFICO", nullable = false, updatable = false)
    private Integer ordenGeografico;
}
