/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.unary.CastStringNode;
import com.oracle.truffle.r.nodes.unary.CastStringNodeGen;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

@RBuiltin(name = "nzchar", kind = PRIMITIVE, parameterNames = {"x"}, behavior = PURE)
public abstract class NZChar extends RBuiltinNode {
    @Child private CastStringNode convertString;

    private String coerceContent(Object content) {
        if (convertString == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            convertString = insert(CastStringNodeGen.create(false, false, false));
        }
        return (String) convertString.execute(content);
    }

    private static byte isNonZeroLength(String s) {
        return s.length() > 0 ? RRuntime.LOGICAL_TRUE : RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    protected RLogicalVector rev(@SuppressWarnings("unused") RNull value) {
        return RDataFactory.createEmptyLogicalVector();
    }

    @Specialization
    protected byte rev(int value) {
        return isNonZeroLength(coerceContent(value));
    }

    @Specialization
    protected byte rev(double value) {
        return isNonZeroLength(coerceContent(value));
    }

    @Specialization
    protected byte rev(byte value) {
        return isNonZeroLength(coerceContent(value));
    }

    @Specialization
    protected RLogicalVector rev(RStringVector vector) {
        int len = vector.getLength();
        byte[] result = new byte[len];
        for (int i = 0; i < len; i++) {
            result[i] = isNonZeroLength(vector.getDataAt(i));
        }
        return RDataFactory.createLogicalVector(result, RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization
    protected RLogicalVector rev(RAbstractVector vector) {
        int len = vector.getLength();
        byte[] result = new byte[len];
        for (int i = 0; i < len; i++) {
            result[i] = isNonZeroLength(coerceContent(vector.getDataAtAsObject(i)));
        }
        return RDataFactory.createLogicalVector(result, RDataFactory.COMPLETE_VECTOR);
    }
}
