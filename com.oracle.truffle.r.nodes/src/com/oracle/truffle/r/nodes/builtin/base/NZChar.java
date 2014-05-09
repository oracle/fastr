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
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.nodes.unary.ConvertNode.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin("nzchar")
public abstract class NZChar extends RBuiltinNode {
    @Child CastStringNode convertString;

    private String coerceContent(VirtualFrame frame, Object content) {
        if (convertString == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            convertString = insert(CastStringNodeFactory.create(null, false, true, false, false));
        }
        try {
            return (String) convertString.executeCast(frame, content);
        } catch (ConversionFailedException e) {
            CompilerDirectives.transferToInterpreter();
            throw RError.getCharacterExpected(getEncapsulatingSourceSection());
        }
    }

    private static byte isNonZeroLength(String s) {
        return s.length() > 0 ? RRuntime.LOGICAL_TRUE : RRuntime.LOGICAL_FALSE;
    }

    @SuppressWarnings("unused")
    @Specialization
    public RLogicalVector rev(VirtualFrame frame, RNull value) {
        controlVisibility();
        return RDataFactory.createEmptyLogicalVector();
    }

    @Specialization
    public byte rev(VirtualFrame frame, int value) {
        controlVisibility();
        return isNonZeroLength(coerceContent(frame, value));
    }

    @Specialization
    public byte rev(VirtualFrame frame, double value) {
        controlVisibility();
        return isNonZeroLength(coerceContent(frame, value));
    }

    @Specialization
    public byte rev(VirtualFrame frame, byte value) {
        controlVisibility();
        return isNonZeroLength(coerceContent(frame, value));
    }

    @Specialization
    public RLogicalVector rev(RStringVector vector) {
        controlVisibility();
        int len = vector.getLength();
        byte[] result = new byte[len];
        for (int i = 0; i < len; i++) {
            result[i] = isNonZeroLength(vector.getDataAt(i));
        }
        return RDataFactory.createLogicalVector(result, RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization
    public RLogicalVector rev(VirtualFrame frame, RAbstractVector vector) {
        controlVisibility();
        int len = vector.getLength();
        byte[] result = new byte[len];
        for (int i = 0; i < len; i++) {
            result[i] = isNonZeroLength(coerceContent(frame, vector.getDataAtAsObject(i)));
        }
        return RDataFactory.createLogicalVector(result, RDataFactory.COMPLETE_VECTOR);
    }

}
