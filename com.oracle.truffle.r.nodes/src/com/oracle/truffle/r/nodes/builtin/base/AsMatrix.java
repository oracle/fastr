/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.model.*;

/**
 * TODO flesh this out, it only supports the identify function currently (for b25).
 */
@RBuiltin(name = "as.matrix", kind = SUBSTITUTE)
// TODO revert to R
public abstract class AsMatrix extends RBuiltinNode {

    @Specialization(order = 0, guards = "isMatrix")
    public Object doAsMatrix(RAbstractVector vec) {
        controlVisibility();
        return vec;
    }

    @Specialization(order = 100)
    public Object doAsMatrixNot(@SuppressWarnings("unused") Object vec) {
        controlVisibility();
        throw RError.getGenericError(getEncapsulatingSourceSection(), "invslid or unimplemented arg to as.matrix");
    }

    public boolean isMatrix(RAbstractVector vec) {
        return vec.isMatrix();
    }

}
