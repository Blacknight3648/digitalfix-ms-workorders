package df.digitalfix_ms_workorders.model.ubicacion;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Comuna de Chile (catálogo fijo, sembrado por la migración V3). La región se obtiene a través de
 * la comuna: DIRECCION guarda solo ID_COMUNA para no repetir la región (3FN).
 */
@Entity
@Table(name = "COMUNA",
        uniqueConstraints = @UniqueConstraint(name = "UK_COMUNA_REGION_NOMBRE", columnNames = {"ID_REGION", "NOMBRE_COMUNA"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comuna {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_COMUNA", nullable = false, updatable = false)
    private Long idComuna;

    @Column(name = "NOMBRE_COMUNA", nullable = false, updatable = false, length = 60)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_REGION", nullable = false, updatable = false,
            foreignKey = @ForeignKey(name = "FK_COMUNA_REGION"))
    private Region region;
}
