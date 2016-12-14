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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.instanceOf;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.runtime.RVisibility.CUSTOM;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.access.FrameSlotNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetNamesAttributeNode;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.GetFunctionsFactory.GetNodeGen;
import com.oracle.truffle.r.nodes.function.GetCallerFrameNode;
import com.oracle.truffle.r.nodes.function.RCallBaseNode;
import com.oracle.truffle.r.nodes.function.RCallNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.REmpty;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RPromise.Closure;
import com.oracle.truffle.r.runtime.data.RPromise.PromiseState;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.nodes.InternalRSyntaxNodeChildren;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

// TODO Implement completely, this is a simple implementation that works when the envir argument is ignored
@RBuiltin(name = "do.call", visibility = CUSTOM, kind = INTERNAL, parameterNames = {"what", "args", "envir"}, behavior = COMPLEX)
public abstract class DoCall extends RBuiltinNode implements InternalRSyntaxNodeChildren {

    @Child private GetFunctions.Get getNode;
    @Child private GetCallerFrameNode getCallerFrame;
    @Child private GetNamesAttributeNode getNamesNode = GetNamesAttributeNode.create();

    private final BranchProfile containsRLanguageProfile = BranchProfile.create();
    private final BranchProfile containsRSymbolProfile = BranchProfile.create();

    private final Object argsIdentifier = new Object();
    @Child private RCallBaseNode call = RCallNode.createExplicitCall(argsIdentifier);
    @Child private FrameSlotNode slot = FrameSlotNode.createTemp(argsIdentifier, true);

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.arg("what").defaultError(Message.MUST_BE_STRING_OR_FUNCTION, "what").mustBe(instanceOf(RFunction.class).or(stringValue()));
        casts.arg("args").mustBe(RAbstractListVector.class, Message.SECOND_ARGUMENT_LIST);
        casts.arg("envir").mustBe(REnvironment.class, Message.MUST_BE_ENVIRON, "envir");
    }

    @Specialization
    protected Object doCall(VirtualFrame frame, String what, RList argsAsList, REnvironment env) {
        RFunction func;
        if (getNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getNode = insert(GetNodeGen.create());
        }
        func = (RFunction) getNode.execute(frame, what, env, RType.Function.getName(), true);
        return doCall(frame, func, argsAsList, env);
    }

    @Specialization
    protected Object doCall(VirtualFrame frame, RStringVector what, RList argsAsList, REnvironment env) {
        if (what.getLength() != 1) {
            throw RError.error(this, RError.Message.MUST_BE_STRING_OR_FUNCTION, "what");
        }
        return doCall(frame, what.getDataAt(0), argsAsList, env);
    }

    @Specialization
    protected Object doCall(VirtualFrame frame, RFunction func, RList argsAsList, @SuppressWarnings("unused") REnvironment env) {
        /*
         * To re-create the illusion of a normal call, turn the values in argsAsList into promises.
         */
        Object[] argValues = argsAsList.getDataCopy();
        RStringVector n = getNamesNode.getNames(argsAsList);
        ArgumentsSignature signature;
        if (n == null) {
            signature = ArgumentsSignature.empty(argValues.length);
        } else {
            String[] argNames = new String[argValues.length];
            for (int i = 0; i < argValues.length; i++) {
                String name = n.getDataAt(i);
                argNames[i] = name == null ? null : name.isEmpty() ? null : name;
            }
            signature = ArgumentsSignature.get(argNames);
        }
        MaterializedFrame callerFrame = null;
        for (int i = 0; i < argValues.length; i++) {
            Object arg = argValues[i];
            if (arg instanceof RLanguage) {
                containsRLanguageProfile.enter();
                callerFrame = getCallerFrame(frame, callerFrame);
                RLanguage lang = (RLanguage) arg;
                argValues[i] = createRLanguagePromise(callerFrame, lang);
            } else if (arg instanceof RSymbol) {
                containsRSymbolProfile.enter();
                RSymbol symbol = (RSymbol) arg;
                if (symbol.getName().isEmpty()) {
                    argValues[i] = REmpty.instance;
                } else {
                    callerFrame = getCallerFrame(frame, callerFrame);
                    argValues[i] = createLookupPromise(callerFrame, symbol);
                }
            }
        }
        FrameSlot frameSlot = slot.executeFrameSlot(frame);
        try {
            frame.setObject(frameSlot, new RArgsValuesAndNames(argValues, signature));
            return call.execute(frame, func);
        } finally {
            frame.setObject(frameSlot, null);
        }
    }

    @TruffleBoundary
    private static RPromise createLookupPromise(MaterializedFrame callerFrame, RSymbol symbol) {
        Closure closure = RPromise.Closure.create(RContext.getASTBuilder().lookup(RSyntaxNode.SOURCE_UNAVAILABLE, symbol.getName(), false).asRNode());
        return RDataFactory.createPromise(PromiseState.Supplied, closure, callerFrame);
    }

    @TruffleBoundary
    private static RPromise createRLanguagePromise(MaterializedFrame callerFrame, RLanguage lang) {
        return RDataFactory.createPromise(PromiseState.Supplied, RPromise.Closure.create(RASTUtils.cloneNode(lang.getRep())), callerFrame);
    }

    private MaterializedFrame getCallerFrame(VirtualFrame frame, MaterializedFrame callerFrame) {
        if (getCallerFrame == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getCallerFrame = insert(new GetCallerFrameNode());
        }
        if (callerFrame == null) {
            return getCallerFrame.execute(frame);
        } else {
            return callerFrame;
        }
    }
}
