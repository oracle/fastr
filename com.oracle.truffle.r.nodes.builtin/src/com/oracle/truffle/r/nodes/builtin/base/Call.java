/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

/**
 * Construct a call object ({@link RLanguage}) from a name and optional arguments.
 */
@RBuiltin(name = "call", kind = PRIMITIVE, parameterNames = {"name", "..."})
public abstract class Call extends RBuiltinNode {

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance)};
    }

    @Specialization
    protected RLanguage call(String name, @SuppressWarnings("unused") RMissing args) {
        return makeCall(name, null);
    }

    @Specialization
    protected RLanguage call(String name, RArgsValuesAndNames args) {
        return makeCall(name, args);
    }

    @Fallback
    @SuppressWarnings("unused")
    protected RLanguage call(Object name, Object args) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.FIRST_ARG_MUST_BE_STRING);
    }

    @TruffleBoundary
    protected static RLanguage makeCall(String name, RArgsValuesAndNames args) {
        return makeCall0(name, args);
    }

    @TruffleBoundary
    protected static RLanguage makeCall(RFunction function, RArgsValuesAndNames args) {
        return makeCall0(function, args);
    }

    @TruffleBoundary
    /**
     *
     * @param fn an {@link RFunction} or {@link String}
     * @param argsAndNames if not {@code null} the argument values and (optional) names
     * @return the {@link RLanguage} instance denoting the call
     */
    private static RLanguage makeCall0(Object fn, RArgsValuesAndNames argsAndNames) {
        int argLength = argsAndNames == null ? 0 : argsAndNames.length();
        RNode[] args = new RNode[argLength];
        Object[] values = argsAndNames == null ? null : argsAndNames.getValues();
        String[] names = argsAndNames == null ? new String[0] : argsAndNames.getNames();

        for (int i = 0; i < argLength; i++) {
            args[i] = RASTUtils.createNodeForValue(values[i]);
        }

        // TODO: handle replacement calls
        boolean isReplacement = false;
        final CallArgumentsNode callArgsNode = CallArgumentsNode.create(!isReplacement, false, args, names);
        return RDataFactory.createLanguage(RASTUtils.createCall(fn, callArgsNode));
    }

}
