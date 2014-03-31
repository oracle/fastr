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

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

/**
 * The {@code} .Internal part of the {@code array} function. The R code may alter the arguments
 * before calling {@code .Internal}.
 *
 * <pre>
 * array <- function(data = NA, dim = length(data), dimnames = NULL) { .Internal.array(data, dim, dimnames) }
 * </pre>
 *
 * TODO complete. This is sufficient for the b25 benchmark use.
 */
@RBuiltin(".Internal.array")
public abstract class Array extends RBuiltinNode {

    @Specialization(order = 0, guards = "lengthMatches")
    public Object doArray(RAbstractVector vec, int dim, @SuppressWarnings("unused") RNull dimnames) {
        controlVisibility();
        return vec.copyWithNewDimensions(new int[]{dim});
    }

    @Specialization(order = 1, guards = "totalLengthMatches")
    public Object doArray(RAbstractVector vec, RDoubleVector dim, @SuppressWarnings("unused") RNull dimnames) {
        controlVisibility();
        int[] dims = new int[dim.getLength()];
        for (int i = 0; i < dim.getLength(); i++) {
            dims[i] = (int) dim.getDataAt(i);
        }
        return vec.copyWithNewDimensions(dims);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 100)
    public Object doArray(Object vec, Object dim, Object dimnames) {
        controlVisibility();
        throw RError.getGenericError(getEncapsulatingSourceSection(), "unimplemented or invalid argument types to 'array'");
    }

    public static boolean lengthMatches(RAbstractVector vec, int dim, @SuppressWarnings("unused") RNull dimnames) {
        return dim == vec.getLength();
    }

    public static boolean totalLengthMatches(RAbstractVector vec, RDoubleVector dim, @SuppressWarnings("unused") RNull dimnames) {
        return totalLength(dim) == vec.getLength();
    }

    private static int totalLength(RDoubleVector dim) {
        int sum = 0;
        for (int i = 0; i < dim.getLength(); i++) {
            int d = (int) dim.getDataAt(i);
            sum = i == 0 ? d : sum * d;
        }
        return sum;
    }
}
