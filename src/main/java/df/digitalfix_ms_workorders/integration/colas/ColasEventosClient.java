package df.digitalfix_ms_workorders.integration.colas;

import df.digitalfix_ms_workorders.dto.event.DispatchTicketEvent;
import df.digitalfix_ms_workorders.dto.event.ReportRequestEvent;
import df.digitalfix_ms_workorders.dto.event.WorkOrderNotificationEvent;
import df.digitalfix_ms_workorders.messaging.PublicacionEventoException;
import df.digitalfix_ms_workorders.messaging.PublicadorEventosOrden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Adaptador HTTP de {@link PublicadorEventosOrden} hacia ms-admin-queues, el dueño de toda la
 * mensajería. Este servicio no se conecta a RabbitMQ: entrega cada evento en
 * {@code POST /internal/v1/eventos/{flujo}} indicando qué pasó (eventType), a qué orden pertenece
 * (correlationId) y los datos de negocio. ms-admin-queues elige exchange y routing key, arma el
 * sobre y responde 202 solo cuando el broker confirmó el mensaje.
 */
@Slf4j
@Component
public class ColasEventosClient implements PublicadorEventosOrden {

    static final String RUTA = "/internal/v1/eventos/{flujo}";

    // Flujos de la topología (app.rabbitmq.flujos en ms-admin-queues)
    static final String FLUJO_EMAIL = "email";
    static final String FLUJO_DESPACHO = "dispatch";
    static final String FLUJO_INFORME = "report";

    static final String TIPO_CAMBIO_ESTADO = "WORK_ORDER_STATUS_CHANGED";
    static final String TIPO_DESPACHO = "CREW_DISPATCH_ASSIGNED";
    static final String TIPO_INFORME = "WORK_ORDER_REPORT_REQUESTED";

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    // ms-admin-queues espera la confirmación del broker (hasta 5 s) antes de responder.
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(8);

    /** Cuerpo de la publicación; {@code subtipo} viaja solo en las notificaciones al cliente. */
    record Solicitud(String eventType, String correlationId, String subtipo, Object data) {
    }

    private final RestClient restClient;

    @Autowired
    public ColasEventosClient(@Value("${digitalfix.ms-admin-queues.url}") String msAdminQueuesUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        this.restClient = RestClient.builder()
                .baseUrl(msAdminQueuesUrl)
                .requestFactory(requestFactory)
                .build();
    }

    ColasEventosClient(RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * El estado de la orden viaja como subtipo: ms-admin-queues lo publica por el exchange topic
     * como {@code email.<estado>}, así otro consumidor podría suscribirse solo a algunos estados.
     */
    @Override
    public void publicarNotificacionCliente(WorkOrderNotificationEvent evento) {
        String estado = Objects.requireNonNull(evento.getOrderStatus(), "La notificación necesita el estado de la orden");
        enviar(FLUJO_EMAIL, new Solicitud(TIPO_CAMBIO_ESTADO, correlacion(evento.getWorkOrderId()),
                estado.toLowerCase(Locale.ROOT), evento));
    }

    @Override
    public void publicarTicketDespacho(DispatchTicketEvent evento) {
        enviar(FLUJO_DESPACHO, new Solicitud(TIPO_DESPACHO, correlacion(evento.getWorkOrderId()), null, evento));
    }

    @Override
    public void publicarSolicitudInforme(ReportRequestEvent evento) {
        enviar(FLUJO_INFORME, new Solicitud(TIPO_INFORME, correlacion(evento.getWorkOrderId()), null, evento));
    }

    private static String correlacion(Long idOrden) {
        return "OT-" + idOrden;
    }

    private void enviar(String flujo, Solicitud solicitud) {
        try {
            Map<?, ?> respuesta = restClient.post()
                    .uri(RUTA, flujo)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(solicitud)
                    .retrieve()
                    .body(Map.class);
            log.info("Evento {} de {} encolado en el flujo {} (eventId {})", solicitud.eventType(),
                    solicitud.correlationId(), flujo, respuesta != null ? respuesta.get("eventId") : "?");
        } catch (RestClientException e) {
            throw new PublicacionEventoException("ms-admin-queues no aceptó el evento " + solicitud.eventType()
                    + " de " + solicitud.correlationId() + ": " + e.getMessage(), e);
        }
    }
}
