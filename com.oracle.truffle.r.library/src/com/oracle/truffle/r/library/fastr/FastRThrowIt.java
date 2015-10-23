package com.oracle.truffle.r.library.fastr;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.UnsupportedSpecializationException;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.BrowserQuitException;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

public class FastRThrowIt {
    public abstract static class ThrowIt extends RExternalBuiltinNode.Arg1 {
        @Specialization
        @TruffleBoundary
        protected RNull throwit(RAbstractStringVector x) {
            String name = x.getDataAt(0);
            switch (name) {
                case "AIX":
                    throw new ArrayIndexOutOfBoundsException();
                case "NPE":
                    throw new NullPointerException();
                case "ASE":
                    throw new AssertionError();
                case "RTE":
                    throw new RuntimeException();
                case "USE":
                    throw new UnsupportedSpecializationException(null, null, new Object[0]);
                case "RINT":
                    throw RInternalError.shouldNotReachHere();
                case "DBE":
                    throw new Utils.DebugExitException();
                case "BRQ":
                    throw new BrowserQuitException();
                default:
                    throw RError.error(RError.NO_NODE, RError.Message.GENERIC, "unknown case: " + name);
            }
        }
    }
}
