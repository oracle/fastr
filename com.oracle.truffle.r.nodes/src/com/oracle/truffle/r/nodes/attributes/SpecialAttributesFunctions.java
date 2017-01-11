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
package com.oracle.truffle.r.nodes.attributes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctionsFactory.GetDimAttributeNodeGen;
import com.oracle.truffle.r.nodes.function.opt.ShareObjectNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RAttributesLayout;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RInteger;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RScalarVector;
import com.oracle.truffle.r.runtime.data.RSequence;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * This class defines a number of nodes used to handle the special attributes, such as names, dims,
 * dimnames and rownames.
 */
public final class SpecialAttributesFunctions {

    /**
     * A node used in guards, for example, to determine whether an attribute is a special one.
     */
    public static final class IsSpecialAttributeNode extends RBaseNode {

        private final BranchProfile namesProfile = BranchProfile.create();
        private final BranchProfile dimProfile = BranchProfile.create();
        private final BranchProfile dimNamesProfile = BranchProfile.create();
        private final BranchProfile rowNamesProfile = BranchProfile.create();
        private final BranchProfile classProfile = BranchProfile.create();

        public static IsSpecialAttributeNode create() {
            return new IsSpecialAttributeNode();
        }

        /**
         * The fast-path method.
         */
        public boolean execute(String name) {
            assert name.intern() == name;
            if (name == RRuntime.NAMES_ATTR_KEY) {
                namesProfile.enter();
                return true;
            } else if (name == RRuntime.DIM_ATTR_KEY) {
                dimProfile.enter();
                return true;
            } else if (name == RRuntime.DIMNAMES_ATTR_KEY) {
                dimNamesProfile.enter();
                return true;
            } else if (name == RRuntime.ROWNAMES_ATTR_KEY) {
                rowNamesProfile.enter();
                return true;
            } else if (name == RRuntime.CLASS_ATTR_KEY) {
                classProfile.enter();
                return false;
            }
            return false;
        }

        /**
         * The slow-path method.
         */
        public static boolean isSpecialAttribute(String name) {
            assert name.intern() == name;
            return name == RRuntime.NAMES_ATTR_KEY ||
                            name == RRuntime.DIM_ATTR_KEY ||
                            name == RRuntime.DIMNAMES_ATTR_KEY ||
                            name == RRuntime.ROWNAMES_ATTR_KEY ||
                            name == RRuntime.CLASS_ATTR_KEY;

        }
    }

    /**
     * A node for setting a value to any special attribute.
     */
    public static final class GenericSpecialAttributeNode extends RBaseNode {

        private final BranchProfile namesProfile = BranchProfile.create();
        private final BranchProfile dimProfile = BranchProfile.create();
        private final BranchProfile dimNamesProfile = BranchProfile.create();
        private final BranchProfile rowNamesProfile = BranchProfile.create();

        @Child private SetNamesAttributeNode namesAttrNode;
        @Child private SetDimAttributeNode dimAttrNode;
        @Child private SetDimNamesAttributeNode dimNamesAttrNode;
        @Child private SetRowNamesAttributeNode rowNamesAttrNode;

        public static GenericSpecialAttributeNode create() {
            return new GenericSpecialAttributeNode();
        }

