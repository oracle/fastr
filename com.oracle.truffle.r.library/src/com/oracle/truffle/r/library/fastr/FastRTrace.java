/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.nodes.instrumentation.trace.TraceHandling;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

public class FastRTrace {
    public abstract static class Trace extends RExternalBuiltinNode.Arg8 {

        @SuppressWarnings("unused")
        @Specialization
        @TruffleBoundary
        protected Object trace(RFunction what, Object tracer, Object exit, Object at, byte print, RNull signature, REnvironment where, byte edit) {
            RSyntaxNode tracerNode;
            if (tracer instanceof RFunction) {
                tracerNode = RASTUtils.createCall(tracer, false, ArgumentsSignature.empty(0));
            } else if (tracer instanceof RLanguage) {
                tracerNode = ((RLanguage) tracer).getRep().asRSyntaxNode();
            } else {
                throw RError.error(this, RError.Message.GENERIC, "tracer is unexpected type");
            }
            TraceHandling.enableStatementTrace(what, tracerNode);
            // supposed to return the function name
            return RNull.instance;
        }

    }

}
