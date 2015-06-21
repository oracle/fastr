/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.function;

import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

/**
 * {@link WrapArgumentBaseNode} is a super class of wrappers handling functino arguments.
 *
 */
public abstract class WrapArgumentBaseNode extends RNode implements RSyntaxNode {

    @Child protected RNode operand;

    private final BranchProfile everSeenVector;
    private final BranchProfile everSeenDataFrame;
    private final BranchProfile everSeenFactor;

    protected final BranchProfile everSeenShared;
    protected final BranchProfile everSeenTemporary;
    protected final BranchProfile everSeenNonTemporary;

    protected final BranchProfile shareable;
    protected final BranchProfile nonShareable;

    protected WrapArgumentBaseNode(RNode operand, boolean initProfiles) {
        this.operand = operand;
        if (initProfiles) {
            everSeenVector = BranchProfile.create();
            everSeenDataFrame = BranchProfile.create();
            everSeenFactor = BranchProfile.create();
            everSeenShared = BranchProfile.create();
            everSeenTemporary = BranchProfile.create();
            everSeenNonTemporary = BranchProfile.create();
            shareable = BranchProfile.create();
            nonShareable = BranchProfile.create();
        } else {
            everSeenShared = null;
            everSeenTemporary = null;
            everSeenNonTemporary = null;
            everSeenVector = null;
            everSeenDataFrame = null;
            everSeenFactor = null;
            shareable = null;
            nonShareable = null;
        }
    }

    protected RVector getVector(Object result) {
        if (result instanceof RVector) {
            everSeenVector.enter();
            return (RVector) result;
        } else if (result instanceof RDataFrame) {
            everSeenDataFrame.enter();
            return ((RDataFrame) result).getVector();
        } else if (result instanceof RFactor) {
            everSeenFactor.enter();
            return ((RFactor) result).getVector();
        } else {
            nonShareable.enter();
            return null;
        }
    }

    public RNode getOperand() {
        return operand;
    }

    @Override
    public boolean isBackbone() {
        return true;
    }

    @Override
    public void deparse(RDeparse.State state) {
        RSyntaxNode.cast(getOperand()).deparse(state);
    }

    @Override
    public void serialize(RSerialize.State state) {
        RSyntaxNode.cast(getOperand()).serialize(state);
    }

}
