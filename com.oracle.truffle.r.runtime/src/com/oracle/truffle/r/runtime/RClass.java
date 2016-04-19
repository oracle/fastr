package com.oracle.truffle.r.runtime;

import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RVector;

/**
 * Enumeration of all values of 'class' attribute that have some special handling.
 */
public enum RClass {
    DataFrame(RRuntime.DATA_FRAME_CLASS);

    private final String name;

    RClass(String name) {
        this.name = name;
    }

    public boolean isInstanceOf(Object o) {
        // TODO: parameterize the check of RVector
        return o instanceof RVector && hasOneEqualTo((RStringVector) ((RVector) o).getAttribute("class"), name);
    }

    public String getName() {
        return name;
    }

    private static boolean hasOneEqualTo(RStringVector vector, String value) {
        return vector != null && vector.getLength() == 1 && vector.getDataAt(0).equals(value);
    }
}
