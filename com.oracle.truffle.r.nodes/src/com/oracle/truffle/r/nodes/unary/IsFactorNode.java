/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

package com.oracle.truffle.r.nodes.unary;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.r.nodes.helpers.InheritsCheckNode;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;

public final class IsFactorNode extends UnaryNode {

    @Child private TypeofNode typeofNode;
    @Child private InheritsCheckNode inheritsCheck;

    public boolean executeIsFactor(Object x) {
        if (typeofNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            typeofNode = insert(TypeofNodeGen.create());
        }
        if (typeofNode.execute(x) != RType.Integer) {
            // Note: R does not allow to set class 'factor' to an arbitrary object, unlike with
            // data.frame
            return false;
        }
        if (inheritsCheck == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            inheritsCheck = insert(new InheritsCheckNode(RRuntime.CLASS_FACTOR));
        }

        return inheritsCheck.execute(x);
    }

    public Object execute(Object value) {
        return executeIsFactor(value);
    }
}
