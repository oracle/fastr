/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.control;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@NodeChild("operand")
public abstract class RLengthNode extends RNode {

    @Override
    public final Object execute(VirtualFrame frame) {
        return executeInteger(frame);
    }

    @Override
    public abstract int executeInteger(VirtualFrame frame);

    public abstract int executeInteger(Object value);

    public static RLengthNode create() {
        return RLengthNodeGen.create(null);
    }

    @Specialization
    @SuppressWarnings("unused")
    protected int getLength(RNull operand) {
        return 0;
    }

    @Specialization
    @SuppressWarnings("unused")
    protected int getLength(int operand) {
        return 1;
    }

    @Specialization
    @SuppressWarnings("unused")
    protected int getLength(double operand) {
        return 1;
    }

    @Specialization
    protected int getLength(RExpression operand, @Cached("createRecursive()") RLengthNode recursiveLength) {
        return recursiveLength.executeInteger(operand.getList());
    }

    @Specialization
    protected int getLength(RAbstractContainer operand) {
        return operand.getLength();
    }

    public static RLengthNode createRecursive() {
        return RLengthNodeGen.create(null);
    }
}
