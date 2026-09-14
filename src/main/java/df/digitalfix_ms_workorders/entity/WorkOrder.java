package df.digitalfix_ms_workorders.entity;

import df.digitalfix_ms_workorders.enums.WorkOrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "WORK_ORDERS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "CLIENTE", nullable = false, length = 150)
    private String cliente;

    @Column(name = "SERVICIO", nullable = false, length = 200)
    private String servicio;

    @Column(name = "TECNICO", length = 150)
    private String tecnico;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", nullable = false, length = 30)
    private WorkOrderStatus estado;

    @Column(name = "FECHA_CREACION", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    public void prePersist() {
        if (this.fechaCreacion == null) {
            this.fechaCreacion = LocalDateTime.now();
        }
        if (this.estado == null) {
            this.estado = WorkOrderStatus.CREADA;
        }
    }
}
