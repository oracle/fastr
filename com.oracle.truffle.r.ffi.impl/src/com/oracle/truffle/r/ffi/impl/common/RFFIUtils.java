/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.ffi.impl.common;

import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RObject;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.ffi.RFFIContext;
import com.oracle.truffle.r.runtime.ffi.RFFILog;

/**
 * Mostly support for tracing R FFI up/down calls. Currently tracing of the arguments to calls is
 * limited. The type of the argument is printed as is the value for types with simple (short)
 * values. Potentially complex types, e.g, {@link RPairList} do not have their values printed.
 *
 * Embedded mode requires special treatment. Experimentally the primary embedding app, RStudio, sets
 * a very constrained environment (i.e. does not propagate environment variables set in the shell
 * that launches RStudio.) and sets the cwd to "/", which is not writeable.
 */
public class RFFIUtils {

    private static Node isPointerNode;
    private static Node asPointerNode;

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
    public static void traceUpCall(String name, List<Object> args) {
        traceCall(CallMode.UP, name, getContext().getCallDepth(), args.toArray());
    }

    @TruffleBoundary
    public static void traceUpCallReturn(String name, Object result) {
        traceCall(CallMode.UP_RETURN, name, getContext().getCallDepth(), result);
    }

    @TruffleBoundary
    public static void traceDownCall(String name, Object... args) {
        traceCall(CallMode.DOWN, name, getContext().getCallDepth(), args);
    }

    @TruffleBoundary
    public static void traceDownCallReturn(String name, Object result) {
        traceCall(CallMode.DOWN_RETURN, name, getContext().getCallDepth(), result);
    }

    public static boolean traceEnabled() {
        return RFFILog.traceEnabled();
    }

    private static void traceCall(CallMode mode, String name, int depthValue, Object... args) {
        if (traceEnabled()) {
            StringBuilder sb = new StringBuilder();
            sb.append("CallRFFI[");
            sb.append(mode.printName);
            sb.append(':');
            sb.append(depthValue);
            sb.append(']');
            sb.append(name);
            sb.append('(');
            printArgs(mode, sb, args);
            sb.append(')');
            RFFILog.write(sb.toString());
        }
    }

    private static void printArgs(CallMode mode, StringBuilder sb, Object[] args) {
        boolean first = true;
        for (Object arg : args) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            if (arg == null) {
                sb.append("null");
                continue;
            }
            sb.append(arg.getClass().getSimpleName()).append('(').append(arg.hashCode());
            if (arg instanceof TruffleObject && ForeignAccess.sendIsPointer(getIsPointerNode(), (TruffleObject) arg)) {
                try {
                    sb.append(";ptr:").append(String.valueOf(ForeignAccess.sendAsPointer(getAsPointerNode(), (TruffleObject) arg)));
                } catch (UnsupportedMessageException e) {
                    throw RInternalError.shouldNotReachHere();
                }
            } else if (arg instanceof RSymbol) {
                sb.append(';').append("\"" + arg.toString() + "\"");
            } else if (arg instanceof RAbstractVector) {
                RAbstractVector vec = (RAbstractVector) arg;
                if (vec.getLength() == 0) {
                    sb.append(";empty");
                } else {
                    sb.append(";len:" + vec.getLength() + ";data:");
                    for (int i = 0; i < Math.min(3, vec.getLength()); i++) {
                        String str = ((RAbstractVector) arg).getDataAtAsObject(0).toString();
                        str = str.length() > 30 ? str.substring(0, 27) + "..." : str;
                        sb.append(',').append(str);
                    }
                }
            }
            // Note: it makes sense to include native mirrors only once they have been create
            // already
            if (mode.logNativeMirror && arg instanceof RObject) {
                sb.append(";" + ((RObject) arg).getNativeMirror());
            }
            sb.append(')');
        }
    }

    // Error handling
    static RuntimeException unimplemented() {
        CompilerDirectives.transferToInterpreter();
        return unimplemented("");
    }

    static RuntimeException unimplemented(String message) {
        CompilerDirectives.transferToInterpreter();
        if (traceEnabled()) {
            RFFILog.printf("Error: unimplemented %s", message);
            RFFILog.printStackTrace();
        }
        throw RInternalError.unimplemented(message);
    }

    static void guarantee(boolean condition) {
        guarantee(condition, "");
    }

    static void guarantee(boolean condition, String message) {
        if (!condition) {
            CompilerDirectives.transferToInterpreter();
            unimplemented(message);
        }
    }

    public static <T> T guaranteeInstanceOf(Object x, Class<T> clazz) {
        if (x == null) {
            CompilerDirectives.transferToInterpreter();
            if (traceEnabled()) {
                RFFILog.printf("Error: unexpected type: null instead of " + clazz.getSimpleName());
                RFFILog.printStackTrace();
            }
            unimplemented("unexpected type: null instead of " + clazz.getSimpleName());
        } else if (!clazz.isInstance(x)) {
            CompilerDirectives.transferToInterpreter();
            if (traceEnabled()) {
                RFFILog.printf("Error: unexpected type: %s is %s instead of %s", x, x.getClass().getSimpleName(), clazz.getSimpleName());
                RFFILog.printStackTrace();
            }
            unimplemented("unexpected type: " + x + " is " + x.getClass().getSimpleName() + " instead of " + clazz.getSimpleName());
        }
        return clazz.cast(x);
    }

    public static void logException(Throwable ex) {
        if (traceEnabled()) {
            RFFILog.printf("Error: %s, Message: %s", ex.getClass().getSimpleName(), ex.getMessage());
            RFFILog.printStackTrace();
        }
    }

    private static RFFIContext getContext() {
        return RContext.getInstance().getRFFI();
    }

    private static Node getIsPointerNode() {
        if (isPointerNode == null) {
            isPointerNode = Message.IS_POINTER.createNode();
        }
        return isPointerNode;
    }

    private static Node getAsPointerNode() {
        if (asPointerNode == null) {
            asPointerNode = Message.AS_POINTER.createNode();
        }
        return asPointerNode;
    }
}
