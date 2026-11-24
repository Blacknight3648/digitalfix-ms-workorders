package df.digitalfix_ms_workorders.messaging;

import df.digitalfix_ms_workorders.dto.event.WorkOrderKafkaEvent;
import df.digitalfix_ms_workorders.enums.EstadoOrden;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Reglas de negocio de los avisos de una OT. Una falla de la mensajería en un aviso no impide los demás.
class OrdenTrabajoEventosListenerTest {

    private PublicadorEventosOrden publicador;
    private PublicadorEventosKafka publicadorKafka;
    private OrdenTrabajoEventosListener listener;

    @BeforeEach
    void setUp() {
        publicador = mock(PublicadorEventosOrden.class);
        publicadorKafka = mock(PublicadorEventosKafka.class);
        listener = new OrdenTrabajoEventosListener(publicador, publicadorKafka);
    }

    private static OrdenTrabajoEvento evento(OrdenTrabajoEvento.Tipo tipo, EstadoOrden estado) {
        return new OrdenTrabajoEvento(tipo, 15L, estado, EstadoOrden.CREADA, 9L, "cliente@digitalfix.cl",
                "Comercial Rojas Ltda.", "+56912345678", "Av. Providencia 1234", "Revisión tablero",
                new BigDecimal("125000"), 3L, "Alfa", 7L);
    }

    @Test
    void asignarCuadrillaAvisaAlClienteYGeneraElTicket() {
        listener.alConfirmar(evento(OrdenTrabajoEvento.Tipo.CUADRILLA_ASIGNADA, EstadoOrden.ASIGNADA));

        verify(publicador).publicarNotificacionCliente(any());
        verify(publicador).publicarTicketDespacho(any());
        verify(publicadorKafka).publicarEventoOrden(any());
    }

    @Test
    void cerrarLaOrdenPideElInforme() {
        listener.alConfirmar(evento(OrdenTrabajoEvento.Tipo.ESTADO_CAMBIADO, EstadoOrden.CERRADA));

        verify(publicador).publicarNotificacionCliente(any());
        verify(publicador).publicarSolicitudInforme(any());
        verify(publicador, never()).publicarTicketDespacho(any());

        ArgumentCaptor<WorkOrderKafkaEvent> kafkaCaptor = ArgumentCaptor.forClass(WorkOrderKafkaEvent.class);
        verify(publicadorKafka).publicarEventoOrden(kafkaCaptor.capture());
        assertEquals(15L, kafkaCaptor.getValue().getOrderId());
        assertEquals("CERRADA", kafkaCaptor.getValue().getNewStatus());
        assertEquals("CREADA", kafkaCaptor.getValue().getPreviousStatus());
        assertEquals(9L, kafkaCaptor.getValue().getClientId());
        assertEquals(7L, kafkaCaptor.getValue().getAssignedTechnicianId());
        assertNotNull(kafkaCaptor.getValue().getEventId());
        assertNotNull(kafkaCaptor.getValue().getTimestamp());
    }

    @Test
    void siFallaElCorreoIgualSePublicaElTicketDeDespachoYKafka() {
        doThrow(new PublicacionEventoException("ms-admin-queues no responde", null))
                .when(publicador).publicarNotificacionCliente(any());

        listener.alConfirmar(evento(OrdenTrabajoEvento.Tipo.CUADRILLA_ASIGNADA, EstadoOrden.ASIGNADA));

        verify(publicador).publicarTicketDespacho(any());
        verify(publicadorKafka).publicarEventoOrden(any());
    }

    @Test
    void siFallaKafkaNoInterrumpeLosAvisosDeAdminQueues() {
        doThrow(new PublicacionEventoException("Kafka no disponible", null))
                .when(publicadorKafka).publicarEventoOrden(any());

        listener.alConfirmar(evento(OrdenTrabajoEvento.Tipo.CUADRILLA_ASIGNADA, EstadoOrden.ASIGNADA));

        verify(publicador).publicarNotificacionCliente(any());
        verify(publicador).publicarTicketDespacho(any());
    }
}
