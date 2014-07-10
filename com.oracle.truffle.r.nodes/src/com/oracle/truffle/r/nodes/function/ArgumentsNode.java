package com.oracle.truffle.r.nodes.function;

import com.oracle.truffle.r.nodes.*;

/**
 * Base class that represents a list of argument/name pairs with some convenience methods. Semantics
 * of {@link #arguments} and {@link #names} have to be defined by subclasses!
 */
public abstract class ArgumentsNode extends RNode {

    public static final String VARARG_NAME = "...";
    public static final int NO_VARARG = -1;

    /**
     * A list of arguments. Single arguments may be <code>null</code>
     */
    @Children protected final RNode[] arguments;

    /**
     * A list of arguments. Single names may be <code>null</code>
     */
    protected final String[] names;

    /**
     * The number of {@link #names} given (i.e., not <code>null</code>)
     */
    private final int nameCount;

    protected ArgumentsNode(RNode[] arguments, String[] names) {
        super();
        this.arguments = arguments;
        this.names = names;
        this.nameCount = countNonNull(names);
    }

    private static int countNonNull(String[] names) {
        int count = 0;
        for (int i = 0; i < names.length; i++) {
            if (names[i] != null) {
                count++;
            }
        }
        return count;
    }

    /**
     * @return Whether one of {@link #names} matches {@link #VARARG_NAME} ( {@value #VARARG_NAME})
     */
    public boolean hasVarArgs() {
        return getVarArgIndex() != NO_VARARG;
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
     * @return {@link #arguments}
     */
    public RNode[] getArguments() {
        return arguments;
    }

    /**
     * @return {@link #names}
     */
    public String[] getNames() {
        return names;
    }

    /**
     * @return {@link #nameCount}
     */
    public int getNameCount() {
        return nameCount;
    }
}
