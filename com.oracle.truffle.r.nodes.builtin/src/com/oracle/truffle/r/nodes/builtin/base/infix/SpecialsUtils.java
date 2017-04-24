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
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetDimAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetNamesAttributeNode;
import com.oracle.truffle.r.nodes.builtin.base.infix.SpecialsUtilsFactory.ConvertIndexNodeGen;
import com.oracle.truffle.r.nodes.builtin.base.infix.SpecialsUtilsFactory.ConvertValueNodeGen;
import com.oracle.truffle.r.nodes.builtin.base.infix.SpecialsUtilsFactory.ProfiledSubscriptSpecial2NodeGen;
import com.oracle.truffle.r.nodes.builtin.base.infix.SpecialsUtilsFactory.ProfiledSubscriptSpecialNodeGen;
import com.oracle.truffle.r.nodes.builtin.base.infix.SpecialsUtilsFactory.ProfiledSubsetSpecial2NodeGen;
import com.oracle.truffle.r.nodes.builtin.base.infix.SpecialsUtilsFactory.ProfiledSubsetSpecialNodeGen;
import com.oracle.truffle.r.nodes.function.ClassHierarchyNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
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

    @NodeChild(value = "vector", type = RNode.class)
    @NodeChild(value = "index", type = ConvertIndex.class)
    protected abstract static class ProfiledSubscriptSpecialBase extends SubscriptSpecialCommon {

        protected static final int CACHE_LIMIT = 3;

        @Child protected SubscriptSpecialBase defaultAccessNode;

        protected ProfiledSubscriptSpecialBase(boolean inReplacement) {
            super(inReplacement);
        }

        protected SubscriptSpecialBase createAccessNode() {
            return null;
        }

        @Specialization(limit = "CACHE_LIMIT", guards = "vector.getClass() == clazz")
        public Object access(VirtualFrame frame, RAbstractVector vector, int index, @Cached(value = "vector.getClass()") Class<?> clazz,
                        @Cached("createAccessNode()") SubscriptSpecialBase accessNodeCached) {
            return accessNodeCached.execute(frame, clazz.cast(vector), index);
        }

        @Fallback
        public Object accessGeneric(VirtualFrame frame, Object vector, Object index) {
            if (defaultAccessNode == null) {
                defaultAccessNode = insert(createAccessNode());
            }
            return defaultAccessNode.execute(frame, vector, index);
        }
    }

    public abstract static class ProfiledSubscriptSpecial extends ProfiledSubscriptSpecialBase {

        protected ProfiledSubscriptSpecial(boolean inReplacement) {
            super(inReplacement);
        }

        @Override
        protected SubscriptSpecialBase createAccessNode() {
            return SubscriptSpecialNodeGen.create(inReplacement);
        }

        public static ProfiledSubscriptSpecial create(boolean inReplacement, SubscriptSpecial accessNode, RNode vectorNode, ConvertIndex indexNode) {
            return ProfiledSubscriptSpecialNodeGen.create(inReplacement, accessNode, vectorNode, indexNode);
        }

    }

    public abstract static class ProfiledSubsetSpecial extends ProfiledSubscriptSpecialBase {

        @Child protected SubsetSpecial accessNode;

        protected ProfiledSubsetSpecial(boolean inReplacement) {
            super(inReplacement);
        }

        @Override
        protected SubscriptSpecialBase createAccessNode() {
            return SubsetSpecialNodeGen.create(inReplacement);
        }

        public static ProfiledSubsetSpecial create(boolean inReplacement, SubsetSpecial accessNode, RNode vectorNode, ConvertIndex indexNode) {
            return ProfiledSubsetSpecialNodeGen.create(inReplacement, accessNode, vectorNode, indexNode);
        }

    }

    @NodeChild(value = "vector", type = RNode.class)
    @NodeChild(value = "index1", type = ConvertIndex.class)
    @NodeChild(value = "index2", type = ConvertIndex.class)
    protected abstract static class ProfiledSubscriptSpecial2Base extends SubscriptSpecialCommon {

        protected static final int CACHE_LIMIT = 3;

        @Child protected SubscriptSpecial2Base defaultAccessNode;

        protected ProfiledSubscriptSpecial2Base(boolean inReplacement) {
            super(inReplacement);
        }

        protected SubscriptSpecial2Base createAccessNode() {
            return null;
        }

        @Specialization(limit = "CACHE_LIMIT", guards = "vector.getClass() == clazz")
        public Object access(VirtualFrame frame, RAbstractVector vector, int index1, int index2, @Cached("vector.getClass()") Class<?> clazz,
                        @Cached("createAccessNode()") SubscriptSpecial2Base accessNodeCached) {
            return accessNodeCached.execute(frame, clazz.cast(vector), index1, index2);
        }

        @Fallback
        public Object accessGeneric(VirtualFrame frame, Object vector, Object index1, Object index2) {
            if (defaultAccessNode == null) {
                defaultAccessNode = insert(createAccessNode());
            }
            return defaultAccessNode.execute(frame, vector, index1, index2);
        }
    }

    public abstract static class ProfiledSubscriptSpecial2 extends ProfiledSubscriptSpecial2Base {

        @Child protected SubscriptSpecial2 accessNode;

        protected ProfiledSubscriptSpecial2(boolean inReplacement) {
            super(inReplacement);
        }

        @Override
        protected SubscriptSpecial2Base createAccessNode() {
            return SubscriptSpecial2NodeGen.create(inReplacement);
        }

        public static ProfiledSubscriptSpecial2 create(boolean inReplacement, SubscriptSpecial2 accessNode, RNode vectorNode, ConvertIndex indexNode1, ConvertIndex indexNode2) {
            return ProfiledSubscriptSpecial2NodeGen.create(inReplacement, accessNode, vectorNode, indexNode1, indexNode2);
        }
    }

    public abstract static class ProfiledSubsetSpecial2 extends ProfiledSubscriptSpecial2Base {

        @Child protected SubsetSpecial2 accessNode;

        protected ProfiledSubsetSpecial2(boolean inReplacement) {
            super(inReplacement);
        }

        @Override
        protected SubscriptSpecial2Base createAccessNode() {
            return SubsetSpecial2NodeGen.create(inReplacement);
        }

        public static ProfiledSubsetSpecial2 create(boolean inReplacement, SubsetSpecial2 accessNode, RNode vectorNode, ConvertIndex indexNode1, ConvertIndex indexNode2) {
            return ProfiledSubsetSpecial2NodeGen.create(inReplacement, accessNode, vectorNode, indexNode1, indexNode2);
        }
    }

    /**
     * Common code shared between specials doing subset/subscript related operation.
     */
    abstract static class SubscriptSpecialCommon1 extends Node {

        protected final boolean inReplacement;

        protected SubscriptSpecialCommon1(boolean inReplacement) {
            this.inReplacement = inReplacement;
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

    /**
     * Common code shared between specials doing subset/subscript related operation.
     */
    abstract static class SubscriptSpecialCommon extends RNode {

        protected final boolean inReplacement;

        protected SubscriptSpecialCommon(boolean inReplacement) {
            this.inReplacement = inReplacement;
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

    abstract static class SubscriptSpecial2Common1 extends SubscriptSpecialCommon1 {

        protected SubscriptSpecial2Common1(boolean inReplacement) {
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

        @Child private ClassHierarchyNode hierarchyNode = ClassHierarchyNode.create();
        @Child protected GetNamesAttributeNode getNamesNode = GetNamesAttributeNode.create();

        protected final boolean isSimpleList(RList list) {
            return hierarchyNode.execute(list) == null;
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
    public static final class ProfiledValue extends RBaseNode {

        private final ValueProfile profile = ValueProfile.createClassProfile();

        @Child private RNode delegate;

        protected ProfiledValue(RNode delegate) {
            this.delegate = delegate;
        }

        public Object execute(VirtualFrame frame) {
            return profile.profile(delegate.execute(frame));
        }

        @Override
        protected RSyntaxNode getRSyntaxNode() {
            return delegate.asRSyntaxNode();
        }
    }

    @NodeInfo(cost = NodeCost.NONE)
    @NodeChild(value = "delegate", type = RNode.class)
    public abstract static class ConvertIndex extends RNode {

        protected abstract RNode getDelegate();

        @Specialization
        protected static int convertInteger(int value) {
            return value;
        }

        @Specialization(rewriteOn = IllegalArgumentException.class)
        protected int convertDouble(double value) {
            int intValue = (int) value;
            if (intValue == 0) {
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

        @Specialization(guards = {"value.getLength() == 1", "hierarchyNode.execute(value) == null"})
        protected static int convertIntVector(RIntVector value,
                        @Cached("create()") @SuppressWarnings("unused") ClassHierarchyNode hierarchyNode) {
            return value.getDataAt(0);
        }

        @Specialization(guards = {"value.getLength() == 1", "hierarchyNode.execute(value) == null"})
        protected static double convertDoubleVector(RDoubleVector value,
                        @Cached("create()") @SuppressWarnings("unused") ClassHierarchyNode hierarchyNode) {
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

    public static ProfiledValue profile(RNode value) {
        return new ProfiledValue(value);
    }

    public static ConvertIndex convertIndex(RNode value) {
        return ConvertIndexNodeGen.create(value);
    }

    public static ConvertValue convertValue(RNode value) {
        return ConvertValueNodeGen.create(value);
    }
}
