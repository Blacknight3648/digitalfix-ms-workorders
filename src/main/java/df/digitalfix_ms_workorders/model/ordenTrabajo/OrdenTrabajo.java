package df.digitalfix_ms_workorders.model.ordenTrabajo;

import df.digitalfix_ms_workorders.enums.EstadoOrden;
import df.digitalfix_ms_workorders.model.clientes.Cliente;
import df.digitalfix_ms_workorders.model.common.DatoInvalidoException;
import df.digitalfix_ms_workorders.model.common.EntidadAuditable;
import df.digitalfix_ms_workorders.model.common.OperacionNoPermitidaException;
import df.digitalfix_ms_workorders.model.common.PoliticaTransicionEstado;
import df.digitalfix_ms_workorders.model.common.SanitizadorTexto;
import df.digitalfix_ms_workorders.model.cuadrilla.Cuadrilla;
import df.digitalfix_ms_workorders.model.proveedores.Proveedor;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Orden de trabajo (OT). Es la raíz del agregado: los detalles de servicio y de repuesto sólo se
 * crean o eliminan a través de ella, para que las reglas (estado editable, duplicados) se cumplan
 * siempre y todo se persista en una misma transacción.
 */
@Entity
@Table(name = "ORDEN_TRABAJO", indexes = {
        @Index(name = "IDX_OT_ESTADO", columnList = "ESTADO_OT"),
        @Index(name = "IDX_OT_CLIENTE", columnList = "ID_CLIENTE")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrdenTrabajo extends EntidadAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_OT", nullable = false, updatable = false)
    private Long idOrdenTrabajo;

    @NotNull(message = "El cliente es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_CLIENTE", nullable = false, foreignKey = @ForeignKey(name = "FK_OT_CLIENTE"))
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_CUADRILLA", foreignKey = @ForeignKey(name = "FK_OT_CUADRILLA"))
    private Cuadrilla cuadrilla;

    @NotNull
    @Column(name = "FECHA_INGRESO_OT", nullable = false, updatable = false)
    private LocalDateTime fechaIngreso;

    @Column(name = "FECHA_CIERRE_OT")
    private LocalDateTime fechaCierre;

    @Size(max = 1000, message = "Las observaciones no pueden exceder 1000 caracteres")
    @Column(name = "OBSERVACIONES_OT", length = 1000)
    private String observaciones;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO_OT", nullable = false, length = 30)
    private EstadoOrden estado;

    @NotNull
    @Column(name = "FECHA_ACTUALIZACION_ESTADO", nullable = false)
    private LocalDateTime fechaActualizacionEstado;

    @Valid
    @BatchSize(size = 50)
    @OneToMany(mappedBy = "ordenTrabajo", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<DetalleOtServicio> detallesServicio = new ArrayList<>();

    @Valid
    @BatchSize(size = 50)
    @OneToMany(mappedBy = "ordenTrabajo", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<DetalleOtRepuesto> detallesRepuesto = new ArrayList<>();

    public OrdenTrabajo(Cliente cliente, String observaciones) {
        this.cliente = Objects.requireNonNull(cliente, "El cliente es obligatorio");
        this.observaciones = SanitizadorTexto.textoMultilinea(observaciones);
        this.estado = EstadoOrden.CREADA;
        this.fechaIngreso = LocalDateTime.now();
        this.fechaActualizacionEstado = this.fechaIngreso;
    }

    public List<DetalleOtServicio> getDetallesServicio() {
        return Collections.unmodifiableList(detallesServicio);
    }

    public List<DetalleOtRepuesto> getDetallesRepuesto() {
        return Collections.unmodifiableList(detallesRepuesto);
    }

    public void actualizarObservaciones(String observaciones) {
        verificarEditable();
        this.observaciones = SanitizadorTexto.textoMultilinea(observaciones);
    }

    /** Regla del caso: no se puede asignar (ni avanzar a ejecución) sin un técnico o cuadrilla asignada. */
    public void cambiarEstado(EstadoOrden nuevoEstado) {
        PoliticaTransicionEstado.validar(this.estado, nuevoEstado);
        if (nuevoEstado == EstadoOrden.ASIGNADA && !tieneTecnicoAsignado()) {
            throw new OperacionNoPermitidaException("No se puede asignar la orden sin un técnico en al menos un servicio o una cuadrilla asignada");
        }
        this.estado = nuevoEstado;
        this.fechaActualizacionEstado = LocalDateTime.now();
        if (nuevoEstado == EstadoOrden.CERRADA) {
            this.fechaCierre = this.fechaActualizacionEstado;
        }
        if (PoliticaTransicionEstado.esTerminal(nuevoEstado) && this.cuadrilla != null) {
            this.cuadrilla.liberar();
        }
    }

    /**
     * Asignación de cuadrilla de terreno: grupo organizado que se desplaza y ejecuta en conjunto.
     * Avanza el estado de la OT a ASIGNADA y marca la cuadrilla como EN_TERRENO.
     */
    public void asignarCuadrilla(Cuadrilla cuadrilla) {
        verificarEditable();
        PoliticaTransicionEstado.validar(this.estado, EstadoOrden.ASIGNADA);
        Objects.requireNonNull(cuadrilla, "La cuadrilla es obligatoria");
        cuadrilla.asignarATerreno();
        this.cuadrilla = cuadrilla;
        this.estado = EstadoOrden.ASIGNADA;
        this.fechaActualizacionEstado = LocalDateTime.now();
    }

    public DetalleOtServicio agregarServicio(Long idServicio, Long idTecnico,
                                             String descripcionManoObra, BigDecimal precioManoObra) {
        verificarEditable();
        boolean duplicado = detallesServicio.stream()
                .anyMatch(d -> d.getIdServicio().equals(idServicio));
        if (duplicado) {
            throw new DatoInvalidoException("El servicio ya está registrado en esta orden de trabajo");
        }
        DetalleOtServicio detalle = new DetalleOtServicio(this, idServicio, idTecnico, descripcionManoObra, precioManoObra);
        detallesServicio.add(detalle);
        return detalle;
    }

    public void asignarTecnico(Long idDetalleServicio, Long idTecnico) {
        verificarEditable();
        buscarDetalleServicio(idDetalleServicio).asignarTecnico(idTecnico);
    }

    public void quitarServicio(Long idDetalleServicio) {
        verificarEditable();
        detallesServicio.remove(buscarDetalleServicio(idDetalleServicio));
    }

    /** Si el mismo repuesto del mismo proveedor ya existe, se acumula la cantidad. */
    public DetalleOtRepuesto agregarRepuesto(Long idRepuesto, Proveedor proveedor, int cantidad) {
        verificarEditable();
        for (DetalleOtRepuesto existente : detallesRepuesto) {
            if (existente.corresponde(idRepuesto, proveedor)) {
                existente.incrementarCantidad(cantidad);
                return existente;
            }
        }
        DetalleOtRepuesto detalle = new DetalleOtRepuesto(this, idRepuesto, proveedor, cantidad);
        detallesRepuesto.add(detalle);
        return detalle;
    }

    public void quitarRepuesto(Long idDetalleRepuesto) {
        verificarEditable();
        DetalleOtRepuesto detalle = detallesRepuesto.stream()
                .filter(d -> Objects.equals(d.getIdDetalleOtRepuesto(), idDetalleRepuesto))
                .findFirst()
                .orElseThrow(() -> new DatoInvalidoException("El repuesto indicado no pertenece a esta orden de trabajo"));
        detallesRepuesto.remove(detalle);
    }

    /** Sólo una OT recién creada se puede borrar; después debe cancelarse o cerrarse. */
    public boolean esEliminable() {
        return estado == EstadoOrden.CREADA;
    }

    /** Total calculado al vuelo; no se persiste porque dependería de los detalles (3FN). */
    public BigDecimal getTotalManoObra() {
        return detallesServicio.stream()
                .map(DetalleOtServicio::getPrecioManoObra)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean tieneTecnicoAsignado() {
        return this.cuadrilla != null || detallesServicio.stream().anyMatch(d -> d.getIdTecnico() != null);
    }

    private DetalleOtServicio buscarDetalleServicio(Long idDetalleServicio) {
        return detallesServicio.stream()
                .filter(d -> Objects.equals(d.getIdDetalleOtServicio(), idDetalleServicio))
                .findFirst()
                .orElseThrow(() -> new DatoInvalidoException("El servicio indicado no pertenece a esta orden de trabajo"));
    }

    private void verificarEditable() {
        if (PoliticaTransicionEstado.esTerminal(estado)) {
            throw new OperacionNoPermitidaException("La orden de trabajo está " + estado + " y no admite cambios");
        }
    }
}