        public void execute(RAttributable x, String name, Object value) {
            assert name.intern() == name;
            if (name == RRuntime.NAMES_ATTR_KEY) {
                namesProfile.enter();
                if (namesAttrNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    namesAttrNode = insert(SetNamesAttributeNode.create());
                }
                namesAttrNode.execute(x, value);
            } else if (name == RRuntime.DIM_ATTR_KEY) {
                dimProfile.enter();
                if (dimAttrNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    dimAttrNode = insert(SetDimAttributeNode.create());
                }
                dimAttrNode.execute(x, value);
            } else if (name == RRuntime.DIMNAMES_ATTR_KEY) {
                dimNamesProfile.enter();
                if (dimNamesAttrNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    dimNamesAttrNode = insert(SetDimNamesAttributeNode.create());
                }
                dimNamesAttrNode.execute(x, value);
            } else if (name == RRuntime.ROWNAMES_ATTR_KEY) {
                rowNamesProfile.enter();
                if (rowNamesAttrNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    rowNamesAttrNode = insert(SetRowNamesAttributeNode.create());
                }
                rowNamesAttrNode.execute(x, value);
            } else if (name == RRuntime.CLASS_ATTR_KEY) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                throw RInternalError.unimplemented("The \"class\" attribute should be set using a separate method");
            } else {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                throw RInternalError.shouldNotReachHere();
            }
        }
    }

    /**
     * A factory method for creating a node setting the given special attribute.
     *
     * @param name the special attribute name
     * @return the node
     */
    public static SetSpecialAttributeNode createSetSpecialAttributeNode(String name) {
        assert name.intern() == name;
        if (name == RRuntime.NAMES_ATTR_KEY) {
            return SetNamesAttributeNode.create();
        } else if (name == RRuntime.DIM_ATTR_KEY) {
            return SetDimAttributeNode.create();
        } else if (name == RRuntime.DIMNAMES_ATTR_KEY) {
            return SetDimNamesAttributeNode.create();
        } else if (name == RRuntime.ROWNAMES_ATTR_KEY) {
            return SetRowNamesAttributeNode.create();
        } else if (name == RRuntime.CLASS_ATTR_KEY) {
            return SetClassAttributeNode.create();
        } else {
            throw RInternalError.shouldNotReachHere();
        }
    }

    /**
     * A factory method for creating a node removing the given special attribute.
     *
     * @param name the special attribute name
     * @return the node
     */
    public static RemoveSpecialAttributeNode createRemoveSpecialAttributeNode(String name) {
        assert name.intern() == name;
        if (name == RRuntime.NAMES_ATTR_KEY) {
            return RemoveNamesAttributeNode.create();
        } else if (name == RRuntime.DIM_ATTR_KEY) {
            return RemoveDimAttributeNode.create();
        } else if (name == RRuntime.DIMNAMES_ATTR_KEY) {
            return RemoveDimNamesAttributeNode.create();
        } else if (name == RRuntime.ROWNAMES_ATTR_KEY) {
            return RemoveRowNamesAttributeNode.create();
        } else if (name == RRuntime.CLASS_ATTR_KEY) {
            return RemoveClassAttributeNode.create();
        } else {
            throw RInternalError.shouldNotReachHere();
        }
    }

    /**
     * A factory method for creating a node retrieving the given special attribute.
     *
     * @param name the special attribute name
     * @return the node
     */
    public static GetFixedAttributeNode createGetSpecialAttributeNode(String name) {
        assert name.intern() == name;
        if (name == RRuntime.NAMES_ATTR_KEY) {
            return GetNamesAttributeNode.create();
        } else if (name == RRuntime.DIM_ATTR_KEY) {
            return GetDimAttributeNode.create();
        } else if (name == RRuntime.DIMNAMES_ATTR_KEY) {
            return GetDimNamesAttributeNode.create();
        } else if (name == RRuntime.ROWNAMES_ATTR_KEY) {
            return GetRowNamesAttributeNode.create();
        } else if (name == RRuntime.CLASS_ATTR_KEY) {
            return GetClassAttributeNode.create();
        } else {
            throw RInternalError.shouldNotReachHere();
        }
    }

    /**
     * The base class for the nodes setting values to special attributes.
     */
    public abstract static class SetSpecialAttributeNode extends SetFixedAttributeNode {

        protected SetSpecialAttributeNode(String name) {
            super(name);
        }

        public abstract void execute(RAttributable x, Object attrValue);

    }

    /**
     * The base class for the nodes removing values from special attributes.
     */
    public abstract static class RemoveSpecialAttributeNode extends RemoveFixedAttributeNode {

        protected RemoveSpecialAttributeNode(String name) {
            super(name);
        }

        public abstract void execute(RAttributable x);

        @Specialization(insertBefore = "removeAttrFromAttributable")
        protected void removeAttrFromVector(RVector<?> x,
                        @Cached("create()") BranchProfile attrNullProfile,
                        @Cached("create()") BranchProfile attrEmptyProfile) {
            DynamicObject attributes = x.getAttributes();
            if (attributes == null) {
                attrNullProfile.enter();
                return;
            }

            attributes.delete(name);

            if (attributes.isEmpty()) {
                attrEmptyProfile.enter();
                x.initAttributes(null);
            }
        }

    }

    public abstract static class SetNamesAttributeNode extends SetSpecialAttributeNode {

        private final ConditionProfile nullDimNamesProfile = ConditionProfile.createBinaryProfile();

        protected SetNamesAttributeNode() {
            super(RRuntime.NAMES_ATTR_KEY);
        }

        public static SetNamesAttributeNode create() {
            return SpecialAttributesFunctionsFactory.SetNamesAttributeNodeGen.create();
        }

        public void setNames(RAbstractContainer x, RStringVector newNames) {
            if (nullDimNamesProfile.profile(newNames == null)) {
                execute(x, RNull.instance);
            } else {
                execute(x, newNames);
            }
        }

        @Specialization(insertBefore = "setAttrInAttributable")
        protected void setNamesInLanguage(RLanguage x, RStringVector newNames,
                        @Cached("createBinaryProfile()") ConditionProfile pairListProfile) {
            RPairList pl = x.getPairListInternal();
            if (pairListProfile.profile(pl == null)) {
                /* See getNames */
                RContext.getRRuntimeASTAccess().setNames(x, newNames);
            } else {
                pl.setNames(newNames);
            }
        }

        @Specialization(insertBefore = "setAttrInAttributable")
        protected void resetDimNames(RAbstractContainer x, @SuppressWarnings("unused") RNull rnull,
                        @Cached("create()") RemoveNamesAttributeNode removeNamesAttrNode) {
            removeNamesAttrNode.execute(x);
        }

        @Specialization(insertBefore = "setAttrInAttributable")
        protected void setNamesInVector(RAbstractVector x, RStringVector newNames,
                        @Cached("create()") BranchProfile namesTooLongProfile,
                        @Cached("createBinaryProfile()") ConditionProfile useDimNamesProfile,
                        @Cached("create()") GetDimAttributeNode getDimNode,
                        @Cached("create()") SetDimNamesAttributeNode setDimNamesNode,
                        @Cached("create()") BranchProfile attrNullProfile,
                        @Cached("createBinaryProfile()") ConditionProfile attrStorageProfile,
                        @Cached("createClassProfile()") ValueProfile xTypeProfile,
                        @Cached("create()") ShareObjectNode updateRefCountNode) {
            RAbstractVector xProfiled = xTypeProfile.profile(x);
            if (newNames.getLength() > xProfiled.getLength()) {
                namesTooLongProfile.enter();
                throw RError.error(this, RError.Message.ATTRIBUTE_VECTOR_SAME_LENGTH, RRuntime.NAMES_ATTR_KEY, newNames.getLength(), xProfiled.getLength());
            }

            int[] dimensions = getDimNode.getDimensions(x);
            if (useDimNamesProfile.profile(dimensions != null && dimensions.length == 1)) {
                // for one dimensional array, "names" is really "dimnames[[1]]" (see R
                // documentation for "names" function)
                RList newDimNames = RDataFactory.createList(new Object[]{newNames});
                newDimNames.elementNamePrefix = RRuntime.DIMNAMES_LIST_ELEMENT_NAME_PREFIX;
                setDimNamesNode.setDimNames(xProfiled, newDimNames);
            } else {
                assert newNames != xProfiled;
                DynamicObject attrs = xProfiled.getAttributes();
                if (attrs == null) {
                    attrNullProfile.enter();
                    attrs = RAttributesLayout.createNames(newNames);
                    xProfiled.initAttributes(attrs);
                    return;
                }

                super.setAttrInAttributable(xProfiled, newNames, attrNullProfile, attrStorageProfile, xTypeProfile, updateRefCountNode);
            }
        }

        @Specialization(insertBefore = "setAttrInAttributable", guards = "!isRAbstractVector(x)")
        @TruffleBoundary
        protected void setNamesInContainer(RAbstractContainer x, RStringVector newNames,
                        @Cached("createClassProfile()") ValueProfile contClassProfile) {
            RAbstractContainer xProfiled = contClassProfile.profile(x);
            xProfiled.setNames(newNames);
        }
    }

    public abstract static class RemoveNamesAttributeNode extends RemoveSpecialAttributeNode {

        protected RemoveNamesAttributeNode() {
            super(RRuntime.NAMES_ATTR_KEY);
        }

        @Override
        @Specialization
        protected void removeAttrFallback(DynamicObject attrs) {
            super.removeAttrFallback(attrs);
        }

        public static RemoveNamesAttributeNode create() {
            return SpecialAttributesFunctionsFactory.RemoveNamesAttributeNodeGen.create();
        }
    }

    public abstract static class GetNamesAttributeNode extends GetFixedAttributeNode {

        protected GetNamesAttributeNode() {
            super(RRuntime.NAMES_ATTR_KEY);
        }

        public static GetNamesAttributeNode create() {
            return SpecialAttributesFunctionsFactory.GetNamesAttributeNodeGen.create();
        }

        public final RStringVector getNames(Object x) {
            return (RStringVector) execute(x);
        }

        @Specialization(insertBefore = "getAttrFromAttributable")
        protected Object getScalarVectorNames(@SuppressWarnings("unused") RScalarVector x) {
            return null;
        }

        @Specialization(insertBefore = "getAttrFromAttributable")
        protected Object getPairListNames(RPairList x) {
            return x.getNames();
        }

        @Specialization(insertBefore = "getAttrFromAttributable")
        protected Object getSequenceVectorNames(@SuppressWarnings("unused") RSequence x) {
            return null;
        }

        @Specialization(insertBefore = "getAttrFromAttributable")
        protected Object getLanguageNames(RLanguage x,
                        @Cached("createBinaryProfile()") ConditionProfile pairListProfile) {
            RPairList pl = x.getPairListInternal();
            if (pairListProfile.profile(pl == null)) {
                /*
                 * "names" for a language object is a special case, that is applicable to calls and
                 * returns the names of the actual arguments, if any. E.g. f(x=1, 3) would return
                 * c("", "x", ""). GnuR defines it as returning the "tag" values on the pairlist
                 * that represents the call. Well, we don't have a pairlist, (we could get one by
                 * serializing the expression), so we do it by AST walking.
                 */
                RStringVector names = RContext.getRRuntimeASTAccess().getNames(x);
                return names;
            } else {
                return pl.getNames();
            }
        }

        @Specialization(insertBefore = "getAttrFromAttributable")
        protected Object getVectorNames(RAbstractVector x,
                        @Cached("create()") BranchProfile attrNullProfile,
                        @Cached("createBinaryProfile()") ConditionProfile attrStorageProfile,
                        @Cached("createClassProfile()") ValueProfile xTypeProfile,
                        @Cached("create()") BranchProfile namesNullProfile,
                        @Cached("create()") BranchProfile dimNamesAvlProfile,
                        @Cached("create()") GetDimNamesAttributeNode getDimNames) {
            RStringVector names = (RStringVector) super.getAttrFromAttributable(x, attrNullProfile, attrStorageProfile, xTypeProfile);
            if (names == null) {
                namesNullProfile.enter();
                RList dimNames = getDimNames.getDimNames(x);
                if (dimNames != null && dimNames.getLength() == 1) {
                    dimNamesAvlProfile.enter();
                    return dimNames.getDataAt(0);
                }
                return null;
            }
            return names;
        }

        @Specialization(insertBefore = "getAttrFromAttributable", guards = "!isRAbstractVector(x)")
        @TruffleBoundary
        protected Object getVectorNames(RAbstractContainer x,
                        @Cached("createClassProfile()") ValueProfile xTypeProfile) {
            return xTypeProfile.profile(x).getNames();
        }
    }

    public abstract static class SetDimAttributeNode extends SetSpecialAttributeNode {

        private final ConditionProfile nullDimProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile naDimProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile negativeDimProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile dimNotMatchLengthProfile = ConditionProfile.createBinaryProfile();
        private final ValueProfile contArgClassProfile = ValueProfile.createClassProfile();
        private final ValueProfile dimArgClassProfile = ValueProfile.createClassProfile();
        private final LoopConditionProfile verifyLoopProfile = LoopConditionProfile.createCountingProfile();

        protected SetDimAttributeNode() {
            super(RRuntime.DIM_ATTR_KEY);
        }

        public static SetDimAttributeNode create() {
            return SpecialAttributesFunctionsFactory.SetDimAttributeNodeGen.create();
        }

        public void setDimensions(RAbstractContainer x, int[] dims) {
            if (nullDimProfile.profile(dims == null)) {
                execute(x, RNull.instance);
            } else {
                execute(x, RDataFactory.createIntVector(dims, RDataFactory.COMPLETE_VECTOR));
            }
        }

        @Specialization(insertBefore = "setAttrInAttributable")
        protected void resetDims(RAbstractContainer x, @SuppressWarnings("unused") RNull rnull,
                        @Cached("create()") RemoveDimAttributeNode removeDimAttrNode,
                        @Cached("create()") SetDimNamesAttributeNode setDimNamesNode) {
            removeDimAttrNode.execute(x);
            setDimNamesNode.setDimNames(x, null);
        }

        @Specialization(insertBefore = "setAttrInAttributable")
        protected void setOneDimInVector(RVector<?> x, int dim,
                        @Cached("create()") BranchProfile attrNullProfile,
                        @Cached("createBinaryProfile()") ConditionProfile attrStorageProfile,
                        @Cached("createClassProfile()") ValueProfile xTypeProfile,
                        @Cached("create()") ShareObjectNode updateRefCountNode) {
            RAbstractContainer xProfiled = contArgClassProfile.profile(x);

            int[] dims = new int[]{dim};
            verifyOneDimensions(xProfiled.getLength(), dim);

            RIntVector dimVec = RDataFactory.createIntVector(dims, RDataFactory.COMPLETE_VECTOR);

            DynamicObject attrs = xProfiled.getAttributes();
            if (attrs == null) {
                attrNullProfile.enter();
                attrs = RAttributesLayout.createDim(dimVec);
                xProfiled.initAttributes(attrs);
                updateRefCountNode.execute(dimVec);
                return;
            }

            super.setAttrInAttributable(x, dimVec, attrNullProfile, attrStorageProfile, xTypeProfile, updateRefCountNode);
        }

        @Specialization(insertBefore = "setAttrInAttributable")
        protected void setDimsInVector(RAbstractVector x, RAbstractIntVector dims,
                        @Cached("create()") BranchProfile attrNullProfile,
                        @Cached("createBinaryProfile()") ConditionProfile attrStorageProfile,
                        @Cached("createClassProfile()") ValueProfile xTypeProfile,
                        @Cached("create()") ShareObjectNode updateRefCountNode) {
            RAbstractContainer xProfiled = contArgClassProfile.profile(x);
            verifyDimensions(xProfiled.getLength(), dims);

            DynamicObject attrs = xProfiled.getAttributes();
            if (attrs == null) {
                attrNullProfile.enter();
                attrs = RAttributesLayout.createDim(dims);
                xProfiled.initAttributes(attrs);
                updateRefCountNode.execute(dims);
                return;
            }

            super.setAttrInAttributable(x, dims, attrNullProfile, attrStorageProfile, xTypeProfile, updateRefCountNode);
        }

        @Specialization(insertBefore = "setAttrInAttributable", guards = "!isRAbstractVector(x)")
        protected void setDimsInContainerFallback(RAbstractContainer x, RAbstractIntVector dims,
                        @Cached("create()") SetDimAttributeNode setDimNode) {
            int[] dimsArr = dims.materialize().getDataCopy();
            setDimNode.setDimensions(x, dimsArr);
        }

        private void verifyOneDimensions(int vectorLength, int dim) {
            int length = dim;
            if (naDimProfile.profile(RRuntime.isNA(dim))) {
                throw RError.error(this, RError.Message.DIMS_CONTAIN_NA);
            } else if (negativeDimProfile.profile(dim < 0)) {
                throw RError.error(this, RError.Message.DIMS_CONTAIN_NEGATIVE_VALUES);
            }
            if (dimNotMatchLengthProfile.profile(length != vectorLength && vectorLength > 0)) {
                CompilerDirectives.transferToInterpreter();
                throw RError.error(this, RError.Message.DIMS_DONT_MATCH_LENGTH, length, vectorLength);
            }
        }

        public void verifyDimensions(int vectorLength, RAbstractIntVector dims) {
            RAbstractIntVector dimsProfiled = dimArgClassProfile.profile(dims);
            int dimLen = dims.getLength();
            verifyLoopProfile.profileCounted(dimLen);
            int length = 1;
            for (int i = 0; i < dimLen; i++) {
                int dim = dimsProfiled.getDataAt(i);
                if (naDimProfile.profile(RRuntime.isNA(dim))) {
                    CompilerDirectives.transferToInterpreter();
                    throw RError.error(this, RError.Message.DIMS_CONTAIN_NA);
                } else if (negativeDimProfile.profile(dim < 0)) {
                    CompilerDirectives.transferToInterpreter();
                    throw RError.error(this, RError.Message.DIMS_CONTAIN_NEGATIVE_VALUES);
                }
                length *= dim;
            }
            if (length != vectorLength && vectorLength > 0) {
                CompilerDirectives.transferToInterpreter();
                throw RError.error(this, RError.Message.DIMS_DONT_MATCH_LENGTH, length, vectorLength);
            }
        }

    }

    public abstract static class RemoveDimAttributeNode extends RemoveSpecialAttributeNode {

        protected RemoveDimAttributeNode() {
            super(RRuntime.DIM_ATTR_KEY);
        }

        public static RemoveDimAttributeNode create() {
            return SpecialAttributesFunctionsFactory.RemoveDimAttributeNodeGen.create();
        }

        @Override
        @Specialization
        protected void removeAttrFallback(DynamicObject attrs) {
            super.removeAttrFallback(attrs);
        }

    }

    public abstract static class GetDimAttributeNode extends GetFixedAttributeNode {

        private final BranchProfile isLanguageProfile = BranchProfile.create();
        private final BranchProfile isPairListProfile = BranchProfile.create();
        private final ConditionProfile nullDimsProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile nonEmptyDimsProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile twoDimsOrMoreProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile isContainerProfile = ConditionProfile.createBinaryProfile();

        protected GetDimAttributeNode() {
            super(RRuntime.DIM_ATTR_KEY);
        }

        public static GetDimAttributeNode create() {
            return GetDimAttributeNodeGen.create();
        }

        public final int[] getDimensions(Object x) {
            // Let's handle the following two types directly so as to avoid wrapping and unwrapping
            // RIntVector. The getContainerDims spec would be invoked otherwise.
            if (x instanceof RLanguage) {
                isLanguageProfile.enter();
                return ((RLanguage) x).getDimensions();
            }
            if (x instanceof RPairList) {
                isPairListProfile.enter();
                return ((RPairList) x).getDimensions();
            }
            RIntVector dims = (RIntVector) execute(x);
            return nullDimsProfile.profile(dims == null) ? null : dims.getInternalStore();
        }

        public static boolean isArray(int[] dimensions) {
            return dimensions != null && dimensions.length > 0;
        }

        public static boolean isMatrix(int[] dimensions) {
            return dimensions != null && dimensions.length == 2;
        }

        public final boolean isArray(RAbstractVector vector) {
            RIntVector dims = (RIntVector) execute(vector);
            return nullDimsProfile.profile(dims == null) ? false : dims.getLength() > 0;
        }

        public final boolean isMatrix(RAbstractVector vector) {
            RIntVector dims = (RIntVector) execute(vector);
            return nullDimsProfile.profile(dims == null) ? false : dims.getLength() == 2;
        }

        @Specialization(insertBefore = "getAttrFromAttributable")
        protected Object getScalarVectorDims(@SuppressWarnings("unused") RScalarVector x) {
            return null;
        }

        @Specialization(insertBefore = "getAttrFromAttributable")
        protected Object getScalarVectorDims(@SuppressWarnings("unused") RSequence x) {
            return null;
        }

        @Specialization(insertBefore = "getAttrFromAttributable")
        protected Object getVectorDims(RAbstractVector x,
                        @Cached("create()") BranchProfile attrNullProfile,
                        @Cached("createBinaryProfile()") ConditionProfile attrStorageProfile,
                        @Cached("createClassProfile()") ValueProfile xTypeProfile) {
            return super.getAttrFromAttributable(x, attrNullProfile, attrStorageProfile, xTypeProfile);
        }

        @Specialization(insertBefore = "getAttrFromAttributable")
        protected Object getContainerDims(RAbstractContainer x,
                        @Cached("createClassProfile()") ValueProfile xTypeProfile,
                        @Cached("createBinaryProfile()") ConditionProfile nullResultProfile) {
            int[] res = xTypeProfile.profile(x).getDimensions();
            return nullResultProfile.profile(res == null) ? null : RDataFactory.createIntVector(res, true);
        }

        public int nrows(Object x) {
            if (isContainerProfile.profile(x instanceof RAbstractContainer)) {
                RAbstractContainer xa = (RAbstractContainer) x;
                int[] dims = getDimensions(xa);
                if (nonEmptyDimsProfile.profile(dims != null && dims.length > 0)) {
                    return dims[0];
                } else {
                    return xa.getLength();
                }
            } else {
                throw RError.error(RError.SHOW_CALLER2, RError.Message.OBJECT_NOT_MATRIX);
            }
        }

        public int ncols(Object x) {
            if (isContainerProfile.profile(x instanceof RAbstractContainer)) {
                RAbstractContainer xa = (RAbstractContainer) x;
                int[] dims = getDimensions(xa);
                if (nonEmptyDimsProfile.profile(dims != null && dims.length > 0)) {
                    if (twoDimsOrMoreProfile.profile(dims.length >= 2)) {
                        return dims[1];
                    } else {
                        return 1;
                    }
                } else {
                    return 1;
                }
            } else {
                throw RError.error(RError.SHOW_CALLER2, RError.Message.OBJECT_NOT_MATRIX);
            }
        }

    }

    public abstract static class SetDimNamesAttributeNode extends SetSpecialAttributeNode {

        private final ConditionProfile nullDimNamesProfile = ConditionProfile.createBinaryProfile();

        protected SetDimNamesAttributeNode() {
            super(RRuntime.DIMNAMES_ATTR_KEY);
        }

        public static SetDimNamesAttributeNode create() {
            return SpecialAttributesFunctionsFactory.SetDimNamesAttributeNodeGen.create();
        }

        public void setDimNames(RAbstractContainer x, RList dimNames) {
            if (nullDimNamesProfile.profile(dimNames == null)) {
                execute(x, RNull.instance);
            } else {
                execute(x, dimNames);
            }
        }

        @Specialization(insertBefore = "setAttrInAttributable")
        protected void resetDimNames(RAbstractContainer x, @SuppressWarnings("unused") RNull rnull,
                        @Cached("create()") RemoveDimNamesAttributeNode removeDimNamesAttrNode) {
            removeDimNamesAttrNode.execute(x);
        }

        @Specialization(insertBefore = "setAttrInAttributable")
        protected void setDimNamesInLanguage(RLanguage x, RAbstractVector newDimNames,
                        @Cached("createBinaryProfile()") ConditionProfile pairListProfile,
                        @Cached("create()") BranchProfile attrNullProfile,
                        @Cached("createBinaryProfile()") ConditionProfile attrStorageProfile,
                        @Cached("createClassProfile()") ValueProfile typeProfile,
                        @Cached("create()") ShareObjectNode updateRefCountNode) {
            RPairList pl = x.getPairListInternal();
            RAttributable attr = pairListProfile.profile(pl == null) ? x : pl;
            setAttrInAttributable(attr, newDimNames, attrNullProfile, attrStorageProfile, typeProfile, updateRefCountNode);
        }

        @Specialization(insertBefore = "setAttrInAttributable")
        protected void setDimNamesInVector(RVector<?> x, RList newDimNames,
                        @Cached("create()") GetDimAttributeNode getDimNode,
                        @Cached("create()") BranchProfile nullDimsProfile,
                        @Cached("create()") BranchProfile dimsLengthProfile,
                        @Cached("createCountingProfile()") LoopConditionProfile loopProfile,
                        @Cached("create()") BranchProfile invalidDimProfile,
                        @Cached("create()") BranchProfile nullDimProfile,
                        @Cached("create()") BranchProfile resizeDimsProfile,
                        @Cached("create()") BranchProfile attrNullProfile,
                        @Cached("createBinaryProfile()") ConditionProfile attrStorageProfile,
                        @Cached("createClassProfile()") ValueProfile xTypeProfile,
                        @Cached("create()") ShareObjectNode updateRefCountNode) {
            int[] dimensions = getDimNode.getDimensions(x);
            if (dimensions == null) {
                nullDimsProfile.enter();
                throw RError.error(this, RError.Message.DIMNAMES_NONARRAY);
            }
            int newDimNamesLength = newDimNames.getLength();
            if (newDimNamesLength > dimensions.length) {
                dimsLengthProfile.enter();
                throw RError.error(this, RError.Message.DIMNAMES_DONT_MATCH_DIMS, newDimNamesLength,
                                dimensions.length);
            }

            loopProfile.profileCounted(newDimNamesLength);
            for (int i = 0; loopProfile.inject(i < newDimNamesLength); i++) {
                Object dimObject = newDimNames.getDataAt(i);

                if ((dimObject instanceof String && dimensions[i] != 1) ||
                                (dimObject instanceof RStringVector && !isValidDimLength((RStringVector) dimObject, dimensions[i]))) {
                    invalidDimProfile.enter();
                    throw RError.error(this, RError.Message.DIMNAMES_DONT_MATCH_EXTENT, i + 1);
                }

                if (dimObject == null || (dimObject instanceof RStringVector && ((RStringVector) dimObject).getLength() == 0)) {
                    nullDimProfile.enter();
                    newDimNames.updateDataAt(i, RNull.instance, null);
                }
            }

            RList resDimNames = newDimNames;
            if (newDimNamesLength < dimensions.length) {
                resizeDimsProfile.enter();
                // resize the array and fill the missing entries with NULL-s
                resDimNames = (RList) resDimNames.copyResized(dimensions.length, true);
                resDimNames.setAttributes(newDimNames);
                for (int i = newDimNamesLength; i < dimensions.length; i++) {
                    resDimNames.updateDataAt(i, RNull.instance, null);
                }
            }
            resDimNames.elementNamePrefix = RRuntime.DIMNAMES_LIST_ELEMENT_NAME_PREFIX;

            if (x.getAttributes() == null) {
                attrNullProfile.enter();
                x.initAttributes(RAttributesLayout.createDimNames(resDimNames));
                updateRefCountNode.execute(resDimNames);
                return;
            }

            super.setAttrInAttributable(x, resDimNames, attrNullProfile, attrStorageProfile, xTypeProfile, updateRefCountNode);
        }

        private static boolean isValidDimLength(RStringVector x, int expectedDim) {
            int len = x.getLength();
            return len == 0 || len == expectedDim;
        }

        @Specialization(insertBefore = "setAttrInAttributable")
        @TruffleBoundary
        protected void setDimNamesInContainer(RAbstractContainer x, RList dimNames, @Cached("createClassProfile()") ValueProfile contClassProfile) {
            RAbstractContainer xProfiled = contClassProfile.profile(x);
            xProfiled.setDimNames(dimNames);
        }

    }

    public abstract static class RemoveDimNamesAttributeNode extends RemoveSpecialAttributeNode {

        protected RemoveDimNamesAttributeNode() {
            super(RRuntime.DIMNAMES_ATTR_KEY);
        }

        @Override
        @Specialization
        protected void removeAttrFallback(DynamicObject attrs) {
            super.removeAttrFallback(attrs);
        }

        public static RemoveDimNamesAttributeNode create() {
            return SpecialAttributesFunctionsFactory.RemoveDimNamesAttributeNodeGen.create();
        }
    }

    public abstract static class GetDimNamesAttributeNode extends GetFixedAttributeNode {

        protected GetDimNamesAttributeNode() {
            super(RRuntime.DIMNAMES_ATTR_KEY);
        }

        public static GetDimNamesAttributeNode create() {
            return SpecialAttributesFunctionsFactory.GetDimNamesAttributeNodeGen.create();
        }

        public final RList getDimNames(Object x) {
            return (RList) execute(x);
        }

        @Specialization(insertBefore = "getAttrFromAttributable")
        protected Object getVectorDimNames(@SuppressWarnings("unused") RPairList x) {
            return null;
        }

        @Specialization(insertBefore = "getAttrFromAttributable")
        protected Object getLanguageDimNames(RLanguage x,
                        @Cached("createBinaryProfile()") ConditionProfile pairListProfile,
                        @Cached("create()") BranchProfile attrNullProfile,
                        @Cached("createBinaryProfile()") ConditionProfile attrStorageProfile,
                        @Cached("createClassProfile()") ValueProfile xTypeProfile,
                        @Cached("createBinaryProfile()") ConditionProfile nullRowNamesProfile) {
            RPairList pl = x.getPairListInternal();
            RAttributable attr = pairListProfile.profile(pl == null) ? x : pl;
            Object res = super.getAttrFromAttributable(attr, attrNullProfile, attrStorageProfile, xTypeProfile);
            return nullRowNamesProfile.profile(res == null) ? RNull.instance : res;
        }

        @Specialization(insertBefore = "getAttrFromAttributable")
        protected Object getVectorDimNames(RAbstractVector x,
                        @Cached("create()") BranchProfile attrNullProfile,
                        @Cached("createBinaryProfile()") ConditionProfile attrStorageProfile,
                        @Cached("createClassProfile()") ValueProfile xTypeProfile) {
            return super.getAttrFromAttributable(x, attrNullProfile, attrStorageProfile, xTypeProfile);
        }

        @Specialization(insertBefore = "getAttrFromAttributable", guards = "!isRAbstractVector(x)")
        @TruffleBoundary
        protected Object getVectorDimNames(RAbstractContainer x,
                        @Cached("createClassProfile()") ValueProfile xTypeProfile) {
            return xTypeProfile.profile(x).getDimNames();
        }

    }

    public abstract static class SetRowNamesAttributeNode extends SetSpecialAttributeNode {

        private final ConditionProfile nullRowNamesProfile = ConditionProfile.createBinaryProfile();

        protected SetRowNamesAttributeNode() {
            super(RRuntime.ROWNAMES_ATTR_KEY);
        }

        public static SetRowNamesAttributeNode create() {
            return SpecialAttributesFunctionsFactory.SetRowNamesAttributeNodeGen.create();
        }

        public void setRowNames(RAbstractContainer x, RAbstractVector rowNames) {
            if (nullRowNamesProfile.profile(rowNames == null)) {
                execute(x, RNull.instance);
            } else {
                execute(x, rowNames);
            }
        }

        @Specialization(insertBefore = "setAttrInAttributable")
        protected void resetRowNames(RVector<?> x, @SuppressWarnings("unused") RNull rnull,
                        @Cached("create()") RemoveRowNamesAttributeNode removeRowNamesAttrNode) {
            removeRowNamesAttrNode.execute(x);
        }

        @Specialization(insertBefore = "setAttrInAttributable")
        protected void setRowNamesInLanguage(RLanguage x, RAbstractVector newRowNames,
                        @Cached("createBinaryProfile()") ConditionProfile pairListProfile,
                        @Cached("create()") BranchProfile attrNullProfile,
                        @Cached("createBinaryProfile()") ConditionProfile attrStorageProfile,
                        @Cached("createClassProfile()") ValueProfile typeProfile,
                        @Cached("create()") ShareObjectNode updateRefCountNode) {
            RPairList pl = x.getPairListInternal();
            RAttributable attr = pairListProfile.profile(pl == null) ? x : pl;
            setAttrInAttributable(attr, newRowNames, attrNullProfile, attrStorageProfile, typeProfile, updateRefCountNode);
        }

        @Specialization(insertBefore = "setAttrInAttributable")
        protected void setRowNamesInVector(RAbstractVector x, RAbstractVector newRowNames,
                        @Cached("create()") BranchProfile attrNullProfile,
                        @Cached("createBinaryProfile()") ConditionProfile attrStorageProfile,
                        @Cached("createClassProfile()") ValueProfile xTypeProfile,
                        @Cached("create()") ShareObjectNode updateRefCountNode) {
            if (x.getAttributes() == null) {
                attrNullProfile.enter();
                x.initAttributes(RAttributesLayout.createRowNames(newRowNames));
                updateRefCountNode.execute(newRowNames);
                return;
            }
            setAttrInAttributable(x, newRowNames, attrNullProfile, attrStorageProfile, xTypeProfile, updateRefCountNode);
        }

        @Specialization(insertBefore = "setAttrInAttributable", guards = "!isRAbstractVector(x)")
        @TruffleBoundary
        protected void setRowNamesInContainer(RAbstractContainer x, RAbstractVector rowNames, @Cached("createClassProfile()") ValueProfile contClassProfile) {
            RAbstractContainer xProfiled = contClassProfile.profile(x);
            xProfiled.setRowNames(rowNames);
        }

    }

    public abstract static class RemoveRowNamesAttributeNode extends RemoveSpecialAttributeNode {

        protected RemoveRowNamesAttributeNode() {
            super(RRuntime.DIMNAMES_ATTR_KEY);
        }

        public static RemoveRowNamesAttributeNode create() {
            return SpecialAttributesFunctionsFactory.RemoveRowNamesAttributeNodeGen.create();
        }

        @Override
        @Specialization
        protected void removeAttrFallback(DynamicObject attrs) {
            super.removeAttrFallback(attrs);
        }
    }

    public abstract static class GetRowNamesAttributeNode extends GetFixedAttributeNode {

        protected GetRowNamesAttributeNode() {
            super(RRuntime.ROWNAMES_ATTR_KEY);
        }

        public static GetRowNamesAttributeNode create() {
            return SpecialAttributesFunctionsFactory.GetRowNamesAttributeNodeGen.create();
        }

        public Object getRowNames(RAbstractContainer x) {
            return execute(x);
        }

        @Specialization(insertBefore = "getAttrFromAttributable")
        protected Object getScalarVectorRowNames(@SuppressWarnings("unused") RScalarVector x) {
            return RNull.instance;
        }

        @Specialization(insertBefore = "getAttrFromAttributable")
        protected Object getSequenceRowNames(@SuppressWarnings("unused") RSequence x) {
            return RNull.instance;
        }

        @Specialization(insertBefore = "getAttrFromAttributable")
        protected Object getLanguageRowNames(RLanguage x,
                        @Cached("createBinaryProfile()") ConditionProfile pairListProfile,
                        @Cached("create()") BranchProfile attrNullProfile,
                        @Cached("createBinaryProfile()") ConditionProfile attrStorageProfile,
                        @Cached("createBinaryProfile()") ConditionProfile nullRowNamesProfile,
                        @Cached("createClassProfile()") ValueProfile xTypeProfile) {
            RPairList pl = x.getPairListInternal();
            RAttributable attr = pairListProfile.profile(pl == null) ? x : pl;
            Object res = super.getAttrFromAttributable(attr, attrNullProfile, attrStorageProfile, xTypeProfile);
            return nullRowNamesProfile.profile(res == null) ? RNull.instance : res;
        }

        @Specialization(insertBefore = "getAttrFromAttributable")
        protected Object getVectorRowNames(RAbstractVector x,
                        @Cached("create()") BranchProfile attrNullProfile,
                        @Cached("createBinaryProfile()") ConditionProfile attrStorageProfile,
                        @Cached("createBinaryProfile()") ConditionProfile nullRowNamesProfile,
                        @Cached("createClassProfile()") ValueProfile xTypeProfile) {
            Object res = super.getAttrFromAttributable(x, attrNullProfile, attrStorageProfile, xTypeProfile);
            return nullRowNamesProfile.profile(res == null) ? RNull.instance : res;
        }

        @Specialization(insertBefore = "getAttrFromAttributable")
        @TruffleBoundary
        protected Object getVectorRowNames(RAbstractContainer x) {
            return x.getRowNames();
        }

    }

    public abstract static class SetClassAttributeNode extends SetSpecialAttributeNode {

        protected SetClassAttributeNode() {
            super(RRuntime.CLASS_ATTR_KEY);
        }

        public static SetClassAttributeNode create() {
            return SpecialAttributesFunctionsFactory.SetClassAttributeNodeGen.create();
        }

        public void reset(RAttributable x) {
            execute(x, RNull.instance);
        }

        @Specialization(insertBefore = "setAttrInAttributable")
        protected <T> void handleVectorNullClass(RAbstractVector vector, @SuppressWarnings("unused") RNull classAttr,
                        @Cached("createClass()") RemoveFixedAttributeNode removeClassAttrNode,
                        @Cached("createBinaryProfile()") ConditionProfile initAttrProfile,
                        @Cached("create()") BranchProfile nullAttrProfile,
                        @Cached("createBinaryProfile()") ConditionProfile nullClassProfile,
                        @Cached("createBinaryProfile()") ConditionProfile notNullClassProfile,
                        @Cached("createBinaryProfile()") ConditionProfile attrStorageProfile,
                        @Cached("createClassProfile()") ValueProfile xTypeProfile,
                        @Cached("create()") ShareObjectNode updateRefCountNode) {
            handleVector(vector, null, removeClassAttrNode, initAttrProfile, nullAttrProfile, nullClassProfile, notNullClassProfile, attrStorageProfile, xTypeProfile, updateRefCountNode);
        }

        @Specialization(insertBefore = "setAttrInAttributable")
        protected <T> void handleVector(RAbstractVector vector, RStringVector classAttr,
                        @Cached("createClass()") RemoveFixedAttributeNode removeClassAttrNode,
                        @Cached("createBinaryProfile()") ConditionProfile initAttrProfile,
                        @Cached("create()") BranchProfile nullAttrProfile,
                        @Cached("createBinaryProfile()") ConditionProfile nullClassProfile,
                        @Cached("createBinaryProfile()") ConditionProfile notNullClassProfile,
                        @Cached("createBinaryProfile()") ConditionProfile attrStorageProfile,
                        @Cached("createClassProfile()") ValueProfile xTypeProfile,
                        @Cached("create()") ShareObjectNode updateRefCountNode) {

            DynamicObject attrs = vector.getAttributes();
            boolean initializeAttrs = initAttrProfile.profile(attrs == null && classAttr != null && classAttr.getLength() != 0);
            if (initializeAttrs) {
                nullAttrProfile.enter();
                attrs = RAttributesLayout.createClass(classAttr);
                vector.initAttributes(attrs);
                updateRefCountNode.execute(classAttr);
            }
            if (nullClassProfile.profile(attrs != null && (classAttr == null || classAttr.getLength() == 0))) {
                removeClassAttrNode.execute(vector);
            } else if (notNullClassProfile.profile(classAttr != null && classAttr.getLength() != 0)) {
                for (int i = 0; i < classAttr.getLength(); i++) {
                    String attr = classAttr.getDataAt(i);
                    if (RRuntime.CLASS_FACTOR.equals(attr)) {
                        // TODO: Isn't this redundant when the same operation is done after the
                        // loop?
                        if (!initializeAttrs) {
                            super.setAttrInAttributable(vector, classAttr, nullAttrProfile, attrStorageProfile, xTypeProfile, updateRefCountNode);
                        }
                        if (vector.getElementClass() != RInteger.class) {
                            // N.B. there used to be conversion to integer under certain
                            // circumstances.
                            // However, it seems that it was dead/obsolete code so it was removed.
                            // Notes: this can only happen if the class is set by hand to some
                            // non-integral vector, i.e. attr(doubles, 'class') <- 'factor'. GnuR
                            // also
                            // does not update the 'class' attr with other, possibly
                            // valid classes when it reaches this error.
                            throw RError.error(RError.SHOW_CALLER2, RError.Message.ADDING_INVALID_CLASS, "factor");
                        }
                    }
                }

                if (!initializeAttrs) {
                    super.setAttrInAttributable(vector, classAttr, nullAttrProfile, attrStorageProfile, xTypeProfile, updateRefCountNode);
                }
            }
        }

        @Specialization(insertBefore = "setAttrInAttributable", guards = "!isRAbstractVector(x)")
        protected void handleAttributable(RAttributable x, @SuppressWarnings("unused") RNull classAttr,
                        @Cached("create()") RemoveClassAttributeNode removeClassNode) {
            removeClassNode.execute(x);
        }

    }

    public abstract static class RemoveClassAttributeNode extends RemoveSpecialAttributeNode {

        protected RemoveClassAttributeNode() {
            super(RRuntime.CLASS_ATTR_KEY);
        }

        public static RemoveClassAttributeNode create() {
            return SpecialAttributesFunctionsFactory.RemoveClassAttributeNodeGen.create();
        }

        @Override
        @Specialization
        protected void removeAttrFallback(DynamicObject attrs) {
            super.removeAttrFallback(attrs);
        }

    }

    public abstract static class GetClassAttributeNode extends GetFixedAttributeNode {

        protected GetClassAttributeNode() {
            super(RRuntime.CLASS_ATTR_KEY);
        }

        public static GetClassAttributeNode create() {
            return SpecialAttributesFunctionsFactory.GetClassAttributeNodeGen.create();
        }

        public final RStringVector getClassAttr(Object x) {
            return (RStringVector) execute(x);
        }

        public final boolean isObject(Object x) {
            return getClassAttr(x) != null ? true : false;
        }

        public final RStringVector getClassHierarchy(RAttributable x) {
            Object v = execute(x);
            RStringVector result = v instanceof RStringVector ? (RStringVector) v : x.getImplicitClass();
            return result != null ? result : RDataFactory.createEmptyStringVector();
        }

        @Specialization(insertBefore = "getAttrFromAttributable")
        protected Object getVectorClass(RAbstractVector x,
                        @Cached("create()") BranchProfile attrNullProfile,
                        @Cached("createBinaryProfile()") ConditionProfile attrStorageProfile,
                        @Cached("createClassProfile()") ValueProfile xTypeProfile) {
            return super.getAttrFromAttributable(x, attrNullProfile, attrStorageProfile, xTypeProfile);
        }

        @Specialization(insertBefore = "getAttrFromAttributable", guards = "!isRAbstractVector(x)")
        @TruffleBoundary
        protected Object getVectorClass(RAbstractContainer x) {
            return x.getClassAttr();
        }

    }

}
