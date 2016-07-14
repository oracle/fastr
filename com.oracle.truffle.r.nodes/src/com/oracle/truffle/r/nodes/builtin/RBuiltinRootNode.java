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
package com.oracle.truffle.r.nodes.builtin;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.nodes.RRootNode;
import com.oracle.truffle.r.nodes.function.FormalArguments;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.FastPathFactory;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;

public final class RBuiltinRootNode extends RRootNode {

    @Child private RBuiltinNode builtin;
    private final RBuiltinFactory factory;

    RBuiltinRootNode(RBuiltinFactory factory, RBuiltinNode builtin, FormalArguments formalArguments, FrameDescriptor frameDescriptor, FastPathFactory fastPath) {
        super(null, formalArguments, frameDescriptor, fastPath);
        this.factory = factory;
        this.builtin = builtin;
    }

    @Override
    public RootCallTarget duplicateWithNewFrameDescriptor() {
        FrameDescriptor frameDescriptor = new FrameDescriptor();
        FrameSlotChangeMonitor.initializeFunctionFrameDescriptor("builtin", frameDescriptor);
        return Truffle.getRuntime().createCallTarget(new RBuiltinRootNode(factory, (RBuiltinNode) builtin.deepCopy(), getFormalArguments(), frameDescriptor, getFastPath()));
    }

    @Override
    public boolean isInstrumentable() {
        return false;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        verifyEnclosingAssumptions(frame);
        Object result = null;
        try {
            result = builtin.execute(frame);
        } catch (NullPointerException | ArrayIndexOutOfBoundsException | AssertionError e) {
            CompilerDirectives.transferToInterpreter();
            throw new RInternalError(e, "internal error");
        }

        RContext.getInstance().setVisible(factory.getVisibility());
        return result;
    }

    public RBuiltinNode getBuiltinNode() {
        return builtin;
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
    public String getSourceCode() {
        throw RInternalError.shouldNotReachHere();
    }

    @Override
    public String getName() {
        return "RBuiltin(" + builtin + ")";
    }

    @Override
    public String toString() {
        return getName();
    }
}
