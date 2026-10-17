package df.digitalfix_ms_workorders.repository;

import df.digitalfix_ms_workorders.enums.EstadoCuadrilla;
import df.digitalfix_ms_workorders.model.cuadrilla.Cuadrilla;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CuadrillaRepository extends JpaRepository<Cuadrilla, Long> {

    boolean existsByNombreIgnoreCase(String nombre);

    List<Cuadrilla> findByEstado(EstadoCuadrilla estado);

    /**
     * Bloqueo pesimista para asignación concurrente (ACID - Aislamiento).
     * En Oracle ejecuta "SELECT ... FOR UPDATE" asegurando que dos supervisores
     * no puedan asignar la misma cuadrilla a dos órdenes distintas en paralelo.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Cuadrilla c WHERE c.idCuadrilla = :id")
    Optional<Cuadrilla> findByIdConBloqueo(@Param("id") Long id);
}
