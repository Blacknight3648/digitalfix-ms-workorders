package df.digitalfix_ms_workorders.model.common;

/** La operación no es válida en el estado actual de la entidad (se responde 409). */
public class OperacionNoPermitidaException extends IllegalStateException {

    public OperacionNoPermitidaException(String mensaje) {
        super(mensaje);
    }
}
