/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.runtime.data.*;

@NodeChild(value = "operand", type = RNode.class)
public abstract class WrapArgumentNode extends RProxyNode {

    private final BranchProfile everSeenShared = BranchProfile.create();
    private final BranchProfile everSeenTemporary = BranchProfile.create();
    private final BranchProfile everSeenNonTemporary = BranchProfile.create();

    private final boolean modeChange;

    protected WrapArgumentNode(boolean modeChange) {
        this.modeChange = modeChange;
    }

    protected WrapArgumentNode(WrapArgumentNode other) {
        this.modeChange = other.modeChange;
    }

    @Override
    protected RVector proxyVector(RVector vector) {
        if (modeChange) {
            // mark vector as wrapped only if changing its mode to shared; otherwise make sure that
            // it can be seen as "truly" shared by marking vector unwrapped
            if (vector.isShared()) {
                everSeenShared.enter();
                return vector;
            }
            if (vector.isTemporary()) {
                everSeenTemporary.enter();
                vector.markNonTemporary();
                return vector;
            }
            everSeenNonTemporary.enter();
            vector.makeShared();
        }
        return vector;
    }

    @Override
    protected RDataFrame proxyDataFrame(RDataFrame dataFrame) {
        proxyVector(dataFrame.getVector());
        return dataFrame;
    }

    public abstract RNode getOperand();

    public static WrapArgumentNode create(RNode operand, boolean modeChange) {
        if (operand instanceof WrapArgumentNode) {
            return (WrapArgumentNode) operand;
        } else {
            WrapArgumentNode wan = WrapArgumentNodeFactory.create(modeChange, operand);
            wan.assignSourceSection(operand.getSourceSection());
            return wan;
        }
    }
}
