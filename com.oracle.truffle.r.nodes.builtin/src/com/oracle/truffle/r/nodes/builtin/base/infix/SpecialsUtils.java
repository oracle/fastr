/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base.infix;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.r.nodes.attributes.HasAttributesNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetDimAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetNamesAttributeNode;
import com.oracle.truffle.r.nodes.builtin.base.infix.SpecialsUtilsFactory.ConvertIndexNodeGen;
import com.oracle.truffle.r.nodes.builtin.base.infix.SpecialsUtilsFactory.ConvertValueNodeGen;
import com.oracle.truffle.r.nodes.function.ClassHierarchyNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

/**
 * Helper methods for implementing special calls.
 *
 * @see com.oracle.truffle.r.runtime.builtins.RSpecialFactory
 */
class SpecialsUtils {

    private static final String valueArgName = "value".intern();

    public static boolean isCorrectUpdateSignature(ArgumentsSignature signature) {
        if (signature.getLength() == 3) {
            return signature.getName(0) == null && signature.getName(1) == null && signature.getName(2) == valueArgName;
        } else if (signature.getLength() == 4) {
            return signature.getName(0) == null && signature.getName(1) == null && signature.getName(2) == null && signature.getName(3) == valueArgName;
        }
        return false;
    }

    /**
     * Common code shared between specials doing subset/subscript related operation.
     */
    abstract static class SubscriptSpecialCommon extends Node {

        protected final boolean inReplacement;

        protected SubscriptSpecialCommon(boolean inReplacement) {
            this.inReplacement = inReplacement;
        }

        protected boolean simpleVector(@SuppressWarnings("unused") RAbstractVector vector) {
            return true;
        }

        /**
         * Checks whether the given (1-based) index is valid for the given vector.
         */
        protected static boolean isValidIndex(RAbstractVector vector, int index) {
            return index >= 1 && index <= vector.getLength();
        }

        /**
         * Checks if the value is single element that can be put into a list or vector as is,
         * because in the case of vectors on the LSH of update we take each element and put it into
         * the RHS of the update function.
         */
        protected static boolean isSingleElement(Object value) {
            return value instanceof Integer || value instanceof Double || value instanceof Byte || value instanceof String;
        }
    }

    abstract static class SubscriptSpecial2Common extends SubscriptSpecialCommon {

        protected SubscriptSpecial2Common(boolean inReplacement) {
            super(inReplacement);
        }

        @Child private GetDimAttributeNode getDimensions = GetDimAttributeNode.create();

        protected int matrixIndex(RAbstractVector vector, int index1, int index2) {
            return index1 - 1 + ((index2 - 1) * getDimensions.getDimensions(vector)[0]);
        }

        /**
         * Checks whether the given (1-based) indexes are valid for the given matrix.
         */
        protected boolean isValidIndex(RAbstractVector vector, int index1, int index2) {
            int[] dimensions = getDimensions.getDimensions(vector);
            return dimensions != null && dimensions.length == 2 && index1 >= 1 && index1 <= dimensions[0] && index2 >= 1 && index2 <= dimensions[1];
        }
    }

    /**
     * Common code shared between specials accessing/updating fields.
     */
    abstract static class ListFieldSpecialBase extends RNode {

        @Child protected GetNamesAttributeNode getNamesNode = GetNamesAttributeNode.create();

        protected final boolean isSimpleList(RList list) {
            return true;
        }

        protected static int getIndex(RStringVector names, String field) {
            if (names != null) {
                int fieldHash = field.hashCode();
                for (int i = 0; i < names.getLength(); i++) {
                    String current = names.getDataAt(i);
                    if (current == field || hashCodeEquals(current, fieldHash) && contentsEquals(current, field)) {
                        return i;
                    }
                }
            }
            return -1;
        }

        @TruffleBoundary
        private static boolean contentsEquals(String current, String field) {
            return field.equals(current);
        }

        @TruffleBoundary
        private static boolean hashCodeEquals(String current, int fieldHash) {
            return current.hashCode() == fieldHash;
        }
    }

    @NodeInfo(cost = NodeCost.NONE)
    @NodeChild(value = "delegate", type = RNode.class)
    public abstract static class ConvertIndex extends RNode {

        protected abstract RNode getDelegate();

        public abstract Object execute(Object value);

        @Specialization
        protected static int convertInteger(int value) {
            return value;
        }

        @Specialization(rewriteOn = IllegalArgumentException.class)
        protected int convertDouble(double value) {
            int intValue = (int) value;
            if (intValue <= 0) {
                /*
                 * Conversion from double to an index differs in subscript and subset for values in
                 * the ]0..1[ range (subscript interprets 0.1 as 1, whereas subset treats it as 0).
                 * We avoid this special case by simply going to the more generic case for this
                 * range. Additionally, (int) Double.NaN is 0, which is also caught by this case.
                 */
                CompilerDirectives.transferToInterpreterAndInvalidate();
                throw new IllegalArgumentException();
            } else {
                return intValue;
            }
        }

        @Specialization(replaces = {"convertInteger", "convertDouble"})
        protected Object convert(Object value) {
            return value;
        }

        @Override
        protected RSyntaxNode getRSyntaxNode() {
            return getDelegate().asRSyntaxNode();
        }
    }

    @NodeInfo(cost = NodeCost.NONE)
    @NodeChild(value = "delegate", type = RNode.class)
    public abstract static class ConvertValue extends RNode {

        protected abstract RNode getDelegate();

        @Specialization
        protected static int convert(int value) {
            return value;
        }

        @Specialization
        protected static double convert(double value) {
            return value;
        }

        @Specialization(guards = {"value.getLength() == 1", "hierarchyNode.execute(value) == null", "hasAttrsNode.execute(value)"})
        protected static int convertIntVector(RIntVector value,
                        @Cached("create()") @SuppressWarnings("unused") ClassHierarchyNode hierarchyNode,
                        @Cached("create()") @SuppressWarnings("unused") HasAttributesNode hasAttrsNode) {
            return value.getDataAt(0);
        }

        @Specialization(guards = {"value.getLength() == 1", "hierarchyNode.execute(value) == null", "hasAttrsNode.execute(value)"})
        protected static double convertDoubleVector(RDoubleVector value,
                        @Cached("create()") @SuppressWarnings("unused") ClassHierarchyNode hierarchyNode,
                        @Cached("create()") @SuppressWarnings("unused") HasAttributesNode hasAttrsNode) {
            return value.getDataAt(0);
        }

        @Specialization(replaces = {"convertIntVector", "convertDoubleVector"})
        protected Object convert(Object value) {
            return value;
        }

        @Override
        protected RSyntaxNode getRSyntaxNode() {
            return getDelegate().asRSyntaxNode();
        }
    }

    public static ConvertIndex convertIndex(RNode value) {
        return ConvertIndexNodeGen.create(value);
    }

    public static ConvertValue convertValue(RNode value) {
        return ConvertValueNodeGen.create(value);
    }
}
