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

import static com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "dimnames<-", kind = PRIMITIVE, parameterNames = {"x", ""})
// 2nd parameter is "value", but should not be matched against, so ""
@SuppressWarnings("unused")
public abstract class UpdateDimNames extends RInvisibleBuiltinNode {

    @Child private CastStringNode castStringNode;
    @Child private CastToVectorNode castVectorNode;

    private Object castString(VirtualFrame frame, Object o) {
        initCastStringNode();
        return castStringNode.executeCast(frame, o);
    }

    @TruffleBoundary
    private void initCastStringNode() {
        if (castStringNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castStringNode = insert(CastStringNodeFactory.create(null, true, true, false, false));
        }
    }

    private RAbstractVector castVector(VirtualFrame frame, Object value) {
        initCastVectorNode();
        return ((RAbstractVector) castVectorNode.executeObject(frame, value)).materialize();
    }

    @TruffleBoundary
    private void initCastVectorNode() {
        if (castVectorNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castVectorNode = insert(CastToVectorNodeFactory.create(null, false, false, false, false));
        }
    }

    public abstract RAbstractVector executeList(VirtualFrame frame, RAbstractVector vector, Object o);

    public RList convertToListOfStrings(VirtualFrame frame, RList list) {
        assert (!list.isShared());
        for (int i = 0; i < list.getLength(); i++) {
            Object element = list.getDataAt(i);
            if (element != RNull.instance) {
                Object s = castString(frame, castVector(frame, element));
                list.updateDataAt(i, s, null);
            }
        }
        return list;
    }

    @Specialization
    @TruffleBoundary
    protected RAbstractVector updateDimnames(RAbstractVector vector, RNull list) {
        RVector v = vector.materialize();
        v.setDimNames(null, getEncapsulatingSourceSection());
        controlVisibility();
        return v;
    }

    @Specialization(guards = "isZeroLength")
    @TruffleBoundary
    protected RAbstractVector updateDimnamesEmpty(RAbstractVector vector, RList list) {
        RVector v = vector.materialize();
        v.setDimNames(null, getEncapsulatingSourceSection());
        controlVisibility();
        return v;
    }

    @Specialization(guards = "!isZeroLength")
    protected RAbstractVector updateDimnames(VirtualFrame frame, RAbstractVector vector, RList list) {
        RVector v = vector.materialize();
        v.setDimNames(convertToListOfStrings(frame, list), getEncapsulatingSourceSection());
        controlVisibility();
        return v;
    }

    @Specialization(guards = "!isVectorList")
    protected RAbstractVector updateDimnamesError(RAbstractVector vector, RAbstractVector v) {
        controlVisibility();
        CompilerDirectives.transferToInterpreter();
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.DIMNAMES_LIST);
    }

    @Specialization
    protected RAbstractVector updateDimnamesError(RAbstractVector vector, RFunction v) {
        controlVisibility();
        CompilerDirectives.transferToInterpreter();
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.DIMNAMES_LIST);
    }

    protected boolean isVectorList(RAbstractVector vector, RAbstractVector v) {
        return v.getElementClass() == Object.class;
    }

    protected boolean isZeroLength(VirtualFrame frame, RAbstractVector vector, RList list) {
        return list.getLength() == 0;
    }
}
