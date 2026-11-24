package df.digitalfix_ms_workorders.messaging;

/** ms-admin-queues no aceptó el evento (no respondió o el broker no lo encoló). */
public class PublicacionEventoException extends RuntimeException {

    public PublicacionEventoException(String message, Throwable cause) {
        super(message, cause);
    }
}
