package df.digitalfix_ms_workorders.service.impl;

import df.digitalfix_ms_workorders.dto.Cliente.ClienteRequest;
import df.digitalfix_ms_workorders.dto.OrdenServicio.DetalleServicioRequest;
import df.digitalfix_ms_workorders.dto.OrdenTrabajo.OrdenTrabajoRequest;
import df.digitalfix_ms_workorders.dto.OrdenTrabajo.OrdenTrabajoResponse;
import df.digitalfix_ms_workorders.dto.Portal.OrdenPortalRequest;
import df.digitalfix_ms_workorders.dto.Portal.SeguimientoOrdenResponse;
import df.digitalfix_ms_workorders.dto.Portal.ServicioPortalResponse;
import df.digitalfix_ms_workorders.dto.Servicio.ServicioResponse;
import df.digitalfix_ms_workorders.enums.EstadoOrden;
import df.digitalfix_ms_workorders.model.common.DatoInvalidoException;
import df.digitalfix_ms_workorders.model.common.Rut;
import df.digitalfix_ms_workorders.repository.ClienteRepository;
import df.digitalfix_ms_workorders.service.ClienteService;
import df.digitalfix_ms_workorders.service.OrdenTrabajoService;
import df.digitalfix_ms_workorders.service.PortalExternoService;
import df.digitalfix_ms_workorders.service.ServicioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortalExternoServiceImpl implements PortalExternoService {

    static final int MAX_OBSERVACIONES = 1000;

    // Hitos que ve el cliente, en el orden del flujo (CANCELADA se trata aparte).
    private static final List<Object[]> HITOS = List.of(
            new Object[]{EstadoOrden.CREADA, "Orden creada", "Solicitud recibida por la central"},
            new Object[]{EstadoOrden.ASIGNADA, "Cuadrilla asignada", "Un equipo técnico tomó tu orden"},
            new Object[]{EstadoOrden.EN_DESPLAZAMIENTO, "En camino", "El equipo va hacia tu dirección"},
            new Object[]{EstadoOrden.EN_EJECUCION, "En terreno", "Trabajo en ejecución"},
            new Object[]{EstadoOrden.CERRADA, "Finalizada", "Trabajo terminado y certificado"}
    );

    private static final Map<EstadoOrden, String> ETIQUETA = Map.of(
            EstadoOrden.CREADA, "Orden creada",
            EstadoOrden.ASIGNADA, "Cuadrilla asignada",
            EstadoOrden.EN_DESPLAZAMIENTO, "En camino",
            EstadoOrden.EN_EJECUCION, "En terreno",
            EstadoOrden.CERRADA, "Finalizada",
            EstadoOrden.CANCELADA, "Cancelada"
    );

    private final ClienteRepository clienteRepository;
    private final ClienteService clienteService;
    private final OrdenTrabajoService ordenTrabajoService;
    private final ServicioService servicioService;

    @Override
    public List<ServicioPortalResponse> serviciosActivos() {
        return servicioService.listar(true).stream().map(ServicioPortalResponse::from).toList();
    }

    // Una sola transacción: si la OT no se puede crear (p. ej. un servicio inactivo), tampoco
    // queda creado el cliente nuevo.
    @Override
    @Transactional
    public SeguimientoOrdenResponse crearOrden(OrdenPortalRequest request) {
        Long idCliente = obtenerOCrearCliente(request);

        List<DetalleServicioRequest> servicios = Optional.ofNullable(request.servicios()).orElse(List.of())
                .stream().distinct()
                .map(id -> new DetalleServicioRequest(id, null, null, null))
                .toList();

        OrdenTrabajoResponse orden = ordenTrabajoService.crear(
                new OrdenTrabajoRequest(idCliente, observaciones(request), servicios));
        log.info("OT {} creada desde el Portal Cliente (pedido {})", orden.idOrdenTrabajo(), request.codigoPedido());
        return seguimientoDe(orden, nombresServicios());
    }

    @Override
    public List<SeguimientoOrdenResponse> ordenesDelCliente(String rut) {
        Rut rutCliente = Rut.of(rut);
        return clienteRepository.findByRut_Numero(rutCliente.getNumero())
                .map(cliente -> {
                    Map<Long, String> nombres = nombresServicios();
                    return ordenTrabajoService.listar(null, cliente.getIdCliente()).stream()
                            .sorted(Comparator.comparing(OrdenTrabajoResponse::fechaIngreso,
                                    Comparator.nullsLast(Comparator.reverseOrder())))
                            .map(o -> seguimientoDe(o, nombres))
                            .toList();
                })
                .orElse(List.of());
    }

    @Override
    public SeguimientoOrdenResponse seguimiento(Long idOrdenTrabajo, String rut) {
        String rutFormateado = Rut.of(rut).formateado();
        OrdenTrabajoResponse orden = ordenTrabajoService.buscarPorId(idOrdenTrabajo);
        // Misma respuesta si no existe o es de otro cliente: no se revela qué órdenes existen.
        if (!rutFormateado.equals(orden.cliente().rut())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe una orden " + idOrdenTrabajo + " para este cliente");
        }
        return seguimientoDe(orden, nombresServicios());
    }

    // ---------------------------------------------------------------------

    private Long obtenerOCrearCliente(OrdenPortalRequest request) {
        OrdenPortalRequest.ClientePortal datos = request.cliente();
        Rut rut = Rut.of(datos.rut());
        return clienteRepository.findByRut_Numero(rut.getNumero())
                .map(c -> c.getIdCliente())
                .orElseGet(() -> {
                    if (request.direccion() == null) {
                        throw new DatoInvalidoException(
                                "La dirección es obligatoria para registrar al cliente " + rut.formateado());
                    }
                    var creado = clienteService.crear(new ClienteRequest(
                            rut.formateado(), datos.nombre(), datos.telefono(), null, datos.email(), request.direccion()));
                    log.info("Cliente {} creado desde el Portal Cliente", creado.rut());
                    return creado.idCliente();
                });
    }

    // Lo que el supervisor necesita para operar la orden en el portal de empleados.
    static String observaciones(OrdenPortalRequest request) {
        StringBuilder sb = new StringBuilder("[Portal Cliente ").append(request.codigoPedido()).append("]");
        if (tieneTexto(request.notas())) {
            sb.append(' ').append(request.notas().strip());
        }
        if (tieneTexto(request.horarioPreferido())) {
            sb.append("\nHorario preferido: ").append(request.horarioPreferido().strip());
        }
        if (request.direccion() != null) {
            sb.append("\nDirección de atención: ").append(request.direccion().calle().strip())
                    .append(' ').append(request.direccion().numero().strip());
            if (tieneTexto(request.direccion().detalle())) {
                sb.append(", ").append(request.direccion().detalle().strip());
            }
        }
        List<OrdenPortalRequest.RepuestoSolicitado> repuestos = Optional.ofNullable(request.repuestos()).orElse(List.of());
        if (!repuestos.isEmpty()) {
            sb.append("\nRepuestos solicitados (agregar con su proveedor):");
            repuestos.forEach(r -> sb.append("\n- ")
                    .append(tieneTexto(r.codigo()) ? r.codigo().strip() : "Repuesto #" + r.idRepuesto())
                    .append(tieneTexto(r.nombre()) ? " " + r.nombre().strip() : "")
                    .append(" x").append(r.cantidad()));
            if (tieneTexto(request.tokenReservaStock())) {
                sb.append("\nReserva de stock: ").append(request.tokenReservaStock().strip());
            }
        }
        return sb.length() <= MAX_OBSERVACIONES ? sb.toString() : sb.substring(0, MAX_OBSERVACIONES - 1) + "…";
    }

    private Map<Long, String> nombresServicios() {
        return servicioService.listar(false).stream()
                .collect(Collectors.toMap(ServicioResponse::idServicio, ServicioResponse::nombre, (a, b) -> a));
    }

    static SeguimientoOrdenResponse seguimientoDe(OrdenTrabajoResponse orden, Map<Long, String> nombresServicios) {
        EstadoOrden estado = orden.estado();
        int indiceActual = indiceHito(estado);

        List<SeguimientoOrdenResponse.Hito> hitos = new ArrayList<>();
        for (int i = 0; i < HITOS.size(); i++) {
            EstadoOrden e = (EstadoOrden) HITOS.get(i)[0];
            boolean cancelada = estado == EstadoOrden.CANCELADA;
            hitos.add(new SeguimientoOrdenResponse.Hito(
                    e, (String) HITOS.get(i)[1], (String) HITOS.get(i)[2],
                    !cancelada && (i < indiceActual || estado == EstadoOrden.CERRADA),
                    !cancelada && i == indiceActual));
        }
        if (estado == EstadoOrden.CANCELADA) {
            hitos.set(0, new SeguimientoOrdenResponse.Hito(EstadoOrden.CREADA, "Orden creada",
                    "Solicitud recibida por la central", true, false));
            hitos.add(new SeguimientoOrdenResponse.Hito(EstadoOrden.CANCELADA, "Cancelada",
                    "La orden fue cancelada", true, true));
        }

        Function<OrdenTrabajoResponse.Servicio, SeguimientoOrdenResponse.ServicioContratado> aServicio = s ->
                new SeguimientoOrdenResponse.ServicioContratado(
                        s.idServicio(),
                        nombresServicios.getOrDefault(s.idServicio(),
                                tieneTexto(s.descripcionManoObra()) ? s.descripcionManoObra() : "Servicio técnico"),
                        s.precioManoObra());

        return new SeguimientoOrdenResponse(
                orden.idOrdenTrabajo(),
                "OT-" + orden.idOrdenTrabajo(),
                estado,
                ETIQUETA.get(estado),
                orden.fechaIngreso(),
                orden.fechaActualizacionEstado(),
                orden.fechaCierre(),
                orden.cuadrilla() != null ? orden.cuadrilla().nombre() : null,
                hitos,
                orden.servicios().stream().map(aServicio).toList(),
                orden.totalManoObra());
    }

    private static int indiceHito(EstadoOrden estado) {
        for (int i = 0; i < HITOS.size(); i++) {
            if (HITOS.get(i)[0] == estado) {
                return i;
            }
        }
        return 0;
    }

    private static boolean tieneTexto(String s) {
        return s != null && !s.isBlank();
    }
}
