package com.oracle.truffle.r.nodes.access;

import com.oracle.truffle.r.runtime.*;

/**
 * A simple wrapper class that represents a symbol that might me read or written to.
 */
public class Symbol {
    /**
     * The {@link Symbol}s identifier.
     */
    private final String name;

    /**
     * Whether this is "..."
     */
    private final boolean isVarArg;

    /**
     * Whether this is "..n"
     */
    private final boolean isVarArgGetter;

    /**
     * @param symbolStr
     * @return A fresh instance of {@link Symbol} containing the given {@link #name}
     */
    public static Symbol create(String symbolStr) {
        return new Symbol(symbolStr);
    }

    /**
     * @param symbolObj
     * @return A fresh instance of {@link Symbol} containing the given Object's {@link #toString()}
     */
    public static Symbol create(Object symbolObj) {
        return new Symbol(RRuntime.toString(symbolObj));
    }

    /**
     * @param name {@link #name}
     * @see Symbol
     */
    Symbol(String name) {
        this.name = name;

        this.isVarArg = name.equals("...");
        this.isVarArgGetter = !isVarArg && name.startsWith("..");
    }

    /**
     * @return {@link #isVarArg}
     */
    public boolean isVarArg() {
        return isVarArg;
    }

    /**
     * @return {@link #isVarArgGetter}
     */
    public boolean isVarArgGetter() {
        return isVarArgGetter;
    }

    /**
     * @return {@link #name}
     */
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
