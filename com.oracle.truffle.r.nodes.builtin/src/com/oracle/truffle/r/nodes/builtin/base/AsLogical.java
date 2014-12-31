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
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "as.logical", kind = PRIMITIVE, parameterNames = {"x", "..."})
@SuppressWarnings("unused")
public abstract class AsLogical extends RBuiltinNode {

    @Child private CastLogicalNode castLogicalNode;

    public abstract RLogicalVector executeRLogicalVector(VirtualFrame frame, Object o) throws UnexpectedResultException;

    private byte castLogical(VirtualFrame frame, Object o) {
        if (castLogicalNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castLogicalNode = insert(CastLogicalNodeGen.create(null, false, false, false));
        }
        return (byte) castLogicalNode.executeByte(frame, o);
    }

    private RLogicalVector castLogicalVector(VirtualFrame frame, Object o) {
        if (castLogicalNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castLogicalNode = insert(CastLogicalNodeGen.create(null, false, false, false));
        }
        return (RLogicalVector) castLogicalNode.executeLogical(frame, o);
    }

    @Specialization
    protected byte asLogical(byte value) {
        controlVisibility();
        return value;
    }

    @Specialization
    protected byte asLogical(VirtualFrame frame, int value) {
        controlVisibility();
        return castLogical(frame, value);
    }

    @Specialization
    protected byte asLogical(VirtualFrame frame, double value) {
        controlVisibility();
        return castLogical(frame, value);
    }

    @Specialization
    protected byte asLogical(VirtualFrame frame, RComplex value) {
        controlVisibility();
        return castLogical(frame, value);
    }

    @Specialization
    protected byte asLogical(VirtualFrame frame, String value) {
        controlVisibility();
        return castLogical(frame, value);
    }

    @Specialization
    protected RLogicalVector asLogical(RNull vector) {
        controlVisibility();
        return RDataFactory.createLogicalVector(0);
    }

    @Specialization
    protected RLogicalVector asLogical(RLogicalVector vector) {
        controlVisibility();
        return RDataFactory.createLogicalVector(vector.getDataCopy(), vector.isComplete());
    }

    @Specialization
    protected RLogicalVector asLogical(VirtualFrame frame, RAbstractContainer container) {
        controlVisibility();
        return castLogicalVector(frame, container);
    }
}
