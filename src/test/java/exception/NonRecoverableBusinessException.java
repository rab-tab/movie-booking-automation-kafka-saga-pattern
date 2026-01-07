package exception;

public class NonRecoverableBusinessException extends RuntimeException {

    public NonRecoverableBusinessException(String message) {
        super(message);
    }
}
