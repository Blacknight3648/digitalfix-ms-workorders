package df.digitalfix_ms_workorders.service;

import df.digitalfix_ms_workorders.dto.CambioEstadoRequest;
import df.digitalfix_ms_workorders.dto.Cuadrilla.AsignarCuadrillaRequest;
import df.digitalfix_ms_workorders.dto.OrdenTrabajo.OrdenTrabajoRequest;
import df.digitalfix_ms_workorders.dto.event.DispatchTicketEvent;
import df.digitalfix_ms_workorders.dto.event.EventEnvelopeDTO;
import df.digitalfix_ms_workorders.dto.event.ReportRequestEvent;
import df.digitalfix_ms_workorders.dto.event.WorkOrderNotificationEvent;
import df.digitalfix_ms_workorders.enums.EstadoOrden;
import df.digitalfix_ms_workorders.integration.catalogo.InventarioRepuestos;
import df.digitalfix_ms_workorders.model.clientes.Cliente;
import df.digitalfix_ms_workorders.model.cuadrilla.Cuadrilla;
import df.digitalfix_ms_workorders.model.ordenTrabajo.OrdenTrabajo;
import df.digitalfix_ms_workorders.repository.ClienteRepository;
import df.digitalfix_ms_workorders.repository.CuadrillaRepository;
import df.digitalfix_ms_workorders.repository.OrdenTrabajoRepository;
import df.digitalfix_ms_workorders.repository.ProveedorRepository;
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
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private OrdenTrabajoService service;

    private Cliente cliente;
    private OrdenTrabajo orden;

    @BeforeEach
    void setUp() {
        cliente = mock(Cliente.class, RETURNS_DEEP_STUBS);
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
    }

    private static void assertEnvelopeComun(EventEnvelopeDTO<?> envelope, String expectedType) {
        assertNotNull(envelope.getEventId());
        assertFalse(envelope.getEventId().isBlank());
        assertNotNull(envelope.getTraceId());
        assertTrue(envelope.getTraceId().startsWith("TRACE-"));
        assertNotNull(envelope.getTimestamp());
        assertEquals(expectedType, envelope.getEventType());
        assertEquals("OT-" + ID_ORDEN, envelope.getCorrelationId());
    }

    @Test
    @DisplayName("crear() publica EventEnvelopeDTO hacia cmd.direct con routing key email.send")
    void crearPublicaEmailSendEnCmdDirect() {
        service.crear(new OrdenTrabajoRequest(1L, "Revisión tablero", List.of()));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(eq("cmd.direct"), eq("email.send"), captor.capture(),
                any(MessagePostProcessor.class), any(CorrelationData.class));

        EventEnvelopeDTO<?> envelope = assertInstanceOf(EventEnvelopeDTO.class, captor.getValue());
        assertEnvelopeComun(envelope, "WORK_ORDER_STATUS_CHANGED");
        WorkOrderNotificationEvent payload = assertInstanceOf(WorkOrderNotificationEvent.class, envelope.getPayload());
        assertEquals(ID_ORDEN, payload.getWorkOrderId());
        assertEquals("cliente@digitalfix.com", payload.getClientEmail());
        assertEquals("CREADA", payload.getOrderStatus());
    }

    @Test
    @DisplayName("asignarCuadrilla() publica email.send y dispatch.ticket hacia cmd.direct")
    void asignarCuadrillaPublicaEmailYDispatchEnCmdDirect() {
        service.asignarCuadrilla(ID_ORDEN, new AsignarCuadrillaRequest(7L));

        ArgumentCaptor<Object> emailCaptor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(eq("cmd.direct"), eq("email.send"), emailCaptor.capture(),
                any(MessagePostProcessor.class), any(CorrelationData.class));
        assertEnvelopeComun(assertInstanceOf(EventEnvelopeDTO.class, emailCaptor.getValue()), "WORK_ORDER_STATUS_CHANGED");

        ArgumentCaptor<Object> dispatchCaptor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(eq("cmd.direct"), eq("dispatch.ticket"), dispatchCaptor.capture(),
                any(MessagePostProcessor.class), any(CorrelationData.class));
        EventEnvelopeDTO<?> dispatchEnvelope = assertInstanceOf(EventEnvelopeDTO.class, dispatchCaptor.getValue());
        assertEnvelopeComun(dispatchEnvelope, "CREW_DISPATCH_ASSIGNED");
        DispatchTicketEvent dispatchPayload = assertInstanceOf(DispatchTicketEvent.class, dispatchEnvelope.getPayload());
        assertEquals(ID_ORDEN, dispatchPayload.getWorkOrderId());
        assertEquals("70", dispatchPayload.getTechnicianId());
    }

    @Test
    @DisplayName("cambiarEstado(CERRADA) publica email.send y report.gen hacia cmd.direct")
    void cambiarEstadoACerradaPublicaEmailYReportEnCmdDirect() {
        orden.asignarCuadrilla(new Cuadrilla("Cuadrilla Sur", 70L, Set.of(77L)));
        orden.cambiarEstado(EstadoOrden.EN_DESPLAZAMIENTO);
        orden.cambiarEstado(EstadoOrden.EN_EJECUCION);

        service.cambiarEstado(ID_ORDEN, new CambioEstadoRequest(EstadoOrden.CERRADA));

        verify(rabbitTemplate).convertAndSend(eq("cmd.direct"), eq("email.send"), any(Object.class),
                any(MessagePostProcessor.class), any(CorrelationData.class));

        ArgumentCaptor<Object> reportCaptor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(eq("cmd.direct"), eq("report.gen"), reportCaptor.capture(),
                any(MessagePostProcessor.class), any(CorrelationData.class));
        EventEnvelopeDTO<?> reportEnvelope = assertInstanceOf(EventEnvelopeDTO.class, reportCaptor.getValue());
        assertEnvelopeComun(reportEnvelope, "WORK_ORDER_REPORT_REQUESTED");
        ReportRequestEvent reportPayload = assertInstanceOf(ReportRequestEvent.class, reportEnvelope.getPayload());
        assertEquals(ID_ORDEN, reportPayload.getWorkOrderId());
    }
}
