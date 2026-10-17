package df.digitalfix_ms_workorders.repository;

import df.digitalfix_ms_workorders.model.clientes.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    boolean existsByRut_Numero(Integer numero);

    Optional<Cliente> findByRut_Numero(Integer numero);
}
