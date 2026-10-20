package df.digitalfix_ms_workorders.dto.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderNotificationEvent {

    private Long workOrderId;
    // ms-notify (EmailNotificationDTO) lo recibe como "emailTo".
    @JsonProperty("emailTo")
    private String clientEmail;
    private String clientName;
    private String orderStatus;
    private String description;
    private BigDecimal totalAmount;

    // Campos que ms-notify necesita para armar el correo HTML (plantilla Thymeleaf).
    private String subject;

    @Builder.Default
    private String templateName = "order-notification";

    // Variables de la plantilla: status, serviceAddress, technicianName, etc.
    private Map<String, Object> templateAttributes;
}
