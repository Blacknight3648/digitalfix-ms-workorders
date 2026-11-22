package df.digitalfix_ms_workorders.service;

import df.digitalfix_ms_workorders.dto.OrdenTrabajo.OrdenTrabajoResponse;
import df.digitalfix_ms_workorders.dto.Portal.OrdenPortalRequest;
import df.digitalfix_ms_workorders.dto.Portal.SeguimientoOrdenResponse;
import df.digitalfix_ms_workorders.dto.Ubicacion.DireccionRequest;
import df.digitalfix_ms_workorders.enums.EstadoOrden;
import df.digitalfix_ms_workorders.security.PortalApiKeyFilter;
import df.digitalfix_ms_workorders.service.impl.PortalExternoServiceImpl;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PortalExternoServiceTest {

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void observaciones_IncluyeRepuestosYReservaParaElSupervisor() {
        var request = new OrdenPortalRequest(
                "PED-000123",
                new OrdenPortalRequest.ClientePortal("12.345.678-5", "Juan Pérez", "+56912345678", "juan@correo.cl"),
                new DireccionRequest(1L, "Av. Providencia", "1234", "Depto 501", null, null),
                List.of(3L),
                List.of(new OrdenPortalRequest.RepuestoSolicitado(7L, "REP-0007", "Interruptor 2x16A", 2)),
                "Lunes AM",
                "Tablero se calienta",
                "HOLD-AB12CD34");

        String obs = ReflectionTestUtils.invokeMethod(PortalExternoServiceImpl.class, "observaciones", request);

        assertThat(obs)
                .startsWith("[Portal Cliente PED-000123] Tablero se calienta")
                .contains("Horario preferido: Lunes AM")
                .contains("Dirección de atención: Av. Providencia 1234, Depto 501")
                .contains("- REP-0007 Interruptor 2x16A x2")
                .contains("Reserva de stock: HOLD-AB12CD34");
    }

    @Test
    void seguimiento_MarcaHitosCompletadosYActual_SinDatosInternos() {
        var orden = orden(EstadoOrden.EN_DESPLAZAMIENTO);

        SeguimientoOrdenResponse s = ReflectionTestUtils.invokeMethod(
                PortalExternoServiceImpl.class, "seguimientoDe", orden, Map.of(3L, "Revisión de tablero"));

        assertThat(s.codigoSeguimiento()).isEqualTo("OT-42");
        assertThat(s.estadoEtiqueta()).isEqualTo("En camino");
        assertThat(s.hitos()).extracting(SeguimientoOrdenResponse.Hito::completado)
                .containsExactly(true, true, false, false, false);
        assertThat(s.hitos()).extracting(SeguimientoOrdenResponse.Hito::actual)
                .containsExactly(false, false, true, false, false);
        assertThat(s.servicios()).singleElement()
                .extracting(SeguimientoOrdenResponse.ServicioContratado::nombre).isEqualTo("Revisión de tablero");
    }

    @Test
    void seguimiento_Cancelada_AgregaHitoCancelada() {
        SeguimientoOrdenResponse s = ReflectionTestUtils.invokeMethod(
                PortalExternoServiceImpl.class, "seguimientoDe", orden(EstadoOrden.CANCELADA), Map.of());

        assertThat(s.hitos()).last().satisfies(h -> {
            assertThat(h.estado()).isEqualTo(EstadoOrden.CANCELADA);
            assertThat(h.actual()).isTrue();
        });
    }

    @Test
    void filtroApiKey_SoloAutenticaConLaClaveCorrecta() throws Exception {
        var filtro = new PortalApiKeyFilter("clave-secreta");

        var correcta = new MockHttpServletRequest();
        correcta.addHeader(PortalApiKeyFilter.HEADER, "clave-secreta");
        filtro.doFilter(correcta, new MockHttpServletResponse(), mock(FilterChain.class));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();

        SecurityContextHolder.clearContext();
        var incorrecta = new MockHttpServletRequest();
        incorrecta.addHeader(PortalApiKeyFilter.HEADER, "otra");
        filtro.doFilter(incorrecta, new MockHttpServletResponse(), mock(FilterChain.class));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void filtroApiKey_SinClaveConfigurada_NuncaAutentica() throws Exception {
        var filtro = new PortalApiKeyFilter("");
        var request = new MockHttpServletRequest();
        request.addHeader(PortalApiKeyFilter.HEADER, "");
        filtro.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private static OrdenTrabajoResponse orden(EstadoOrden estado) {
        return new OrdenTrabajoResponse(
                42L,
                new OrdenTrabajoResponse.ClienteResumen(1L, "12.345.678-5", "Juan Pérez"),
                null,
                estado,
                "nota interna",
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                List.of(new OrdenTrabajoResponse.Servicio(9L, 3L, 5L, "desc", new BigDecimal("45000"))),
                List.of(),
                new BigDecimal("45000"));
    }
}
