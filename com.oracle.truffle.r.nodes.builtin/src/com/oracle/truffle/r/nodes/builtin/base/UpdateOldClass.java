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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.nodes.builtin.RInvisibleBuiltinNode;
import com.oracle.truffle.r.nodes.unary.CastStringNode;
import com.oracle.truffle.r.nodes.unary.CastStringNodeFactory;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

import static com.oracle.truffle.api.CompilerDirectives.SlowPath;
import static com.oracle.truffle.r.runtime.RBuiltinKind.PRIMITIVE;

// oldClass<- (as opposed to class<-), simply sets the attribute (without handling "implicit" attributes)
@RBuiltin(name = "oldClass<-", kind = PRIMITIVE, parameterNames = {"x", ""})
// 2nd parameter is "value", but should not be matched against, so ""
public abstract class UpdateOldClass extends RInvisibleBuiltinNode {

    @Child private CastStringNode castStringNode;

    public abstract Object execute(VirtualFrame frame, RAbstractContainer vector, Object o);

    @Specialization(guards = "!isStringVector")
    protected Object setOldClass(VirtualFrame frame, RAbstractContainer arg, RAbstractVector className) {
        controlVisibility();
        if (className.getLength() == 0) {
            return setOldClass(arg, RNull.instance);
        }
        initCastStringNode();
        Object result = castStringNode.executeCast(frame, className);
        return setOldClass(arg, (RStringVector) result);
    }

    @SlowPath
    private void initCastStringNode() {
        if (castStringNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castStringNode = insert(CastStringNodeFactory.create(null, false, false, false, false));
        }
    }

    @Specialization
    @SlowPath
    protected Object setOldClass(RAbstractContainer arg, String className) {
        return setOldClass(arg, RDataFactory.createStringVector(className));
    }

    @Specialization
    @SlowPath
    protected Object setOldClass(RAbstractContainer arg, RStringVector className) {
        controlVisibility();
        RVector resultVector = arg.materializeNonSharedVector();
        return RVector.setClassAttr(resultVector, className, arg.getElementClass() == RVector.class ? arg : null);
    }

    @Specialization
    @SlowPath
    protected Object setOldClass(RAbstractContainer arg, @SuppressWarnings("unused") RNull className) {
        controlVisibility();
        RVector resultVector = arg.materializeNonSharedVector();
        return RVector.setClassAttr(resultVector, null, arg.getElementClass() == RVector.class ? arg : null);
    }

    @SlowPath
    public Object setOldClass(RFunction arg, @SuppressWarnings("unused") Object className) {
        controlVisibility();
        return arg;
    }

    protected boolean isStringVector(@SuppressWarnings("unused") RAbstractContainer arg, RAbstractVector className) {
        return className.getElementClass() == RString.class;
    }

}
