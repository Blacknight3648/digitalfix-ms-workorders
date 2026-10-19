package df.digitalfix_ms_workorders.model.common;

/**
 * Expresiones regulares compartidas por las anotaciones {@code @Pattern} de los modelos.
 * Son listas blancas: sólo se acepta lo que coincide explícitamente.
 */
public final class PatronesValidacion {

    /** Nombres y apellidos: letras (con tildes/ñ), espacios, apóstrofo, punto y guion. */
    public static final String NOMBRE_PERSONA = "^\\p{L}[\\p{L} '.-]*$";

    /** Razón social o giro: letras, números y signos habituales en nombres de empresa. */
    public static final String RAZON_SOCIAL = "^[\\p{L}\\p{N}][\\p{L}\\p{N} .,&'()/-]*$";

    /** Teléfono chileno normalizado por {@link SanitizadorTexto#telefono(String)}. */
    public static final String TELEFONO_CHILE = "^\\+56\\d{9}$";

    /** Código de servicio del catálogo: mayúsculas, números y guiones, sin empezar ni terminar en guion. */
    public static final String CODIGO_SERVICIO = "^[A-Z0-9]+(-[A-Z0-9]+)*$";

    /** Nombre de calle o detalle (depto, oficina): letras, números y signos habituales en direcciones. */
    public static final String CALLE = "^[\\p{L}\\p{N}][\\p{L}\\p{N} .,#º°'/-]*$";

    /** Número de la dirección: hasta 6 dígitos con letra opcional (1234, 1234B) o S/N. */
    public static final String NUMERO_DIRECCION = "^(\\d{1,6}[A-Z]?|S/N)$";

    /** Código postal chileno (Correos de Chile): 7 dígitos. */
    public static final String CODIGO_POSTAL_CHILE = "^\\d{7}$";

    private PatronesValidacion() {
    }
}
