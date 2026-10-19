package df.digitalfix_ms_workorders.model.servicios;

import df.digitalfix_ms_workorders.model.common.EntidadAuditable;
import df.digitalfix_ms_workorders.model.common.PatronesValidacion;
import df.digitalfix_ms_workorders.model.common.SanitizadorTexto;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;

/**
 * Servicio técnico del catálogo (tabla SERVICIO del MER): lo que el cliente elige al crear su orden
 * en vez de escribir la labor a mano. DETALLE_OT_SERVICIO y DETALLE_ORDEN_SERVICIO lo referencian
 * por ID_SERVICIO y guardan su propio precio (el cobrado en esa orden), así cambiar la tarifa no
 * altera órdenes ya registradas.
 * <p>
 * Un servicio ya usado en órdenes no se borra: se desactiva y deja de ofrecerse.
 */
@Entity
@Table(name = "SERVICIO",
        uniqueConstraints = @UniqueConstraint(name = "UK_SERVICIO_CODIGO", columnNames = "CODIGO"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Servicio extends EntidadAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_SERVICIO", nullable = false, updatable = false)
    private Long idServicio;

    /** Código interno correlativo generado por el sistema (ej. "SRV-0001"); no cambia después de creado. */
    @NotBlank(message = "El código es obligatorio")
    @Size(max = 30, message = "El código no puede exceder 30 caracteres")
    @Pattern(regexp = PatronesValidacion.CODIGO_SERVICIO,
            message = "El código solo admite letras mayúsculas, números y guiones (ej. ELEC-TAB-01)")
    @Column(name = "CODIGO", nullable = false, updatable = false, length = 30)
    private String codigo;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    @Pattern(regexp = PatronesValidacion.RAZON_SOCIAL, message = "El nombre contiene caracteres no permitidos")
    @Column(name = "NOMBRE", nullable = false, length = 100)
    private String nombre;

    @Size(max = 255, message = "La descripción no puede exceder 255 caracteres")
    @Column(name = "DESCRIPCION", length = 255)
    private String descripcion;

    /** Tarifa oficial de mano de obra en CLP; se copia como precio al agregarlo a una orden. */
    @NotNull(message = "La tarifa es obligatoria")
    @DecimalMin(value = "0.00", message = "La tarifa no puede ser negativa")
    @Digits(integer = 10, fraction = 2, message = "La tarifa admite hasta 10 enteros y 2 decimales")
    @Column(name = "TARIFA", nullable = false, precision = 12, scale = 2)
    private BigDecimal tarifa;

    @Column(name = "ACTIVO", nullable = false)
    private boolean activo = true;

    public Servicio(String codigo, String nombre, String descripcion, BigDecimal tarifa) {
        this.codigo = normalizarCodigo(codigo);
        actualizarDatos(nombre, descripcion, tarifa);
    }

    public void actualizarDatos(String nombre, String descripcion, BigDecimal tarifa) {
        this.nombre = SanitizadorTexto.capitalizarPrimeraLetra(nombre);
        this.descripcion = SanitizadorTexto.textoMultilinea(descripcion);
        this.tarifa = Objects.requireNonNull(tarifa, "La tarifa es obligatoria");
    }

    public void activar() {
        this.activo = true;
    }

    public void desactivar() {
        this.activo = false;
    }

    public static String normalizarCodigo(String codigo) {
        return codigo == null ? null : codigo.trim().toUpperCase(Locale.ROOT);
    }
}
