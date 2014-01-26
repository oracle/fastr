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

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin("as.vector")
public abstract class AsVector extends RBuiltinNode {

    private static final String[] PARAMETER_NAMES = new String[]{"x", "mode"};

    // @Child protected AsComplex asComplex;
    // @Child protected AsLogical asLogical;

    @Override
    public Object[] getParameterNames() {
        return PARAMETER_NAMES;
    }

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RRuntime.TYPE_ANY)};
    }

    @Specialization(order = 10)
    public Object asVector(RNull x, @SuppressWarnings("unused") RMissing mode) {
        return x;
    }

    @Specialization(order = 20, guards = "modeIsAnyOrMatches")
    public RAbstractVector asVector(RAbstractVector x, @SuppressWarnings("unused") String mode) {
        return x.copyWithNewDimensions(null);
    }

    // FIXME comment these in (and extend) once polymorphic nodes are supported

    // @Specialization(order = 40, guards = "castToLogical")
    // public RLogicalVector asVectorLogical(VirtualFrame frame, RAbstractVector x,
    // @SuppressWarnings("unused") String mode) {
    // if (asLogical == null) {
    // CompilerDirectives.transferToInterpreter();
    // asLogical = adoptChild(AsLogicalFactory.create(new RNode[1], getContext(), getBuiltin()));
    // }
    // try {
    // return asLogical.executeRLogicalVector(frame, x).copyWithNewDimensions(null);
    // } catch (UnexpectedResultException ure) {
    // throw new IllegalStateException(ure);
    // }
    // }

    // @Specialization(order = 60, guards = "castToComplex")
    // public RComplexVector asVectorComplex(VirtualFrame frame, RAbstractVector x,
    // @SuppressWarnings("unused") String mode) {
    // if (asComplex == null) {
    // CompilerDirectives.transferToInterpreter();
    // asComplex = adoptChild(AsComplexFactory.create(new RNode[1], getContext(), getBuiltin()));
    // }
    // try {
    // return asComplex.executeRComplexVector(frame, x).copyWithNewDimensions(null);
    // } catch (UnexpectedResultException ure) {
    // throw new IllegalStateException(ure);
    // }
    // }

    // protected boolean castToLogical(RAbstractVector x, String mode) {
    // return x.getElementClass() != RLogical.class && RRuntime.TYPE_LOGICAL.equals(mode);
    // }

    // protected boolean castToComplex(RAbstractVector x, String mode) {
    // return x.getElementClass() != RComplex.class && RRuntime.TYPE_COMPLEX.equals(mode);
    // }

    protected boolean modeIsAnyOrMatches(RAbstractVector x, String mode) {
        return RRuntime.TYPE_ANY.equals(mode) || RRuntime.classToString(x.getElementClass()).equals(mode) || x.getElementClass() == RDouble.class && RRuntime.TYPE_DOUBLE.equals(mode);
    }

}
