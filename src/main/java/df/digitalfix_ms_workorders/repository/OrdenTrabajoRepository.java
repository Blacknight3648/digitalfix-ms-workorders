package df.digitalfix_ms_workorders.repository;

import df.digitalfix_ms_workorders.enums.EstadoOrden;
import df.digitalfix_ms_workorders.model.ordenTrabajo.OrdenTrabajo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// El EntityGraph trae el cliente en la misma consulta para evitar N+1 al armar las respuestas.
@Repository
public interface OrdenTrabajoRepository extends JpaRepository<OrdenTrabajo, Long> {

    @Override
    @EntityGraph(attributePaths = "cliente")
    List<OrdenTrabajo> findAll();

    @Override
    @EntityGraph(attributePaths = "cliente")
    Optional<OrdenTrabajo> findById(Long id);

    @EntityGraph(attributePaths = "cliente")
    List<OrdenTrabajo> findByEstado(EstadoOrden estado);
}
