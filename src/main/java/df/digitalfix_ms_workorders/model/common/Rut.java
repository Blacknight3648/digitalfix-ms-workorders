package df.digitalfix_ms_workorders.model.common;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Objeto de valor inmutable para un RUT/RUN chileno.
 * <p>
 * Sólo se puede construir con {@link #of(String)} o {@link #of(int, char)}, que validan el formato
 * estricto y el dígito verificador (módulo 11). Así ninguna entidad puede quedar con un RUT inválido.
 * Las columnas se renombran en cada entidad con {@code @AttributeOverride}.
 */
@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Rut implements Serializable {

    public static final int NUMERO_MINIMO = 1_000_000;
    public static final int NUMERO_MAXIMO = 99_999_999;
    private static final int LARGO_MAXIMO_ENTRADA = 12;

    /** Acepta 12.345.678-5, 12345678-5 o 123456785; el DV puede ser dígito o K. */
    private static final Pattern FORMATO = Pattern.compile("^(\\d{1,2}\\.\\d{3}\\.\\d{3}|\\d{7,8})-?([0-9K])$");

    @Column(name = "RUN", nullable = false)
    private Integer numero;

    @Column(name = "DV", nullable = false, length = 1)
    private String dv;

    private Rut(int numero, char dv) {
        this.numero = numero;
        this.dv = String.valueOf(dv);
    }

    public static Rut of(String valor) {
        if (valor == null) {
            throw new DatoInvalidoException("El RUT es obligatorio");
        }
        String limpio = valor.strip().toUpperCase(Locale.ROOT);
        if (limpio.length() > LARGO_MAXIMO_ENTRADA) {
            throw new DatoInvalidoException("Formato de RUT inválido");
        }
        Matcher matcher = FORMATO.matcher(limpio);
        if (!matcher.matches()) {
            throw new DatoInvalidoException("Formato de RUT inválido");
        }
        int numero = Integer.parseInt(matcher.group(1).replace(".", ""));
        return of(numero, matcher.group(2).charAt(0));
    }

    public static Rut of(int numero, char dv) {
        if (numero < NUMERO_MINIMO || numero > NUMERO_MAXIMO) {
            throw new DatoInvalidoException("El número de RUT está fuera de rango");
        }
        char dvNormalizado = Character.toUpperCase(dv);
        if (calcularDv(numero) != dvNormalizado) {
            throw new DatoInvalidoException("El dígito verificador del RUT no es válido");
        }
        return new Rut(numero, dvNormalizado);
    }

    public static char calcularDv(int numero) {
        int suma = 0;
        int factor = 2;
        for (int resto = numero; resto > 0; resto /= 10) {
            suma += (resto % 10) * factor;
            factor = factor == 7 ? 2 : factor + 1;
        }
        int resultado = 11 - (suma % 11);
        if (resultado == 11) {
            return '0';
        }
        return resultado == 10 ? 'K' : Character.forDigit(resultado, 10);
    }

    /** Formato de presentación: 12.345.678-5. */
    public String formateado() {
        return String.format(Locale.ROOT, "%,d", numero).replace(',', '.') + "-" + dv;
    }

    @Override
    public String toString() {
        return formateado();
    }
}
