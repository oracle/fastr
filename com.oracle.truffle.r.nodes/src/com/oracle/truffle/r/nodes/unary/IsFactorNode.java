/*
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

package com.oracle.truffle.r.nodes.unary;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.r.nodes.helpers.InheritsCheckNode;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.nodes.unary.UnaryNode;

public final class IsFactorNode extends UnaryNode {

    @Child private TypeofNode typeofNode;
    @Child private InheritsCheckNode inheritsCheck;

    public boolean executeIsFactor(Object x) {
        if (typeofNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            typeofNode = insert(TypeofNode.create());
        }
        if (typeofNode.execute(x) != RType.Integer) {
            // Note: R does not allow to set class 'factor' to an arbitrary object, unlike with
            // data.frame
            return false;
        }
        if (inheritsCheck == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            inheritsCheck = insert(InheritsCheckNode.create(RRuntime.CLASS_FACTOR));
        }

        return inheritsCheck.execute(x);
    }

    public Object execute(Object value) {
        return executeIsFactor(value);
    }
}
