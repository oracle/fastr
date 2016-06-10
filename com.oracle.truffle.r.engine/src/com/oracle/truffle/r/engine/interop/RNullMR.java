package com.oracle.truffle.r.engine.interop;

import com.oracle.truffle.api.interop.CanResolve;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.engine.TruffleRLanguage;
import com.oracle.truffle.r.runtime.data.RNull;

@MessageResolution(receiverType = RNull.class, language = TruffleRLanguage.class)
public class RNullMR {
    @Resolve(message = "IS_BOXED")
    public abstract static class RNullIsBoxedNode extends Node {
        protected Object access(@SuppressWarnings("unused") RNull receiver) {
            return false;
        }
    }

    @Resolve(message = "HAS_SIZE")
    public abstract static class RNullHasSizeNode extends Node {
        protected Object access(@SuppressWarnings("unused") RNull receiver) {
            return false;
        }
    }

    @Resolve(message = "IS_NULL")
    public abstract static class RNullIsNullNode extends Node {
        protected Object access(@SuppressWarnings("unused") RNull receiver) {
            return true;
        }
    }

    @CanResolve
    public abstract static class RNullCheck extends Node {

        protected static boolean test(TruffleObject receiver) {
            return receiver instanceof RNull;
        }
    }

}
