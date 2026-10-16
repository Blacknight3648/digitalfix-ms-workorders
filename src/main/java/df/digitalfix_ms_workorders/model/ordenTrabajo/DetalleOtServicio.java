package df.digitalfix_ms_workorders.model.ordenTrabajo;

import df.digitalfix_ms_workorders.model.common.SanitizadorTexto;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Servicio (mano de obra) realizado dentro de una OT.
 * <p>
 * 3FN: no se copia NA_SERVICIO ni el nombre del servicio, que dependen de SERVICIO y no de este
 * detalle. El precio sí se guarda porque es el valor cobrado en esta OT (histórico), no el oficial.
 * SERVICIO y el técnico (USUARIO) viven en otros microservicios, por eso se referencian por ID.
 */
@Entity
@Table(name = "DETALLE_OT_SERVICIO",
        uniqueConstraints = @UniqueConstraint(name = "UK_DET_OT_SERVICIO", columnNames = {"ID_OT", "ID_SERVICIO"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DetalleOtServicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_DETALLE_OT_SERVICIO", nullable = false, updatable = false)
    private Long idDetalleOtServicio;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_OT", nullable = false, updatable = false,
            foreignKey = @ForeignKey(name = "FK_DET_OT_SERVICIO_OT"))
    private OrdenTrabajo ordenTrabajo;

    @NotNull(message = "El servicio es obligatorio")
    @Positive(message = "El ID del servicio no es válido")
    @Column(name = "ID_SERVICIO", nullable = false, updatable = false)
    private Long idServicio;

    /** Técnico que ejecutó el servicio; puede asignarse después de crear el detalle. */
    @Positive(message = "El ID del técnico no es válido")
    @Column(name = "ID_TECNICO")
    private Long idTecnico;

    @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
    @Column(name = "DESCRIPCION_MANO_OBRA", length = 500)
    private String descripcionManoObra;

    @NotNull(message = "El precio de mano de obra es obligatorio")
    @DecimalMin(value = "0.00", message = "El precio no puede ser negativo")
    @Digits(integer = 10, fraction = 2, message = "El precio admite hasta 10 enteros y 2 decimales")
    @Column(name = "PRECIO_MANO_OBRA", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioManoObra;

    DetalleOtServicio(OrdenTrabajo ordenTrabajo, Long idServicio, Long idTecnico,
                      String descripcionManoObra, BigDecimal precioManoObra) {
        this.ordenTrabajo = Objects.requireNonNull(ordenTrabajo);
        this.idServicio = Objects.requireNonNull(idServicio, "El servicio es obligatorio");
        this.idTecnico = idTecnico;
        this.descripcionManoObra = SanitizadorTexto.textoMultilinea(descripcionManoObra);
        this.precioManoObra = Objects.requireNonNull(precioManoObra, "El precio de mano de obra es obligatorio");
    }

    void asignarTecnico(Long idTecnico) {
        this.idTecnico = Objects.requireNonNull(idTecnico, "El técnico es obligatorio");
    }
}
