package df.digitalfix_ms_workorders.model.cuadrilla;

import df.digitalfix_ms_workorders.enums.EstadoCuadrilla;
import df.digitalfix_ms_workorders.model.common.DatoInvalidoException;
import df.digitalfix_ms_workorders.model.common.EntidadAuditable;
import df.digitalfix_ms_workorders.model.common.OperacionNoPermitidaException;
import df.digitalfix_ms_workorders.model.common.SanitizadorTexto;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Cuadrilla de terreno: grupo organizado de trabajadores que se desplaza directamente al lugar
 * donde se requiere una labor específica para ejecutarla en conjunto.
 * Encapsula la composición del equipo y su disponibilidad operativa.
 */
@Entity
@Table(name = "CUADRILLA", uniqueConstraints = @UniqueConstraint(name = "UK_CUADRILLA_NOMBRE", columnNames = "NOMBRE_CUADRILLA"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cuadrilla extends EntidadAuditable {

    public static final int MINIMO_TRABAJADORES = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_CUADRILLA", nullable = false, updatable = false)
    private Long idCuadrilla;

    @NotBlank(message = "El nombre de la cuadrilla es obligatorio")
    @Size(max = 100, message = "El nombre de la cuadrilla no puede exceder 100 caracteres")
    @Column(name = "NOMBRE_CUADRILLA", nullable = false, length = 100)
    private String nombre;

    @NotNull(message = "El técnico líder es obligatorio")
    @Positive(message = "El ID del técnico líder no es válido")
    @Column(name = "ID_TECNICO_LIDER", nullable = false)
    private Long idTecnicoLider;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO_CUADRILLA", nullable = false, length = 30)
    private EstadoCuadrilla estado;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "CUADRILLA_MIEMBRO",
            joinColumns = @JoinColumn(name = "ID_CUADRILLA", foreignKey = @ForeignKey(name = "FK_MIEMBRO_CUADRILLA")))
    @Column(name = "ID_TECNICO", nullable = false)
    private Set<Long> miembros = new HashSet<>();

    public Cuadrilla(String nombre, Long idTecnicoLider, Set<Long> otrosMiembros) {
        this.nombre = SanitizadorTexto.texto(nombre);
        this.idTecnicoLider = Objects.requireNonNull(idTecnicoLider, "El técnico líder es obligatorio");
        this.estado = EstadoCuadrilla.DISPONIBLE;
        this.miembros.add(idTecnicoLider);
        if (otrosMiembros != null) {
            otrosMiembros.stream().filter(Objects::nonNull).forEach(this.miembros::add);
        }
    }

    public Set<Long> getMiembros() {
        return Collections.unmodifiableSet(miembros);
    }

    public void actualizarDatos(String nombre, Long idTecnicoLider, EstadoCuadrilla estado) {
        if (this.estado == EstadoCuadrilla.EN_TERRENO && estado == EstadoCuadrilla.INACTIVA) {
            throw new OperacionNoPermitidaException("No se puede desactivar una cuadrilla que está actualmente en terreno");
        }
        this.nombre = SanitizadorTexto.texto(nombre);
        if (idTecnicoLider != null) {
            this.idTecnicoLider = idTecnicoLider;
            this.miembros.add(idTecnicoLider);
        }
        if (estado != null) {
            this.estado = estado;
        }
    }

    public void agregarMiembro(Long idTecnico) {
        Objects.requireNonNull(idTecnico, "El técnico es obligatorio");
        this.miembros.add(idTecnico);
    }

    public void quitarMiembro(Long idTecnico) {
        if (Objects.equals(this.idTecnicoLider, idTecnico)) {
            throw new OperacionNoPermitidaException("No se puede remover al técnico líder de la cuadrilla sin reasignar el liderazgo");
        }
        if (this.estado == EstadoCuadrilla.EN_TERRENO && this.miembros.size() <= MINIMO_TRABAJADORES) {
            throw new OperacionNoPermitidaException("La cuadrilla está en terreno y no puede quedar con menos de " + MINIMO_TRABAJADORES + " integrantes");
        }
        this.miembros.remove(idTecnico);
    }

    /**
     * Regla de negocio de Cuadrilla:
     * Para asignarse a terreno, debe estar DISPONIBLE y contar con al menos MINIMO_TRABAJADORES (líder + apoyo).
     */
    public void asignarATerreno() {
        if (this.estado == EstadoCuadrilla.INACTIVA) {
            throw new OperacionNoPermitidaException("La cuadrilla " + nombre + " está INACTIVA y no puede asignarse");
        }
        if (this.estado == EstadoCuadrilla.EN_TERRENO) {
            throw new OperacionNoPermitidaException("La cuadrilla " + nombre + " ya se encuentra en terreno atendiendo otra orden");
        }
        if (this.miembros.size() < MINIMO_TRABAJADORES) {
            throw new DatoInvalidoException("La cuadrilla debe tener al menos " + MINIMO_TRABAJADORES +
                    " trabajadores (incluyendo al líder) para operar en conjunto en terreno");
        }
        this.estado = EstadoCuadrilla.EN_TERRENO;
    }

    public void liberar() {
        if (this.estado == EstadoCuadrilla.EN_TERRENO) {
            this.estado = EstadoCuadrilla.DISPONIBLE;
        }
    }
}
