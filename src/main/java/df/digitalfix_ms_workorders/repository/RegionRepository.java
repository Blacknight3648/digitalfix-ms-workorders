package df.digitalfix_ms_workorders.repository;

import df.digitalfix_ms_workorders.model.ubicacion.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegionRepository extends JpaRepository<Region, Long> {

    List<Region> findAllByOrderByOrdenGeograficoAsc();
}
