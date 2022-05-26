/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.runtime.ffi;

import static com.oracle.truffle.r.runtime.RLogger.LOGGER_RFFI;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.MapCursor;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLogger;
import com.oracle.truffle.r.runtime.Collections;
import com.oracle.truffle.r.runtime.RLogger;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RBaseObject;
import com.oracle.truffle.r.runtime.data.RPairList;

/**
 * Support for logging R FFI.
 * 
 * <p>
 * Currently logging of the arguments to calls is limited. The type of the argument is printed as is
 * the value for types with simple (short) values. Potentially complex types, e.g, {@link RPairList}
 * do not have their values printed.
 * </p>
 *
 * <p>
 * Embedded mode requires special treatment. Experimentally the primary embedding app, RStudio, sets
 * a very constrained environment (i.e. does not propagate environment variables set in the shell
 * that launches RStudio.) and sets the cwd to "/", which is not writeable.
 * </p>
 */
public class RFFILog {

    /**
     * @see RLogger#LOGGER_RFFI
     */
    private static final TruffleLogger LOGGER = RLogger.getLogger(LOGGER_RFFI);

    private enum CallMode {
        UP("U", true),
        UP_RETURN("UR", false),
        DOWN("D", false),
        DOWN_RETURN("DR", true);

        private final String printName;
        private final boolean logNativeMirror;

        CallMode(String printName, boolean logNativeMirror) {
            this.printName = printName;
            this.logNativeMirror = logNativeMirror;
        }
    }

    @TruffleBoundary
    public static void logRObject(String message, Object obj) {
        log(message + " " + rObjectToDebugString(obj));
    }

    @TruffleBoundary
    private static String rObjectToDebugString(Object obj) {
        Object mirror = obj instanceof RBaseObject ? ((RBaseObject) obj).getNativeMirror() : null;
        return String.format("[%s, native mirror: %s]", Utils.getDebugInfo(obj), mirror);
    }

    @TruffleBoundary
    public static void logUpCall(String name, List<Object> args) {
        logCall(CallMode.UP, name, getContext().getCallDepth(), args.toArray());
    }

    @TruffleBoundary
    public static void logUpCall(String name, Object... args) {
        logCall(CallMode.UP, name, getContext().getCallDepth(), args);
    }

    @TruffleBoundary
    public static void logUpCallReturn(String name, Object result) {
        logCall(CallMode.UP_RETURN, name, getContext().getCallDepth(), result);
    }

    @TruffleBoundary
    public static void logDownCall(String name, Object... args) {
        logCall(CallMode.DOWN, name, getContext().getCallDepth(), args);
    }

    @TruffleBoundary
    public static void logDownCallReturn(String name, Object result) {
        logCall(CallMode.DOWN_RETURN, name, getContext().getCallDepth(), result);
    }

    public static boolean logEnabled() {
        return LOGGER.isLoggable(Level.FINE);
    }

    private static boolean finerLogEnabled() {
        return LOGGER.isLoggable(Level.FINER);
    }

    private static void logCall(CallMode mode, String name, int depthValue, Object... args) {
        if (logEnabled()) {
            log(callToString(mode, depthValue, name, args));
            if (finerLogEnabled()) {
                logNativeObjects();
            }
        }
    }

    @TruffleBoundary
    private static void logNativeObjects() {
        logPreserveList();
        logProtectStack();
    }

    private static void logPreserveList() {
        assert finerLogEnabled();
        StringBuilder sb = new StringBuilder();
        sb.append("preserveList = [");
        EconomicMap<RBaseObject, AtomicInteger> preserveList = getContext().rffiContextState.preserveList;
        MapCursor<RBaseObject, AtomicInteger> iterator = preserveList.getEntries();
        while (iterator.advance()) {
            sb.append("{");
            RBaseObject object = iterator.getKey();
            int id = iterator.getValue().get();
            sb.append(Long.toHexString(id));
            sb.append(":");
            sb.append(rObjectToDebugString(object));
            sb.append("}");
            sb.append(",");
        }
        if (preserveList.size() > 0) {
            // Delete last comma
            sb.deleteCharAt(sb.length() - 1);
        }
        sb.append("]");
        LOGGER.log(Level.FINER, sb.toString());
    }

    private static void logProtectStack() {
        assert finerLogEnabled();
        StringBuilder sb = new StringBuilder();
        sb.append("protectStack = [");
        Collections.ArrayListObj<RBaseObject> protectStack = getContext().rffiContextState.protectStack;
        for (int i = 0; i < protectStack.size(); i++) {
            sb.append(rObjectToDebugString(protectStack.get(i)));
            if (i < protectStack.size() - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        LOGGER.log(Level.FINER, sb.toString());
    }

    @TruffleBoundary
    private static String callToString(CallMode mode, int depthValue, String name, Object[] args) {
        StringBuilder sb = new StringBuilder();
        sb.append("CallRFFI[");
        sb.append(mode.printName);
        sb.append(':');
        sb.append(depthValue);
        sb.append("] ");
        sb.append(name);
        sb.append('(');
        argsToString(mode, sb, args);
        sb.append(')');
        return sb.toString();
    }

    @TruffleBoundary
    private static void argsToString(CallMode mode, StringBuilder sb, Object[] args) {
        boolean first = true;
        for (Object arg : args) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            // Note: it makes sense to include native mirrors only once they have been created
            // already
            String additional = "";
            if (mode.logNativeMirror && arg instanceof RBaseObject) {
                additional = ((RBaseObject) arg).getNativeMirror().toString();
            }
            Utils.printDebugInfo(sb, arg, additional);
        }
    }

    public static void logException(Throwable ex) {
        if (logEnabled()) {
            log("Error: {0}, Message: {1}", ex.getClass().getSimpleName(), ex.getMessage());
            logStackTrace();
        }
    }

    private static RFFIContext getContext() {
        return RContext.getInstance().getRFFI();
    }

    public static void log(String value) {
        assert LOGGER.isLoggable(Level.FINE) : "check traceEnabled() before calling RFFILog.log";
        LOGGER.fine(appendCtxAndThreadInfo(value));
    }

    @TruffleBoundary
    private static String appendCtxAndThreadInfo(String value) {
        StringBuilder sb = new StringBuilder();
        sb.append(value);
        sb.append(" [ctx:").append(System.identityHashCode(RContext.getInstance()));
        sb.append(",thread:").append(Thread.currentThread().getId()).append(']');
        return sb.toString();
    }

    public static void logStackTrace() {
        assert LOGGER.isLoggable(Level.FINE) : "check traceEnabled() before calling RFFILog.logStackTrace";
        LOGGER.log(Level.FINE, null, new RuntimeException());
    }

    public static void log(String message, Object... parameters) {
        assert LOGGER.isLoggable(Level.FINE) : "check traceEnabled() before calling RFFILog.log";
        // TODO log also ctx and thread?
        LOGGER.log(Level.FINE, message, parameters);
    }
}
