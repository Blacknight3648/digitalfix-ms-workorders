package df.digitalfix_ms_workorders.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class Swagger {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("DigitalFix - Microservicio de Órdenes de Trabajo")
                        .description("API REST para la gestión de Clientes, Proveedores, Órdenes de Servicio y Órdenes de Trabajo (OT) de la plataforma de mantención eléctrica DigitalFix.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Equipo de Desarrollo DigitalFix")
                                .email("soporte@digitalfix.cl"))
                        .license(new License()
                                .name("Uso Académico / Proyecto Semestral")))
                .servers(List.of(
                        new Server().url("/").description("Servidor actual / API Gateway")
                ));
    }
}
