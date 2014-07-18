package com.oracle.truffle.r.nodes.function;

interface ArgumentsTrait {

    public static final String VARARG_NAME = "...";
    public static final int NO_VARARG = -1;

    String[] getNames();

    /**
     * @return The number of non-<code>null</code> values in the {@link #getNames()} String array
     */
    default int getNameCount() {
        return countNonNull(getNames());
    }

    /**
     * @param names
     * @return The number of non-<code>null</code> values in the given String array
     */
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
     * @return {@link #getVarArgIndex(String[])}
     */
    default int getVarArgIndex() {
        return getVarArgIndex(getNames());
    }

    /**
     * @return Whether one of {@link #getNames()} matches {@link #VARARG_NAME} (
     *         {@value #VARARG_NAME})
     */
    default boolean hasVarArgs() {
        return getVarArgIndex() != NO_VARARG;
    }

    /**
     * @param names
     * @return The index of {@link #VARARG_NAME} ({@value #VARARG_NAME}) inside names, or
     *         {@link #NO_VARARG} ( {@value #NO_VARARG}) if there is none
     */
    static int getVarArgIndex(String[] names) {
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            if (VARARG_NAME.equals(name)) {
                return i;
            }
        }
        return NO_VARARG;
    }
}
