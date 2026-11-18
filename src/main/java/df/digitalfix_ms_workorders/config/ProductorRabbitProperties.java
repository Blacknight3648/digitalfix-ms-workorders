package df.digitalfix_ms_workorders.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Nombres de la topología RabbitMQ que usa el productor ({@code digitalfix.rabbitmq} en
 * application.yaml). Las colas no están aquí: las declara ms-notify, que es quien consume. Se
 * valida al arrancar, así un nombre faltante se detecta antes de publicar el primer evento.
 */
@Validated
@ConfigurationProperties(prefix = "digitalfix.rabbitmq")
public record ProductorRabbitProperties(
        @Valid @NotNull Exchanges exchanges,
        @Valid @NotNull RoutingKeys routingKeys
) {

    /**
     * Exchanges donde publica ms-workorders: {@code direct} para comandos punto a punto (despacho,
     * informe) y {@code topic} para las notificaciones al cliente, que llevan el estado en la clave.
     */
    public record Exchanges(@NotBlank String direct, @NotBlank String topic) {
    }

    /**
     * Routing keys por caso de uso en {@code cmd.direct}.
     */
    public record RoutingKeys(
            @NotBlank String notificacionCliente,
            @NotBlank String dispatch,
            @NotBlank String report
    ) {
    }
}
