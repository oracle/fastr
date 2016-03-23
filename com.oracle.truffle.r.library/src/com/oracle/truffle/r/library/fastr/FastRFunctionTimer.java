/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.library.fastr;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.nodes.instrumentation.RNodeTimer;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

public class FastRFunctionTimer {
    public abstract static class CreateFunctionTimer extends RExternalBuiltinNode.Arg1 {
        @Specialization
        @TruffleBoundary
        protected RNull createFunctionTimer(RFunction function) {
            if (!function.isBuiltin()) {
                RNodeTimer.StatementListener.installTimer(function);
            }
            return RNull.instance;
        }

        @SuppressWarnings("unused")
        @Fallback
        protected Object fallback(Object a1) {
            throw RError.error(this, RError.Message.INVALID_ARGUMENT, "func");
        }
    }

    public abstract static class GetFunctionTimer extends RExternalBuiltinNode.Arg2 {
        @Specialization
        @TruffleBoundary
        protected Object getFunctionTimer(RFunction function, RAbstractStringVector scale) {
            if (!function.isBuiltin()) {
                long timeInfo = RNodeTimer.StatementListener.findTimer(function);
                if (timeInfo < 0) {
                    throw RError.error(this, RError.Message.GENERIC, "no associated timer");
                } else {
                    double timeVal = timeInfo;
                    switch (scale.getDataAt(0)) {
                        case "nanos":
                            break;
                        case "micros":
                            timeVal = timeVal / 1000.0;
                            break;
                        case "millis":
                            timeVal = timeVal / 1000000.0;
                            break;
                        case "secs":
                            timeVal = timeVal / 1000000000.0;
                            break;
                    }
                    return RDataFactory.createDoubleVectorFromScalar(timeVal);
                }
            }
            return RNull.instance;
        }

        @SuppressWarnings("unused")
        @Fallback
        protected Object fallback(Object a1, Object a2) {
            throw RError.error(this, RError.Message.INVALID_ARGUMENT, "func");
        }
    }
}
