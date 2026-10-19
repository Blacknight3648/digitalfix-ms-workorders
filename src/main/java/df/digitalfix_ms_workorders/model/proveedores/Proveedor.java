package df.digitalfix_ms_workorders.model.proveedores;

import df.digitalfix_ms_workorders.enums.TipoProveedor;
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
 * Proveedor de servicios tercerizados o de productos (repuestos e insumos), según {@link TipoProveedor}.
 * El RUT se valida con dígito verificador (ver {@link Rut}) y la dirección es propia del proveedor
 * (1 a 1), normalizada en DIRECCION → COMUNA → REGION (3FN).
 */
@Entity
@Table(name = "PROVEEDOR",
        uniqueConstraints = @UniqueConstraint(name = "UK_PROVEEDOR_RUN", columnNames = "RUN_PROVEEDOR"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Proveedor extends EntidadAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_PROVEEDOR", nullable = false, updatable = false)
    private Long idProveedor;

    @NotNull(message = "El RUT del proveedor es obligatorio")
    @Embedded
    @AttributeOverride(name = "numero", column = @Column(name = "RUN_PROVEEDOR", nullable = false, updatable = false))
    @AttributeOverride(name = "dv", column = @Column(name = "DV_PROVEEDOR", nullable = false, updatable = false, length = 1))
    private Rut rut;

    @NotBlank(message = "La razón social es obligatoria")
    @Size(max = 150, message = "La razón social no puede exceder 150 caracteres")
    @Pattern(regexp = PatronesValidacion.RAZON_SOCIAL, message = "La razón social contiene caracteres no permitidos")
    @Column(name = "RAZON_SOCIAL_PROVEEDOR", nullable = false, length = 150)
    private String razonSocial;

    @NotNull(message = "El tipo de proveedor es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO_PROVEEDOR", nullable = false, length = 20)
    private TipoProveedor tipo;

    @Size(max = 150, message = "El giro no puede exceder 150 caracteres")
    @Pattern(regexp = PatronesValidacion.RAZON_SOCIAL, message = "El giro contiene caracteres no permitidos")
    @Column(name = "GIRO_PROVEEDOR", length = 150)
    private String giro;

    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = PatronesValidacion.TELEFONO_CHILE, message = "El teléfono debe tener el formato +56XXXXXXXXX")
    @Column(name = "TELEFONO_PROVEEDOR", nullable = false, length = 12)
    private String telefono;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene un formato válido")
    @Size(max = 254, message = "El correo no puede exceder 254 caracteres")
    @Column(name = "EMAIL_PROVEEDOR", nullable = false, length = 254)
    private String email;

    /** Nula solo en proveedores registrados antes de la dirección normalizada; la API la exige. */
    @Valid
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "ID_DIRECCION", foreignKey = @ForeignKey(name = "FK_PROVEEDOR_DIRECCION"))
    private Direccion direccion;

    public Proveedor(Rut rut, String razonSocial, TipoProveedor tipo, String giro, String telefono, String email,
                     Direccion direccion) {
        this.rut = Objects.requireNonNull(rut, "El RUT del proveedor es obligatorio");
        actualizarDatosComerciales(razonSocial, tipo, giro);
        actualizarContacto(telefono, email);
        asignarDireccion(direccion);
    }

    public void actualizarDatosComerciales(String razonSocial, TipoProveedor tipo, String giro) {
        this.razonSocial = SanitizadorTexto.texto(razonSocial);
        this.tipo = Objects.requireNonNull(tipo, "El tipo de proveedor es obligatorio");
        this.giro = SanitizadorTexto.texto(giro);
    }

    public void actualizarContacto(String telefono, String email) {
        this.telefono = SanitizadorTexto.telefono(telefono);
        this.email = SanitizadorTexto.email(email);
    }

    public void asignarDireccion(Direccion direccion) {
        this.direccion = Objects.requireNonNull(direccion, "La dirección es obligatoria");
    }

    public boolean abasteceProductos() {
        return tipo == TipoProveedor.PRODUCTOS;
    }
}
