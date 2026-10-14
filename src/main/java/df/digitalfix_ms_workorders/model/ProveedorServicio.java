package df.digitalfix_ms_workorders.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "PROVEEDOR_SERVICIO")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ProveedorServicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id_proveedor_Servicio;
    private Integer run_proveedor; //Hay que crear un validador con cálculo para evitar inyecciones y sanitizar los datos
    private String dv_proveedor; //Hay que limpiar la validación con cálculo de dv y formato estricto de entrada (Sólo aceptará los números de los dv y k en un formato específico)
    private String razon_social_proveedor;
    private String giro_proveedor;
    private Integer telefono_proveedor;
    private String correo_proveedor;

}
