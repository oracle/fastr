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

    private BranchProfile everSeenShared = new BranchProfile();
    private BranchProfile everSeenTemporary = new BranchProfile();
    private BranchProfile everSeenNonTemporary = new BranchProfile();

    @Override
    protected RVector proxyVector(RVector vector) {
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
        return vector;
    }

    @Override
    protected RDataFrame proxyDataFrame(RDataFrame dataFrame) {
        proxyVector(dataFrame.getVector());
        return dataFrame;
    }

    public abstract RNode getOperand();

    public static WrapArgumentNode create(RNode operand) {
        if (operand instanceof WrapArgumentNode) {
            return (WrapArgumentNode) operand;
        } else {
            WrapArgumentNode wan = WrapArgumentNodeFactory.create(operand);
            wan.assignSourceSection(operand.getSourceSection());
            return wan;
        }
    }
}
