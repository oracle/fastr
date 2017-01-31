/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.r.nodes.function.CallMatcherNode;
import com.oracle.truffle.r.runtime.RInternalCode;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RFunction;

/**
 * Node that can route external calls like .External to an R function. The reference count of the
 * arguments does not get incremented for this call, like for other built-in calls, however, this
 * means that the R code should not update its arguments unless it makes a copy!
 */
public final class RInternalCodeBuiltinNode extends RExternalBuiltinNode {

    private final RContext context;
    private final String basePackage;
    private final Source code;
    private final String functionName;

    @Child private CallMatcherNode call = CallMatcherNode.create(true);
    @CompilationFinal private RFunction function;

    static {
        Casts.noCasts(RInternalCodeBuiltinNode.class);
    }

    public RInternalCodeBuiltinNode(RContext context, String basePackage, Source code, String functionName) {
        this.context = context;
        this.basePackage = basePackage;
        this.code = code;
        this.functionName = functionName;
    }

    @Override
    protected Object call(RArgsValuesAndNames args) {
        throw RInternalError.shouldNotReachHere();
    }

    @Override
    public Object call(VirtualFrame frame, RArgsValuesAndNames actualArgs) {
        if (function == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            RInternalCode internalCode = RInternalCode.lookup(context, basePackage, code);
            this.function = internalCode.lookupFunction(functionName);
            if (this.function == null) {
                throw RInternalError.shouldNotReachHere("Could not load RInternalCodeBuiltin function '" + functionName + "'.");
            }
        }

        return call.execute(frame, actualArgs.getSignature(), actualArgs.getArguments(), function, functionName, null);
    }
}
