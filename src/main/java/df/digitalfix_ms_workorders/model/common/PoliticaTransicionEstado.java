package df.digitalfix_ms_workorders.model.common;

import df.digitalfix_ms_workorders.enums.EstadoOrden;

import java.util.Map;
import java.util.Set;

import static df.digitalfix_ms_workorders.enums.EstadoOrden.*;

/**
 * Transiciones de estado permitidas para órdenes de servicio y de trabajo.
 * Flujo lineal CREADA → ASIGNADA → EN_DESPLAZAMIENTO → EN_EJECUCION → CERRADA;
 * CANCELADA sólo desde CREADA. CERRADA y CANCELADA son terminales.
 */
public final class PoliticaTransicionEstado {

    private static final Map<EstadoOrden, Set<EstadoOrden>> TRANSICIONES = Map.of(
            CREADA, Set.of(ASIGNADA, CANCELADA),
            ASIGNADA, Set.of(EN_DESPLAZAMIENTO),
            EN_DESPLAZAMIENTO, Set.of(EN_EJECUCION),
            EN_EJECUCION, Set.of(CERRADA),
            CERRADA, Set.of(),
            CANCELADA, Set.of()
    );

    private PoliticaTransicionEstado() {
    }

    public static boolean esPermitida(EstadoOrden actual, EstadoOrden nuevo) {
        return actual != null && nuevo != null && TRANSICIONES.get(actual).contains(nuevo);
    }

    public static boolean esTerminal(EstadoOrden estado) {
        return TRANSICIONES.get(estado).isEmpty();
    }

    public static void validar(EstadoOrden actual, EstadoOrden nuevo) {
        if (!esPermitida(actual, nuevo)) {
            throw new OperacionNoPermitidaException(
                    "Transición de estado no permitida: " + actual + " -> " + nuevo);
        }
    }
}
