package df.digitalfix_ms_workorders.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

// Body para crear (POST) o editar (PUT) una orden de trabajo. No incluye
// "estado": eso se maneja aparte en WorkOrderStatusRequest, vía
// PUT /api/workorders/{id}/status, igual que en el enunciado del caso.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkOrderRequest {

    @NotBlank(message = "El cliente es obligatorio")
    private String cliente;

    @NotBlank(message = "El servicio es obligatorio")
    private String servicio;

    private String tecnico;
}
