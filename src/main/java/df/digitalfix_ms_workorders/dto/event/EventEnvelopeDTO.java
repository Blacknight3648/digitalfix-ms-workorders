package df.digitalfix_ms_workorders.dto.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventEnvelopeDTO<T> {

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    private String eventType;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    private String traceId;

    // Agrupa todos los mensajes de una misma orden ("OT-15"), igual que la referencia del stock.
    private String correlationId;

    // En el JSON viaja como "data": es el contrato que consume ms-notify (EventEnvelopeDTO#data).
    @JsonProperty("data")
    private T payload;
}
