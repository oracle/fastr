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
import com.oracle.truffle.r.nodes.function.S3FunctionLookupNode.NoGenericMethodException;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "dim", kind = PRIMITIVE, parameterNames = {"x"})
@SuppressWarnings("unused")
public abstract class Dim extends RBuiltinNode {

    private static final String NAME = "dim";

    @Child private ShortRowNames shortRowNames;
    @Child private UseMethodInternalNode dcn;

    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

    private int dataFrameRowNames(VirtualFrame frame, RDataFrame operand) {
        if (shortRowNames == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            shortRowNames = insert(ShortRowNamesNodeGen.create(new RNode[2], null, null));
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

    @Specialization(guards = {"!isObject(frame, container)", "!hasDimensions(container)"})
    protected RNull dim(VirtualFrame frame, RAbstractContainer container) {
        controlVisibility();
        return RNull.instance;
    }

    @Specialization(guards = {"!isObject(frame, container)", "hasDimensions(container)"})
    protected Object dimWithDimensions(VirtualFrame frame, RAbstractContainer container) {
        controlVisibility();
        return RDataFactory.createIntVector(container.getDimensions(), RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization(guards = "isObject(frame, container)")
    protected Object dimObject(VirtualFrame frame, RAbstractContainer container) {
        controlVisibility();
        if (dcn == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            dcn = insert(new UseMethodInternalNode(NAME, ArgumentsSignature.get(""), true));
        }
        try {
            return dcn.execute(frame, container, new Object[]{container});
        } catch (S3FunctionLookupNode.NoGenericMethodException e) {
            return hasDimensions(container) ? dimWithDimensions(frame, container) : RNull.instance;
        }

    }

    public static boolean hasDimensions(RAbstractContainer container) {
        return container.hasDimensions();
    }

    @SuppressFBWarnings(value = "ES_COMPARING_STRINGS_WITH_EQ", justification = "generic name is interned in the interpreted code for faster comparison")
    protected boolean isObject(VirtualFrame frame, RAbstractContainer container) {
        return container.isObject(attrProfiles) && !(RArguments.getS3Args(frame) != null && RArguments.getS3Args(frame).generic == NAME);
    }

}
