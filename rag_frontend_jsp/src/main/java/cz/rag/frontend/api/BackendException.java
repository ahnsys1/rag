package cz.rag.frontend.api;

/** Failure talking to the FastAPI backend; {@code status == 0} means it is unreachable. */
public class BackendException extends RuntimeException {

    private final int status;

    public BackendException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
