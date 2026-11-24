package df.digitalfix_ms_workorders.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Evento publicado en el tópico Kafka {@code workorders.events} para analítica (ms-report) y
 * auditoría. Mantiene el contrato esperado por {@code WorkOrderEventDTO} en ms-report.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderKafkaEvent {

    private String eventId;
    private String eventType;
    private Long orderId;
    private String previousStatus;
    private String newStatus;
    private Long assignedTechnicianId;
    private Long clientId;
    private String timestamp; // ISO-8601 UTC
    private String traceId;
}
