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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.builtin.RBuiltin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

@RBuiltin(value = "paste", lastParameterKind = LastParameterKind.VAR_ARGS_SPECIALIZE)
public abstract class Paste extends RBuiltinNode {

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

    private static final Object[] PARAMETER_NAMES = new Object[]{"...", "sep", "collapse"};

    @Override
    public Object[] getParameterNames() {
        return PARAMETER_NAMES;
    }

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{null, ConstantNode.create(" "), ConstantNode.create(RNull.instance)};
    }

    @Specialization
    @SuppressWarnings("unused")
    public RStringVector paste(RMissing value, Object sep, Object collapse) {
        controlVisibility();
        return RDataFactory.createEmptyStringVector();
    }

    @Specialization
    @SuppressWarnings("unused")
    public RStringVector paste(RNull value, Object sep, Object collapse) {
        controlVisibility();
        return RDataFactory.createEmptyStringVector();
    }

    @SuppressWarnings("unused")
    @Specialization
    @SlowPath
    public String paste(int value, Object sep, Object collapse) {
        controlVisibility();
        return String.valueOf(value);
    }

    @SuppressWarnings("unused")
    @Specialization
    @SlowPath
    public String paste(double value, Object sep, Object collapse) {
        controlVisibility();
        return String.valueOf(value);
    }

    @SuppressWarnings("unused")
    @Specialization
    public String paste(byte value, Object sep, Object collapse) {
        controlVisibility();
        return RRuntime.logicalToString(value);
    }

    @SuppressWarnings("unused")
    @Specialization
    public String paste(String value, Object sep, Object collapse) {
        controlVisibility();
        return value;
    }

    @SuppressWarnings("unused")
    @Specialization
    public String paste(RComplex value, Object sep, Object collapse) {
        controlVisibility();
        return RRuntime.toString(value);
    }

    @SuppressWarnings("unused")
    @Specialization
    public RStringVector paste(RStringVector vector, Object sep, Object collapse) {
        controlVisibility();
        return checkCollapse(vector, collapse);
    }

    @SuppressWarnings("unused")
    @Specialization
    public RStringVector paste(VirtualFrame frame, RIntVector vector, Object sep, Object collapse) {
        controlVisibility();
        return checkCollapse(castCharacterVector(frame, vector), collapse);
    }

    @SuppressWarnings("unused")
    @Specialization
    public RStringVector paste(VirtualFrame frame, RDoubleVector vector, Object sep, Object collapse) {
        controlVisibility();
        return checkCollapse(castCharacterVector(frame, vector), collapse);
    }

    @SuppressWarnings("unused")
    @Specialization
    public RStringVector paste(VirtualFrame frame, RIntSequence sequence, Object sep, Object collapse) {
        controlVisibility();
        return checkCollapse(castCharacterVector(frame, sequence), collapse);
    }

    @SuppressWarnings("unused")
    @Specialization
    public RStringVector paste(VirtualFrame frame, RDoubleSequence sequence, Object sep, Object collapse) {
        controlVisibility();
        return checkCollapse(castCharacterVector(frame, sequence), collapse);
    }

    @SuppressWarnings("unused")
    @Specialization
    public RStringVector paste(VirtualFrame frame, RLogicalVector vector, Object sep, Object collapse) {
        controlVisibility();
        return checkCollapse(castCharacterVector(frame, vector), collapse);
    }

    @Specialization
    public RStringVector paste(VirtualFrame frame, Object[] args, Object sep, Object collapse) {
        controlVisibility();
        Object[] converted = new Object[args.length];
        int maxLength = 1;
        for (int i = 0; i < args.length; i++) {
            Object element = args[i];
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
            for (int j = 0; j < args.length; j++) {
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

    private static RStringVector checkCollapse(RStringVector result, Object collapse) {
        if (collapse != RNull.instance) {
            String collapseString = RRuntime.toString(collapse);
            return buildString(result, collapseString);
        } else {
            return result;
        }
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

    private static RStringVector buildString(RStringVector vector, String collapseString) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < vector.getLength(); i++) {
            if (i > 0) {
                sb.append(collapseString);
            }
            sb.append(vector.getDataAt(i));
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
