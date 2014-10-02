/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

package com.oracle.truffle.r.nodes.unary;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

public abstract class IsFactorNode extends UnaryNode {
    @Child private TypeofNode typeofNode;
    @Child private InheritsNode inheritsNode;

    public abstract byte execute(VirtualFrame frame, Object x);

    @Specialization
    protected byte isFactor(VirtualFrame frame, Object x) {
        if (typeofNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            typeofNode = insert(TypeofNodeFactory.create(null));
        }
        if (typeofNode.execute(frame, x) != RType.Integer) {
            return RRuntime.LOGICAL_FALSE;
        }
        if (inheritsNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            inheritsNode = insert(InheritsNodeFactory.create(null, null));
        }
        return inheritsNode.execute(frame, x, RDataFactory.createStringVector(RType.Factor.getName()));
    }
}
