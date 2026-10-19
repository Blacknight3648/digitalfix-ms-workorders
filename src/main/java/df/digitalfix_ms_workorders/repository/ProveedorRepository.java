package df.digitalfix_ms_workorders.repository;

import df.digitalfix_ms_workorders.enums.TipoProveedor;
import df.digitalfix_ms_workorders.model.proveedores.Proveedor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// El EntityGraph trae dirección, comuna y región en la misma consulta para evitar N+1 al listar.
@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    boolean existsByRut_Numero(Integer numero);

    Optional<Proveedor> findByRut_Numero(Integer numero);

    @EntityGraph(attributePaths = {"direccion", "direccion.comuna", "direccion.comuna.region"})
    List<Proveedor> findAllByOrderByRazonSocialAsc();

    @EntityGraph(attributePaths = {"direccion", "direccion.comuna", "direccion.comuna.region"})
    List<Proveedor> findByTipoOrderByRazonSocialAsc(TipoProveedor tipo);

    @Override
    @EntityGraph(attributePaths = {"direccion", "direccion.comuna", "direccion.comuna.region"})
    Optional<Proveedor> findById(Long id);
}
