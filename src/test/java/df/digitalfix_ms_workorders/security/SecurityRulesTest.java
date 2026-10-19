package df.digitalfix_ms_workorders.security;

import df.digitalfix_ms_workorders.controller.ClienteController;
import df.digitalfix_ms_workorders.controller.CuadrillaController;
import df.digitalfix_ms_workorders.controller.OrdenTrabajoController;
import df.digitalfix_ms_workorders.service.ClienteService;
import df.digitalfix_ms_workorders.service.CuadrillaService;
import df.digitalfix_ms_workorders.service.OrdenTrabajoService;
import df.digitalfix_ms_workorders.service.UserRoleClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Reglas de SecurityConfig de ms-workorders. jwt() entrega un token ya validado
// con el rol indicado; "pasa la seguridad" significa cualquier respuesta que no
// sea 401/403 (por ejemplo 400 por un cuerpo vacío o 200 con el servicio mockeado).
@WebMvcTest(controllers = {OrdenTrabajoController.class, ClienteController.class, CuadrillaController.class})
@Import({SecurityConfig.class, EntraJwtAuthenticationConverter.class, JsonSecurityErrorHandler.class})
@TestPropertySource(properties = {"AZURE_TENANT_ID=test-tenant", "AZURE_CLIENT_ID=test-client"})
class SecurityRulesTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;
    @MockitoBean
    private UserRoleClient userRoleClient;
    @MockitoBean
    private OrdenTrabajoService ordenTrabajoService;
    @MockitoBean
    private ClienteService clienteService;
    @MockitoBean
    private CuadrillaService cuadrillaService;

    private static final ResultMatcher PASA_LA_SEGURIDAD = result ->
            assertThat(result.getResponse().getStatus()).isNotIn(401, 403);

    private MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder request, String role) {
        return request.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role)))
                .contentType(MediaType.APPLICATION_JSON).content("{}");
    }

    @Test
    void sinTokenResponde401EnJson() throws Exception {
        mockMvc.perform(get("/api/ordenes-trabajo"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void clienteConsultaCreaYEditaSusOrdenes() throws Exception {
        mockMvc.perform(as(get("/api/ordenes-trabajo"), "CLIENTE")).andExpect(PASA_LA_SEGURIDAD);
        mockMvc.perform(as(post("/api/ordenes-trabajo"), "CLIENTE")).andExpect(PASA_LA_SEGURIDAD);
        mockMvc.perform(as(put("/api/ordenes-trabajo/1"), "CLIENTE")).andExpect(PASA_LA_SEGURIDAD);
        mockMvc.perform(as(get("/api/clientes"), "CLIENTE")).andExpect(PASA_LA_SEGURIDAD);
    }

    @Test
    void clienteNoOperaLaOrden() throws Exception {
        mockMvc.perform(as(put("/api/ordenes-trabajo/1/estado"), "CLIENTE")).andExpect(status().isForbidden());
        mockMvc.perform(as(put("/api/ordenes-trabajo/1/cuadrilla"), "CLIENTE")).andExpect(status().isForbidden());
        mockMvc.perform(as(post("/api/cuadrillas"), "CLIENTE")).andExpect(status().isForbidden());
    }

    @Test
    void supervisorOperaLaOrdenYLasCuadrillas() throws Exception {
        mockMvc.perform(as(put("/api/ordenes-trabajo/1/estado"), "SUPERVISOR")).andExpect(PASA_LA_SEGURIDAD);
        mockMvc.perform(as(put("/api/ordenes-trabajo/1/cuadrilla"), "SUPERVISOR")).andExpect(PASA_LA_SEGURIDAD);
        mockMvc.perform(as(get("/api/cuadrillas"), "SUPERVISOR")).andExpect(PASA_LA_SEGURIDAD);
    }

    @Test
    void auditorNoUsaElServicioDeOrdenes() throws Exception {
        // El auditor consulta el timeline en ms-audit, no las órdenes.
        mockMvc.perform(as(get("/api/ordenes-trabajo"), "AUDITOR")).andExpect(status().isForbidden());
    }

    @Test
    void superadminPuedeTodoPorJerarquiaDeRoles() throws Exception {
        mockMvc.perform(as(put("/api/ordenes-trabajo/1/estado"), "SUPERADMIN")).andExpect(PASA_LA_SEGURIDAD);
        mockMvc.perform(as(post("/api/clientes"), "SUPERADMIN")).andExpect(PASA_LA_SEGURIDAD);
        mockMvc.perform(as(get("/api/cuadrillas"), "SUPERADMIN")).andExpect(PASA_LA_SEGURIDAD);
    }
}
