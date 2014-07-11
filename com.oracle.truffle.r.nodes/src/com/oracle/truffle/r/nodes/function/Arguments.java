package com.oracle.truffle.r.nodes.function;

import com.oracle.truffle.r.nodes.*;

/**
 * Simple generic base class for pairs of {@link #arguments} and {@link #names} (that are not
 * {@link RNode}s)
 *
 * @param <T> The type of {@link #arguments}
 */
public class Arguments<T> {

    public static final String VARARG_NAME = "...";
    public static final int NO_VARARG = -1;

    protected final T[] arguments;
    protected final String[] names;

    /**
     * Cache use for {@link #getNameCount()}
     */
    private Integer nameCountCache = null;

    Arguments(T[] arguments, String[] names) {
        this.arguments = arguments;
        this.names = names;
    }

    /**
     * @return The index of {@link #VARARG_NAME} ({@value #VARARG_NAME}), or {@link #NO_VARARG} (
     *         {@value #NO_VARARG}) if there is none
     */
    public int getVarArgIndex() {
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            if (VARARG_NAME.equals(name)) {
                return i;
            }
        }
        return NO_VARARG;
    }

    /**
     * @return Whether one of {@link #names} matches {@link #VARARG_NAME} ( {@value #VARARG_NAME})
     */
    public boolean hasVarArgs() {
        return getVarArgIndex() != NO_VARARG;
    }

    /**
     * @return The number of {@link #names} that are not <code>null</code>
     */
    public int getNameCount() {
        if (nameCountCache == null) {
            nameCountCache = countNonNull(names);
        }
        return nameCountCache;
    }

    static int countNonNull(String[] names) {
        int count = 0;
        for (int i = 0; i < names.length; i++) {
            if (names[i] != null) {
                count++;
            }
        }
        return count;
    }

    /**
     * @return The number of arguments
     */
    public int getNrOfArgs() {
        return arguments.length;
    }
}
