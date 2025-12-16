package io.github.Romariok.orkestro.utils.exception;

public class BusinessException extends ServiceException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}


