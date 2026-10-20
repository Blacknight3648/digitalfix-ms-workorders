package df.digitalfix_ms_workorders.messaging;

import df.digitalfix_ms_workorders.enums.EstadoOrden;
import df.digitalfix_ms_workorders.model.clientes.Cliente;
import df.digitalfix_ms_workorders.model.cuadrilla.Cuadrilla;
import df.digitalfix_ms_workorders.model.ordenTrabajo.OrdenTrabajo;

import java.math.BigDecimal;

/**
 * Evento de dominio de una orden de trabajo. Se arma dentro de la transacción (con las entidades
 * cargadas) y se publica a RabbitMQ recién después del commit (ver {@link OrdenTrabajoEventosListener}),
 * así nunca se notifica un cambio que terminó en rollback.
 */
public record OrdenTrabajoEvento(
        Tipo tipo,
        Long idOrden,
        EstadoOrden estado,
        String clienteEmail,
        String clienteNombre,
        String clienteTelefono,
        String direccion,
        String observaciones,
        BigDecimal total,
        Long idCuadrilla,
        String nombreCuadrilla,
        Long idTecnicoLider
) {

    public enum Tipo { CREADA, ESTADO_CAMBIADO, CUADRILLA_ASIGNADA }

    public static OrdenTrabajoEvento de(Tipo tipo, OrdenTrabajo orden) {
        Cliente cliente = orden.getCliente();
        Cuadrilla cuadrilla = orden.getCuadrilla();
        return new OrdenTrabajoEvento(
                tipo,
                orden.getIdOrdenTrabajo(),
                orden.getEstado(),
                cliente.getEmail(),
                cliente.getNombreCompleto(),
                cliente.getTelefono(),
                cliente.getDireccion() != null ? cliente.getDireccion().textoCompleto() : null,
                orden.getObservaciones(),
                orden.getTotalManoObra(),
                cuadrilla != null ? cuadrilla.getIdCuadrilla() : null,
                cuadrilla != null ? cuadrilla.getNombre() : null,
                cuadrilla != null ? cuadrilla.getIdTecnicoLider() : null
        );
    }
}
