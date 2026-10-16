package df.digitalfix_ms_workorders.model.ordenServicio;

import df.digitalfix_ms_workorders.model.common.DatoInvalidoException;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Línea de una orden de servicio. El precio unitario es el pactado en esta orden (histórico);
 * el subtotal no se persiste porque se deriva de cantidad × precio (3FN).
 */
@Entity
@Table(name = "DETALLE_ORDEN_SERVICIO",
        uniqueConstraints = @UniqueConstraint(name = "UK_DET_OS_SERVICIO",
                columnNames = {"ID_ORDEN_SERVICIO", "ID_SERVICIO"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DetalleOrdenServicio {

    static final int CANTIDAD_MAXIMA = 999;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_DETALLE_SERVICIO", nullable = false, updatable = false)
    private Long idDetalleServicio;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_ORDEN_SERVICIO", nullable = false, updatable = false,
            foreignKey = @ForeignKey(name = "FK_DET_OS_ORDEN"))
    private OrdenServicio ordenServicio;

    @NotNull(message = "El ID del servicio es obligatorio")
    @Positive(message = "El ID del servicio no es válido")
    @Column(name = "ID_SERVICIO", nullable = false, updatable = false)
    private Long idServicio;

    @NotNull(message = "La cantidad es obligatoria")
    @Positive(message = "La cantidad debe ser mayor a cero")
    @Max(value = CANTIDAD_MAXIMA, message = "La cantidad excede el máximo permitido")
    @Column(name = "CANTIDAD", nullable = false)
    private Integer cantidad;

    @NotNull(message = "El precio unitario es obligatorio")
    @DecimalMin(value = "0.00", message = "El precio no puede ser negativo")
    @Digits(integer = 10, fraction = 2, message = "El precio admite hasta 10 enteros y 2 decimales")
    @Column(name = "PRECIO_UNITARIO", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    DetalleOrdenServicio(OrdenServicio ordenServicio, Long idServicio, int cantidad, BigDecimal precioUnitario) {
        if (cantidad <= 0 || cantidad > CANTIDAD_MAXIMA) {
            throw new DatoInvalidoException("La cantidad debe estar entre 1 y " + CANTIDAD_MAXIMA);
        }
        this.ordenServicio = Objects.requireNonNull(ordenServicio);
        this.idServicio = Objects.requireNonNull(idServicio, "El ID del servicio es obligatorio");
        this.cantidad = cantidad;
        this.precioUnitario = Objects.requireNonNull(precioUnitario, "El precio unitario es obligatorio");
    }

    public BigDecimal getSubtotal() {
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }
}
