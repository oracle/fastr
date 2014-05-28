/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.RBuiltinKind.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "crossprod", kind = INTERNAL)
public abstract class Crossprod extends RBuiltinNode {

    // TODO: this is supposed to be slightly faster than t(x) %*% y but for now it should suffice

    @Child protected MatMult matMult = MatMultFactory.create(new RNode[1], getBuiltin());
    @Child protected Transpose transpose = TransposeFactory.create(new RNode[1], getBuiltin());

    public Object crossprod(VirtualFrame frame, RAbstractVector vector1, RAbstractVector vector2) {
        controlVisibility();
        return matMult.executeObject(frame, transpose.execute(frame, vector1), vector2);
    }

    @Specialization
    @SuppressWarnings("unused")
    public Object dimWithDimensions(VirtualFrame frame, RAbstractVector vector1, RNull vector2) {
        controlVisibility();
        return matMult.executeObject(frame, transpose.execute(frame, vector1), vector1);
    }
}
