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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.nullValue;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.PrimitiveValueProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.unary.CastStringNode;
import com.oracle.truffle.r.nodes.unary.CastStringNodeGen;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;

@RBuiltin(name = "paste", kind = INTERNAL, parameterNames = {"", "sep", "collapse"}, behavior = PURE)
public abstract class Paste extends RBuiltinNode {

    private static final String[] ONE_EMPTY_STRING = new String[]{""};

    public abstract Object executeList(RList value, String sep, Object collapse);

    /**
     * {@code paste} is specified to convert its arguments using {@code as.character}.
     */
    @Child private AsCharacter asCharacterNode;
    @Child private CastStringNode castCharacterNode;

    private final ValueProfile lengthProfile = PrimitiveValueProfile.createEqualityProfile();
    private final ConditionProfile reusedResultProfile = ConditionProfile.createBinaryProfile();
    private final BranchProfile nonNullElementsProfile = BranchProfile.create();
    private final BranchProfile onlyNullElementsProfile = BranchProfile.create();

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.arg(0).mustBe(RAbstractListVector.class);
        casts.arg("sep").mustBe(nullValue().not()).asStringVector().findFirst(Message.INVALID_SEPARATOR);
        casts.arg("collapse").mustBe(Predef.stringValue().or(Predef.nullValue())).mapIf(Predef.stringValue(), Predef.findFirst().stringElement());
    }

    /**
     * FIXME The exact semantics needs checking regarding the use of {@code as.character}. Currently
     * there are problem using it here, so we retain the previous implementation that just uses
     * {@link CastStringNode}.
     */
    private RStringVector castCharacterVector(Object o) {
        if (castCharacterNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castCharacterNode = insert(CastStringNodeGen.create(false, false, false));
        }
        Object ret = castCharacterNode.executeString(o);
        if (ret instanceof String) {
            return RDataFactory.createStringVector((String) ret);
        } else if (ret == RNull.instance) {
            return RDataFactory.createEmptyStringVector();
        } else {
            return (RStringVector) ((RStringVector) ret).copyDropAttributes();
        }
    }

    @Specialization
    protected RStringVector pasteList(RList values, String sep, @SuppressWarnings("unused") RNull collapse) {
        int length = lengthProfile.profile(values.getLength());
        if (hasNonNullElements(values, length)) {
            String[] result = pasteListElements(values, sep, length);
            return RDataFactory.createStringVector(result, RDataFactory.COMPLETE_VECTOR);
        } else {
            return RDataFactory.createEmptyStringVector();
        }
    }

    @Specialization
    protected String pasteList(RList values, String sep, String collapse) {
        int length = lengthProfile.profile(values.getLength());
        if (hasNonNullElements(values, length)) {
            String[] result = pasteListElements(values, sep, length);
            return collapseString(result, collapse);
        } else {
            return "";
        }
    }

    private boolean hasNonNullElements(RList values, int length) {
        for (int i = 0; i < length; i++) {
            if (values.getDataAt(i) != RNull.instance) {
                nonNullElementsProfile.enter();
                return true;
            }
        }
        onlyNullElementsProfile.enter();
        return false;
    }

    private String[] pasteListElements(RList values, String sep, int length) {
        String[][] converted = new String[length][];
        int maxLength = 1;
        for (int i = 0; i < length; i++) {
            Object element = values.getDataAt(i);
            String[] array = castCharacterVector(element).getDataWithoutCopying();
            maxLength = Math.max(maxLength, array.length);
            converted[i] = array.length == 0 ? ONE_EMPTY_STRING : array;
        }
        if (length == 1) {
            return converted[0];
        } else {
            return prepareResult(sep, length, converted, maxLength);
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

    private static String concatStrings(String[][] converted, int index, int length, String sep) {
        // pre compute the string length for the StringBuilder
        int stringLength = -sep.length();
        for (int j = 0; j < length; j++) {
            String element = converted[j][index % converted[j].length];
            stringLength += element.length() + sep.length();
        }
        char[] chars = new char[stringLength];
        int pos = 0;
        for (int j = 0; j < length; j++) {
            if (j != 0) {
                sep.getChars(0, sep.length(), chars, pos);
                pos += sep.length();
            }
            String element = converted[j][index % converted[j].length];
            element.getChars(0, element.length(), chars, pos);
            pos += element.length();
        }
        assert pos == stringLength;
        return new String(chars);
    }

    private static String collapseString(String[] value, String collapseString) {
        int stringLength = -collapseString.length();
        for (int i = 0; i < value.length; i++) {
            stringLength += collapseString.length() + value[i].length();
        }
        char[] chars = new char[stringLength];
        int pos = 0;
        for (int i = 0; i < value.length; i++) {
            if (i > 0) {
                collapseString.getChars(0, collapseString.length(), chars, pos);
                pos += collapseString.length();
            }
            String element = value[i];
            element.getChars(0, element.length(), chars, pos);
            pos += element.length();
        }
        assert pos == stringLength;
        return new String(chars);
    }
}
