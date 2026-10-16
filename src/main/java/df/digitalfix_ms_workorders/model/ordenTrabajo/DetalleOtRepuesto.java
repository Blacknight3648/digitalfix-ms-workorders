package df.digitalfix_ms_workorders.model.ordenTrabajo;

import df.digitalfix_ms_workorders.model.common.DatoInvalidoException;
import df.digitalfix_ms_workorders.model.proveedores.Proveedor;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

/**
 * Repuesto consumido en una OT. REPUESTO vive en el servicio de inventario, por eso se referencia
 * por ID; el proveedor sí está en este servicio y se mapea como FK real.
 */
@Entity
@Table(name = "DETALLE_OT_REPUESTO",
        uniqueConstraints = @UniqueConstraint(name = "UK_DET_OT_REPUESTO",
                columnNames = {"ID_OT", "ID_REPUESTO", "ID_PROVEEDOR"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DetalleOtRepuesto {

    static final int CANTIDAD_MAXIMA = 9_999;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_DETALLE_OT_REPUESTO", nullable = false, updatable = false)
    private Long idDetalleOtRepuesto;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_OT", nullable = false, updatable = false,
            foreignKey = @ForeignKey(name = "FK_DET_OT_REPUESTO_OT"))
    private OrdenTrabajo ordenTrabajo;

    @NotNull(message = "El repuesto es obligatorio")
    @Positive(message = "El ID del repuesto no es válido")
    @Column(name = "ID_REPUESTO", nullable = false, updatable = false)
    private Long idRepuesto;

    @NotNull(message = "El proveedor es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_PROVEEDOR", nullable = false, updatable = false,
            foreignKey = @ForeignKey(name = "FK_DET_OT_REPUESTO_PROVEEDOR"))
    private Proveedor proveedor;

    @NotNull(message = "La cantidad es obligatoria")
    @Positive(message = "La cantidad debe ser mayor a cero")
    @Max(value = CANTIDAD_MAXIMA, message = "La cantidad excede el máximo permitido")
    @Column(name = "CANTIDAD_UTILIZADA", nullable = false)
    private Integer cantidadUtilizada;

    DetalleOtRepuesto(OrdenTrabajo ordenTrabajo, Long idRepuesto, Proveedor proveedor, int cantidad) {
        this.ordenTrabajo = Objects.requireNonNull(ordenTrabajo);
        this.idRepuesto = Objects.requireNonNull(idRepuesto, "El repuesto es obligatorio");
        this.proveedor = Objects.requireNonNull(proveedor, "El proveedor es obligatorio");
        this.cantidadUtilizada = validarCantidad(cantidad);
    }

    boolean corresponde(Long idRepuesto, Proveedor proveedor) {
        return this.idRepuesto.equals(idRepuesto) && mismoProveedor(proveedor);
    }

    /** Compara por ID (seguro con proxies LAZY); si aún no se persiste, por identidad. */
    private boolean mismoProveedor(Proveedor otro) {
        if (this.proveedor == otro) {
            return true;
        }
        Long idActual = this.proveedor.getIdProveedor();
        return idActual != null && otro != null && idActual.equals(otro.getIdProveedor());
    }

    void incrementarCantidad(int cantidad) {
        this.cantidadUtilizada = validarCantidad(Math.addExact(this.cantidadUtilizada, validarCantidad(cantidad)));
    }

    private static int validarCantidad(int cantidad) {
        if (cantidad <= 0 || cantidad > CANTIDAD_MAXIMA) {
            throw new DatoInvalidoException("La cantidad debe estar entre 1 y " + CANTIDAD_MAXIMA);
        }
        return cantidad;
    }
}
