package df.digitalfix_ms_workorders.model.clientes;

import df.digitalfix_ms_workorders.model.common.EntidadAuditable;
import df.digitalfix_ms_workorders.model.common.PatronesValidacion;
import df.digitalfix_ms_workorders.model.common.Rut;
import df.digitalfix_ms_workorders.model.common.SanitizadorTexto;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

/**
 * Cliente final. 3FN: el nombre se separa en componentes atómicos y la dirección se referencia
 * por ID al almacén central de direcciones (no se duplican sus columnas aquí).
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

    @NotBlank(message = "Los nombres son obligatorios")
    @Size(max = 100, message = "Los nombres no pueden exceder 100 caracteres")
    @Pattern(regexp = PatronesValidacion.NOMBRE_PERSONA, message = "Los nombres contienen caracteres no permitidos")
    @Column(name = "NOMBRES_CLIENTE", nullable = false, length = 100)
    private String nombres;

    @NotBlank(message = "El apellido paterno es obligatorio")
    @Size(max = 60, message = "El apellido paterno no puede exceder 60 caracteres")
    @Pattern(regexp = PatronesValidacion.NOMBRE_PERSONA, message = "El apellido paterno contiene caracteres no permitidos")
    @Column(name = "APELLIDO_PATERNO_CLIENTE", nullable = false, length = 60)
    private String apellidoPaterno;

    @Size(max = 60, message = "El apellido materno no puede exceder 60 caracteres")
    @Pattern(regexp = PatronesValidacion.NOMBRE_PERSONA, message = "El apellido materno contiene caracteres no permitidos")
    @Column(name = "APELLIDO_MATERNO_CLIENTE", length = 60)
    private String apellidoMaterno;

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

    /** Referencia a DIRECCION en el almacén central (otro servicio), por eso no es @ManyToOne. */
    @Positive(message = "El ID de dirección no es válido")
    @Column(name = "ID_DIRECCION")
    private Long idDireccion;

    public Cliente(Rut rut, String nombres, String apellidoPaterno, String apellidoMaterno,
                   String telefono, String email) {
        this.rut = Objects.requireNonNull(rut, "El RUT del cliente es obligatorio");
        actualizarNombre(nombres, apellidoPaterno, apellidoMaterno);
        actualizarContacto(telefono, null, email);
    }

    public void actualizarNombre(String nombres, String apellidoPaterno, String apellidoMaterno) {
        this.nombres = SanitizadorTexto.texto(nombres);
        this.apellidoPaterno = SanitizadorTexto.texto(apellidoPaterno);
        this.apellidoMaterno = SanitizadorTexto.texto(apellidoMaterno);
    }

    public void actualizarContacto(String telefono, String telefonoSecundario, String email) {
        this.telefono = SanitizadorTexto.telefono(telefono);
        this.telefonoSecundario = SanitizadorTexto.telefono(telefonoSecundario);
        this.email = SanitizadorTexto.email(email);
    }

    public void asignarDireccion(Long idDireccion) {
        this.idDireccion = idDireccion;
    }

    /** Valor derivado: no se persiste para no introducir una dependencia transitiva. */
    public String getNombreCompleto() {
        return apellidoMaterno == null
                ? nombres + " " + apellidoPaterno
                : nombres + " " + apellidoPaterno + " " + apellidoMaterno;
    }
}
