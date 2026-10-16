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

    private PatronesValidacion() {
    }
}
