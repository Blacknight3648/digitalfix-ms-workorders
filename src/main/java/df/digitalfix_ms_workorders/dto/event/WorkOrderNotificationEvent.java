package df.digitalfix_ms_workorders.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderNotificationEvent {

    private Long workOrderId;
    private String clientEmail;
    private String clientName;
    private String orderStatus;
    private String description;
    private BigDecimal totalAmount;
}
