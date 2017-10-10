/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.nodes.RRootNode;
import com.oracle.truffle.r.nodes.access.AccessArgumentNode;
import com.oracle.truffle.r.nodes.function.FormalArguments;
import com.oracle.truffle.r.nodes.function.RCallNode.BuiltinCallNode;
import com.oracle.truffle.r.nodes.function.visibility.SetVisibilityNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.builtins.FastPathFactory;
import com.oracle.truffle.r.runtime.builtins.RBuiltinKind;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.TruffleRLanguage;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

public final class RBuiltinRootNode extends RRootNode {

    @Child private BuiltinCallNode call;
    @Children private final AccessArgumentNode[] args;
    @Child private SetVisibilityNode visibility = SetVisibilityNode.create();

    private final RBuiltinFactory factory;

    RBuiltinRootNode(TruffleRLanguage language, RBuiltinFactory factory, FrameDescriptor frameDescriptor, FastPathFactory fastPath) {
        super(language, frameDescriptor, fastPath);
        this.factory = factory;
        this.args = new AccessArgumentNode[factory.getSignature().getLength()];
    }

    @Override
    public SourceSection getSourceSection() {
        return RSyntaxNode.INTERNAL;
    }

    @Override
    public FormalArguments getFormalArguments() {
        initialize();
        return call.getFormals();
    }

    @Override
    public ArgumentsSignature getSignature() {
        initialize();
        return call.getFormals().getSignature();
    }

    @Override
    public RootCallTarget duplicateWithNewFrameDescriptor() {
        FrameDescriptor frameDescriptor = new FrameDescriptor();
        FrameSlotChangeMonitor.initializeFunctionFrameDescriptor("builtin", frameDescriptor);
        return Truffle.getRuntime().createCallTarget(new RBuiltinRootNode(getLanguage(RContext.getTruffleRLanguage()), factory, frameDescriptor, getFastPath()));
    }

    @Override
    public boolean isInstrumentable() {
        return false;
    }

    @Override
    @ExplodeLoop
    public Object execute(VirtualFrame frame) {
        verifyEnclosingAssumptions(frame);
        try {
            initialize();
            Object[] arguments = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                if (args[i] == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    args[i] = insert(AccessArgumentNode.create(i));
                    args[i].setFormals(call.getFormals());
                }
                arguments[i] = args[i].execute(frame);
            }
            return call.execute(frame, null, new RArgsValuesAndNames(arguments, factory.getSignature()), null);
        } catch (NullPointerException | ArrayIndexOutOfBoundsException | AssertionError e) {
            CompilerDirectives.transferToInterpreter();
            throw new RInternalError(e, "internal error");
        } finally {
            visibility.execute(frame, factory.getVisibility());
            visibility.executeEndOfFunction(frame);
        }
    }

    private void initialize() {
        if (call == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            RBuiltinNode builtin = factory.getConstructor().get();
            FormalArguments formalArguments = FormalArguments.createForBuiltin(builtin.getDefaultParameterValues(), factory.getSignature());
            if (factory.getKind() == RBuiltinKind.INTERNAL) {
                assert builtin.getDefaultParameterValues().length == 0 : "INTERNAL builtins do not need default values";
                assert factory.getSignature().getVarArgCount() == 0 || factory.getSignature().getVarArgIndex() == factory.getSignature().getLength() - 1 : "only last argument can be vararg";
            }
            call = insert(new BuiltinCallNode(builtin, factory, formalArguments, null, true));
        }
    }

    public RBuiltinNode getBuiltinNode() {
        initialize();
        return call.getBuiltin();
    }

    @Override
    public RBuiltinFactory getBuiltin() {
        return factory;
    }

    @Override
    public boolean needsSplitting() {
        return factory.isAlwaysSplit();
    }

    @Override
    public boolean containsDispatch() {
        return false;
    }

    @Override
    public void setContainsDispatch(boolean containsDispatch) {
        throw RInternalError.shouldNotReachHere("set containsDispatch on builtin " + factory.getName());
    }

    @Override
    public String getName() {
        return "RBuiltin(" + factory.getName() + ")";
    }
}
