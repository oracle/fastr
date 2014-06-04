/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.ffi.*;

/**
 * For now, just some special case functions that are built in to the implementation.
 *
 */
@RBuiltin(name = ".Fortran", kind = RBuiltinKind.PRIMITIVE, isCombine = true)
@NodeField(name = "argNames", type = String[].class)
public abstract class Fortran extends RBuiltinNode {
    private static final Object[] PARAMETER_NAMES = new Object[]{".NAME", "...", "NAOK", "DUP", "PACKAGE", "ENCODING"};

    public abstract String[] getArgNames();

    @Override
    public Object[] getParameterNames() {
        return PARAMETER_NAMES;
    }

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(EMPTY_OBJECT_ARRAY), ConstantNode.create(RRuntime.LOGICAL_FALSE), ConstantNode.create(RRuntime.LOGICAL_FALSE),
                        ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance)};
    }

    private static final RStringVector DQRDC2_NAMES = RDataFactory.createStringVector(new String[]{"qr", null, null, null, null, "rank", "qraux", "pivot", null}, RDataFactory.COMPLETE_VECTOR);

    @SuppressWarnings("unused")
    @Specialization(order = 0, guards = "dqrdc2")
    public RList fortranDqrdc2(String f, Object[] args, byte naok, byte dup, RMissing rPackage, RMissing encoding) {
        controlVisibility();
        try {
            RDoubleVector xVec = (RDoubleVector) args[0];
            int ldx = (int) args[1];
            int n = (int) args[2];
            int p = (int) args[3];
            double tol = (double) args[4];
            RIntVector rankVec = (RIntVector) args[5];
            RDoubleVector qrauxVec = (RDoubleVector) args[6];
            RIntVector pivotVec = (RIntVector) args[7];
            RDoubleVector workVec = (RDoubleVector) args[8];
            double[] x = xVec.getDataCopy();
            int[] rank = rankVec.getDataCopy();
            double[] qraux = qrauxVec.getDataCopy();
            int[] pivot = pivotVec.getDataCopy();
            RFFIFactory.getRFFI().getLinpackRFFI().dqrdc2(x, ldx, n, p, tol, rank, qraux, pivot, workVec.getDataCopy());
            // @formatter:off
            Object[] data = new Object[]{
                            RDataFactory.createDoubleVector(x, RDataFactory.COMPLETE_VECTOR, xVec.getDimensions()),
                            args[1], args[2], args[3], args[4],
                            RDataFactory.createIntVector(rank, RDataFactory.COMPLETE_VECTOR),
                            RDataFactory.createDoubleVector(qraux, RDataFactory.COMPLETE_VECTOR),
                            RDataFactory.createIntVector(pivot, RDataFactory.COMPLETE_VECTOR),
                            args[8]
                            };
            return RDataFactory.createList(data, DQRDC2_NAMES);
        } catch (ClassCastException | ArrayIndexOutOfBoundsException ex) {
            CompilerDirectives.transferToInterpreter();
            throw RError.getGenericError(getEncapsulatingSourceSection(), "incorrect arguments to dqrdc2");
        }
    }

    public boolean dqrdc2(String f) {
        return f.equals("dqrdc2");
    }

    private static final RStringVector DQRCF_NAMES = RDataFactory.createStringVector(new String[]{null, null, null, null, null, null, "coef", "info"}, RDataFactory.COMPLETE_VECTOR);

    @SuppressWarnings("unused")
    @Specialization(order = 1, guards = "dqrcf")
    public RList fortranDqrcf(String f, Object[] args, byte naok, byte dup, RMissing rPackage, RMissing encoding) {
        controlVisibility();
        try {
            RDoubleVector xVec = (RDoubleVector) args[0];
            int n = (int) args[1];
            RIntVector k = (RIntVector) args[2];
            RDoubleVector qrauxVec = (RDoubleVector) args[3];
            RDoubleVector yVec = (RDoubleVector) args[4];
            int ny = (int) args[5];
            RDoubleVector bVec = (RDoubleVector) args[6];
            RIntVector infoVec = (RIntVector) args[7];
            double[] x = xVec.getDataCopy();
            double[] qraux = qrauxVec.getDataCopy();
            double[] y = yVec.getDataCopy();
            double[] b = bVec.getDataCopy();
            int[] info = infoVec.getDataCopy();
            RFFIFactory.getRFFI().getLinpackRFFI().dqrcf(x, n, k.getDataAt(0), qraux, y, ny, b, info);
            // @formatter:off
            RDoubleVector coef = RDataFactory.createDoubleVector(b, RDataFactory.COMPLETE_VECTOR);
            coef.copyAttributesFrom(bVec);
            Object[] data = new Object[]{
                            RDataFactory.createDoubleVector(x, RDataFactory.COMPLETE_VECTOR),
                            args[1],
                            k.copy(),
                            RDataFactory.createDoubleVector(qraux, RDataFactory.COMPLETE_VECTOR),
                            RDataFactory.createDoubleVector(y, RDataFactory.COMPLETE_VECTOR),
                            args[5],
                            coef,
                            RDataFactory.createIntVector(info, RDataFactory.COMPLETE_VECTOR),
            };
            return RDataFactory.createList(data, DQRCF_NAMES);

        } catch (ClassCastException | ArrayIndexOutOfBoundsException ex) {
            CompilerDirectives.transferToInterpreter();
            throw RError.getGenericError(getEncapsulatingSourceSection(), "incorrect arguments to dqrcf");
        }
    }

    public boolean dqrcf(String f) {
        return f.equals("dqrcf");
    }


}
