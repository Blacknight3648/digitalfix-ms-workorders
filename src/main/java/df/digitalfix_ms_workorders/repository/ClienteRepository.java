package df.digitalfix_ms_workorders.repository;

import df.digitalfix_ms_workorders.model.clientes.Cliente;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// El EntityGraph trae dirección, comuna y región en la misma consulta para evitar N+1 al listar.
@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    boolean existsByRut_Numero(Integer numero);

    Optional<Cliente> findByRut_Numero(Integer numero);

    @EntityGraph(attributePaths = {"direccion", "direccion.comuna", "direccion.comuna.region"})
    List<Cliente> findAllByOrderByNombreAsc();

    @Override
    @EntityGraph(attributePaths = {"direccion", "direccion.comuna", "direccion.comuna.region"})
    Optional<Cliente> findById(Long id);
}
