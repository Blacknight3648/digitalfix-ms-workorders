package df.digitalfix_ms_workorders.model.ubicacion;

import df.digitalfix_ms_workorders.model.common.PatronesValidacion;
import df.digitalfix_ms_workorders.model.common.SanitizadorTexto;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Dirección de despacho/atención con el formato habitual en Chile: calle, número, detalle
 * (depto, oficina, block), comuna y código postal. La región no se guarda: depende de la comuna.
 * Cada cliente y cada proveedor tiene la suya (relación 1 a 1 que administra su dueño).
 */
@Entity
@Table(name = "DIRECCION")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Direccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_DIRECCION", nullable = false, updatable = false)
    private Long idDireccion;

    @NotNull(message = "La comuna es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_COMUNA", nullable = false, foreignKey = @ForeignKey(name = "FK_DIRECCION_COMUNA"))
    private Comuna comuna;

    @NotBlank(message = "La calle es obligatoria")
    @Size(max = 120, message = "La calle no puede exceder 120 caracteres")
    @Pattern(regexp = PatronesValidacion.CALLE, message = "La calle contiene caracteres no permitidos")
    @Column(name = "CALLE", nullable = false, length = 120)
    private String calle;

    @NotBlank(message = "El número es obligatorio")
    @Pattern(regexp = PatronesValidacion.NUMERO_DIRECCION, message = "El número debe ser numérico (ej. 1234 o 1234B) o S/N")
    @Column(name = "NUMERO", nullable = false, length = 10)
    private String numero;

    @Size(max = 60, message = "El detalle no puede exceder 60 caracteres")
    @Pattern(regexp = PatronesValidacion.CALLE, message = "El detalle contiene caracteres no permitidos")
    @Column(name = "DETALLE", length = 60)
    private String detalle;

    @Size(max = 200, message = "La referencia no puede exceder 200 caracteres")
    @Column(name = "REFERENCIA", length = 200)
    private String referencia;

    @Pattern(regexp = PatronesValidacion.CODIGO_POSTAL_CHILE, message = "El código postal debe tener 7 dígitos")
    @Column(name = "CODIGO_POSTAL", length = 7)
    private String codigoPostal;

    public Direccion(Comuna comuna, String calle, String numero, String detalle, String referencia, String codigoPostal) {
        actualizar(comuna, calle, numero, detalle, referencia, codigoPostal);
    }

    public void actualizar(Comuna comuna, String calle, String numero, String detalle, String referencia,
                           String codigoPostal) {
        this.comuna = Objects.requireNonNull(comuna, "La comuna es obligatoria");
        this.calle = SanitizadorTexto.texto(calle);
        this.numero = normalizarNumero(numero);
        this.detalle = SanitizadorTexto.texto(detalle);
        this.referencia = SanitizadorTexto.texto(referencia);
        this.codigoPostal = SanitizadorTexto.soloDigitos(codigoPostal);
    }

    /** "Av. Providencia 1234, Depto 501, Providencia, Región Metropolitana de Santiago". */
    public String textoCompleto() {
        return Stream.of(calle + " " + numero, detalle, comuna.getNombre(), comuna.getRegion().getNombre())
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", "));
    }

    private static String normalizarNumero(String numero) {
        String limpio = SanitizadorTexto.texto(numero);
        if (limpio == null) {
            return null;
        }
        String compacto = limpio.replace(" ", "").toUpperCase(Locale.ROOT);
        return compacto.equals("SN") || compacto.equals("S/N") ? "S/N" : compacto;
    }
}
