package df.digitalfix_ms_workorders.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;
import java.util.Set;

// Misma validación del JWT y mismo modelo de roles que ms-login y ms-user.
//
// Validación del token de Entra ID:
//   - Firma con las claves públicas (JWKS) del tenant; vigencia ("exp", "nbf").
//   - Issuer del tenant (formato v2 o v1) y audience = client ID de la API.
//
// Autorización por rol según el caso (sección 3 y pantallas de la sección 6):
//   - Órdenes de trabajo y de servicio: las consultan Admin, Supervisor, Técnico
//     y Cliente; las crean y editan Cliente, Supervisor y Admin; la operación
//     (estado, técnico, cuadrilla, servicios, repuestos) y el borrado son de
//     Supervisor y Admin.
//   - Clientes: los consultan además los Clientes (al crear su orden).
//   - Cuadrillas, proveedores y mantención de clientes: Supervisor y Admin.
//   - Catálogo de servicios: lo consultan los roles de órdenes; lo mantiene solo SUPERADMIN.
//   - SUPERADMIN hereda todos los roles (RoleHierarchy).
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] ORDERS = {"/api/ordenes-trabajo/**", "/api/ordenes-servicio/**"};
    private static final String[] ORDER_ROOTS = {"/api/ordenes-trabajo", "/api/ordenes-servicio"};
    private static final String[] ORDER_ITEMS = {"/api/ordenes-trabajo/*", "/api/ordenes-servicio/*"};

    private final EntraJwtAuthenticationConverter jwtAuthenticationConverter;
    private final JsonSecurityErrorHandler jsonSecurityErrorHandler;

    @Value("${AZURE_TENANT_ID}")
    private String tenantId;

    @Value("${AZURE_CLIENT_ID}")
    private String clientId;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                // Usa la configuración CORS de Spring MVC (WebConfig).
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth

                        /** SWAGGER Y PREFLIGHT CORS */
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        /** ÓRDENES – consulta */
                        .requestMatchers(HttpMethod.GET, ORDERS).hasAnyRole("ADMIN", "SUPERVISOR", "TECNICO", "CLIENTE")
                        .requestMatchers(HttpMethod.GET, ORDER_ROOTS).hasAnyRole("ADMIN", "SUPERVISOR", "TECNICO", "CLIENTE")

                        /** ÓRDENES – crear y editar sus datos (el cliente, mientras la orden está CREADA) */
                        .requestMatchers(HttpMethod.POST, ORDER_ROOTS).hasAnyRole("ADMIN", "SUPERVISOR", "CLIENTE")
                        .requestMatchers(HttpMethod.PUT, ORDER_ITEMS).hasAnyRole("ADMIN", "SUPERVISOR", "CLIENTE")

                        /** ÓRDENES – operación: estado, técnico, cuadrilla, servicios, repuestos y borrado */
                        .requestMatchers(ORDERS).hasAnyRole("ADMIN", "SUPERVISOR")

                        /** CLIENTES – el cliente consulta la lista al crear su orden */
                        .requestMatchers(HttpMethod.GET, "/api/clientes", "/api/clientes/**").hasAnyRole("ADMIN", "SUPERVISOR", "CLIENTE")

                        /** CLIENTES, CUADRILLAS Y PROVEEDORES – mantención */
                        .requestMatchers("/api/clientes/**", "/api/clientes",
                                "/api/cuadrillas/**", "/api/cuadrillas",
                                "/api/proveedores/**", "/api/proveedores").hasAnyRole("ADMIN", "SUPERVISOR")

                        /** UBICACIONES – regiones y comunas: catálogo de lectura para cualquier usuario autenticado */
                        .requestMatchers(HttpMethod.GET, "/api/regiones", "/api/regiones/**").hasAuthority("SCOPE_access_as_user")

                        /** SERVICIOS – catálogo: lo consultan quienes crean u operan órdenes; lo mantiene SUPERADMIN */
                        .requestMatchers(HttpMethod.GET, "/api/servicios", "/api/servicios/**").hasAnyRole("ADMIN", "SUPERVISOR", "TECNICO", "CLIENTE")
                        .requestMatchers("/api/servicios", "/api/servicios/**").hasRole("SUPERADMIN")

                        /** RESTO */
                        .anyRequest().authenticated()
                )

                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder())
                                .jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(jsonSecurityErrorHandler)
                        .accessDeniedHandler(jsonSecurityErrorHandler))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jsonSecurityErrorHandler)
                        .accessDeniedHandler(jsonSecurityErrorHandler))
                .build();
    }

    // SUPERADMIN hereda los permisos de todos los roles del caso (misma
    // jerarquía que ms-login y ms-user).
    @Bean
    static RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withDefaultRolePrefix()
                .role("SUPERADMIN").implies("ADMIN", "SUPERVISOR", "TECNICO", "CLIENTE", "AUDITOR")
                .build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        String issuerV2 = "https://login.microsoftonline.com/" + tenantId + "/v2.0";
        String issuerV1 = "https://sts.windows.net/" + tenantId + "/";

        // Las claves de firma son las mismas para tokens v1 y v2 del tenant.
        NimbusJwtDecoder jwtDecoder = JwtDecoders.fromIssuerLocation(issuerV2);

        Set<String> allowedIssuers = Set.of(issuerV2, issuerV1);
        OAuth2TokenValidator<Jwt> issuerValidator = new JwtClaimValidator<Object>(
                JwtClaimNames.ISS, iss -> iss != null && allowedIssuers.contains(iss.toString()));

        OAuth2TokenValidator<Jwt> audienceValidator = new JwtClaimValidator<Object>(
                JwtClaimNames.AUD, aud -> {
                    if (aud instanceof String s) {
                        return s.equals(clientId) || s.equals("api://" + clientId);
                    }
                    if (aud instanceof List<?> list) {
                        return list.contains(clientId) || list.contains("api://" + clientId);
                    }
                    return false;
                });

        jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(), issuerValidator, audienceValidator));
        return jwtDecoder;
    }
}
