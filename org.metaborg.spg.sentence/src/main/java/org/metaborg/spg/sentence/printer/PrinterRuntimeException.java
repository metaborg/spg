package org.metaborg.spg.sentence.printer;

public class PrinterRuntimeException extends RuntimeException {
    public PrinterRuntimeException() {
    }

    public PrinterRuntimeException(String message) {
        super(message);
    }

    public PrinterRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
