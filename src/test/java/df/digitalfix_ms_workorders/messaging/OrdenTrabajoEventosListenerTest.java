package df.digitalfix_ms_workorders.messaging;

import df.digitalfix_ms_workorders.enums.EstadoOrden;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Reglas de negocio de los avisos de una OT. Una falla de la mensajería en un aviso no impide los demás.
class OrdenTrabajoEventosListenerTest {

    private PublicadorEventosOrden publicador;
    private OrdenTrabajoEventosListener listener;

    @BeforeEach
    void setUp() {
        publicador = mock(PublicadorEventosOrden.class);
        listener = new OrdenTrabajoEventosListener(publicador);
    }

    private static OrdenTrabajoEvento evento(OrdenTrabajoEvento.Tipo tipo, EstadoOrden estado) {
        return new OrdenTrabajoEvento(tipo, 15L, estado, "cliente@digitalfix.cl", "Comercial Rojas Ltda.",
                "+56912345678", "Av. Providencia 1234", "Revisión tablero", new BigDecimal("125000"),
                3L, "Alfa", 7L);
    }

    @Test
    void asignarCuadrillaAvisaAlClienteYGeneraElTicket() {
        listener.alConfirmar(evento(OrdenTrabajoEvento.Tipo.CUADRILLA_ASIGNADA, EstadoOrden.values()[0]));

        verify(publicador).publicarNotificacionCliente(any());
        verify(publicador).publicarTicketDespacho(any());
    }

    @Test
    void cerrarLaOrdenPideElInforme() {
        listener.alConfirmar(evento(OrdenTrabajoEvento.Tipo.ESTADO_CAMBIADO, EstadoOrden.CERRADA));

        verify(publicador).publicarNotificacionCliente(any());
        verify(publicador).publicarSolicitudInforme(any());
        verify(publicador, never()).publicarTicketDespacho(any());
    }

    @Test
    void siFallaElCorreoIgualSePublicaElTicketDeDespacho() {
        doThrow(new PublicacionEventoException("ms-admin-queues no responde", null))
                .when(publicador).publicarNotificacionCliente(any());

        listener.alConfirmar(evento(OrdenTrabajoEvento.Tipo.CUADRILLA_ASIGNADA, EstadoOrden.values()[0]));

        verify(publicador).publicarTicketDespacho(any());
    }
}
