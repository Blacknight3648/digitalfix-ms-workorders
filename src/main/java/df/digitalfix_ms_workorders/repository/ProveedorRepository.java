package df.digitalfix_ms_workorders.repository;

import df.digitalfix_ms_workorders.model.proveedores.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    boolean existsByRut_Numero(Integer numero);

    Optional<Proveedor> findByRut_Numero(Integer numero);
}
