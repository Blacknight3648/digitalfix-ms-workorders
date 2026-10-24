package df.digitalfix_ms_workorders.integration.catalogo;

import df.digitalfix_ms_workorders.model.common.DatoInvalidoException;
import df.digitalfix_ms_workorders.model.common.OperacionNoPermitidaException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Adaptador HTTP hacia ms-catalog. Reenvía el mismo JWT del usuario (como {@code UserRoleClient}
 * con ms-user), así el catálogo vuelve a validar el token y el rol, y registra en
 * MOVIMIENTO_STOCK quién movió el stock.
 * <p>
 * Traduce las respuestas del catálogo a las excepciones de dominio de este servicio, para que el
 * frontend reciba el mismo mensaje (ej. "Stock insuficiente para REP-0003...").
 */
@Slf4j
@Component
public class CatalogoInventarioClient implements InventarioRepuestos {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    // Mismo margen que UserRoleClient: el primer request tras levantar el catálogo descarga el
    // JWKS de Entra ID y abre la conexión a Oracle.
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(8);

    private record Item(Long idRepuesto, Integer cantidad) {
    }

    private record Lote(String referencia, List<Item> items) {
    }

    private record Repuesto(String codigo, String nombre, boolean activo) {
    }

    private final RestClient restClient;

    public CatalogoInventarioClient(@Value("${digitalfix.ms-catalog.url}") String msCatalogUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        this.restClient = RestClient.builder()
                .baseUrl(msCatalogUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public void verificarDisponible(Long idRepuesto) {
        Repuesto repuesto = llamar(() -> restClient.get()
                .uri("/api/catalog/repuestos/{id}", idRepuesto)
                .headers(this::reenviarToken)
                .retrieve()
                .body(Repuesto.class));
        if (repuesto == null || !repuesto.activo()) {
            throw new DatoInvalidoException("El repuesto con id " + idRepuesto + " no está disponible en el catálogo");
        }
    }

    @Override
    public void descontar(String referencia, Map<Long, Integer> cantidades) {
        enviarLote("/api/catalog/stock/salidas", referencia, cantidades);
        log.info("Stock descontado en ms-catalog para {}: {}", referencia, cantidades);
    }

    @Override
    public void devolver(String referencia, Map<Long, Integer> cantidades) {
        enviarLote("/api/catalog/stock/devoluciones", referencia, cantidades);
        log.info("Stock devuelto en ms-catalog para {}: {}", referencia, cantidades);
    }

    private void enviarLote(String ruta, String referencia, Map<Long, Integer> cantidades) {
        List<Item> items = cantidades.entrySet().stream()
                .map(e -> new Item(e.getKey(), e.getValue()))
                .toList();
        llamar(() -> restClient.post()
                .uri(ruta)
                .headers(this::reenviarToken)
                .body(new Lote(referencia, items))
                .retrieve()
                .toBodilessEntity());
    }

    private void reenviarToken(HttpHeaders headers) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwt) {
            headers.setBearerAuth(jwt.getToken().getTokenValue());
        }
    }

    private <T> T llamar(Supplier<T> llamada) {
        try {
            return llamada.get();
        } catch (RestClientResponseException e) {
            throw traducir(e);
        } catch (ResourceAccessException e) {
            log.error("ms-catalog no responde: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "El catálogo de repuestos no está disponible; intente nuevamente en unos segundos");
        }
    }

    // 409 (stock insuficiente, repuesto desactivado) y 400/404 se devuelven con el mensaje del
    // catálogo; un 401/403 o 5xx es un problema de integración, no del usuario.
    private RuntimeException traducir(RestClientResponseException e) {
        String mensaje = mensajeDelCatalogo(e);
        int status = e.getStatusCode().value();
        if (status == HttpStatus.CONFLICT.value()) {
            return new OperacionNoPermitidaException(mensaje);
        }
        if (status == HttpStatus.BAD_REQUEST.value() || status == HttpStatus.NOT_FOUND.value()) {
            return new DatoInvalidoException(mensaje);
        }
        if (status == HttpStatus.FORBIDDEN.value()) {
            return new ResponseStatusException(HttpStatus.FORBIDDEN, "Su rol no permite mover stock en el catálogo de repuestos");
        }
        log.error("ms-catalog respondió {}: {}", status, e.getResponseBodyAsString());
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "El catálogo de repuestos respondió con un error inesperado");
    }

    private String mensajeDelCatalogo(RestClientResponseException e) {
        try {
            Map<?, ?> cuerpo = e.getResponseBodyAs(Map.class);
            Object mensaje = cuerpo != null ? cuerpo.get("message") : null;
            if (mensaje != null) {
                return mensaje.toString();
            }
        } catch (RuntimeException ignorada) {
            // Cuerpo vacío o no JSON: se usa el mensaje genérico.
        }
        return "El catálogo de repuestos rechazó la operación (HTTP " + e.getStatusCode().value() + ")";
    }
}
