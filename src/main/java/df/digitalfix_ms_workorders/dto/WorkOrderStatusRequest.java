package df.digitalfix_ms_workorders.dto;

import df.digitalfix_ms_workorders.enums.WorkOrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkOrderStatusRequest {

    @NotNull(message = "El estado es obligatorio")
    private WorkOrderStatus estado;
}
