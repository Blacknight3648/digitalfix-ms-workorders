package df.digitalfix_ms_workorders.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ORDEN_SERVICIO")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrdenServicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id_orden_servicio;
    private String observacion;
    private LocalDateTime fecha_inicio_servicio;
    private LocalDateTime fecha_cierre_servicio;

}
