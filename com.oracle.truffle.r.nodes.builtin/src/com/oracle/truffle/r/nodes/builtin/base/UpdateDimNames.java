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
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
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

    private Object castString(Object o) {
        if (castStringNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castStringNode = insert(CastStringNodeGen.create(true, true, false, false));
        }
        return castStringNode.execute(o);
    }

    private RAbstractVector castVector(Object value) {
        if (castVectorNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castVectorNode = insert(CastToVectorNodeGen.create(false));
        }
        return ((RAbstractVector) castVectorNode.execute(value)).materialize();
    }

    public abstract RAbstractContainer executeRAbstractContainer(RAbstractContainer container, Object o);

    public RList convertToListOfStrings(RList oldList) {
        RList list = oldList;
        if (list.isShared()) {
            list = (RList) list.copy();
        }
        for (int i = 0; i < list.getLength(); i++) {
            Object element = list.getDataAt(i);
            if (element != RNull.instance) {
                Object s = castString(castVector(element));
                list.updateDataAt(i, s, null);
            }
        }
        return list;
    }

    @Specialization
    @TruffleBoundary
    protected RAbstractContainer updateDimnames(RAbstractContainer container, RNull list) {
        RAbstractContainer result = container.materializeNonShared();
        result.setDimNames(null);
        controlVisibility();
        return result;
    }

    @Specialization(guards = "isZeroLength(list)")
    @TruffleBoundary
    protected RAbstractContainer updateDimnamesEmpty(RAbstractContainer container, RList list) {
        RAbstractContainer result = container.materializeNonShared();
        result.setDimNames(null);
        controlVisibility();
        return result;
    }

    @Specialization(guards = "!isZeroLength(list)")
    protected RAbstractContainer updateDimnames(RAbstractContainer container, RList list) {
        RAbstractContainer result = container.materializeNonShared();
        result.setDimNames(convertToListOfStrings(list));
        controlVisibility();
        return result;
    }

    @Specialization(guards = "!isContainerList(c)")
    protected RAbstractContainer updateDimnamesError(RAbstractContainer container, RAbstractContainer c) {
        controlVisibility();
        CompilerDirectives.transferToInterpreter();
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.DIMNAMES_LIST);
    }

    @Specialization
    protected RAbstractContainer updateDimnamesError(RAbstractContainer container, RFunction v) {
        controlVisibility();
        CompilerDirectives.transferToInterpreter();
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.DIMNAMES_LIST);
    }

    protected boolean isContainerList(RAbstractContainer c) {
        return c instanceof RList;
    }

    protected boolean isZeroLength(RList list) {
        return list.getLength() == 0;
    }
}
