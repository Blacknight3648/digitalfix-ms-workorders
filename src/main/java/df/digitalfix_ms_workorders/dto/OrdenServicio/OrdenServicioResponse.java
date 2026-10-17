package df.digitalfix_ms_workorders.dto.OrdenServicio;

import df.digitalfix_ms_workorders.enums.EstadoOrden;
import df.digitalfix_ms_workorders.model.clientes.Cliente;
import df.digitalfix_ms_workorders.model.ordenServicio.DetalleOrdenServicio;
import df.digitalfix_ms_workorders.model.ordenServicio.OrdenServicio;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrdenServicioResponse(
        Long idOrdenServicio,
        ClienteResumen cliente,
        Long idTecnico,
        EstadoOrden estado,
        String observaciones,
        LocalDateTime fechaInicioServicio,
        LocalDateTime fechaCierreServicio,
        List<Detalle> detalles,
        BigDecimal total
) {

    public record ClienteResumen(Long idCliente, String rut, String nombreCompleto) {
        static ClienteResumen from(Cliente c) {
            return new ClienteResumen(c.getIdCliente(), c.getRut().formateado(), c.getNombreCompleto());
        }
    }

    public record Detalle(
            Long idDetalleServicio,
            Long idServicio,
            Integer cantidad,
            BigDecimal precioUnitario,
            BigDecimal subtotal
    ) {
        static Detalle from(DetalleOrdenServicio d) {
            return new Detalle(
                    d.getIdDetalleServicio(),
                    d.getIdServicio(),
                    d.getCantidad(),
                    d.getPrecioUnitario(),
                    d.getSubtotal()
            );
        }
    }

    public static OrdenServicioResponse from(OrdenServicio orden) {
        return new OrdenServicioResponse(
                orden.getIdOrdenServicio(),
                ClienteResumen.from(orden.getCliente()),
                orden.getIdTecnico(),
                orden.getEstado(),
                orden.getObservaciones(),
                orden.getFechaInicioServicio(),
                orden.getFechaCierreServicio(),
                orden.getDetalles().stream().map(Detalle::from).toList(),
                orden.getTotal()
        );
    }
}
