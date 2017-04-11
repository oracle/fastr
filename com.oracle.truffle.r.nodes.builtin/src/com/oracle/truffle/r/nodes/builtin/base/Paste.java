/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.emptyStringVector;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.nullValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.nodes.builtin.casts.fluent.CastNodeBuilder.newCastBuilder;
import static com.oracle.truffle.r.runtime.RError.Message.NON_STRING_ARG_TO_INTERNAL_PASTE;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.PrimitiveValueProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.binary.BoxPrimitiveNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.function.ClassHierarchyNode;
import com.oracle.truffle.r.nodes.function.call.RExplicitBaseEnvCallDispatcher;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

@RBuiltin(name = "paste", kind = INTERNAL, parameterNames = {"", "sep", "collapse"}, behavior = PURE)
public abstract class Paste extends RBuiltinNode.Arg3 {

    private static final String[] ONE_EMPTY_STRING = new String[]{""};

    public abstract Object executeList(VirtualFrame frame, RList value, String sep, Object collapse);

    @Child private ClassHierarchyNode classHierarchyNode;
    @Child private CastNode asCharacterNode;
    @Child private CastNode castAsCharacterResultNode;
    @Child private RExplicitBaseEnvCallDispatcher asCharacterDispatcher;
    @Child private BoxPrimitiveNode boxPrimitiveNode = BoxPrimitiveNode.create();

    private final ValueProfile lengthProfile = PrimitiveValueProfile.createEqualityProfile();
    private final ConditionProfile reusedResultProfile = ConditionProfile.createBinaryProfile();
    private final BranchProfile nonNullElementsProfile = BranchProfile.create();
    private final BranchProfile onlyNullElementsProfile = BranchProfile.create();
    private final ConditionProfile isNotStringProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile hasNoClassProfile = ConditionProfile.createBinaryProfile();

    static {
        Casts casts = new Casts(Paste.class);
        casts.arg(0).mustBe(RAbstractListVector.class);
        casts.arg("sep").asStringVector().findFirst(Message.INVALID_SEPARATOR);
        casts.arg("collapse").allowNull().mustBe(stringValue()).asStringVector().findFirst();
    }

    private RAbstractStringVector castCharacterVector(VirtualFrame frame, Object o) {
        // Note: GnuR does not actually invoke as.character for character values, even if they have
        // class and uses their value directly
        Object result = o;
        if (isNotStringProfile.profile(!(o instanceof String || o instanceof RAbstractStringVector))) {
            result = castNonStringToCharacterVector(frame, result);
        }
        // box String to RAbstractStringVector
        return (RAbstractStringVector) boxPrimitiveNode.execute(result);
    }

    private Object castNonStringToCharacterVector(VirtualFrame frame, Object result) {
        RStringVector classVec = getClassHierarchyNode().execute(result);
        if (hasNoClassProfile.profile(classVec == null || classVec.getLength() == 0)) {
            // coerce non-string result to string, i.e. do what 'as.character' would do
            return getAsCharacterNode().execute(result);
        }
        // invoke the actual 'as.character' function (with its dispatch)
        ensureAsCharacterFuncNodes();
        return castAsCharacterResultNode.execute(asCharacterDispatcher.call(frame, result));
    }

    @Specialization
    protected RStringVector pasteListNullSep(VirtualFrame frame, RAbstractListVector values, String sep, @SuppressWarnings("unused") RNull collapse) {
        int length = lengthProfile.profile(values.getLength());
        if (hasNonNullElements(values, length)) {
            String[] result = pasteListElements(frame, values, sep, length);
            return RDataFactory.createStringVector(result, RDataFactory.COMPLETE_VECTOR);
        } else {
            return RDataFactory.createEmptyStringVector();
        }
    }

    @Specialization
    protected String pasteList(VirtualFrame frame, RAbstractListVector values, String sep, String collapse) {
        int length = lengthProfile.profile(values.getLength());
        if (hasNonNullElements(values, length)) {
            String[] result = pasteListElements(frame, values, sep, length);
            return collapseString(result, collapse);
        } else {
            return "";
        }
    }

    private boolean hasNonNullElements(RAbstractListVector values, int length) {
        for (int i = 0; i < length; i++) {
            if (values.getDataAt(i) != RNull.instance) {
                nonNullElementsProfile.enter();
                return true;
            }
        }
        onlyNullElementsProfile.enter();
        return false;
    }

    private String[] pasteListElements(VirtualFrame frame, RAbstractListVector values, String sep, int length) {
        String[][] converted = new String[length][];
        int maxLength = 1;
        for (int i = 0; i < length; i++) {
            Object element = values.getDataAt(i);
            String[] array = castCharacterVector(frame, element).materialize().getDataWithoutCopying();
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

    private void ensureAsCharacterFuncNodes() {
        if (asCharacterDispatcher == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            asCharacterDispatcher = insert(RExplicitBaseEnvCallDispatcher.create("as.character"));
        }
        if (castAsCharacterResultNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castAsCharacterResultNode = insert(newCastBuilder().mustBe(stringValue(), NON_STRING_ARG_TO_INTERNAL_PASTE).buildCastNode());
        }
    }

    private ClassHierarchyNode getClassHierarchyNode() {
        if (classHierarchyNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            classHierarchyNode = insert(ClassHierarchyNode.create());
        }
        return classHierarchyNode;
    }

    private CastNode getAsCharacterNode() {
        if (asCharacterNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            asCharacterNode = insert(newCastBuilder().returnIf(nullValue(), emptyStringVector()).asStringVector().buildCastNode());
        }
        return asCharacterNode;
    }
}
