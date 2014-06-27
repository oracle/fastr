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
import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

@RBuiltin(name = "paste", kind = INTERNAL)
public abstract class Paste extends RBuiltinNode {

    public abstract Object executeList(VirtualFrame frame, RList value, String sep, Object collapse);

    @Child CastStringNode castCharacterNode;

    private String castCharacter(VirtualFrame frame, Object o) {
        if (castCharacterNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castCharacterNode = insert(CastStringNodeFactory.create(null, false, true, false, false));
        }
        return (String) castCharacterNode.executeString(frame, o);
    }

    private RStringVector castCharacterVector(VirtualFrame frame, Object o) {
        if (castCharacterNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castCharacterNode = insert(CastStringNodeFactory.create(null, false, true, false, false));
        }
        Object ret = castCharacterNode.executeString(frame, o);
        if (ret instanceof String) {
            return RDataFactory.createStringVector((String) ret);
        } else {
            return (RStringVector) ret;
        }
    }

    @Specialization
    public RStringVector pasteList(VirtualFrame frame, RList values, String sep, Object collapse) {
        controlVisibility();
        if (isEmptyOrNull(values)) {
            return RDataFactory.createEmptyStringVector();
        }
        int length = values.getLength();
        Object[] converted = new Object[length];
        int maxLength = 1;
        for (int i = 0; i < length; i++) {
            Object element = values.getDataAt(i);
            if (element instanceof RVector || element instanceof RSequence) {
                converted[i] = castCharacterVector(frame, element);
                int len = ((RStringVector) converted[i]).getLength();
                if (len > maxLength) {
                    maxLength = len;
                }
            } else {
                converted[i] = castCharacter(frame, element);
            }
        }

        String[] result = new String[maxLength];
        for (int i = 0; i < maxLength; i++) {
            StringBuilder builder = new StringBuilder();
            for (int j = 0; j < length; j++) {
                if (j != 0) {
                    builder.append(sep);
                }
                builder.append(getConvertedElement(converted[j], i));
            }
            result[i] = builderToString(builder);
        }

        if (collapse != RNull.instance) {
            String collapseString = RRuntime.toString(collapse);
            return buildString(result, collapseString);
        } else {
            return RDataFactory.createStringVector(result, RDataFactory.COMPLETE_VECTOR);
        }
    }

    public boolean isEmptyOrNull(RList values) {
        return values.getLength() == 0 || (values.getLength() == 1 && values.getDataAt(0) == RNull.instance);
    }

    private static RStringVector buildString(String[] value, String collapseString) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length; i++) {
            if (i > 0) {
                sb.append(collapseString);
            }
            sb.append(value[i]);
        }
        return RDataFactory.createStringVector(new String[]{buildStringToString(sb)}, RDataFactory.COMPLETE_VECTOR);
    }

    @SlowPath
    private static String buildStringToString(StringBuilder sb) {
        return sb.toString();
    }

    @SlowPath
    private static String builderToString(StringBuilder builder) {
        return buildStringToString(builder);
    }

    private static String getConvertedElement(final Object converted, int index) {
        if (converted instanceof RStringVector) {
            RStringVector sv = (RStringVector) converted;
            if (sv.getLength() == 0) {
                return "";
            }
            return sv.getDataAt(index % sv.getLength());
        } else if (converted instanceof String) {
            return (String) converted;
        }
        assert false;
        return "";
    }

}
