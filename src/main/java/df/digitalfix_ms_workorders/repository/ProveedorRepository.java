package df.digitalfix_ms_workorders.repository;

import df.digitalfix_ms_workorders.model.proveedores.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {
}
