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
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.builtin.base.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

public abstract class IsFactor extends RInvisibleBuiltinNode {
    @Child Typeof typeof;
    @Child Inherits inherits;

    public abstract byte execute(VirtualFrame frame, Object x);

    @Specialization
    public byte isFactor(VirtualFrame frame, Object x) {
        if (typeof == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            typeof = insert(TypeofFactory.create(new RNode[1], this.getBuiltin()));
        }
        if (!typeof.execute(frame, x).equals(RRuntime.TYPE_INTEGER)) {
            return RRuntime.LOGICAL_FALSE;
        }
        if (inherits == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            inherits = insert(InheritsFactory.create(new RNode[3], this.getBuiltin()));
        }
        return inherits.execute(frame, x, RDataFactory.createStringVector(RRuntime.TYPE_FACTOR));
    }
}
