package com.oracle.truffle.r.runtime.instrument;

import java.util.WeakHashMap;

import com.oracle.truffle.api.instrument.StandardInstrumentListener;
import com.oracle.truffle.r.runtime.FunctionUID;
import com.oracle.truffle.r.runtime.context.RContext;

public class Trace {
    public static class ContextStateImpl implements RContext.ContextState {

        /**
         * Records all functions that have debug receivers installed.
         */
        private static final WeakHashMap<FunctionUID, StandardInstrumentListener> receiverMap = new WeakHashMap<>();

        public void put(FunctionUID functionUID, StandardInstrumentListener listener) {
            receiverMap.put(functionUID, listener);
        }

        public StandardInstrumentListener get(FunctionUID functionUID) {
            return receiverMap.get(functionUID);
        }
    }

    public static ContextStateImpl newContext(@SuppressWarnings("unused") RContext context) {
        return new ContextStateImpl();
    }

}
