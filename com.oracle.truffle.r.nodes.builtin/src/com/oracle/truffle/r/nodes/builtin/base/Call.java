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

import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

/**
 * Construct a call object from a name and optional arguments.
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

    @Specialization
    @SuppressWarnings("unused")
    protected RLanguage call(Object name, Object args) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.FIRST_ARG_MUST_BE_STRING);
    }

    @SlowPath
    protected static RLanguage makeCall(String name, RArgsValuesAndNames args) {
        ReadVariableNode functionLookup = ReadVariableNode.create(name, RRuntime.TYPE_FUNCTION, false, true, false, true);
        SourceSection src = new NullSourceSection("call", "call", makeSource(name, args));
        return makeCall0(functionLookup, src, args);
    }

    @SlowPath
    protected static RLanguage makeCall(RFunction function, RArgsValuesAndNames args) {
        ConstantNode func = ConstantNode.create(function);
        SourceSection src = new NullSourceSection("call", "call", makeSource(((RRootNode) function.getTarget().getRootNode()).getSourceCode(), args));
        return makeCall0(func, src, args);
    }

    @SlowPath
    protected static RLanguage makeCall(SourceSection callSource, RFunction function, RArgsValuesAndNames args) {
        ConstantNode func = ConstantNode.create(function);
        return makeCall0(func, callSource, args);
    }

    @SlowPath
    private static RLanguage makeCall0(RNode fn, SourceSection src, RArgsValuesAndNames args) {
        CallArgumentsNode callArgs;
        if (args != null) {
            Object[] argValues = args.getValues();
            RNode[] argValueNodes = new RNode[argValues.length];
            for (int i = 0; i < argValues.length; ++i) {
                if (argValues[i] instanceof RPromise) {
                    argValueNodes[i] = NodeUtil.cloneNode((RNode) ((RPromise) argValues[i]).getRep());
                } else {
                    argValueNodes[i] = ConstantNode.create(argValues[i]);
                }
            }
            callArgs = CallArgumentsNode.create(false, false, argValueNodes, args.getNames());
        } else {
            callArgs = CallArgumentsNode.create(false, false, EMTPY_RNODE_ARRAY, null);
        }
        RCallNode call = RCallNode.createCall(src, fn, callArgs);
        return RDataFactory.createCall(call, args);
    }

    @SlowPath
    private static String makeSource(String name, RArgsValuesAndNames args) {
        StringBuilder sb = new StringBuilder(name);
        sb.append('(');
        if (args != null) {
            String[] names = args.getNames();
            Object[] values = args.getValues();
            for (int i = 0; i < args.length(); ++i) {
                if (names != null && names[i] != null) {
                    sb.append(names[i]).append(" = ");
                }
                // TODO not sure deparse is the right way to do this (might be better to get hold of
                // the source sections of the arguments)
                if (values[i] instanceof RPromise) {
                    sb.append(((RNode) ((RPromise) values[i]).getRep()).getSourceSection().getCode());
                } else {
                    sb.append(RDeparse.deparse(values[i], 60, false, -1)[0]);
                }
                if (i + 1 < args.length()) {
                    sb.append(", ");
                }
            }
        }
        return RRuntime.toString(sb.append(')'));
    }

}
