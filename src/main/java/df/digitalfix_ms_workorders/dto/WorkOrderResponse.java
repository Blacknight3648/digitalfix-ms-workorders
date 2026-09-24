package df.digitalfix_ms_workorders.dto;

import df.digitalfix_ms_workorders.model.WorkOrder;
import df.digitalfix_ms_workorders.enums.WorkOrderStatus;
import lombok.*;

import java.time.LocalDateTime;

// id como String a proposito: calza directo con la interfaz WorkOrder del
// frontend (digitalfix-frontend/src/app/model/work-order.model.ts), que
// tipea "id: string" (viene de los datos mock con ids como "1042").
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkOrderResponse {

    private String id;
    private String cliente;
    private String servicio;
    private String tecnico;
    private WorkOrderStatus estado;
    private LocalDateTime fechaCreacion;

    public static WorkOrderResponse from(WorkOrder entity) {
        return WorkOrderResponse.builder()
                .id(String.valueOf(entity.getId()))
                .cliente(entity.getCliente())
                .servicio(entity.getServicio())
                .tecnico(entity.getTecnico())
                .estado(entity.getEstado())
                .fechaCreacion(entity.getFechaCreacion())
                .build();
    }
}
