package df.digitalfix_ms_workorders.service;

import df.digitalfix_ms_workorders.dto.CambioEstadoRequest;
import df.digitalfix_ms_workorders.dto.Cuadrilla.AsignarCuadrillaRequest;
import df.digitalfix_ms_workorders.dto.Repuesto.DetalleRepuestoRequest;
import df.digitalfix_ms_workorders.enums.EstadoOrden;
import df.digitalfix_ms_workorders.integration.catalogo.InventarioRepuestos;
import df.digitalfix_ms_workorders.model.clientes.Cliente;
import df.digitalfix_ms_workorders.model.common.OperacionNoPermitidaException;
import df.digitalfix_ms_workorders.model.cuadrilla.Cuadrilla;
import df.digitalfix_ms_workorders.model.ordenTrabajo.DetalleOtRepuesto;
import df.digitalfix_ms_workorders.model.ordenTrabajo.OrdenTrabajo;
import df.digitalfix_ms_workorders.model.proveedores.Proveedor;
import df.digitalfix_ms_workorders.repository.CuadrillaRepository;
import df.digitalfix_ms_workorders.repository.OrdenTrabajoRepository;
import df.digitalfix_ms_workorders.repository.ProveedorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

// Regla del caso: el stock del repuesto disminuye al asignar la orden. Se prueba el caso de uso
// con el agregado OrdenTrabajo real y el inventario (ms-catalog) simulado.
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrdenTrabajoServiceStockTest {

    private static final long ID_ORDEN = 15L;

    @Mock
    private OrdenTrabajoRepository ordenTrabajoRepository;
    @Mock
    private ProveedorRepository proveedorRepository;
    @Mock
    private CuadrillaRepository cuadrillaRepository;
    @Mock
    private InventarioRepuestos inventario;
    @Mock
    private ApplicationEventPublisher eventos;

    @InjectMocks
    private OrdenTrabajoService service;

    private OrdenTrabajo orden;
    private Proveedor proveedorA;
    private Proveedor proveedorB;

    @BeforeEach
    void setUp() {
        orden = new OrdenTrabajo(mock(Cliente.class, RETURNS_DEEP_STUBS), "Cambio de tablero");
        ReflectionTestUtils.setField(orden, "idOrdenTrabajo", ID_ORDEN);
        proveedorA = proveedor(1L);
        proveedorB = proveedor(2L);

        when(ordenTrabajoRepository.findById(ID_ORDEN)).thenReturn(Optional.of(orden));
        when(ordenTrabajoRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedorA));
        when(cuadrillaRepository.findByIdConBloqueo(7L))
                .thenReturn(Optional.of(new Cuadrilla("Cuadrilla Norte", 70L, Set.of(77L))));
    }

    private static Proveedor proveedor(long id) {
        Proveedor proveedor = mock(Proveedor.class);
        when(proveedor.getIdProveedor()).thenReturn(id);
        when(proveedor.abasteceProductos()).thenReturn(true);
        return proveedor;
    }

    @Test
    void agregarRepuestoEnOrdenCreadaSoloVerificaQueExista() {
        service.agregarRepuesto(ID_ORDEN, new DetalleRepuestoRequest(10L, 1L, 2));

        verify(inventario).verificarDisponible(10L);
        verify(inventario, never()).descontar(anyString(), anyMap());
    }

    @Test
    void asignarCuadrillaDescuentaTodosLosRepuestosAgrupadosPorId() {
        orden.agregarRepuesto(10L, proveedorA, 2);
        orden.agregarRepuesto(10L, proveedorB, 1); // mismo repuesto, otro proveedor: se suma
        orden.agregarRepuesto(20L, proveedorA, 5);

        service.asignarCuadrilla(ID_ORDEN, new AsignarCuadrillaRequest(7L));

        verify(inventario).descontar("OT-15", Map.of(10L, 3, 20L, 5));
    }

    @Test
    void asignarPorCambioDeEstadoTambienDescuenta() {
        orden.agregarRepuesto(10L, proveedorA, 2);
        orden.agregarServicio(5L, 30L, "Cambio de automático", BigDecimal.TEN); // técnico asignado

        service.cambiarEstado(ID_ORDEN, new CambioEstadoRequest(EstadoOrden.ASIGNADA));

        verify(inventario).descontar("OT-15", Map.of(10L, 2));
    }

    @Test
    void ordenSinRepuestosNoLlamaAlCatalogo() {
        service.asignarCuadrilla(ID_ORDEN, new AsignarCuadrillaRequest(7L));

        verifyNoInteractions(inventario);
    }

    @Test
    void avanzarDesdeAsignadaNoVuelveADescontar() {
        orden.agregarRepuesto(10L, proveedorA, 2);
        orden.asignarCuadrilla(new Cuadrilla("Cuadrilla Sur", 71L, Set.of(77L)));

        service.cambiarEstado(ID_ORDEN, new CambioEstadoRequest(EstadoOrden.EN_DESPLAZAMIENTO));

        verify(inventario, never()).descontar(anyString(), anyMap());
    }

    @Test
    void agregarRepuestoEnOrdenAsignadaLoDescuentaDeInmediato() {
        orden.asignarCuadrilla(new Cuadrilla("Cuadrilla Sur", 71L, Set.of(77L)));

        service.agregarRepuesto(ID_ORDEN, new DetalleRepuestoRequest(10L, 1L, 4));

        verify(inventario).descontar("OT-15", Map.of(10L, 4));
    }

    @Test
    void quitarRepuestoDeOrdenAsignadaDevuelveElStock() {
        DetalleOtRepuesto detalle = orden.agregarRepuesto(10L, proveedorA, 3);
        ReflectionTestUtils.setField(detalle, "idDetalleOtRepuesto", 99L);
        orden.asignarCuadrilla(new Cuadrilla("Cuadrilla Sur", 71L, Set.of(77L)));

        service.quitarRepuesto(ID_ORDEN, 99L);

        verify(inventario).devolver("OT-15", Map.of(10L, 3));
    }

    @Test
    void stockInsuficienteImpideAsignarYNoNotifica() {
        orden.agregarRepuesto(10L, proveedorA, 50);
        doThrow(new OperacionNoPermitidaException("Stock insuficiente para REP-0010"))
                .when(inventario).descontar(anyString(), anyMap());

        assertThatThrownBy(() -> service.asignarCuadrilla(ID_ORDEN, new AsignarCuadrillaRequest(7L)))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("Stock insuficiente");
        verify(eventos, never()).publishEvent(any());
    }
}
