package df.digitalfix_ms_workorders.model.ordenServicio;

import df.digitalfix_ms_workorders.enums.EstadoOrden;
import df.digitalfix_ms_workorders.model.clientes.Cliente;
import df.digitalfix_ms_workorders.model.common.DatoInvalidoException;
import df.digitalfix_ms_workorders.model.common.EntidadAuditable;
import df.digitalfix_ms_workorders.model.common.OperacionNoPermitidaException;
import df.digitalfix_ms_workorders.model.common.PoliticaTransicionEstado;
import df.digitalfix_ms_workorders.model.common.SanitizadorTexto;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Orden de servicio solicitada por un cliente. Raíz de agregado: sus detalles sólo se modifican
 * a través de ella. El total no se persiste, se calcula desde los detalles (3FN).
 */
@Entity
@Table(name = "ORDEN_SERVICIO", indexes = {
        @Index(name = "IDX_OS_ESTADO", columnList = "ESTADO"),
        @Index(name = "IDX_OS_CLIENTE", columnList = "ID_CLIENTE")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrdenServicio extends EntidadAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_ORDEN_SERVICIO", nullable = false, updatable = false)
    private Long idOrdenServicio;

    @NotNull(message = "El cliente es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_CLIENTE", nullable = false, foreignKey = @ForeignKey(name = "FK_OS_CLIENTE"))
    private Cliente cliente;

    /** Técnico asignado (USUARIO en otro servicio); se asigna después de crear la orden. */
    @Positive(message = "El ID del técnico no es válido")
    @Column(name = "ID_TECNICO")
    private Long idTecnico;

    @Size(max = 500, message = "Las observaciones no pueden exceder los 500 caracteres")
    @Column(name = "OBSERVACIONES_OS", length = 500)
    private String observaciones;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", nullable = false, length = 30)
    private EstadoOrden estado;

    @NotNull
    @Column(name = "FECHA_INICIO_SERVICIO", nullable = false, updatable = false)
    private LocalDateTime fechaInicioServicio;

    @Column(name = "FECHA_CIERRE_SERVICIO")
    private LocalDateTime fechaCierreServicio;

    @Valid
    @OneToMany(mappedBy = "ordenServicio", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<DetalleOrdenServicio> detalles = new ArrayList<>();

    public OrdenServicio(Cliente cliente, String observaciones) {
        this.cliente = Objects.requireNonNull(cliente, "El cliente es obligatorio");
        this.observaciones = SanitizadorTexto.textoMultilinea(observaciones);
        this.estado = EstadoOrden.CREADA;
        this.fechaInicioServicio = LocalDateTime.now();
    }

    public List<DetalleOrdenServicio> getDetalles() {
        return Collections.unmodifiableList(detalles);
    }

    public void actualizarObservaciones(String observaciones) {
        verificarEditable();
        this.observaciones = SanitizadorTexto.textoMultilinea(observaciones);
    }

    /** Asignar técnico avanza la orden de CREADA a ASIGNADA. */
    public void asignarTecnico(Long idTecnico) {
        Objects.requireNonNull(idTecnico, "El técnico es obligatorio");
        PoliticaTransicionEstado.validar(this.estado, EstadoOrden.ASIGNADA);
        this.idTecnico = idTecnico;
        aplicarEstado(EstadoOrden.ASIGNADA);
    }

    public void cambiarEstado(EstadoOrden nuevoEstado) {
        PoliticaTransicionEstado.validar(this.estado, nuevoEstado);
        if (nuevoEstado == EstadoOrden.ASIGNADA && this.idTecnico == null) {
            throw new OperacionNoPermitidaException("Para asignar la orden use asignarTecnico(idTecnico)");
        }
        aplicarEstado(nuevoEstado);
    }

    private void aplicarEstado(EstadoOrden nuevoEstado) {
        this.estado = nuevoEstado;
        if (nuevoEstado == EstadoOrden.CERRADA) {
            this.fechaCierreServicio = LocalDateTime.now();
        }
    }

    public DetalleOrdenServicio agregarDetalle(Long idServicio, int cantidad, BigDecimal precioUnitario) {
        verificarEditable();
        boolean duplicado = detalles.stream().anyMatch(d -> d.getIdServicio().equals(idServicio));
        if (duplicado) {
            throw new DatoInvalidoException("El servicio ya está registrado en esta orden");
        }
        DetalleOrdenServicio detalle = new DetalleOrdenServicio(this, idServicio, cantidad, precioUnitario);
        detalles.add(detalle);
        return detalle;
    }

    public void quitarDetalle(DetalleOrdenServicio detalle) {
        verificarEditable();
        detalles.remove(detalle);
    }

    public BigDecimal getTotal() {
        return detalles.stream()
                .map(DetalleOrdenServicio::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void verificarEditable() {
        if (PoliticaTransicionEstado.esTerminal(estado)) {
            throw new OperacionNoPermitidaException("La orden de servicio está " + estado + " y no admite cambios");
        }
    }
}
