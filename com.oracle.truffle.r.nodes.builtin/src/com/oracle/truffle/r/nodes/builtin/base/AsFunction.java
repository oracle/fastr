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
package com.oracle.truffle.r.nodes.builtin.base;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.r.nodes.access.AccessArgumentNode;
import com.oracle.truffle.r.nodes.access.ConstantNode;
import com.oracle.truffle.r.nodes.access.WriteVariableNode;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.function.FormalArguments;
import com.oracle.truffle.r.nodes.function.FunctionDefinitionNode;
import com.oracle.truffle.r.nodes.function.SaveArgumentsNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RBuiltinKind;
import com.oracle.truffle.r.runtime.RDeparse;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RExpression;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

@RBuiltin(name = "as.function.default", kind = RBuiltinKind.INTERNAL, parameterNames = {"x", "envir"})
public abstract class AsFunction extends RBuiltinNode {
    @Specialization
    @TruffleBoundary
    protected RFunction asFunction(RList x, REnvironment envir) {
        if (x.getLength() == 0) {
            throw RError.error(this, RError.Message.GENERIC, "argument must have length at least 1");
        }
        SaveArgumentsNode saveArguments;
        FormalArguments formals;
        if (x.getLength() == 1) {
            // no arguments
            saveArguments = SaveArgumentsNode.NO_ARGS;
            formals = FormalArguments.NO_ARGS;
        } else {
            assert x.getNames() != null;
            RStringVector names = x.getNames();
            String[] argumentNames = new String[x.getLength() - 1];
            RNode[] defaultValues = new RNode[x.getLength() - 1];
            AccessArgumentNode[] argAccessNodes = new AccessArgumentNode[x.getLength() - 1];
            RNode[] init = new RNode[x.getLength() - 1];
            for (int i = 0; i < x.getLength() - 1; i++) {
                final RNode defaultValue;
                Object arg = x.getDataAt(i);
                if (arg == RMissing.instance) {
                    defaultValue = null;
                } else if (arg == RNull.instance) {
                    defaultValue = ConstantNode.create(RNull.instance);
                } else if (arg instanceof RLanguage) {
                    defaultValue = (RNode) ((RLanguage) arg).getRep();
                } else if (arg instanceof RSymbol) {
                    RSymbol symbol = (RSymbol) arg;
                    if (symbol.isMissing()) {
                        defaultValue = null;
                    } else {
                        defaultValue = ReadVariableNode.create(symbol.getName());
                    }
                } else if (RRuntime.asAbstractVector(arg) instanceof RAbstractVector) {
                    defaultValue = ConstantNode.create(arg);
                } else {
                    throw RInternalError.unimplemented();
                }
                AccessArgumentNode accessArg = AccessArgumentNode.create(i);
                argAccessNodes[i] = accessArg;
                String argName = names.getDataAt(i);
                init[i] = WriteVariableNode.createArgSave(argName, accessArg);

                // Store formal arguments
                argumentNames[i] = argName;
                defaultValues[i] = defaultValue;
            }
            saveArguments = new SaveArgumentsNode(init);
            formals = FormalArguments.createForFunction(defaultValues, ArgumentsSignature.get(argumentNames));
            for (AccessArgumentNode access : argAccessNodes) {
                access.setFormals(formals);
            }
        }

        RBaseNode body;
        Object bodyObject = x.getDataAt(x.getLength() - 1);
        if (bodyObject instanceof RLanguage) {
            body = ((RLanguage) x.getDataAt(x.getLength() - 1)).getRep();
        } else if (bodyObject instanceof RSymbol) {
            body = ReadVariableNode.create(((RSymbol) bodyObject).getName());
        } else {
            assert bodyObject instanceof Integer || bodyObject instanceof Double || bodyObject instanceof Byte || bodyObject instanceof String;
            body = ConstantNode.create(bodyObject);
        }
        if (!RBaseNode.isRSyntaxNode(body)) {
            throw RInternalError.unimplemented();
        }
        FrameDescriptor descriptor = new FrameDescriptor();
        FrameSlotChangeMonitor.initializeFunctionFrameDescriptor("<as.function.default>", descriptor);
        FrameSlotChangeMonitor.initializeEnclosingFrame(descriptor, envir.getFrame());
        FunctionDefinitionNode rootNode = FunctionDefinitionNode.create(RSyntaxNode.EAGER_DEPARSE, descriptor, null, saveArguments, (RSyntaxNode) body, formals, "from AsFunction", null);
        RDeparse.ensureSourceSection(rootNode);
        RootCallTarget callTarget = Truffle.getRuntime().createCallTarget(rootNode);
        boolean containsDispatch = ((FunctionDefinitionNode) callTarget.getRootNode()).containsDispatch();
        return RDataFactory.createFunction(RFunction.NO_NAME, callTarget, null, envir.getFrame(), null, containsDispatch);
    }

    @Specialization
    @TruffleBoundary
    protected RFunction asFunction(RExpression x, REnvironment envir) {
        return asFunction(x.getList(), envir);
    }

    @SuppressWarnings("unused")
    @Fallback
    @TruffleBoundary
    protected RFunction asFunction(Object x, Object envir) {
        throw RError.error(this, RError.Message.TYPE_EXPECTED, RType.List.getName());
    }
}
