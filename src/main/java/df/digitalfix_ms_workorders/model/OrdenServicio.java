package df.digitalfix_ms_workorders.model;

import jakarta.persistence.Entity;
import lombok.*;

import java.time.LocalDateTime;

@Entity (name = "ORDEN_SERVICIO")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrdenServicio {

    private Long id_orden_servicio;
    private String observacion;
    private LocalDateTime fecha_inicio_servicio;
    private LocalDateTime fecha_cierre_servicio;

}
