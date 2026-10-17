package df.digitalfix_ms_workorders.dto.OrdenTrabajo;

import df.digitalfix_ms_workorders.enums.EstadoCuadrilla;
import df.digitalfix_ms_workorders.enums.EstadoOrden;
import df.digitalfix_ms_workorders.model.clientes.Cliente;
import df.digitalfix_ms_workorders.model.cuadrilla.Cuadrilla;
import df.digitalfix_ms_workorders.model.ordenTrabajo.DetalleOtRepuesto;
import df.digitalfix_ms_workorders.model.ordenTrabajo.DetalleOtServicio;
import df.digitalfix_ms_workorders.model.ordenTrabajo.OrdenTrabajo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

// Se arma dentro de la transacción del servicio, porque cliente y detalles se cargan en forma LAZY.
public record OrdenTrabajoResponse(
        Long idOrdenTrabajo,
        ClienteResumen cliente,
        CuadrillaResumen cuadrilla,
        EstadoOrden estado,
        String observaciones,
        LocalDateTime fechaIngreso,
        LocalDateTime fechaActualizacionEstado,
        LocalDateTime fechaCierre,
        List<Servicio> servicios,
        List<Repuesto> repuestos,
        BigDecimal totalManoObra
) {

    public record ClienteResumen(Long idCliente, String rut, String nombreCompleto) {

        static ClienteResumen from(Cliente cliente) {
            return new ClienteResumen(cliente.getIdCliente(), cliente.getRut().formateado(), cliente.getNombreCompleto());
        }
    }

    public record CuadrillaResumen(Long idCuadrilla, String nombre, Long idTecnicoLider,
                                   EstadoCuadrilla estado, Set<Long> miembros) {

        static CuadrillaResumen from(Cuadrilla c) {
            if (c == null) {
                return null;
            }
            return new CuadrillaResumen(c.getIdCuadrilla(), c.getNombre(), c.getIdTecnicoLider(),
                    c.getEstado(), c.getMiembros());
        }
    }

    public record Servicio(Long idDetalle, Long idServicio, Long idTecnico,
                           String descripcionManoObra, BigDecimal precioManoObra) {

        static Servicio from(DetalleOtServicio detalle) {
            return new Servicio(detalle.getIdDetalleOtServicio(), detalle.getIdServicio(), detalle.getIdTecnico(),
                    detalle.getDescripcionManoObra(), detalle.getPrecioManoObra());
        }
    }

    public record Repuesto(Long idDetalle, Long idRepuesto, Long idProveedor, Integer cantidadUtilizada) {

        static Repuesto from(DetalleOtRepuesto detalle) {
            return new Repuesto(detalle.getIdDetalleOtRepuesto(), detalle.getIdRepuesto(),
                    detalle.getProveedor().getIdProveedor(), detalle.getCantidadUtilizada());
        }
    }

    public static OrdenTrabajoResponse from(OrdenTrabajo orden) {
        return new OrdenTrabajoResponse(
                orden.getIdOrdenTrabajo(),
                ClienteResumen.from(orden.getCliente()),
                CuadrillaResumen.from(orden.getCuadrilla()),
                orden.getEstado(),
                orden.getObservaciones(),
                orden.getFechaIngreso(),
                orden.getFechaActualizacionEstado(),
                orden.getFechaCierre(),
                orden.getDetallesServicio().stream().map(Servicio::from).toList(),
                orden.getDetallesRepuesto().stream().map(Repuesto::from).toList(),
                orden.getTotalManoObra());
    }
}
