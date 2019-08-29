/*
 * Copyright (c) 2019, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.ffi.impl.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.gnur.SEXPTYPE;

import java.util.Arrays;

import static com.oracle.truffle.r.ffi.impl.common.RFFIUtils.unimplemented;

@ReportPolymorphism
@GenerateUncached
public abstract class RfAllocVectorNode extends FFIUpCallNode.Arg2 {
    protected static final int SEXPTYPE_COUNT = SEXPTYPE.values().length + 1;

    public abstract Object execute(int mode, long n);

    public static RfAllocVectorNode create() {
        return RfAllocVectorNodeGen.create();
    }

    protected static SEXPTYPE getType(int mode) {
        return SEXPTYPE.mapInt(mode);
    }

    @Specialization(guards = "mode == type.code", limit = "SEXPTYPE_COUNT")
    public Object doIt(@SuppressWarnings("unused") int mode, long n,
                    @Cached(value = "getType(mode)", allowUncached = true) SEXPTYPE type) {
        CompilerAsserts.compilationConstant(type);
        if (n > Integer.MAX_VALUE) {
            CompilerDirectives.transferToInterpreter();
            throw RError.error(RError.SHOW_CALLER, RError.Message.LONG_VECTORS_NOT_SUPPORTED);
            // TODO check long vector
        }
        int ni = (int) n;
        switch (type) {
            case INTSXP:
                return RDataFactory.createIntVector(new int[ni], RDataFactory.COMPLETE_VECTOR);
            case REALSXP:
                return RDataFactory.createDoubleVector(new double[ni], RDataFactory.COMPLETE_VECTOR);
            case LGLSXP:
                return RDataFactory.createLogicalVector(new byte[ni], RDataFactory.COMPLETE_VECTOR);
            case STRSXP:
                // fill list with empty strings
                String[] data = new String[ni];
                Arrays.fill(data, "");
                return RDataFactory.createStringVector(data, RDataFactory.COMPLETE_VECTOR);
            case CPLXSXP:
                return RDataFactory.createComplexVector(new double[2 * ni], RDataFactory.COMPLETE_VECTOR);
            case RAWSXP:
                return RDataFactory.createRawVector(new byte[ni]);
            case VECSXP:
                return RDataFactory.createList(ni);
            case LISTSXP:
            case LANGSXP:
                return RDataFactory.createPairList(ni, type);
            case NILSXP:
                return RNull.instance;
            default:
                throw unimplemented("unexpected SEXPTYPE " + type);
        }
    }
}
