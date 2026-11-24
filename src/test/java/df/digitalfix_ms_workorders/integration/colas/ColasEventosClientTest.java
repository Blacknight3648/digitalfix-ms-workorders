package df.digitalfix_ms_workorders.integration.colas;

import df.digitalfix_ms_workorders.dto.event.DispatchTicketEvent;
import df.digitalfix_ms_workorders.dto.event.ReportRequestEvent;
import df.digitalfix_ms_workorders.dto.event.WorkOrderNotificationEvent;
import df.digitalfix_ms_workorders.messaging.PublicacionEventoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.ExpectedCount.never;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

// Contrato con ms-admin-queues: el productor solo dice flujo, tipo de evento, orden y datos; nunca
// exchanges ni routing keys. Los nombres de campos de data son los que espera ms-notify.
class ColasEventosClientTest {

    private static final String BASE = "http://ms-admin-queues";
    private static final String ACEPTADO = "{\"eventId\":\"evt-1\",\"exchange\":\"cmd.topic\",\"routingKey\":\"email.asignada\"}";

    private MockRestServiceServer servidor;
    private ColasEventosClient cliente;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        servidor = MockRestServiceServer.bindTo(builder).build();
        cliente = new ColasEventosClient(builder.build());
    }

    private void esperarPublicacion(String flujo, org.springframework.test.web.client.RequestMatcher... matchers) {
        var expectativa = servidor.expect(requestTo(BASE + "/internal/v1/eventos/" + flujo))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        for (var matcher : matchers) {
            expectativa = expectativa.andExpect(matcher);
        }
        expectativa.andRespond(withStatus(HttpStatus.ACCEPTED).contentType(MediaType.APPLICATION_JSON).body(ACEPTADO));
    }

    @Test
    @DisplayName("Notificación al cliente: flujo email con el estado como subtipo y emailTo para ms-notify")
    void publicaNotificacionCliente() {
        esperarPublicacion("email",
                jsonPath("$.eventType").value(ColasEventosClient.TIPO_CAMBIO_ESTADO),
                jsonPath("$.correlationId").value("OT-55"),
                jsonPath("$.subtipo").value("asignada"),
                jsonPath("$.data.workOrderId").value(55),
                jsonPath("$.data.emailTo").value("cliente@digitalfix.com"),
                jsonPath("$.data.templateName").value("order-notification"));

        cliente.publicarNotificacionCliente(WorkOrderNotificationEvent.builder()
                .workOrderId(55L)
                .clientEmail("cliente@digitalfix.com")
                .clientName("Carlos Soto")
                .orderStatus("ASIGNADA")
                .totalAmount(new BigDecimal("350000.00"))
                .build());

        servidor.verify();
    }

    @Test
    @DisplayName("Ticket de despacho: flujo dispatch sin subtipo y fecha ISO-8601")
    void publicaTicketDespacho() {
        esperarPublicacion("dispatch",
                jsonPath("$.eventType").value(ColasEventosClient.TIPO_DESPACHO),
                jsonPath("$.correlationId").value("OT-77"),
                jsonPath("$.subtipo").doesNotExist(),
                jsonPath("$.data.technicianId").value("15"),
                jsonPath("$.data.scheduledDate").value("2026-11-06T09:30:00"));

        cliente.publicarTicketDespacho(DispatchTicketEvent.builder()
                .workOrderId(77L)
                .technicianId("15")
                .crewId("3")
                .serviceAddress("Calle Los Alerces 456")
                .clientPhone("+56987654321")
                .scheduledDate(LocalDateTime.of(2026, 11, 6, 9, 30))
                .serviceDescription("Revisión de líneas de media tensión")
                .build());

        servidor.verify();
    }

    @Test
    @DisplayName("Informe PDF: flujo report")
    void publicaSolicitudInforme() {
        esperarPublicacion("report",
                jsonPath("$.eventType").value(ColasEventosClient.TIPO_INFORME),
                jsonPath("$.correlationId").value("OT-88"),
                jsonPath("$.data.reportType").value("INFORME_CIERRE"));

        cliente.publicarSolicitudInforme(ReportRequestEvent.builder()
                .workOrderId(88L)
                .reportType("INFORME_CIERRE")
                .recipientEmail("cliente@digitalfix.com")
                .requestedBy("ms-workorders")
                .build());

        servidor.verify();
    }

    @Test
    @DisplayName("Si la mensajería no acepta el evento se lanza PublicacionEventoException")
    void mensajeriaNoDisponible() {
        servidor.expect(requestTo(BASE + "/internal/v1/eventos/report"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThrows(PublicacionEventoException.class, () -> cliente.publicarSolicitudInforme(
                ReportRequestEvent.builder().workOrderId(9L).build()));
    }

    @Test
    @DisplayName("Sin estado no se puede notificar: falla antes de llamar a la mensajería")
    void notificacionSinEstadoFalla() {
        servidor.expect(never(), requestTo(BASE + "/internal/v1/eventos/email"));

        assertThrows(NullPointerException.class, () -> cliente.publicarNotificacionCliente(
                WorkOrderNotificationEvent.builder().workOrderId(1L).build()));
        servidor.verify();
    }
}
