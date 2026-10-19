package df.digitalfix_ms_workorders.repository;

import df.digitalfix_ms_workorders.model.ubicacion.Comuna;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComunaRepository extends JpaRepository<Comuna, Long> {

    List<Comuna> findByRegion_IdRegionOrderByNombreAsc(Long idRegion);

    // La región se necesita para armar el texto completo de la dirección.
    @Override
    @EntityGraph(attributePaths = "region")
    Optional<Comuna> findById(Long id);
}
