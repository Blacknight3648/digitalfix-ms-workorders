package df.digitalfix_ms_workorders.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequestEvent {

    private Long workOrderId;
    private String reportType; // Ejemplos: "CERTIFICADO_TECNICO", "INFORME_CIERRE"
    private String recipientEmail;
    private String requestedBy;
}
