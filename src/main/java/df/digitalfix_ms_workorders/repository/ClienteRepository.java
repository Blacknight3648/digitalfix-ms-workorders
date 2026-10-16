package df.digitalfix_ms_workorders.repository;

import df.digitalfix_ms_workorders.model.clientes.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
}
