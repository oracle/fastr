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

import static com.oracle.truffle.r.runtime.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.access.FrameSlotNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.GetFunctionsFactory.GetNodeGen;
import com.oracle.truffle.r.nodes.function.GetCallerFrameNode;
import com.oracle.truffle.r.nodes.function.RCallNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.RVisibility;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RAttributeProfiles;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.REmpty;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.nodes.InternalRSyntaxNodeChildren;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

// TODO Implement properly, this is a simple implementation that works when the environment doesn't matter
@RBuiltin(name = "do.call", visibility = RVisibility.CUSTOM, kind = INTERNAL, parameterNames = {"what", "args", "envir"})
public abstract class DoCall extends RBuiltinNode implements InternalRSyntaxNodeChildren {

    @Child private GetFunctions.Get getNode;
    @Child private GetCallerFrameNode getCallerFrame;

    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();
    private final BranchProfile errorProfile = BranchProfile.create();
    private final BranchProfile containsRLanguageProfile = BranchProfile.create();
    private final BranchProfile containsRSymbolProfile = BranchProfile.create();

    private final Object argsIdentifier = new Object();
    @Child private RCallNode call = RCallNode.createExplicitCall(argsIdentifier);
    @Child private FrameSlotNode slot = FrameSlotNode.createTemp(argsIdentifier, true);

    @Specialization
    protected Object doDoCall(VirtualFrame frame, Object what, RList argsAsList, REnvironment env) {
        /*
         * Step 1: handle the variants of "what" (could be done in extra specializations) and assign
         * "func".
         */
        RFunction func;
        if (what instanceof RFunction) {
            func = (RFunction) what;
        } else if (what instanceof String || (what instanceof RAbstractStringVector && ((RAbstractStringVector) what).getLength() == 1)) {
            if (getNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getNode = insert(GetNodeGen.create(null));
            }
            func = (RFunction) getNode.execute(frame, what, env, RType.Function.getName(), RRuntime.LOGICAL_TRUE);
        } else {
            errorProfile.enter();
            throw RError.error(this, RError.Message.MUST_BE_STRING_OR_FUNCTION, "what");
        }

        /*
         * Step 2: To re-create the illusion of a normal call, turn the values in argsAsList into
         * promises.
         */
        Object[] argValues = argsAsList.getDataCopy();
        RStringVector n = argsAsList.getNames(attrProfiles);
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
                argValues[i] = createArgPromise(callerFrame, RASTUtils.cloneNode(lang.getRep()));
            } else if (arg instanceof RSymbol) {
                containsRSymbolProfile.enter();
                RSymbol symbol = (RSymbol) arg;
                if (symbol.getName().isEmpty()) {
                    argValues[i] = REmpty.instance;
                } else {
                    callerFrame = getCallerFrame(frame, callerFrame);
                    argValues[i] = createArgPromise(callerFrame, RASTUtils.createReadVariableNode(symbol.getName()));
                }
            }
        }
        FrameSlot frameSlot = slot.executeFrameSlot(frame);
        frame.setObject(frameSlot, new RArgsValuesAndNames(argValues, signature));
        return call.execute(frame, func);
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

    @TruffleBoundary
    private static RPromise createArgPromise(MaterializedFrame frame, RBaseNode rep) {
        return RDataFactory.createPromise(RPromise.PromiseType.ARG_SUPPLIED, frame, RPromise.Closure.create(rep));
    }
}
