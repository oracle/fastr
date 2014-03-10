package com.oracle.truffle.r.nodes.builtin.base;

import java.nio.file.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

/**
 * R.home builtin. TODO GnuR seems to allow other types as the component, e.g. integer, although the
 * spec does not mention coercions.
 */
@RBuiltin({"R.home"})
public abstract class Rhome extends RBuiltinNode {

    @Specialization(order = 0)
    public Object doRhome(@SuppressWarnings("unused") RMissing component) {
        return RDataFactory.createStringVector(rHomeEnv());
    }

    @Specialization(order = 1)
    public Object doRhome(String component) {
        String rHome = rHomeEnv();
        String result = component.equals("home") ? rHome : FileSystems.getDefault().getPath(rHome, component).toAbsolutePath().toString();
        return RDataFactory.createStringVector(result);
    }

    @Generic
    public Object doRhomeGeneric(@SuppressWarnings("unused") Object x) {
        throw RError.getWrongTypeOfArgument(getSourceSection());
    }

    private static String rHomeEnv() {
        return System.getenv("R_HOME");
    }

}
