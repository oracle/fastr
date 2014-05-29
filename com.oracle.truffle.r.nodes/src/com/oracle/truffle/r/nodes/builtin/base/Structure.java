package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.RBuiltinKind.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.builtin.RBuiltin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

/**
 * A temporary substitution to work around bug with {@code list(...)} used in R version.
 */
@RBuiltin(name = "structure", kind = SUBSTITUTE, lastParameterKind = LastParameterKind.VAR_ARGS_SPECIALIZE, isCombine = true)
@NodeField(name = "argNames", type = String[].class)
public abstract class Structure extends RBuiltinNode {
    private static final Object[] PARAMETER_NAMES = new Object[]{".Data", "..."};

// private static final Object[] PARAMETER_NAMES = new Object[]{"..."};

    public abstract String[] getArgNames();

    @Override
    public Object[] getParameterNames() {
        return PARAMETER_NAMES;
    }

    @SuppressWarnings("unused")
    @Specialization
    public Object structure(RMissing obj, RMissing args) {
        CompilerDirectives.transferToInterpreter();
        throw RError.getGenericError(getEncapsulatingSourceSection(), "argument \".Data\" is missing, with no default");

    }

    @Specialization
    public Object structure(RAbstractContainer obj, Object args) {
        if (!(args instanceof RMissing)) {
            Object[] values = args instanceof Object[] ? (Object[]) args : new Object[]{args};
            String[] argNames = getArgNames();
            validateArgNames(argNames);
            for (int i = 0; i < values.length; i++) {
                obj.setAttr(argNames[i + 1], fixupValue(values[i]));
            }
        }
        return obj;
    }

    Object fixupValue(Object value) {
        if (value instanceof String) {
            return RDataFactory.createStringVectorFromScalar((String) value);
        }
        return value;
    }

    private void validateArgNames(String[] argNames) throws RError {
        // first "name" is the container
        boolean ok = argNames != null;
        if (argNames != null) {
            for (int i = 1; i < argNames.length; i++) {
                if (argNames[i] == null) {
                    ok = false;
                }
            }
        }
        if (!ok) {
            CompilerDirectives.transferToInterpreter();
            throw RError.getGenericError(getEncapsulatingSourceSection(), "attributes must be named");
        }
    }
}
