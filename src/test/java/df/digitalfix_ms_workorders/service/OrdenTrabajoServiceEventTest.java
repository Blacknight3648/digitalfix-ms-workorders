package df.digitalfix_ms_workorders.service;

import df.digitalfix_ms_workorders.dto.CambioEstadoRequest;
import df.digitalfix_ms_workorders.dto.Cuadrilla.AsignarCuadrillaRequest;
import df.digitalfix_ms_workorders.dto.OrdenTrabajo.OrdenTrabajoRequest;
import df.digitalfix_ms_workorders.dto.event.DispatchTicketEvent;
import df.digitalfix_ms_workorders.dto.event.ReportRequestEvent;
import df.digitalfix_ms_workorders.dto.event.WorkOrderKafkaEvent;
import df.digitalfix_ms_workorders.dto.event.WorkOrderNotificationEvent;
import df.digitalfix_ms_workorders.enums.EstadoOrden;
import df.digitalfix_ms_workorders.integration.catalogo.InventarioRepuestos;
import df.digitalfix_ms_workorders.messaging.OrdenTrabajoEvento;
import df.digitalfix_ms_workorders.messaging.OrdenTrabajoEventosListener;
import df.digitalfix_ms_workorders.messaging.PublicadorEventosKafka;
import df.digitalfix_ms_workorders.messaging.PublicadorEventosOrden;
import df.digitalfix_ms_workorders.model.clientes.Cliente;
import df.digitalfix_ms_workorders.model.cuadrilla.Cuadrilla;
import df.digitalfix_ms_workorders.model.ordenTrabajo.OrdenTrabajo;
import df.digitalfix_ms_workorders.repository.ClienteRepository;
import df.digitalfix_ms_workorders.repository.CuadrillaRepository;
import df.digitalfix_ms_workorders.repository.OrdenTrabajoRepository;
import df.digitalfix_ms_workorders.repository.ProveedorRepository;
import df.digitalfix_ms_workorders.service.impl.OrdenTrabajoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrdenTrabajoServiceEventTest {

    private static final long ID_ORDEN = 42L;

    @Mock
    private OrdenTrabajoRepository ordenTrabajoRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private ProveedorRepository proveedorRepository;
    @Mock
    private CuadrillaRepository cuadrillaRepository;
    @Mock
    private ServicioService servicioService;
    @Mock
    private InventarioRepuestos inventario;
    @Mock
    private ApplicationEventPublisher eventos;
    @Mock
    private PublicadorEventosOrden publicador;
    @Mock
    private PublicadorEventosKafka publicadorKafka;

    @InjectMocks
    private OrdenTrabajoServiceImpl service;

    private Cliente cliente;
    private OrdenTrabajo orden;

    @BeforeEach
    void setUp() {
        cliente = mock(Cliente.class, RETURNS_DEEP_STUBS);
        when(cliente.getIdCliente()).thenReturn(1L);
        when(cliente.getEmail()).thenReturn("cliente@digitalfix.com");
        when(cliente.getNombreCompleto()).thenReturn("Ana Torres");
        when(cliente.getTelefono()).thenReturn("+56912345678");
        when(cliente.getDireccion().textoCompleto()).thenReturn("Av. Providencia 123");

        orden = new OrdenTrabajo(cliente, "Mantención eléctrica");
        ReflectionTestUtils.setField(orden, "idOrdenTrabajo", ID_ORDEN);

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(ordenTrabajoRepository.findById(ID_ORDEN)).thenReturn(Optional.of(orden));
        when(ordenTrabajoRepository.saveAndFlush(any())).thenAnswer(inv -> {
            OrdenTrabajo ot = inv.getArgument(0);
            if (ot.getIdOrdenTrabajo() == null) {
                ReflectionTestUtils.setField(ot, "idOrdenTrabajo", ID_ORDEN);
            }
            return ot;
        });
        when(cuadrillaRepository.findByIdConBloqueo(7L))
                .thenReturn(Optional.of(new Cuadrilla("Cuadrilla Sur", 70L, Set.of(77L))));

        OrdenTrabajoEventosListener listener = new OrdenTrabajoEventosListener(publicador, publicadorKafka);
        doAnswer(inv -> {
            Object ev = inv.getArgument(0);
            if (ev instanceof OrdenTrabajoEvento ote) {
                listener.alConfirmar(ote);
            }
            return null;
        }).when(eventos).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("crear() publica notificación al cliente vía PublicadorEventosOrden")
    void crearPublicaNotificacionCliente() {
        service.crear(new OrdenTrabajoRequest(1L, "Revisión tablero", List.of()));

        ArgumentCaptor<WorkOrderNotificationEvent> captor = ArgumentCaptor.forClass(WorkOrderNotificationEvent.class);
        verify(publicador).publicarNotificacionCliente(captor.capture());

        WorkOrderNotificationEvent payload = captor.getValue();
        assertEquals(ID_ORDEN, payload.getWorkOrderId());
        assertEquals("cliente@digitalfix.com", payload.getClientEmail());
        assertEquals("CREADA", payload.getOrderStatus());
        verifyNoMoreInteractions(publicador);

        ArgumentCaptor<WorkOrderKafkaEvent> kafkaCaptor = ArgumentCaptor.forClass(WorkOrderKafkaEvent.class);
        verify(publicadorKafka).publicarEventoOrden(kafkaCaptor.capture());
        assertEquals(ID_ORDEN, kafkaCaptor.getValue().getOrderId());
        assertNull(kafkaCaptor.getValue().getPreviousStatus());
        assertEquals("CREADA", kafkaCaptor.getValue().getNewStatus());
        assertEquals(1L, kafkaCaptor.getValue().getClientId());
        assertEquals("WORK_ORDER_CREATED", kafkaCaptor.getValue().getEventType());
    }

    @Test
    @DisplayName("asignarCuadrilla() publica notificación, ticket de despacho y evento Kafka")
    void asignarCuadrillaPublicaNotificacionYDespacho() {
        service.asignarCuadrilla(ID_ORDEN, new AsignarCuadrillaRequest(7L));

        ArgumentCaptor<WorkOrderNotificationEvent> emailCaptor = ArgumentCaptor.forClass(WorkOrderNotificationEvent.class);
        verify(publicador).publicarNotificacionCliente(emailCaptor.capture());
        assertEquals(ID_ORDEN, emailCaptor.getValue().getWorkOrderId());

        ArgumentCaptor<DispatchTicketEvent> dispatchCaptor = ArgumentCaptor.forClass(DispatchTicketEvent.class);
        verify(publicador).publicarTicketDespacho(dispatchCaptor.capture());
        DispatchTicketEvent dispatchPayload = dispatchCaptor.getValue();
        assertEquals(ID_ORDEN, dispatchPayload.getWorkOrderId());
        assertEquals("70", dispatchPayload.getTechnicianId());

        ArgumentCaptor<WorkOrderKafkaEvent> kafkaCaptor = ArgumentCaptor.forClass(WorkOrderKafkaEvent.class);
        verify(publicadorKafka).publicarEventoOrden(kafkaCaptor.capture());
        assertEquals("CREADA", kafkaCaptor.getValue().getPreviousStatus());
        assertEquals("ASIGNADA", kafkaCaptor.getValue().getNewStatus());
        assertEquals(70L, kafkaCaptor.getValue().getAssignedTechnicianId());
    }

    @Test
    @DisplayName("cambiarEstado(CERRADA) publica notificación, solicitud de informe y evento Kafka")
    void cambiarEstadoACerradaPublicaNotificacionYInforme() {
        orden.asignarCuadrilla(new Cuadrilla("Cuadrilla Sur", 70L, Set.of(77L)));
        orden.cambiarEstado(EstadoOrden.EN_DESPLAZAMIENTO);
        orden.cambiarEstado(EstadoOrden.EN_EJECUCION);

        service.cambiarEstado(ID_ORDEN, new CambioEstadoRequest(EstadoOrden.CERRADA));

        verify(publicador).publicarNotificacionCliente(any(WorkOrderNotificationEvent.class));

        ArgumentCaptor<ReportRequestEvent> reportCaptor = ArgumentCaptor.forClass(ReportRequestEvent.class);
        verify(publicador).publicarSolicitudInforme(reportCaptor.capture());
        ReportRequestEvent reportPayload = reportCaptor.getValue();
        assertEquals(ID_ORDEN, reportPayload.getWorkOrderId());
        assertEquals("INFORME_CIERRE", reportPayload.getReportType());

        ArgumentCaptor<WorkOrderKafkaEvent> kafkaCaptor = ArgumentCaptor.forClass(WorkOrderKafkaEvent.class);
        verify(publicadorKafka).publicarEventoOrden(kafkaCaptor.capture());
        assertEquals("EN_EJECUCION", kafkaCaptor.getValue().getPreviousStatus());
        assertEquals("CERRADA", kafkaCaptor.getValue().getNewStatus());
    }
}
