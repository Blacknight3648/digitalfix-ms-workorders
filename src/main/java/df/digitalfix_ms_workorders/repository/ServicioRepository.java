package df.digitalfix_ms_workorders.repository;

import df.digitalfix_ms_workorders.model.servicios.Servicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServicioRepository extends JpaRepository<Servicio, Long> {

    boolean existsByCodigo(String codigo);

    // Correlativo del código de servicio (secuencia creada por la migración V3): lo genera la base
    // de datos para que dos altas simultáneas nunca reciban el mismo código.
    @Query(value = "SELECT SEQ_CODIGO_SERVICIO.NEXTVAL FROM DUAL", nativeQuery = true)
    long siguienteCorrelativoCodigo();

    List<Servicio> findAllByOrderByNombreAsc();

    List<Servicio> findByActivoTrueOrderByNombreAsc();

    // Las órdenes referencian el servicio solo por ID (sin relación JPA), por eso se consulta aparte
    // antes de borrar: un servicio ya usado se desactiva en vez de eliminarse.
    @Query("select count(d) > 0 from DetalleOtServicio d where d.idServicio = :idServicio")
    boolean usadoEnOrdenesTrabajo(@Param("idServicio") Long idServicio);

    @Query("select count(d) > 0 from DetalleOrdenServicio d where d.idServicio = :idServicio")
    boolean usadoEnOrdenesServicio(@Param("idServicio") Long idServicio);
}
