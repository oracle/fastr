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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "length<-", kind = PRIMITIVE, parameterNames = {"x", ""})
// 2nd parameter is "value", but should not be matched against, so ""
public abstract class UpdateLength extends RInvisibleBuiltinNode {

    @CreateCast("arguments")
    protected RNode[] castStatusArgument(RNode[] arguments) {
        // length argument is at index 1, and cast to int
        arguments[1] = CastIntegerNodeFactory.create(arguments[1], true, false, false);
        return arguments;
    }

    @Specialization(guards = "isLengthOne")
    protected RAbstractVector updateLength(RAbstractVector vector, RAbstractIntVector lengthVector) {
        controlVisibility();
        int length = lengthVector.getDataAt(0);
        RVector resultVector = vector.materialize();
        if (resultVector.isShared()) {
            resultVector = resultVector.copy();
            resultVector.markNonTemporary();
        }
        resultVector.resizeWithNames(length);
        return resultVector;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "!isLengthOne")
    protected RAbstractVector updateLengthError(VirtualFrame frame, RAbstractVector vector, RAbstractIntVector lengthVector) {
        controlVisibility();
        throw RError.error(frame, this.getEncapsulatingSourceSection(), RError.Message.INVALID_UNNAMED_VALUE);
    }

    @SuppressWarnings("unused")
    @Specialization
    protected Object updateLengthError(VirtualFrame frame, Object vector, Object lengthVector) {
        controlVisibility();
        throw RError.error(frame, this.getEncapsulatingSourceSection(), RError.Message.INVALID_UNNAMED_VALUE);
    }

    protected static boolean isLengthOne(@SuppressWarnings("unused") RAbstractVector vector, RAbstractIntVector length) {
        return length.getLength() == 1;
    }

}
