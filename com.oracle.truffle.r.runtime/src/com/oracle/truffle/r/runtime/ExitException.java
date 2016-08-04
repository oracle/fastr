package com.oracle.truffle.r.runtime;

/**
 * This exception is thrown when a Polyglot R engine wants to exit, usually via the {@code quit}
 * builtin. It allows systems using multiple contexts via {@code .fastr.context.op} to handle exits
 * gracefully.
 */
public class ExitException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private int status;

    public ExitException(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
