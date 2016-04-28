/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.fastr;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.GetFunctions;
import com.oracle.truffle.r.nodes.builtin.base.GetFunctionsFactory.GetNodeGen;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RBuiltinKind;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

@RBuiltin(name = ".fastr.trace", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"what", "tracer", "exit", "at", "print", "signature", "where", "edit"})
public abstract class FastRTrace extends RBuiltinNode {

    @Child private GetFunctions.Get getNode;

    @Specialization
    protected Object trace(VirtualFrame frame, RAbstractStringVector what, Object tracer, Object exit, Object at, byte print, RNull signature, REnvironment where, byte edit) {
        if (getNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getNode = insert(GetNodeGen.create(null));
        }
        RFunction func = (RFunction) getNode.execute(frame, what, where, RType.Function.getName(), RRuntime.LOGICAL_TRUE);
        return trace(func, tracer, exit, at, print, signature, where, edit);
    }

    @SuppressWarnings("unused")
    @Specialization
    @TruffleBoundary
    protected Object trace(RFunction what, Object tracer, Object exit, Object at, byte print, RNull signature, REnvironment where, byte edit) {
        controlVisibility();
        RSyntaxNode tracerNode;
        if (tracer instanceof RFunction) {
            tracerNode = RASTUtils.createCall(tracer, false, ArgumentsSignature.empty(0));
        } else if (tracer instanceof RLanguage) {
            tracerNode = ((RLanguage) tracer).getRep().asRSyntaxNode();
        } else {
            throw RError.error(this, RError.Message.GENERIC, "tracer is unexpected type");
        }
        RContext.getRRuntimeASTAccess().enableStatementTrace(what, tracerNode);
        // supposed to return the function name
        return RNull.instance;
    }
}
