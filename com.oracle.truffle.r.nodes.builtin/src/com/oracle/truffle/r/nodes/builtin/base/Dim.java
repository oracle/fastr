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
import com.oracle.truffle.api.nodes.Node.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "dim", kind = PRIMITIVE, parameterNames = {"x"})
@SuppressWarnings("unused")
public abstract class Dim extends RBuiltinNode {

    @Child ShortRowNames shortRowNames;
    @Child private DispatchedCallNode dcn;

    private int dataFrameRowNames(VirtualFrame frame, RDataFrame operand) {
        if (shortRowNames == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            shortRowNames = insert(ShortRowNamesFactory.create(new RNode[2], getBuiltin(), getSuppliedArgsNames()));
        }
        return (int) shortRowNames.executeObject(frame, operand, 2);
    }

    @Specialization
    protected RNull dim(RNull vector) {
        controlVisibility();
        return RNull.instance;
    }

    @Specialization
    protected RNull dim(int vector) {
        controlVisibility();
        return RNull.instance;
    }

    @Specialization
    protected RNull dim(double vector) {
        controlVisibility();
        return RNull.instance;
    }

    @Specialization
    protected RNull dim(byte vector) {
        controlVisibility();
        return RNull.instance;
    }

    @Specialization(guards = {"!isObject", "!hasDimensions"})
    protected RNull dim(RAbstractContainer container) {
        controlVisibility();
        return RNull.instance;
    }

    @Specialization(guards = {"!isObject", "hasDimensions"})
    protected Object dimWithDimensions(RAbstractContainer container) {
        controlVisibility();
        return RDataFactory.createIntVector(container.getDimensions(), RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization(guards = "isObject")
    protected Object dimObject(VirtualFrame frame, RAbstractContainer container) {
        controlVisibility();
        if (dcn == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            dcn = insert(DispatchedCallNode.create("dim", RRuntime.USE_METHOD, getSuppliedArgsNames()));
        }
        try {
            return dcn.executeInternal(frame, container.getClassHierarchy(), new Object[]{container});
        } catch (RError e) {
            return hasDimensions(container) ? dimWithDimensions(container) : RNull.instance;
        }

    }

    public static boolean hasDimensions(RAbstractContainer container) {
        return container.hasDimensions();
    }

    protected boolean isObject(RAbstractContainer container) {
        return container.isObject();
    }

}
