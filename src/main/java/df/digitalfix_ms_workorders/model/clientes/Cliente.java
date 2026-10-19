package df.digitalfix_ms_workorders.model.clientes;

import df.digitalfix_ms_workorders.model.common.EntidadAuditable;
import df.digitalfix_ms_workorders.model.common.PatronesValidacion;
import df.digitalfix_ms_workorders.model.common.Rut;
import df.digitalfix_ms_workorders.model.common.SanitizadorTexto;
import df.digitalfix_ms_workorders.model.ubicacion.Direccion;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

/**
 * Cliente final: persona o empresa, identificada por su RUT. El nombre se pide en un solo campo
 * (nombre completo o razón social) para que el registro sea simple. La dirección es propia del
 * cliente (1 a 1) y se normaliza en DIRECCION → COMUNA → REGION (3FN).
 */
@Entity
@Table(name = "CLIENTE",
        uniqueConstraints = @UniqueConstraint(name = "UK_CLIENTE_RUN", columnNames = "RUN_CLIENTE"),
        indexes = @Index(name = "IDX_CLIENTE_EMAIL", columnList = "EMAIL_CLIENTE"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cliente extends EntidadAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_CLIENTE", nullable = false, updatable = false)
    private Long idCliente;

    @NotNull(message = "El RUT del cliente es obligatorio")
    @Embedded
    @AttributeOverride(name = "numero", column = @Column(name = "RUN_CLIENTE", nullable = false, updatable = false))
    @AttributeOverride(name = "dv", column = @Column(name = "DV_CLIENTE", nullable = false, updatable = false, length = 1))
    private Rut rut;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 150, message = "El nombre no puede exceder 150 caracteres")
    @Pattern(regexp = PatronesValidacion.RAZON_SOCIAL, message = "El nombre contiene caracteres no permitidos")
    @Column(name = "NOMBRE_CLIENTE", nullable = false, length = 150)
    private String nombre;

    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = PatronesValidacion.TELEFONO_CHILE, message = "El teléfono debe tener el formato +56XXXXXXXXX")
    @Column(name = "TELEFONO_CLIENTE", nullable = false, length = 12)
    private String telefono;

    @Pattern(regexp = PatronesValidacion.TELEFONO_CHILE, message = "El teléfono secundario debe tener el formato +56XXXXXXXXX")
    @Column(name = "TELEFONO_SECUNDARIO_CLIENTE", length = 12)
    private String telefonoSecundario;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene un formato válido")
    @Size(max = 254, message = "El correo no puede exceder 254 caracteres")
    @Column(name = "EMAIL_CLIENTE", nullable = false, length = 254)
    private String email;

    /**
     * Dónde se presta el servicio o se entrega el repuesto. Nula solo en clientes registrados
     * antes de que existiera la dirección normalizada (la API la exige al crear y editar).
     */
    @Valid
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "ID_DIRECCION", foreignKey = @ForeignKey(name = "FK_CLIENTE_DIRECCION"))
    private Direccion direccion;

    public Cliente(Rut rut, String nombre, String telefono, String telefonoSecundario, String email,
                   Direccion direccion) {
        this.rut = Objects.requireNonNull(rut, "El RUT del cliente es obligatorio");
        actualizarNombre(nombre);
        actualizarContacto(telefono, telefonoSecundario, email);
        asignarDireccion(direccion);
    }

    public void actualizarNombre(String nombre) {
        this.nombre = SanitizadorTexto.texto(nombre);
    }

    public void actualizarContacto(String telefono, String telefonoSecundario, String email) {
        this.telefono = SanitizadorTexto.telefono(telefono);
        this.telefonoSecundario = SanitizadorTexto.telefono(telefonoSecundario);
        this.email = SanitizadorTexto.email(email);
    }

    public void asignarDireccion(Direccion direccion) {
        this.direccion = Objects.requireNonNull(direccion, "La dirección es obligatoria");
    }

    /** Se mantiene para las respuestas de órdenes, que exponen "nombreCompleto". */
    public String getNombreCompleto() {
        return nombre;
    }
}
