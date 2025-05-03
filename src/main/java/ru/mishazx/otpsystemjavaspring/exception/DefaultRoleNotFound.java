package ru.mishazx.otpsystemjavaspring.exception;

public class DefaultRoleNotFound extends RuntimeException {
    public DefaultRoleNotFound(String message) {
        super(message);
    }
}
