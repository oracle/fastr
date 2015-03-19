/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.nodes.function.DispatchedCallNode.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "length<-", kind = PRIMITIVE, parameterNames = {"x", ""})
// 2nd parameter is "value", but should not be matched against, so ""
public abstract class UpdateLength extends RInvisibleBuiltinNode {

    @Child private DispatchedCallNode dcn;

    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

    @CreateCast("arguments")
    protected RNode[] castStatusArgument(RNode[] arguments) {
        // length argument is at index 1, and cast to int
        arguments[1] = CastIntegerNodeGen.create(arguments[1], true, false, false);
        return arguments;
    }

    @Specialization(guards = {"isLengthOne(lengthVector)", "isObject(frame, container)"})
    protected Object updateLengthObject(VirtualFrame frame, RAbstractContainer container, RAbstractIntVector lengthVector) {
        if (dcn == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            dcn = insert(DispatchedCallNode.create("length<-", DispatchType.UseMethod, getSuppliedSignature()));
        }
        try {
            return dcn.executeInternal(frame, container.getClassHierarchy(), new Object[]{container, lengthVector});
        } catch (NoGenericMethodException e) {
            return updateLength(frame, container, lengthVector);
        }

    }

    @Specialization(guards = {"isLengthOne(lengthVector)", "!isObject(frame, container)"})
    protected RAbstractContainer updateLength(@SuppressWarnings("unused") VirtualFrame frame, RAbstractContainer container, RAbstractIntVector lengthVector) {
        controlVisibility();
        int length = lengthVector.getDataAt(0);
        // TODO: we can potentially avoid making a copy during materialization and then during
        // resizing but is it worth it for that case?
        RVector vector = container.materializeNonSharedVector();
        return (RAbstractContainer) vector.resize(length, true);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "!isLengthOne(lengthVector)")
    protected RAbstractContainer updateLengthError(RAbstractContainer container, RAbstractIntVector lengthVector) {
        controlVisibility();
        CompilerDirectives.transferToInterpreter();
        throw RError.error(this.getEncapsulatingSourceSection(), RError.Message.INVALID_UNNAMED_VALUE);
    }

    @SuppressWarnings("unused")
    @Specialization
    protected Object updateLengthError(Object vector, Object lengthVector) {
        controlVisibility();
        CompilerDirectives.transferToInterpreter();
        throw RError.error(this.getEncapsulatingSourceSection(), RError.Message.INVALID_UNNAMED_VALUE);
    }

    protected static boolean isLengthOne(RAbstractIntVector length) {
        return length.getLength() == 1;
    }

    protected boolean isObject(VirtualFrame frame, RAbstractContainer container) {
        // if execution got here via S3 dispatch, treat objects as non-objects
        return container.isObject(attrProfiles) && !RArguments.hasS3Args(frame);
    }

}
