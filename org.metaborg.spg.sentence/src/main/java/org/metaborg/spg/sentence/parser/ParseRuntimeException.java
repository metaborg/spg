package org.metaborg.spg.sentence.parser;

public class ParseRuntimeException extends RuntimeException {
    public ParseRuntimeException() {
    }

    public ParseRuntimeException(String message) {
        super(message);
    }

    public ParseRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
