package io.github.Romariok.orkestro.utils.exception;

public class InternalServiceException extends ServiceException {

    public InternalServiceException(String message) {
        super(message);
    }

    public InternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}


