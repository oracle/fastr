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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "as.raw", kind = PRIMITIVE, parameterNames = {"x"})
@SuppressWarnings("unused")
public abstract class AsRaw extends RBuiltinNode {

    @Child private CastIntegerNode castInteger;

    @Child private CastRawNode castRawNode;

    private void initCast() {
        if (castRawNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castRawNode = insert(CastRawNodeGen.create(null, false, false, false));
        }
    }

    private RRaw castRaw(VirtualFrame frame, int o) {
        initCast();
        return (RRaw) castRawNode.executeRaw(frame, o);
    }

    private RRaw castRaw(VirtualFrame frame, double o) {
        initCast();
        return (RRaw) castRawNode.executeRaw(frame, o);
    }

    private RRaw castRaw(VirtualFrame frame, byte o) {
        initCast();
        return (RRaw) castRawNode.executeRaw(frame, o);
    }

    private RRaw castRaw(VirtualFrame frame, Object o) {
        initCast();
        return (RRaw) castRawNode.executeRaw(frame, o);
    }

    private RRawVector castRawVector(VirtualFrame frame, Object o) {
        initCast();
        return (RRawVector) castRawNode.executeRaw(frame, o);
    }

    public abstract RRaw executeRaw(VirtualFrame frame, Object o);

    @Specialization
    protected RRawVector asRaw(RNull vector) {
        controlVisibility();
        return RDataFactory.createRawVector(0);
    }

    @Specialization
    protected RRaw asRaw(VirtualFrame frame, byte logical) {
        controlVisibility();
        return castRaw(frame, logical);
    }

    @Specialization
    protected RRaw asRaw(VirtualFrame frame, int value) {
        controlVisibility();
        return castRaw(frame, value);
    }

    @Specialization
    protected RRaw asRaw(VirtualFrame frame, double value) {
        controlVisibility();
        return castRaw(frame, value);
    }

    @Specialization
    protected RRaw asRaw(VirtualFrame frame, RComplex value) {
        controlVisibility();
        return castRaw(frame, value);
    }

    @Specialization
    protected RRaw asRaw(VirtualFrame frame, String value) {
        controlVisibility();
        return castRaw(frame, value);
    }

    @Specialization
    protected RRaw asRaw(RRaw value) {
        controlVisibility();
        return value;
    }

    @Specialization(guards = {"!isListVector", "!isRawVector"})
    protected RRawVector asRaw(VirtualFrame frame, RAbstractVector vector) {
        controlVisibility();
        return castRawVector(frame, vector);
    }

    @Specialization
    protected RRawVector asRaw(RRawVector value) {
        controlVisibility();
        return RDataFactory.createRawVector(value.getDataCopy());
    }

    @Specialization
    protected RRawVector asRaw(VirtualFrame frame, RList value) {
        controlVisibility();
        int length = value.getLength();
        RRawVector result = RDataFactory.createRawVector(length);
        for (int i = 0; i < length; ++i) {
            result.updateDataAt(i, castRaw(frame, value.getDataAt(i)));
        }
        return result;
    }

    protected boolean isListVector(RAbstractVector vector) {
        return vector.getElementClass() == Object.class;
    }

    protected boolean isRawVector(RAbstractVector vector) {
        return vector.getElementClass() == RRaw.class;
    }
}
