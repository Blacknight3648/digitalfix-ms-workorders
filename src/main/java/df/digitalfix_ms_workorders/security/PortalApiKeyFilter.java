package df.digitalfix_ms_workorders.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

// Autenticación servicio a servicio del Portal Cliente (backend monolito) para /api/externo/**.
//
// El portal no usa Entra ID (sus usuarios son locales), así que no puede enviar un JWT del tenant.
// En su lugar presenta una clave compartida en el header X-Portal-Api-Key, que se compara en tiempo
// constante con PORTAL_API_KEY. Si la variable no está configurada, la API externa queda cerrada.
//
// No es un @Component a propósito: así Spring Boot no lo registra como filtro global del servlet
// y solo actúa dentro de la cadena de seguridad de /api/externo/** (SecurityConfig).
public class PortalApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Portal-Api-Key";
    public static final String ROL = "ROLE_PORTAL_CLIENTE";

    private final byte[] claveEsperada;

    public PortalApiKeyFilter(String claveEsperada) {
        this.claveEsperada = claveEsperada == null || claveEsperada.isBlank()
                ? null
                : claveEsperada.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String recibida = request.getHeader(HEADER);
        if (claveEsperada != null && recibida != null
                && MessageDigest.isEqual(claveEsperada, recibida.getBytes(StandardCharsets.UTF_8))) {
            var autenticacion = new UsernamePasswordAuthenticationToken(
                    "portal-cliente", null, AuthorityUtils.createAuthorityList(ROL));
            SecurityContextHolder.getContext().setAuthentication(autenticacion);
        }
        chain.doFilter(request, response);
    }
}
