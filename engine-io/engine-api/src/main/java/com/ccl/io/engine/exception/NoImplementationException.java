package com.ccl.io.engine.exception;

public class NoImplementationException extends UnsupportedOperationException {

    public NoImplementationException() {
        super("No implementation available");
    }

    public NoImplementationException(String message) {
        super(message);
    }

}
