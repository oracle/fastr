/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.env.frame.*;

/**
 * The base class for R code that can be executed, namely {@link FunctionDefinitionNode} and
 * {@link RBuiltinNode}.
 */
public abstract class RRootNode extends RootNode {

    @CompilationFinal private StableValue<MaterializedFrame> enclosingFrameAssumption;
    @CompilationFinal private StableValue<FrameDescriptor> enclosingFrameDescriptorAssumption;
    private final ValueProfile enclosingFrameProfile = ValueProfile.createClassProfile();
    @CompilationFinal protected boolean checkSingletonFrame = true;
    private final ValueProfile functionProfile = ValueProfile.createIdentityProfile();

    /**
     * The formal arguments this function is supposed to take.
     */
    private final FormalArguments formalArguments;

    protected RRootNode(SourceSection src, FormalArguments formalArguments, FrameDescriptor frameDescriptor) {
        super(src, frameDescriptor);
        this.formalArguments = formalArguments;
        this.enclosingFrameAssumption = FrameSlotChangeMonitor.getEnclosingFrameAssumption(frameDescriptor);
        this.enclosingFrameDescriptorAssumption = FrameSlotChangeMonitor.getEnclosingFrameDescriptorAssumption(frameDescriptor);
    }

    protected void verifyEnclosingAssumptions(VirtualFrame vf) {
        RArguments.setFunction(vf, functionProfile.profile(RArguments.getFunction(vf)));

        if (checkSingletonFrame) {
            checkSingletonFrame = FrameSlotChangeMonitor.checkSingletonFrame(vf);
        }
        if (enclosingFrameAssumption != null) {
            try {
                enclosingFrameAssumption.getAssumption().check();
            } catch (InvalidAssumptionException e) {
                enclosingFrameAssumption = FrameSlotChangeMonitor.getEnclosingFrameAssumption(getFrameDescriptor());
            }
            if (enclosingFrameAssumption != null) {
                MaterializedFrame enclosingFrame = RArguments.getEnclosingFrame(vf);
                if (enclosingFrameAssumption != null) {
                    if (enclosingFrameAssumption.getValue() != enclosingFrame) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        enclosingFrameAssumption = FrameSlotChangeMonitor.getOrInitializeEnclosingFrameAssumption(getFrameDescriptor(), enclosingFrameAssumption, enclosingFrame);
                    }
                }
            }
        }
        if (enclosingFrameDescriptorAssumption != null) {
            try {
                enclosingFrameDescriptorAssumption.getAssumption().check();
            } catch (InvalidAssumptionException e) {
                enclosingFrameDescriptorAssumption = FrameSlotChangeMonitor.getEnclosingFrameDescriptorAssumption(getFrameDescriptor());
            }
            if (enclosingFrameDescriptorAssumption != null) {
                MaterializedFrame enclosingFrame = RArguments.getEnclosingFrame(vf);
                FrameDescriptor enclosingFrameDescriptor = enclosingFrame == null ? null : enclosingFrameProfile.profile(enclosingFrame).getFrameDescriptor();
                if (enclosingFrameDescriptorAssumption.getValue() != enclosingFrameDescriptor) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    enclosingFrameDescriptorAssumption = FrameSlotChangeMonitor.getOrInitializeEnclosingFrameDescriptorAssumption(getFrameDescriptor(), enclosingFrameDescriptorAssumption,
                                    enclosingFrameDescriptor);
                }
            }
        }
    }

    /**
     * @return The names of the {@link FormalArguments} this function expects
     */
    public Object[] getParameterNames() {
        return formalArguments.getNames();
    }

    /**
     * @return The number of parameters this functions expects
     */
    public int getParameterCount() {
        return formalArguments.getArgsCount();
    }

    /**
     * @return {@link #formalArguments}
     */
    public FormalArguments getFormalArguments() {
        return formalArguments;
    }

    @TruffleBoundary
    public String getSourceCode() {
        SourceSection ss = getSourceSection();
        if (ss != null) {
            return ss.getCode();
        } else {
            return null;
        }
    }

    @Override
    public boolean isCloningAllowed() {
        return true;
    }
}
