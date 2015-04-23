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
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

@RBuiltin(name = "paste", kind = INTERNAL, parameterNames = {"", "sep", "collapse"})
public abstract class Paste extends RBuiltinNode {

    private static final String[] ONE_EMPTY_STRING = new String[]{""};

    public abstract Object executeList(VirtualFrame frame, RList value, String sep, Object collapse);

    /**
     * {@code paste} is specified to convert its arguments using {@code as.character}.
     */
    @Child private AsCharacter asCharacterNode;
    @Child private CastStringNode castCharacterNode;

    private final ConditionProfile emptyOrNull = ConditionProfile.createBinaryProfile();
    private final ConditionProfile vectorOrSequence = ConditionProfile.createBinaryProfile();
    private final ConditionProfile noCollapse = ConditionProfile.createBinaryProfile();
    private final ConditionProfile reusedResultProfile = ConditionProfile.createBinaryProfile();

    private RStringVector castCharacter(VirtualFrame frame, Object o) {
        if (asCharacterNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            asCharacterNode = insert(AsCharacterNodeGen.create(new RNode[1], null, null));
        }
        Object ret = asCharacterNode.execute(frame, o);
        if (ret instanceof String) {
            return RDataFactory.createStringVector((String) ret);
        } else {
            return (RStringVector) ret;
        }
    }

    /**
     * FIXME The exact semantics needs checking regarding the use of {@code as.character}. Currently
     * there are problem using it here, so we retain the previous implementation that just uses
     * {@link CastStringNode}.
     */
    private RStringVector castCharacterVector(VirtualFrame frame, Object o) {
        if (castCharacterNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castCharacterNode = insert(CastStringNodeGen.create(null, false, true, false, false));
        }
        Object ret = castCharacterNode.executeString(frame, o);
        if (ret instanceof String) {
            return RDataFactory.createStringVector((String) ret);
        } else {
            return (RStringVector) ret;
        }
    }

    @Specialization
    protected RStringVector pasteList(VirtualFrame frame, RList values, String sep, RNull collapse) {
        return pasteList(frame, values, sep, (Object) collapse);
    }

    @Specialization
    protected RStringVector pasteList(VirtualFrame frame, RList values, String sep, Object collapse) {
        controlVisibility();
        if (emptyOrNull.profile(isEmptyOrNull(values))) {
            return RDataFactory.createEmptyStringVector();
        }
        int length = values.getLength();
        String[][] converted = new String[length][];
        int maxLength = 1;
        for (int i = 0; i < length; i++) {
            Object element = values.getDataAt(i);
            String[] array;
            if (vectorOrSequence.profile(element instanceof RVector || element instanceof RSequence)) {
                array = castCharacterVector(frame, element).getDataWithoutCopying();
            } else {
                array = castCharacter(frame, element).getDataWithoutCopying();
            }
            maxLength = Math.max(maxLength, array.length);
            converted[i] = array.length == 0 ? ONE_EMPTY_STRING : array;
        }

        String[] result = prepareResult(sep, length, converted, maxLength);
        if (noCollapse.profile(collapse != RNull.instance)) {
            String collapseString = RRuntime.toString(collapse);
            return buildString(result, collapseString);
        } else {
            return RDataFactory.createStringVector(result, RDataFactory.COMPLETE_VECTOR);
        }
    }

    private String[] prepareResult(String sep, int length, String[][] converted, int maxLength) {
        String[] result = new String[maxLength];
        String lastResult = null;
        for (int i = 0; i < maxLength; i++) {
            if (i > 0) {
                // check if the next string is composed of the same elements
                int j;
                for (j = 0; j < length; j++) {
                    String element = converted[j][i % converted[j].length];
                    String lastElement = converted[j][(i - 1) % converted[j].length];
                    if (element != lastElement) {
                        break;
                    }
                }
                if (reusedResultProfile.profile(j == length)) {
                    result[i] = lastResult;
                    continue;
                }
            }
            result[i] = lastResult = concatStrings(converted, i, length, sep);
        }
        return result;
    }

    @TruffleBoundary
    private static String concatStrings(String[][] converted, int index, int length, String sep) {
        // pre compute the string length for the StringBuilder
        int stringLength = -sep.length();
        for (int j = 0; j < length; j++) {
            String element = converted[j][index % converted[j].length];
            stringLength += element.length() + sep.length();
        }
        StringBuilder builder = new StringBuilder(stringLength);
        for (int j = 0; j < length; j++) {
            if (j != 0) {
                builder.append(sep);
            }
            builder.append(converted[j][index % converted[j].length]);
        }
        assert builder.length() == stringLength;
        return RRuntime.toString(builder);
    }

    public boolean isEmptyOrNull(RList values) {
        return values.getLength() == 0 || (values.getLength() == 1 && values.getDataAt(0) == RNull.instance);
    }

    @TruffleBoundary
    private static RStringVector buildString(String[] value, String collapseString) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length; i++) {
            if (i > 0) {
                sb.append(collapseString);
            }
            sb.append(value[i]);
        }
        return RDataFactory.createStringVector(new String[]{RRuntime.toString(sb)}, RDataFactory.COMPLETE_VECTOR);
    }
}
