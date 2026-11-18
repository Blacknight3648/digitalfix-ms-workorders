package df.digitalfix_ms_workorders.service;

import df.digitalfix_ms_workorders.dto.*;
import df.digitalfix_ms_workorders.dto.Cuadrilla.AsignarCuadrillaRequest;
import df.digitalfix_ms_workorders.dto.OrdenServicio.DetalleServicioRequest;
import df.digitalfix_ms_workorders.model.common.DatoInvalidoException;
import df.digitalfix_ms_workorders.model.servicios.Servicio;
import java.math.BigDecimal;
import df.digitalfix_ms_workorders.dto.OrdenTrabajo.ActualizarOrdenTrabajoRequest;
import df.digitalfix_ms_workorders.dto.OrdenTrabajo.OrdenTrabajoRequest;
import df.digitalfix_ms_workorders.dto.OrdenTrabajo.OrdenTrabajoResponse;
import df.digitalfix_ms_workorders.dto.Repuesto.DetalleRepuestoRequest;
import df.digitalfix_ms_workorders.dto.Tecnico.AsignarTecnicoRequest;
import df.digitalfix_ms_workorders.enums.EstadoOrden;
import df.digitalfix_ms_workorders.model.clientes.Cliente;
import df.digitalfix_ms_workorders.model.common.OperacionNoPermitidaException;
import df.digitalfix_ms_workorders.model.cuadrilla.Cuadrilla;
import df.digitalfix_ms_workorders.model.ordenTrabajo.OrdenTrabajo;
import df.digitalfix_ms_workorders.model.proveedores.Proveedor;
import df.digitalfix_ms_workorders.repository.ClienteRepository;
import df.digitalfix_ms_workorders.repository.CuadrillaRepository;
import df.digitalfix_ms_workorders.repository.OrdenTrabajoRepository;
import df.digitalfix_ms_workorders.repository.ProveedorRepository;
import df.digitalfix_ms_workorders.dto.event.DispatchTicketEvent;
import df.digitalfix_ms_workorders.dto.event.EventEnvelopeDTO;
import df.digitalfix_ms_workorders.dto.event.ReportRequestEvent;
import df.digitalfix_ms_workorders.dto.event.WorkOrderNotificationEvent;
import df.digitalfix_ms_workorders.integration.catalogo.InventarioRepuestos;
import df.digitalfix_ms_workorders.model.ordenTrabajo.DetalleOtRepuesto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Orquesta los casos de uso de la OT. Las reglas de negocio (estados, duplicados, edición) viven
// en el modelo OrdenTrabajo; aquí sólo se cargan las entidades y se delimita la transacción.
// Se usa saveAndFlush para que los errores de validación/constraints salgan dentro del request.
//
// Stock de repuestos (regla del caso: disminuye al asignar la orden). El stock vive en ms-catalog;
// se mueve con InventarioRepuestos después del saveAndFlush (si la orden no es válida, el catálogo
// ni se entera) y, si aun así la transacción local termina en rollback, se compensa con el
// movimiento inverso. Si el catálogo rechaza (p. ej. stock insuficiente), la excepción revierte
// también la orden: no queda una OT asignada sin sus repuestos.
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrdenTrabajoService {

    private static final String EXCHANGE_DIRECT = "cmd.direct";
    private static final String ROUTING_KEY_EMAIL = "email.send";
    private static final String ROUTING_KEY_DISPATCH = "dispatch.ticket";
    private static final String ROUTING_KEY_REPORT = "report.gen";

    private static final String EVENT_TYPE_STATUS_CHANGED = "WORK_ORDER_STATUS_CHANGED";
    private static final String EVENT_TYPE_DISPATCH = "CREW_DISPATCH_ASSIGNED";
    private static final String EVENT_TYPE_REPORT = "WORK_ORDER_REPORT_REQUESTED";

    private static final String SIN_DIRECCION = "Sin dirección registrada";

    private final OrdenTrabajoRepository ordenTrabajoRepository;
    private final ClienteRepository clienteRepository;
    private final ProveedorRepository proveedorRepository;
    private final CuadrillaRepository cuadrillaRepository;
    private final ServicioService servicioService;
    private final InventarioRepuestos inventario;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public OrdenTrabajoResponse crear(OrdenTrabajoRequest request) {
        Cliente cliente = clienteRepository.findById(request.idCliente())
                .orElseThrow(() -> noEncontrado("No existe un cliente con id " + request.idCliente()));
        OrdenTrabajo orden = new OrdenTrabajo(cliente, request.observaciones());
        if (request.servicios() != null) {
            request.servicios().forEach(s -> agregarServicioDelCatalogo(orden, s));
        }
        OrdenTrabajo guardada = ordenTrabajoRepository.saveAndFlush(orden);
        publicarNotificacionEmail(guardada, "DigitalFix: registramos tu orden de trabajo #" + guardada.getIdOrdenTrabajo());
        return OrdenTrabajoResponse.from(guardada);
    }

    public List<OrdenTrabajoResponse> listar(EstadoOrden estado, Long idCliente) {
        List<OrdenTrabajo> ordenes;
        if (estado != null && idCliente != null) {
            ordenes = ordenTrabajoRepository.findByEstadoAndCliente_IdCliente(estado, idCliente);
        } else if (estado != null) {
            ordenes = ordenTrabajoRepository.findByEstado(estado);
        } else if (idCliente != null) {
            ordenes = ordenTrabajoRepository.findByCliente_IdCliente(idCliente);
        } else {
            ordenes = ordenTrabajoRepository.findAll();
        }
        return ordenes.stream().map(OrdenTrabajoResponse::from).toList();
    }

    public OrdenTrabajoResponse buscarPorId(Long id) {
        return OrdenTrabajoResponse.from(obtener(id));
    }

    @Transactional
    public OrdenTrabajoResponse actualizar(Long id, ActualizarOrdenTrabajoRequest request) {
        OrdenTrabajo orden = obtener(id);
        orden.actualizarObservaciones(request.observaciones());
        return guardar(orden);
    }

    @Transactional
    public OrdenTrabajoResponse cambiarEstado(Long id, CambioEstadoRequest request) {
        OrdenTrabajo orden = obtener(id);
        boolean stockYaDescontado = orden.stockComprometido();
        orden.cambiarEstado(request.estado());
        OrdenTrabajoResponse respuesta = guardar(orden);
        if (!stockYaDescontado && orden.stockComprometido()) {
            descontarStock(orden, orden.cantidadesPorRepuesto());
        }
        publicarNotificacionEmail(orden, "DigitalFix: tu orden #" + orden.getIdOrdenTrabajo() + " cambió a " + orden.getEstado());
        if (orden.getEstado() == EstadoOrden.CERRADA) {
            publicarSolicitudInforme(orden);
        }
        return respuesta;
    }

    @Transactional
    public OrdenTrabajoResponse agregarServicio(Long id, DetalleServicioRequest request) {
        OrdenTrabajo orden = obtener(id);
        agregarServicioDelCatalogo(orden, request);
        return guardar(orden);
    }

    // El servicio debe existir y estar activo en el catálogo; sin precio explícito se cobra su tarifa.
    private void agregarServicioDelCatalogo(OrdenTrabajo orden, DetalleServicioRequest request) {
        Servicio servicio = servicioService.obtenerActivo(request.idServicio());
        BigDecimal precio = request.precioManoObra() != null ? request.precioManoObra() : servicio.getTarifa();
        orden.agregarServicio(servicio.getIdServicio(), request.idTecnico(), request.descripcionManoObra(), precio);
    }

    @Transactional
    public OrdenTrabajoResponse asignarTecnico(Long id, Long idDetalleServicio, AsignarTecnicoRequest request) {
        OrdenTrabajo orden = obtener(id);
        orden.asignarTecnico(idDetalleServicio, request.idTecnico());
        return guardar(orden);
    }

    /**
     * Coordina y despacha una cuadrilla de terreno con bloqueo pesimista (ACID - Aislamiento).
     * Evita que dos supervisores asignen la misma cuadrilla a dos órdenes distintas en concurrencia.
     */
    @Transactional
    public OrdenTrabajoResponse asignarCuadrilla(Long id, AsignarCuadrillaRequest request) {
        OrdenTrabajo orden = obtener(id);
        Cuadrilla cuadrilla = cuadrillaRepository.findByIdConBloqueo(request.idCuadrilla())
                .orElseThrow(() -> noEncontrado("No existe una cuadrilla con id " + request.idCuadrilla()));
        boolean stockYaDescontado = orden.stockComprometido();
        orden.asignarCuadrilla(cuadrilla);
        OrdenTrabajoResponse respuesta = guardar(orden);
        if (!stockYaDescontado) {
            descontarStock(orden, orden.cantidadesPorRepuesto());
        }
        publicarNotificacionEmail(orden, "DigitalFix: tu orden #" + orden.getIdOrdenTrabajo() + " ya tiene cuadrilla asignada");
        publicarTicketDespacho(orden);
        return respuesta;
    }

    @Transactional
    public OrdenTrabajoResponse quitarServicio(Long id, Long idDetalleServicio) {
        OrdenTrabajo orden = obtener(id);
        orden.quitarServicio(idDetalleServicio);
        return guardar(orden);
    }

    @Transactional
    public OrdenTrabajoResponse agregarRepuesto(Long id, DetalleRepuestoRequest request) {
        OrdenTrabajo orden = obtener(id);
        Proveedor proveedor = proveedorRepository.findById(request.idProveedor())
                .orElseThrow(() -> noEncontrado("No existe un proveedor con id " + request.idProveedor()));
        if (!proveedor.abasteceProductos()) {
            throw new DatoInvalidoException("El proveedor " + proveedor.getRazonSocial()
                    + " es de servicios; los repuestos deben venir de un proveedor de productos");
        }
        inventario.verificarDisponible(request.idRepuesto());
        orden.agregarRepuesto(request.idRepuesto(), proveedor, request.cantidad());
        OrdenTrabajoResponse respuesta = guardar(orden);
        // En una orden ya asignada el repuesto se consume de inmediato; en CREADA, al asignarla.
        if (orden.stockComprometido()) {
            descontarStock(orden, Map.of(request.idRepuesto(), request.cantidad()));
        }
        return respuesta;
    }

    @Transactional
    public OrdenTrabajoResponse quitarRepuesto(Long id, Long idDetalleRepuesto) {
        OrdenTrabajo orden = obtener(id);
        DetalleOtRepuesto quitado = orden.quitarRepuesto(idDetalleRepuesto);
        OrdenTrabajoResponse respuesta = guardar(orden);
        if (orden.stockComprometido()) {
            devolverStock(orden, Map.of(quitado.getIdRepuesto(), quitado.getCantidadUtilizada()));
        }
        return respuesta;
    }

    @Transactional
    public void eliminar(Long id) {
        OrdenTrabajo orden = obtener(id);
        if (!orden.esEliminable()) {
            throw new OperacionNoPermitidaException(
                    "Sólo se puede eliminar una orden en estado CREADA; en otro caso debe cancelarse o cerrarse");
        }
        ordenTrabajoRepository.delete(orden);
    }

    private void descontarStock(OrdenTrabajo orden, Map<Long, Integer> cantidades) {
        if (cantidades.isEmpty()) {
            return;
        }
        String referencia = referenciaStock(orden);
        inventario.descontar(referencia, cantidades);
        alRevertir(() -> inventario.devolver(referencia, cantidades), "devolver", referencia);
    }

    private void devolverStock(OrdenTrabajo orden, Map<Long, Integer> cantidades) {
        String referencia = referenciaStock(orden);
        inventario.devolver(referencia, cantidades);
        alRevertir(() -> inventario.descontar(referencia, cantidades), "volver a descontar", referencia);
    }

    // Compensación: el catálogo ya confirmó su movimiento en su propia base, así que si esta
    // transacción no llega a commit se aplica el movimiento inverso. Si la compensación también
    // falla, queda en el log para corregirla a mano (el historial MOVIMIENTO_STOCK la muestra).
    private void alRevertir(Runnable compensacion, String accion, String referencia) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return; // fuera de una transacción no hay rollback que compensar
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_ROLLED_BACK) {
                    return;
                }
                try {
                    compensacion.run();
                    log.warn("Rollback de {}: stock compensado en el catálogo ({})", referencia, accion);
                } catch (RuntimeException e) {
                    log.error("Rollback de {}: no se pudo {} el stock en el catálogo: {}", referencia, accion, e.getMessage());
                }
            }
        });
    }

    private void publicarNotificacionEmail(OrdenTrabajo orden, String asunto) {
        Cliente cliente = orden.getCliente();
        String direccion = cliente.getDireccion() != null ? cliente.getDireccion().textoCompleto() : SIN_DIRECCION;
        Map<String, Object> atributos = new HashMap<>();
        atributos.put("status", orden.getEstado().name());
        atributos.put("serviceAddress", direccion);
        if (orden.getCuadrilla() != null) {
            atributos.put("technicianName", "Cuadrilla " + orden.getCuadrilla().getNombre());
        }

        WorkOrderNotificationEvent payload = WorkOrderNotificationEvent.builder()
                .workOrderId(orden.getIdOrdenTrabajo())
                .clientEmail(cliente.getEmail())
                .clientName(cliente.getNombreCompleto())
                .orderStatus(orden.getEstado().name())
                .description(orden.getObservaciones())
                .totalAmount(orden.getTotalManoObra())
                .subject(asunto)
                .templateAttributes(atributos)
                .build();

        publicarEnvelope(ROUTING_KEY_EMAIL, EVENT_TYPE_STATUS_CHANGED, orden.getIdOrdenTrabajo(), payload);
    }

    private void publicarTicketDespacho(OrdenTrabajo orden) {
        Cliente cliente = orden.getCliente();
        Cuadrilla cuadrilla = orden.getCuadrilla();
        String direccion = cliente.getDireccion() != null ? cliente.getDireccion().textoCompleto() : SIN_DIRECCION;

        DispatchTicketEvent payload = DispatchTicketEvent.builder()
                .workOrderId(orden.getIdOrdenTrabajo())
                .technicianId(cuadrilla != null ? String.valueOf(cuadrilla.getIdTecnicoLider()) : null)
                .crewId(cuadrilla != null ? String.valueOf(cuadrilla.getIdCuadrilla()) : null)
                .serviceAddress(direccion)
                .clientPhone(cliente.getTelefono())
                .scheduledDate(LocalDateTime.now())
                .serviceDescription(orden.getObservaciones() != null
                        ? orden.getObservaciones()
                        : "Orden de trabajo #" + orden.getIdOrdenTrabajo())
                .build();

        publicarEnvelope(ROUTING_KEY_DISPATCH, EVENT_TYPE_DISPATCH, orden.getIdOrdenTrabajo(), payload);
    }

    private void publicarSolicitudInforme(OrdenTrabajo orden) {
        ReportRequestEvent payload = ReportRequestEvent.builder()
                .workOrderId(orden.getIdOrdenTrabajo())
                .reportType("INFORME_CIERRE")
                .recipientEmail(orden.getCliente().getEmail())
                .requestedBy("ms-workorders")
                .build();

        publicarEnvelope(ROUTING_KEY_REPORT, EVENT_TYPE_REPORT, orden.getIdOrdenTrabajo(), payload);
    }

    private <T> void publicarEnvelope(String routingKey, String eventType, Long idOrden, T payload) {
        EventEnvelopeDTO<T> envelope = EventEnvelopeDTO.<T>builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .timestamp(LocalDateTime.now())
                .traceId("TRACE-" + UUID.randomUUID().toString().substring(0, 8))
                .correlationId("OT-" + idOrden)
                .payload(payload)
                .build();

        try {
            rabbitTemplate.convertAndSend(EXCHANGE_DIRECT, routingKey, envelope, mensaje -> {
                mensaje.getMessageProperties().setMessageId(envelope.getEventId());
                mensaje.getMessageProperties().setCorrelationId(envelope.getCorrelationId());
                return mensaje;
            }, new CorrelationData(envelope.getEventId()));

            log.info("Evento {} de {} publicado en {} con routingKey {} (eventId {})",
                    eventType, envelope.getCorrelationId(), EXCHANGE_DIRECT, routingKey, envelope.getEventId());
        } catch (AmqpException e) {
            log.error("No se pudo publicar el evento {} de la OT #{} en RabbitMQ: {}",
                    eventType, idOrden, e.getMessage());
        }
    }

    private static String referenciaStock(OrdenTrabajo orden) {
        return "OT-" + orden.getIdOrdenTrabajo();
    }

    private OrdenTrabajoResponse guardar(OrdenTrabajo orden) {
        return OrdenTrabajoResponse.from(ordenTrabajoRepository.saveAndFlush(orden));
    }

    private OrdenTrabajo obtener(Long id) {
        return ordenTrabajoRepository.findById(id)
                .orElseThrow(() -> noEncontrado("No existe una orden de trabajo con id " + id));
    }

    private ResponseStatusException noEncontrado(String mensaje) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, mensaje);
    }
}
