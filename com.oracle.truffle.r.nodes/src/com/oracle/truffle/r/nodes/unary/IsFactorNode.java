/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

package com.oracle.truffle.r.nodes.unary;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

public abstract class IsFactorNode extends UnaryNode {

    @Child private TypeofNode typeofNode;
    @Child private InheritsNode inheritsNode;

    public abstract byte executeIsFactor(Object c);

    @Specialization
    protected byte isFactor(Object x) {
        if (typeofNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            typeofNode = insert(TypeofNodeGen.create());
        }
        if (typeofNode.execute(x) != RType.Integer) {
            return RRuntime.LOGICAL_FALSE;
        }
        if (inheritsNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            inheritsNode = insert(InheritsNodeGen.create());
        }
        return inheritsNode.execute(x, RDataFactory.createStringVector(RType.Factor.getName()));
    }
}
