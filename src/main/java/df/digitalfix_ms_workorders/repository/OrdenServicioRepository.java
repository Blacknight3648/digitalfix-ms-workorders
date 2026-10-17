package df.digitalfix_ms_workorders.repository;

import df.digitalfix_ms_workorders.enums.EstadoOrden;
import df.digitalfix_ms_workorders.model.ordenServicio.OrdenServicio;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrdenServicioRepository extends JpaRepository<OrdenServicio, Long> {

    @Override
    @EntityGraph(attributePaths = {"cliente", "detalles"})
    List<OrdenServicio> findAll();

    @Override
    @EntityGraph(attributePaths = {"cliente", "detalles"})
    Optional<OrdenServicio> findById(Long id);

    @EntityGraph(attributePaths = {"cliente", "detalles"})
    List<OrdenServicio> findByEstado(EstadoOrden estado);

    @EntityGraph(attributePaths = {"cliente", "detalles"})
    List<OrdenServicio> findByCliente_IdCliente(Long idCliente);

    @EntityGraph(attributePaths = {"cliente", "detalles"})
    List<OrdenServicio> findByEstadoAndCliente_IdCliente(EstadoOrden estado, Long idCliente);
}
