package df.digitalfix_ms_workorders.model.common;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Normaliza texto de entrada antes de asignarlo a una entidad.
 * <p>
 * Sanitizar no reemplaza a la validación: aquí sólo se limpia (espacios, caracteres de control,
 * etiquetas HTML); el formato y el largo los valida Bean Validation en cada entidad. Nunca se
 * trunca un valor en silencio: si excede el largo, la validación lo rechaza.
 */
public final class SanitizadorTexto {

    private static final Pattern ETIQUETAS_HTML = Pattern.compile("<[^>]*>");
    private static final Pattern SIGNOS_ANGULARES = Pattern.compile("[<>]");
    private static final Pattern CONTROL_SIN_SALTOS = Pattern.compile("[\\p{Cntrl}&&[^\\n\\t]]");
    private static final Pattern CONTROL_TODOS = Pattern.compile("\\p{Cntrl}");
    private static final Pattern ESPACIOS_HORIZONTALES = Pattern.compile("[ \\t\\u00A0]+");
    private static final Pattern SALTOS_MULTIPLES = Pattern.compile("\\n{3,}");
    private static final Pattern SEPARADORES_TELEFONO = Pattern.compile("[\\s().-]");
    private static final Pattern MOVIL_SIN_PREFIJO = Pattern.compile("\\d{9}");
    private static final Pattern CON_PREFIJO_SIN_MAS = Pattern.compile("56\\d{9}");

    private SanitizadorTexto() {
    }

    /** Texto de una sola línea (nombres, razón social, etc.). Devuelve {@code null} si queda vacío. */
    public static String texto(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = quitarMarcado(normalizarUnicode(valor));
        limpio = CONTROL_TODOS.matcher(limpio).replaceAll(" ");
        limpio = ESPACIOS_HORIZONTALES.matcher(limpio).replaceAll(" ").trim();
        return limpio.isEmpty() ? null : limpio;
    }

    /** Texto libre multilínea (observaciones, descripciones). Conserva saltos de línea. */
    public static String textoMultilinea(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = quitarMarcado(normalizarUnicode(valor).replace("\r\n", "\n").replace('\r', '\n'));
        limpio = CONTROL_SIN_SALTOS.matcher(limpio).replaceAll("");
        limpio = ESPACIOS_HORIZONTALES.matcher(limpio).replaceAll(" ");
        limpio = SALTOS_MULTIPLES.matcher(limpio).replaceAll("\n\n").strip();
        return limpio.isEmpty() ? null : limpio;
    }

    /** Correo en minúsculas y sin espacios, para comparaciones e índices consistentes. */
    public static String email(String valor) {
        String limpio = texto(valor);
        return limpio == null ? null : limpio.replace(" ", "").toLowerCase(Locale.ROOT);
    }

    /**
     * Teléfono chileno en formato E.164 ({@code +56XXXXXXXXX}). Si la entrada no calza con un
     * formato conocido se devuelve sin separadores para que la validación la rechace.
     */
    public static String telefono(String valor) {
        String limpio = texto(valor);
        if (limpio == null) {
            return null;
        }
        String digitos = SEPARADORES_TELEFONO.matcher(limpio).replaceAll("");
        if (MOVIL_SIN_PREFIJO.matcher(digitos).matches()) {
            return "+56" + digitos;
        }
        if (CON_PREFIJO_SIN_MAS.matcher(digitos).matches()) {
            return "+" + digitos;
        }
        return digitos;
    }

    private static String normalizarUnicode(String valor) {
        return Normalizer.normalize(valor, Normalizer.Form.NFC);
    }

    private static String quitarMarcado(String valor) {
        String sinEtiquetas = ETIQUETAS_HTML.matcher(valor).replaceAll("");
        return SIGNOS_ANGULARES.matcher(sinEtiquetas).replaceAll("");
    }
}
