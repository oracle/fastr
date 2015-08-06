package com.oracle.truffle.r.runtime;

/**
 * Represents the caller of a function and stored in {@link RArguments}. The {@code rep} may not be
 * a syntax node and determining the associated syntax node is handled lazily, as it is only needed
 * in case or error or warning. A value of this type never appears in a Truffle execution.
 *
 */
public class RCaller {
    private Object rep;

    public RCaller(Object rep) {
        this.rep = rep;
    }

    public Object getRep() {
        return rep;
    }

}
