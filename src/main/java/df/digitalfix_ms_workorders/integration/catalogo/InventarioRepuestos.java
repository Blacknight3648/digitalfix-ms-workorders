package df.digitalfix_ms_workorders.integration.catalogo;

import java.util.Map;

/**
 * Inventario de repuestos, que vive en ms-catalog (dueño de REPUESTO y su stock). El caso de uso
 * de la OT sólo conoce esta interfaz; cómo se llega al catálogo (HTTP, JWT, timeouts) queda en
 * {@link CatalogoInventarioClient}.
 * <p>
 * Las cantidades van agrupadas por ID de repuesto. Si el catálogo rechaza la operación se lanza
 * {@code OperacionNoPermitidaException} (409, ej. stock insuficiente) o {@code DatoInvalidoException}
 * (400, ej. repuesto inexistente), y la transacción de la OT se revierte.
 */
public interface InventarioRepuestos {

    /** El repuesto existe y está activo, es decir, se puede agregar a una orden. */
    void verificarDisponible(Long idRepuesto);

    /** Descuenta todo el lote o nada (stock insuficiente en uno = ninguno se descuenta). */
    void descontar(String referencia, Map<Long, Integer> cantidades);

    /** Devuelve a bodega lo que se había descontado. */
    void devolver(String referencia, Map<Long, Integer> cantidades);
}
