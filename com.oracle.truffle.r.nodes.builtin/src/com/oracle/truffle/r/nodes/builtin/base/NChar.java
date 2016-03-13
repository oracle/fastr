/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

// TODO interpret "type" and "allowNA" arguments
@RBuiltin(name = "nchar", kind = INTERNAL, parameterNames = {"x", "type", "allowNA", "keepNA"})
public abstract class NChar extends RBuiltinNode {

    @Child private CastStringNode convertString;

    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.toLogical(2).toLogical(3);
    }

    private String coerceContent(Object content) {
        if (convertString == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            convertString = insert(CastStringNodeGen.create(false, false, false, false));
        }
        try {
            return (String) convertString.executeString(content);
        } catch (ConversionFailedException e) {
            throw RError.error(this, RError.Message.TYPE_EXPECTED, RType.Character.getName());
        }
    }

    @SuppressWarnings("unused")
    @Specialization
    protected RIntVector nchar(RNull value, String type, byte allowNA, byte keepNA) {
        controlVisibility();
        return RDataFactory.createEmptyIntVector();
    }

    @SuppressWarnings("unused")
    @Specialization
    protected int nchar(int value, String type, byte allowNA, byte keepNA) {
        controlVisibility();
        return coerceContent(value).length();
    }

    @SuppressWarnings("unused")
    @Specialization
    protected int nchar(double value, String type, byte allowNA, byte keepNA) {
        controlVisibility();
        return coerceContent(value).length();
    }

    @SuppressWarnings("unused")
    @Specialization
    protected int nchar(byte value, String type, byte allowNA, byte keepNA) {
        controlVisibility();
        return coerceContent(value).length();
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "vector.getLength() == 0")
    protected RIntVector ncharL0(RAbstractStringVector vector, String type, byte allowNA, byte keepNA) {
        controlVisibility();
        return RDataFactory.createEmptyIntVector();
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "vector.getLength() == 1")
    protected int ncharL1(RAbstractStringVector vector, String type, byte allowNA, byte keepNA) {
        controlVisibility();
        return vector.getDataAt(0).length();
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "vector.getLength() > 1")
    protected RIntVector nchar(RAbstractStringVector vector, String type, byte allowNA, byte keepNA) {
        controlVisibility();
        int len = vector.getLength();
        int[] result = new int[len];
        for (int i = 0; i < len; i++) {
            result[i] = vector.getDataAt(i).length();
        }
        return RDataFactory.createIntVector(result, vector.isComplete(), vector.getNames(attrProfiles));
    }

    @SuppressWarnings("unused")
    @Fallback
    protected RIntVector nchar(Object obj, Object type, Object allowNA, Object keepNA) {
        controlVisibility();
        if (obj instanceof RFactor) {
            throw RError.error(this, RError.Message.REQUIRES_CHAR_VECTOR, "nchar");
        }
        if (obj instanceof RAbstractVector) {
            RAbstractVector vector = (RAbstractVector) obj;
            int len = vector.getLength();
            int[] result = new int[len];
            for (int i = 0; i < len; i++) {
                result[i] = coerceContent(vector.getDataAtAsObject(i)).length();
            }
            return RDataFactory.createIntVector(result, vector.isComplete(), vector.getNames(attrProfiles));
        } else {
            throw RError.error(this, RError.Message.CANNOT_COERCE, RRuntime.classToString(obj.getClass(), false), "character");
        }
    }

}
