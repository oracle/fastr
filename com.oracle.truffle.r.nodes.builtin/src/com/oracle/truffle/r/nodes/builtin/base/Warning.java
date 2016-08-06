/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RVisibility.OFF;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.unary.CastStringNode;
import com.oracle.truffle.r.nodes.unary.CastStringNodeGen;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RErrorHandling;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;

@RBuiltin(name = "warning", visibility = OFF, kind = INTERNAL, parameterNames = {"call", "immediate", "nobreaks", "message"}, behavior = COMPLEX)
public abstract class Warning extends RBuiltinNode {

    @Child private CastStringNode castString;

    private Object castString(Object operand) {
        if (castString == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castString = insert(CastStringNodeGen.create(false, false, false, false));
        }
        return castString.execute(operand);
    }

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.toLogical(0);
        casts.toLogical(1);
        casts.toLogical(2);
    }

    @Specialization
    @TruffleBoundary
    protected String warning(byte callL, byte immediateL, byte noBreakWarningL, Object messageObj) {
        String message = RRuntime.asString(castString(messageObj));
        boolean call = RRuntime.fromLogical(callL);
        boolean immediate = RRuntime.fromLogical(immediateL);
        boolean noBreakWarning = RRuntime.fromLogical(noBreakWarningL);
        RErrorHandling.warningcallInternal(call, message, immediate, noBreakWarning);
        return message;
    }

    @SuppressWarnings("unused")
    @Fallback
    @TruffleBoundary
    protected String warning(Object callL, Object immediateL, Object noBreakWarningL, Object message) {
        throw RError.error(this, RError.Message.INVALID_OR_UNIMPLEMENTED_ARGUMENTS);
    }
}
