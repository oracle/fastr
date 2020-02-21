/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.nodes.attributes;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.notEmpty;
import static com.oracle.truffle.r.nodes.builtin.casts.fluent.CastNodeBuilder.newCastBuilder;
import static com.oracle.truffle.r.runtime.DSLConfig.getGenericVectorAccessCacheSize;
import static com.oracle.truffle.r.runtime.RError.Message.LENGTH_ZERO_DIM_INVALID;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.access.vector.ExtractListElement;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctionsFactory.GetClassAttributeNodeGen;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctionsFactory.GetDimAttributeNodeGen;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctionsFactory.IsSpecialAttributeNodeGen;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.data.AbstractContainerLibrary;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary;
import com.oracle.truffle.r.runtime.data.nodes.ShareObjectNode;
import com.oracle.truffle.r.nodes.function.opt.UpdateShareableChildValueNode;
import com.oracle.truffle.r.nodes.helpers.MaterializeNode;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.nodes.unary.CastToVectorNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RAttributesLayout;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RScalarVector;
import com.oracle.truffle.r.runtime.data.RSequence;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.GetReadonlyData;
import com.oracle.truffle.r.runtime.nmath.TOMS708;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

/**
 * This class defines a number of nodes used to handle the special attributes, such as names, dims,
 * dimnames and rownames.
 */
public final class SpecialAttributesFunctions {

    /**
     * A node used in guards, for example, to determine whether an attribute is a special one.
     */
    @GenerateUncached
    public abstract static class IsSpecialAttributeNode extends RBaseNode {

        public static IsSpecialAttributeNode create() {
            return IsSpecialAttributeNodeGen.create();
        }

        /**
         * The fast-path method.
         */
        public abstract boolean execute(String name);

        @Specialization
        public boolean exec(String name,
                        @Cached() BranchProfile namesProfile,
                        @Cached() BranchProfile dimProfile,
                        @Cached() BranchProfile dimNamesProfile,
                        @Cached() BranchProfile rowNamesProfile,
                        @Cached() BranchProfile tspProfile,
                        @Cached() BranchProfile commentProfile,
                        @Cached() BranchProfile classProfile) {
            assert Utils.isInterned(name);
            if (Utils.identityEquals(name, RRuntime.NAMES_ATTR_KEY)) {
                namesProfile.enter();
                return true;
            } else if (Utils.identityEquals(name, RRuntime.DIM_ATTR_KEY)) {
                dimProfile.enter();
                return true;
            } else if (Utils.identityEquals(name, RRuntime.DIMNAMES_ATTR_KEY)) {
                dimNamesProfile.enter();
                return true;
            } else if (Utils.identityEquals(name, RRuntime.ROWNAMES_ATTR_KEY)) {
                rowNamesProfile.enter();
                return true;
            } else if (Utils.identityEquals(name, RRuntime.TSP_ATTR_KEY)) {
                tspProfile.enter();
                return true;
            } else if (Utils.identityEquals(name, RRuntime.COMMENT_ATTR_KEY)) {
                commentProfile.enter();
                return true;
            } else if (Utils.identityEquals(name, RRuntime.CLASS_ATTR_KEY)) {
                classProfile.enter();
                return false;
            }
            return false;
        }

        /**
         * The slow-path method.
         */
        public static boolean isSpecialAttribute(String name) {
            assert Utils.isInterned(name);
            return Utils.identityEquals(name, RRuntime.NAMES_ATTR_KEY) ||
                            Utils.identityEquals(name, RRuntime.DIM_ATTR_KEY) ||
                            Utils.identityEquals(name, RRuntime.DIMNAMES_ATTR_KEY) ||
                            Utils.identityEquals(name, RRuntime.ROWNAMES_ATTR_KEY) ||
                            Utils.identityEquals(name, RRuntime.TSP_ATTR_KEY) ||
                            Utils.identityEquals(name, RRuntime.COMMENT_ATTR_KEY) ||
                            Utils.identityEquals(name, RRuntime.CLASS_ATTR_KEY);

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
        private final BranchProfile tspProfile = BranchProfile.create();
        private final BranchProfile commentProfile = BranchProfile.create();

        @Child private SetNamesAttributeNode namesAttrNode;
        @Child private SetDimAttributeNode dimAttrNode;
        @Child private SetDimNamesAttributeNode dimNamesAttrNode;
        @Child private SetRowNamesAttributeNode rowNamesAttrNode;
        @Child private SetTspAttributeNode tspAttrNode;
        @Child private SetCommentAttributeNode commentAttrNode;

        public static GenericSpecialAttributeNode create() {
            return new GenericSpecialAttributeNode();
        }

        public void execute(RAttributable x, String name, Object value) {
            if (Utils.identityEquals(name, RRuntime.NAMES_ATTR_KEY)) {
                namesProfile.enter();
                if (namesAttrNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    namesAttrNode = insert(SetNamesAttributeNode.create());
                }
                namesAttrNode.setAttr(x, value);
            } else if (Utils.identityEquals(name, RRuntime.DIM_ATTR_KEY)) {
                dimProfile.enter();
                if (dimAttrNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    dimAttrNode = insert(SetDimAttributeNode.create());
                }
                dimAttrNode.setAttr(x, value);
            } else if (Utils.identityEquals(name, RRuntime.DIMNAMES_ATTR_KEY)) {
                dimNamesProfile.enter();
                if (dimNamesAttrNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    dimNamesAttrNode = insert(SetDimNamesAttributeNode.create());
                }
                dimNamesAttrNode.setAttr(x, value);
            } else if (Utils.identityEquals(name, RRuntime.ROWNAMES_ATTR_KEY)) {
                rowNamesProfile.enter();
                if (rowNamesAttrNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    rowNamesAttrNode = insert(SetRowNamesAttributeNode.create());
                }
                rowNamesAttrNode.setAttr(x, value);
            } else if (Utils.identityEquals(name, RRuntime.TSP_ATTR_KEY)) {
                tspProfile.enter();
                if (tspAttrNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    tspAttrNode = insert(SetTspAttributeNode.create());
                }
                tspAttrNode.setAttr(x, value);
            } else if (Utils.identityEquals(name, RRuntime.COMMENT_ATTR_KEY)) {
                commentProfile.enter();
                if (commentAttrNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    commentAttrNode = insert(SetCommentAttributeNode.create());
                }
                commentAttrNode.setAttr(x, value);
            } else if (Utils.identityEquals(name, RRuntime.CLASS_ATTR_KEY)) {
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
        if (Utils.identityEquals(name, RRuntime.NAMES_ATTR_KEY)) {
            return SetNamesAttributeNode.create();
        } else if (Utils.identityEquals(name, RRuntime.DIM_ATTR_KEY)) {
            return SetDimAttributeNode.create();
        } else if (Utils.identityEquals(name, RRuntime.DIMNAMES_ATTR_KEY)) {
            return SetDimNamesAttributeNode.create();
        } else if (Utils.identityEquals(name, RRuntime.ROWNAMES_ATTR_KEY)) {
            return SetRowNamesAttributeNode.create();
        } else if (Utils.identityEquals(name, RRuntime.TSP_ATTR_KEY)) {
            return SetTspAttributeNode.create();
        } else if (Utils.identityEquals(name, RRuntime.COMMENT_ATTR_KEY)) {
            return SetCommentAttributeNode.create();
        } else if (Utils.identityEquals(name, RRuntime.CLASS_ATTR_KEY)) {
            return SetClassAttributeNode.create();
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
        assert Utils.isInterned(name);
        if (Utils.identityEquals(name, RRuntime.NAMES_ATTR_KEY)) {
            return GetNamesAttributeNode.create();
        } else if (Utils.identityEquals(name, RRuntime.DIM_ATTR_KEY)) {
            return GetDimAttributeNode.create();
        } else if (Utils.identityEquals(name, RRuntime.DIMNAMES_ATTR_KEY)) {
            return GetDimNamesAttributeNode.create();
        } else if (Utils.identityEquals(name, RRuntime.ROWNAMES_ATTR_KEY)) {
            return GetRowNamesAttributeNode.create();
        } else if (Utils.identityEquals(name, RRuntime.TSP_ATTR_KEY)) {
            return GetTspAttributeNode.create();
        } else if (Utils.identityEquals(name, RRuntime.COMMENT_ATTR_KEY)) {
            return GetCommentAttributeNode.create();
        } else if (Utils.identityEquals(name, RRuntime.CLASS_ATTR_KEY)) {
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

        protected static boolean defaultImplGuard(Object value) {
            return value != RNull.instance;
        }

        @Specialization(guards = "defaultImplGuard(value)")
        protected void setAttrInAttributable(RAttributable x, Object value,
                        @Cached("create()") BranchProfile attrNullProfile,
                        @Cached("create(getAttributeName())") SetFixedPropertyNode setFixedPropertyNode,
                        @Cached("create()") ShareObjectNode updateRefCountNode) {
            setAttrInAttributableInternal(x, value, attrNullProfile, setFixedPropertyNode, updateRefCountNode);
        }
    }

    public abstract static class SetNamesAttributeNode extends SetSpecialAttributeNode {

        private final ConditionProfile nullDimNamesProfile = ConditionProfile.createBinaryProfile();
        @Child private CastNode castValue = newCastBuilder().allowNull().boxPrimitive().asStringVector(true, true, true).buildCastNode();
        @Child private MaterializeNode materializeNode = MaterializeNode.create();

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

        @Override
        protected Object castValue(Object value) {
            // Note: this cast can handle pairlists too, but:
            // TODO: error when some pairlist/list element is not a single value
            // It seems that we assume that names is RStringVector and nothing else in too many
            // places in the system
            return materializeNode.execute(castValue.doCast(value));
        }

        @Specialization(insertBefore = "setAttrInAttributable")
        protected void resetDimNames(RAbstractContainer x, @SuppressWarnings("unused") RNull rnull,
                        @Cached("createNames()") RemoveFixedAttributeNode removeNamesAttrNode) {
            removeNamesAttrNode.execute(x);
        }

        @Specialization(insertBefore = "setAttrInAttributable")
        protected void setNamesInVector(RAbstractVector x, RStringVector newNamesIn,
                        @Cached("createBinaryProfile()") ConditionProfile useDimNamesProfile,
                        @Cached("create()") BranchProfile resizeNames,
                        @Cached("create()") GetDimAttributeNode getDimNode,
                        @Cached("create()") SetDimNamesAttributeNode setDimNamesNode,
                        @Cached("create()") BranchProfile attrNullProfile,
                        @Cached("createNames()") SetFixedPropertyNode setFixedPropertyNode,
                        @Cached("createClassProfile()") ValueProfile xTypeProfile,
                        @Cached("create()") ShareObjectNode updateRefCountNode) {
            RAbstractVector xProfiled = xTypeProfile.profile(x);
            RStringVector newNames = newNamesIn;
            checkNamesLength(xProfiled, newNames);
            // Make names longer to match the length of "x" if necessary
            if (newNames.getLength() < xProfiled.getLength()) {
                resizeNames.enter();
                // TODO: this should preserve "names" (and make them long enough) and maybe other
                // reg attributes?
                newNames = (RStringVector) newNames.copyResized(xProfiled.getLength(), true);
            }
            int[] dimensions = getDimNode.getDimensions(x);
            if (useDimNamesProfile.profile(dimensions != null && dimensions.length == 1)) {
                // for one dimensional array, "names" is really "dimnames[[1]]" (see R
                // documentation for "names" function)
                RList newDimNames = RDataFactory.createList(new Object[]{newNames});
                setDimNamesNode.setDimNames(xProfiled, newDimNames);
            } else {
                DynamicObject attrs = xProfiled.getAttributes();
                if (attrs == null) {
                    attrNullProfile.enter();
                    attrs = RAttributesLayout.createNames(newNames);
                    xProfiled.initAttributes(attrs);
                    return;
                }

                super.setAttrInAttributable(xProfiled, newNames, attrNullProfile, setFixedPropertyNode, updateRefCountNode);
            }
        }

        @Specialization(insertBefore = "setAttrInAttributable", guards = "!isRAbstractVector(x)")
        @TruffleBoundary
        protected void setNamesInContainer(RAbstractContainer x, RStringVector newNamesIn,
                        @Cached("create()") BranchProfile resizeNames,
                        @Cached("createClassProfile()") ValueProfile contClassProfile) {
            RAbstractContainer xProfiled = contClassProfile.profile(x);
            RStringVector newNames = newNamesIn;
            checkNamesLength(xProfiled, newNames);
            if (newNames.getLength() < xProfiled.getLength()) {
                resizeNames.enter();
                // Note: for RPairList and language (which are the only RAbstractContainers that are
                // not RAbstractVector) we should not fill with NAs, but with empty strings that's
                // what GNU-R does.
                newNames = (RStringVector) newNames.copyResized(xProfiled.getLength(), false);
                Object store = newNames.getInternalStore();
                for (int i = newNamesIn.getLength(); i < xProfiled.getLength(); i++) {
                    newNames.setDataAt(store, i, "");
                }

            }
            xProfiled.setNames(newNames);
        }

        private void checkNamesLength(RAbstractContainer target, RStringVector names) {
            if (names.getLength() > target.getLength()) {
                CompilerDirectives.transferToInterpreter();
                throw error(RError.Message.ATTRIBUTE_VECTOR_SAME_LENGTH, RRuntime.NAMES_ATTR_KEY, names.getLength(), target.getLength());
            }
        }
    }

    @GenerateUncached
    public abstract static class GetNamesAttributeNode extends GetFixedAttributeNode {

        @Override
        protected String getAttributeName() {
            return RRuntime.NAMES_ATTR_KEY;
        }

        public static GetNamesAttributeNode create() {
            return SpecialAttributesFunctionsFactory.GetNamesAttributeNodeGen.create();
        }

        @Override
        public abstract Object execute(RAttributable attr);

        public final RStringVector getNames(RAttributable x) {
            return (RStringVector) execute(x);
        }

        @Specialization(insertBefore = "getAttrFromAttributable")
        protected Object getPairListNames(RPairList x) {
            return x.getNames();
        }

        @Specialization(insertBefore = "getAttrFromAttributable")
        protected Object getVectorNames(RAbstractVector x,
                        @Cached("create()") BranchProfile attrNullProfile,
                        @Cached("createNames()") GetFixedPropertyNode getFixedPropertyNode,
                        @Cached("create()") BranchProfile namesNullProfile,
                        @Cached("create()") BranchProfile dimNamesAvlProfile,
                        @Cached("create()") GetDimNamesAttributeNode getDimNames,
                        @Cached("create()") ExtractListElement extractListElement) {
            RStringVector names = (RStringVector) super.getAttrFromAttributable(x, attrNullProfile, getFixedPropertyNode);
            if (names == null) {
                namesNullProfile.enter();
                RList dimNames = getDimNames.getDimNames(x);
                if (dimNames != null && dimNames.getLength() == 1) {
                    dimNamesAvlProfile.enter();
                    Object dimName = extractListElement.execute(dimNames, 0);
                    // RNull for ".Dimnames=list(NULL)"
                    return (dimName != RNull.instance) ? dimName : null;
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

    public abstract static class ExtractNamesAttributeNode extends RBaseNode {

        @Child private GetNamesAttributeNode getNames = GetNamesAttributeNode.create();
        @Child private UpdateShareableChildValueNode updateRefCount = UpdateShareableChildValueNode.create();

        private final ConditionProfile nonNullValue = ConditionProfile.createBinaryProfile();

        public static ExtractNamesAttributeNode create() {
            return SpecialAttributesFunctionsFactory.ExtractNamesAttributeNodeGen.create();
        }

        public abstract RStringVector execute(RAttributable x);

        @Specialization
        protected RStringVector extractNames(RAttributable x) {
            RStringVector names = getNames.getNames(x);
            if (nonNullValue.profile(names != null)) {
                updateRefCount.updateState(x, names);
            }
            return names;
        }

    }

    public abstract static class SetDimAttributeNode extends SetSpecialAttributeNode {

        private final ConditionProfile nullDimProfile = ConditionProfile.createBinaryProfile();
        @Child private VectorDataLibrary dimArgDataLib = VectorDataLibrary.getFactory().createDispatched(getGenericVectorAccessCacheSize());
        private final LoopConditionProfile verifyLoopProfile = LoopConditionProfile.createCountingProfile();
        @Child private CastNode castValue = newCastBuilder().asIntegerVector().mustBe(notEmpty(), LENGTH_ZERO_DIM_INVALID).buildCastNode();

        protected SetDimAttributeNode() {
            super(RRuntime.DIM_ATTR_KEY);
        }

        public static SetDimAttributeNode create() {
            return SpecialAttributesFunctionsFactory.SetDimAttributeNodeGen.create();
        }

        @Override
        protected Object castValue(Object value) {
            return castValue.doCast(value);
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
                        @Cached("createDim()") RemoveFixedAttributeNode removeDimAttrNode,
                        @Cached("create()") SetDimNamesAttributeNode setDimNamesNode) {
            removeDimAttrNode.execute(x);
            setDimNamesNode.setDimNames(x, null);
        }

        @Specialization(insertBefore = "setAttrInAttributable", limit = "getGenericVectorAccessCacheSize()")
        protected void setOneDimInVector(RAbstractVector x, int dim,
                        @CachedLibrary("x") AbstractContainerLibrary vecLib,
                        @Cached("create()") BranchProfile attrNullProfile,
                        @Cached("createDim()") SetFixedPropertyNode setFixedPropertyNode,
                        @Cached("create()") ShareObjectNode updateRefCountNode) {
            int[] dims = new int[]{dim};
            verifyOneDimensions(vecLib.getLength(x), dim);

            RIntVector dimVec = RDataFactory.createIntVector(dims, RDataFactory.COMPLETE_VECTOR);

            DynamicObject attrs = x.getAttributes();
            if (attrs == null) {
                attrNullProfile.enter();
                attrs = RAttributesLayout.createDim(dimVec);
                x.initAttributes(attrs);
                updateRefCountNode.execute(dimVec);
                return;
            }

            super.setAttrInAttributable(x, dimVec, attrNullProfile, setFixedPropertyNode, updateRefCountNode);
        }

        @Specialization(insertBefore = "setAttrInAttributable", limit = "getGenericVectorAccessCacheSize()")
        protected void setDimsInVector(RAbstractVector x, RIntVector dims,
                        @CachedLibrary("x") AbstractContainerLibrary vecLib,
                        @Cached("create()") BranchProfile attrNullProfile,
                        @Cached("createDim()") SetFixedPropertyNode setFixedPropertyNode,
                        @Cached("create()") ShareObjectNode updateRefCountNode) {
            verifyDimensions(vecLib.getLength(x), dims);

            DynamicObject attrs = x.getAttributes();
            if (attrs == null) {
                attrNullProfile.enter();
                attrs = RAttributesLayout.createDim(dims);
                x.initAttributes(attrs);
                updateRefCountNode.execute(dims);
                return;
            }

            super.setAttrInAttributable(x, dims, attrNullProfile, setFixedPropertyNode, updateRefCountNode);
        }

        @Specialization(insertBefore = "setAttrInAttributable", guards = "!isRAbstractVector(x)")
        protected void setDimsInContainerFallback(RAbstractContainer x, RIntVector dims,
                        @Cached("create()") SetDimAttributeNode setDimNode) {
            int[] dimsArr = dims.materialize().getDataCopy();
            setDimNode.setDimensions(x, dimsArr);
        }

        private void verifyOneDimensions(int vectorLength, int dim) {
            int length = dim;
            if (RRuntime.isNA(dim)) {
                throw error(RError.Message.DIMS_CONTAIN_NA);
            } else if (dim < 0) {
                throw error(RError.Message.DIMS_CONTAIN_NEGATIVE_VALUES);
            }
            if (length != vectorLength && vectorLength > 0) {
                throw error(RError.Message.DIMS_DONT_MATCH_LENGTH, length, vectorLength);
            }
        }

        public void verifyDimensions(int vectorLength, RIntVector dims) {
            Object dimsData = dims.getData();
            int dimLen = dimArgDataLib.getLength(dimsData);
            verifyLoopProfile.profileCounted(dimLen);
            int length = 1;
            for (int i = 0; i < dimLen; i++) {
                int dim = dimArgDataLib.getIntAt(dimsData, i);
                if (RRuntime.isNA(dim)) {
                    throw error(RError.Message.DIMS_CONTAIN_NA);
                } else if (dim < 0) {
                    throw error(RError.Message.DIMS_CONTAIN_NEGATIVE_VALUES);
                }
                length *= dim;
            }
            if (length != vectorLength && vectorLength > 0) {
                throw error(RError.Message.DIMS_DONT_MATCH_LENGTH, length, vectorLength);
            }
        }
    }

    public abstract static class GetDimAttributeNode extends GetFixedAttributeNode {

        private final BranchProfile isPairListProfile = BranchProfile.create();
        private final ConditionProfile nullDimsProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile nonEmptyDimsProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile twoDimsOrMoreProfile = ConditionProfile.createBinaryProfile();
        @Child private GetReadonlyData.Int getReadonlyData;

        public static GetDimAttributeNode create() {
            return GetDimAttributeNodeGen.create();
        }

        @Override
        protected String getAttributeName() {
            return RRuntime.DIM_ATTR_KEY;
        }

        // TODO: getDimensions returns a naked array, which is in many places used to create a fresh
        // vector ignoring the reference counting. This should really return a vector and the users
        // should increment its ref-count if they want to put it into other
        // attributes/list/environment/... This way, we wouldn't need to call getReadonlyData, which
        // may copy the contents.

        public final int[] getDimensions(RAttributable x) {
            // Let's handle the following two types directly so as to avoid wrapping and unwrapping
            // RIntVector. The getContainerDims spec would be invoked otherwise.
            if (x instanceof RPairList) {
                isPairListProfile.enter();
                return ((RPairList) x).getDimensions();
            }
            if (getReadonlyData == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getReadonlyData = insert(GetReadonlyData.Int.create());
            }
            RIntVector dims = (RIntVector) execute(x);
            return nullDimsProfile.profile(dims == null) ? null : getReadonlyData.execute(dims);
        }

        public static boolean isArray(int[] dimensions) {
            return dimensions != null && dimensions.length > 0;
        }

        public static boolean isMatrix(int[] dimensions) {
            return dimensions != null && dimensions.length == 2;
        }

        public static boolean isArray(RIntVector dimensions) {
            return dimensions != null && dimensions.getLength() > 0;
        }

        public static boolean isMatrix(RIntVector dimensions) {
            return dimensions != null && dimensions.getLength() == 2;
        }

        public final boolean isArray(RAbstractVector vector) {
            RIntVector dims = (RIntVector) execute(vector);
            return nullDimsProfile.profile(dims == null) ? false : dims.getLength() > 0;
        }

        public final boolean isMatrix(RAbstractVector vector) {
            RIntVector dims = (RIntVector) execute(vector);
            return nullDimsProfile.profile(dims == null) ? false : dims.getLength() == 2;
        }

        public final boolean isSquareMatrix(RAbstractVector vector) {
            RIntVector dims = (RIntVector) execute(vector);
            if (nullDimsProfile.profile(dims == null) || dims.getLength() != 2) {
                return false;
            }
            return dims.getDataAt(0) == dims.getDataAt(1);
        }

        @Specialization(insertBefore = "getAttrFromAttributable", guards = "isScalarOrSequence(x)")
        protected Object getScalarVectorDims(@SuppressWarnings("unused") RAbstractContainer x) {
            return null;
        }

        @Specialization(insertBefore = "getAttrFromAttributable", guards = "!isScalarOrSequence(x)")
        protected Object getVectorDims(RAbstractVector x,
                        @Cached("create()") BranchProfile attrNullProfile,
                        @Cached("createDim()") GetFixedPropertyNode getFixedPropertyNode) {
            return super.getAttrFromAttributable(x, attrNullProfile, getFixedPropertyNode);
        }

        @Specialization(insertBefore = "getAttrFromAttributable")
        protected Object getContainerDims(RAbstractContainer x,
                        @Cached("createClassProfile()") ValueProfile xTypeProfile,
                        @Cached("createBinaryProfile()") ConditionProfile nullResultProfile) {
            int[] res = xTypeProfile.profile(x).getDimensions();
            return nullResultProfile.profile(res == null) ? null : RDataFactory.createIntVector(res, true);
        }

        public int nrows(Object x) {
            if (x instanceof RAbstractContainer) {
                RAbstractContainer xa = (RAbstractContainer) x;
                int[] dims = getDimensions(xa);
                if (nonEmptyDimsProfile.profile(dims != null && dims.length > 0)) {
                    return dims[0];
                } else {
                    return xa.getLength();
                }
            } else {
                throw error(RError.Message.OBJECT_NOT_MATRIX);
            }
        }

        public int ncols(Object x) {
            if (x instanceof RAbstractContainer) {
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
                throw error(RError.Message.OBJECT_NOT_MATRIX);
            }
        }

        protected static boolean isScalarOrSequence(RAbstractContainer x) {
            return x instanceof RScalarVector || x instanceof RSequence;
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
                        @Cached("createDimNames()") RemoveFixedAttributeNode removeDimNamesAttrNode) {
            removeDimNamesAttrNode.execute(x);
        }

        @Specialization(insertBefore = "setAttrInAttributable")
        protected void setDimNamesInVector(RAbstractContainer x, RList newDimNames,
                        @Cached("create()") GetDimAttributeNode getDimNode,
                        @Cached("createCountingProfile()") LoopConditionProfile loopProfile,
                        @Cached("create()") BranchProfile nullDimProfile,
                        @Cached("createDimNames()") SetFixedPropertyNode setFixedPropertyNode,
                        @Cached("create()") BranchProfile resizeDimsProfile,
                        @Cached("create()") BranchProfile attrNullProfile,
                        @Cached("create()") ShareObjectNode updateRefCountNode) {
            int[] dimensions = getDimNode.getDimensions(x);
            if (dimensions == null) {
                throw error(RError.Message.DIMNAMES_NONARRAY);
            }
            int newDimNamesLength = newDimNames.getLength();
            if (newDimNamesLength > dimensions.length) {
                CompilerDirectives.transferToInterpreter();
                throw error(RError.Message.DIMNAMES_DONT_MATCH_DIMS, newDimNamesLength, dimensions.length);
            }

            loopProfile.profileCounted(newDimNamesLength);
            for (int i = 0; loopProfile.inject(i < newDimNamesLength); i++) {
                Object dimObject = newDimNames.getDataAt(i);

                if (dimObject instanceof RStringVector && ((RStringVector) dimObject).getLength() == 0) {
                    nullDimProfile.enter();
                    newDimNames.updateDataAt(i, RNull.instance, null);
                } else if ((dimObject instanceof String && dimensions[i] != 1) ||
                                (dimObject instanceof RStringVector && !isValidDimLength((RStringVector) dimObject, dimensions[i]))) {
                    CompilerDirectives.transferToInterpreter();
                    throw error(RError.Message.DIMNAMES_DONT_MATCH_EXTENT, i + 1);
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

            if (x.getAttributes() == null) {
                attrNullProfile.enter();
                x.initAttributes(RAttributesLayout.createDimNames(resDimNames));
                updateRefCountNode.execute(resDimNames);
                return;
            }

            super.setAttrInAttributable(x, resDimNames, attrNullProfile, setFixedPropertyNode, updateRefCountNode);
        }

        private static boolean isValidDimLength(RStringVector x, int expectedDim) {
            int len = x.getLength();
            return len == 0 || len == expectedDim;
        }
    }

    @GenerateUncached
    public abstract static class GetDimNamesAttributeNode extends GetFixedAttributeNode {

        public static GetDimNamesAttributeNode create() {
            return SpecialAttributesFunctionsFactory.GetDimNamesAttributeNodeGen.create();
        }

        @Override
        protected String getAttributeName() {
            return RRuntime.DIMNAMES_ATTR_KEY;
        }

        public final RList getDimNames(RAttributable x) {
            Object result = execute(x);
            return result == RNull.instance ? null : (RList) result;
        }

        @Specialization(insertBefore = "getAttrFromAttributable")
        protected Object getVectorDimNames(RAbstractContainer x,
                        @Cached("create()") BranchProfile attrNullProfile,
                        @Cached("createDimNames()") GetFixedPropertyNode getFixedPropertyNode) {
            return super.getAttrFromAttributable(x, attrNullProfile, getFixedPropertyNode);
        }
    }

    public abstract static class ExtractDimNamesAttributeNode extends RBaseNode {

        @Child private GetDimNamesAttributeNode getDimNames = GetDimNamesAttributeNode.create();
        @Child private UpdateShareableChildValueNode updateRefCount = UpdateShareableChildValueNode.create();

        private final ConditionProfile nonNullValue = ConditionProfile.createBinaryProfile();

        public static ExtractDimNamesAttributeNode create() {
            return SpecialAttributesFunctionsFactory.ExtractDimNamesAttributeNodeGen.create();
        }

        public abstract RList execute(RAttributable x);

        @Specialization
        protected RList extractDimNames(RAttributable x) {
            RList dimNames = getDimNames.getDimNames(x);
            if (nonNullValue.profile(dimNames != null)) {
                updateRefCount.updateState(x, dimNames);
            }
            return dimNames;
        }

    }

    public abstract static class InitDimsNamesDimNamesNode extends RBaseNode {

        private final ConditionProfile doAnythingProfile = ConditionProfile.createBinaryProfile();

        @Child private GetDimAttributeNode getDimNode;
        @Child private ExtractNamesAttributeNode extractNamesNode;
        @Child private ExtractDimNamesAttributeNode extractDimNamesNode;

        protected InitDimsNamesDimNamesNode() {
        }

        public static InitDimsNamesDimNamesNode create() {
            return SpecialAttributesFunctionsFactory.InitDimsNamesDimNamesNodeGen.create();
        }

        public void initAttributes(RAbstractContainer x, int[] dimensions, RStringVector names, RList dimNames) {
            if (doAnythingProfile.profile(dimensions != null || names != null || dimNames != null)) {
                execute(x, dimensions, names, dimNames);
            }
        }

        public void initAttributes(RAbstractContainer x, RAbstractContainer source) {
            if (getDimNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getDimNode = insert(GetDimAttributeNode.create());
            }
            if (extractNamesNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                extractNamesNode = insert(ExtractNamesAttributeNode.create());
            }
            if (extractDimNamesNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                extractDimNamesNode = insert(ExtractDimNamesAttributeNode.create());
            }
            this.initAttributes(x, getDimNode.getDimensions(source), extractNamesNode.execute(source), extractDimNamesNode.execute(source));
        }

        public abstract void execute(RAbstractContainer x, int[] dimensions, RStringVector names, RList dimNames);

        @Specialization
        protected void initContainerAttributes(RAbstractContainer x, int[] dimensions, RStringVector initialNames, RList initialDimNames,
                        @Cached("create()") ShareObjectNode shareObjectNode) {
            RStringVector names = initialNames;
            RList dimNames = initialDimNames;
            assert names != x;
            assert dimNames != x;
            DynamicObject attrs = x.getAttributes();
            if (dimNames != null) {
                shareObjectNode.execute(dimNames);
            }
            if (names != null) {
                assert names.getLength() == x.getLength() : "size mismatch: names.length=" + names.getLength() + " vs. length=" + x.getLength();
                if (dimensions != null && dimensions.length == 1) {
                    // one-dimensional arrays do not have names, only dimnames with one value
                    if (dimNames == null) {
                        shareObjectNode.execute(names);
                        dimNames = RDataFactory.createList(new Object[]{names});
                    }
                    names = null;
                } else {
                    shareObjectNode.execute(names);
                }
            }

            if (attrs == null) {
                if (dimensions != null) {
                    RIntVector dimensionsVector = RDataFactory.createIntVector(dimensions, true);
                    if (dimNames != null) {
                        attrs = RAttributesLayout.createDimAndDimNames(dimensionsVector, dimNames);
                        if (names != null) {
                            attrs.define(RRuntime.NAMES_ATTR_KEY, names);
                        }
                    } else {
                        if (names != null) {
                            attrs = RAttributesLayout.createNamesAndDim(names, dimensionsVector);
                        } else {
                            attrs = RAttributesLayout.createDim(dimensionsVector);
                        }
                    }
                } else {
                    if (dimNames != null) {
                        attrs = RAttributesLayout.createDimNames(dimNames);
                        if (names != null) {
                            attrs.define(RRuntime.NAMES_ATTR_KEY, names);
                        }
                    } else {
                        assert (names != null); // only called with at least one attr != null
                        attrs = RAttributesLayout.createNames(names);
                    }
                }
                x.initAttributes(attrs);
            } else { // attrs != null
                if (dimensions != null) {
                    RIntVector dimensionsVector = RDataFactory.createIntVector(dimensions, true);
                    x.setAttr(RRuntime.DIM_ATTR_KEY, dimensionsVector);
                }
                if (names != null) {
                    x.setAttr(RRuntime.NAMES_ATTR_KEY, names);
                }
                if (dimNames != null) {
                    x.setAttr(RRuntime.DIMNAMES_ATTR_KEY, dimNames);
                }
            }
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
        protected void resetRowNames(RAbstractVector x, @SuppressWarnings("unused") RNull rnull,
                        @Cached("createRowNames()") RemoveFixedAttributeNode removeRowNamesAttrNode) {
            removeRowNamesAttrNode.execute(x);
        }

        @Specialization(insertBefore = "setAttrInAttributable")
        protected void setRowNamesInVector(RAbstractContainer x, RAbstractVector newRowNames,
                        @Cached("create()") BranchProfile attrNullProfile,
                        @Cached("createRowNames()") SetFixedPropertyNode setFixedPropertyNode,
                        @Cached("create()") ShareObjectNode updateRefCountNode) {
            if (x.getAttributes() == null) {
                attrNullProfile.enter();
                x.initAttributes(RAttributesLayout.createRowNames(newRowNames));
                updateRefCountNode.execute(newRowNames);
                return;
            }
            setAttrInAttributable(x, newRowNames, attrNullProfile, setFixedPropertyNode, updateRefCountNode);
        }
    }

    @GenerateUncached
    public abstract static class GetRowNamesAttributeNode extends GetFixedAttributeNode {

        public static GetRowNamesAttributeNode create() {
            return SpecialAttributesFunctionsFactory.GetRowNamesAttributeNodeGen.create();
        }

        @Override
        protected String getAttributeName() {
            return RRuntime.ROWNAMES_ATTR_KEY;
        }

        public Object getRowNames(RAbstractContainer x) {
            return execute(x);
        }

        @Specialization(insertBefore = "getAttrFromAttributable", guards = "isScalarOrSequence(x)")
        protected Object getScalarVectorRowNames(@SuppressWarnings("unused") RAbstractContainer x) {
            return null;
        }

        @Specialization(insertBefore = "getAttrFromAttributable", guards = "!isScalarOrSequence(x)")
        protected Object getVectorRowNames(RAbstractContainer x,
                        @Cached("create()") BranchProfile attrNullProfile,
                        @Cached("createRowNames()") GetFixedPropertyNode getFixedPropertyNode) {
            return super.getAttrFromAttributable(x, attrNullProfile, getFixedPropertyNode);
        }

        /**
         * If <code>row.names</code> are in the GnuR compact format they will be converted to an int
         * sequence.
         */
        public static Object convertRowNamesToSeq(Object rowNames) {
            if (rowNames == RNull.instance) {
                return RNull.instance;
            } else {
                if (rowNames instanceof RIntVector) {
                    RIntVector vec = (RIntVector) rowNames;
                    if (vec.getLength() == 2 && RRuntime.isNA(vec.getDataAt(0))) {
                        return RDataFactory.createIntSequence(1, 1, Math.abs(vec.getDataAt(1)));
                    }
                } else if (rowNames instanceof RAbstractDoubleVector) {
                    RAbstractDoubleVector vec = (RAbstractDoubleVector) rowNames;
                    if (vec.getLength() == 2 && RRuntime.isNA(vec.getDataAt(0))) {
                        return RDataFactory.createIntSequence(1, 1, Math.abs((int) (vec.getDataAt(1))));
                    }
                }
                return rowNames;
            }
        }

        public static Object ensureRowNamesCompactFormat(Object rowNames) {
            if (rowNames == RNull.instance) {
                return RNull.instance;
            } else {
                if (rowNames instanceof RAbstractDoubleVector) {
                    RAbstractDoubleVector vec = (RAbstractDoubleVector) rowNames;
                    if (vec.getLength() == 2 && RRuntime.isNA(vec.getDataAt(0))) {
                        return RDataFactory.createIntVector(new int[]{RRuntime.INT_NA, (int) vec.getDataAt(1)}, false);
                    }
                }
                return rowNames;
            }
        }

        protected static boolean isScalarOrSequence(RAbstractContainer x) {
            return x instanceof RScalarVector || x instanceof RSequence;
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
                        @Cached("createClass()") SetFixedPropertyNode setFixedPropertyNode,
                        @Cached("createBinaryProfile()") ConditionProfile notNullClassProfile,
                        @Cached("create()") ShareObjectNode updateRefCountNode) {
            handleVector(vector, null, removeClassAttrNode, initAttrProfile, nullAttrProfile, nullClassProfile, setFixedPropertyNode, notNullClassProfile, updateRefCountNode);
        }

        @Specialization(insertBefore = "setAttrInAttributable")
        protected <T> void handleVector(RAbstractVector vector, RStringVector classAttr,
                        @Cached("createClass()") RemoveFixedAttributeNode removeClassAttrNode,
                        @Cached("createBinaryProfile()") ConditionProfile initAttrProfile,
                        @Cached("create()") BranchProfile nullAttrProfile,
                        @Cached("createBinaryProfile()") ConditionProfile nullClassProfile,
                        @Cached("createClass()") SetFixedPropertyNode setFixedPropertyNode,
                        @Cached("createBinaryProfile()") ConditionProfile notNullClassProfile,
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
                        if (!(vector instanceof RIntVector)) {
                            CompilerDirectives.transferToInterpreter();
                            throw error(RError.Message.ADDING_INVALID_CLASS, "factor");
                        }
                    }
                }

                if (!initializeAttrs) {
                    super.setAttrInAttributable(vector, classAttr, nullAttrProfile, setFixedPropertyNode, updateRefCountNode);
                }
            }
        }

        @Specialization(insertBefore = "setAttrInAttributable", guards = "!isRAbstractVector(x)")
        protected void handleAttributable(RAttributable x, @SuppressWarnings("unused") RNull classAttr,
                        @Cached("createClass()") RemoveFixedAttributeNode removeClassNode) {
            removeClassNode.execute(x);
        }
    }

    @GenerateUncached
    public abstract static class GetClassAttributeNode extends GetFixedAttributeNode {

        public static GetClassAttributeNode create() {
            return GetClassAttributeNodeGen.create();
        }

        public static GetClassAttributeNode getUncached() {
            return GetClassAttributeNodeGen.getUncached();
        }

        @Override
        protected String getAttributeName() {
            return RRuntime.CLASS_ATTR_KEY;
        }

        public final RStringVector getClassAttr(RAttributable x) {
            return (RStringVector) execute(x);
        }

        public final boolean isObject(RAttributable x) {
            return getClassAttr(x) != null;
        }
    }

    public abstract static class ExtractClassAttributeNode extends RBaseNode {

        @Child private GetClassAttributeNode getClassAttr = GetClassAttributeNode.create();
        @Child private UpdateShareableChildValueNode updateRefCount = UpdateShareableChildValueNode.create();

        private final ConditionProfile nonNullValue = ConditionProfile.createBinaryProfile();

        public static ExtractClassAttributeNode create() {
            return SpecialAttributesFunctionsFactory.ExtractClassAttributeNodeGen.create();
        }

        public abstract RStringVector execute(RAttributable x);

        @Specialization
        protected RStringVector extractClassAttr(RAttributable x) {
            RStringVector classAttr = getClassAttr.getClassAttr(x);
            if (nonNullValue.profile(classAttr != null)) {
                updateRefCount.updateState(x, classAttr);
            }
            return classAttr;
        }

    }

    public abstract static class SetTspAttributeNode extends SetSpecialAttributeNode {

        private final ConditionProfile nullTspProfile = ConditionProfile.createBinaryProfile();

        protected SetTspAttributeNode() {
            super(RRuntime.TSP_ATTR_KEY);
        }

        public static SetTspAttributeNode create() {
            return SpecialAttributesFunctionsFactory.SetTspAttributeNodeGen.create();
        }

        public void setTsp(RAttributable x, RAbstractDoubleVector tsp) {
            if (nullTspProfile.profile(tsp == null)) {
                execute(x, RNull.instance);
            } else {
                if (tsp.getLength() != 3) {
                    throw error(RError.Message.TSP_NUMERIC_LENGTH3);
                }
                double start = tsp.getDataAt(0);
                double end = tsp.getDataAt(1);
                double frequency = tsp.getDataAt(2);
                if (frequency <= 0) {
                    throw error(RError.Message.INVALID_TSP);
                }
                int n = RRuntime.nrows(x);
                if (n == 0) {
                    throw error(RError.Message.CANNOT_ASSIGN_EMPTY_VECTOR, "tsp");
                }
                if (TOMS708.fabs(end - start - (n - 1) / frequency) > 1.e-5) {
                    throw error(RError.Message.INVALID_TSP);
                }
                execute(x, tsp);
            }
        }

        @Specialization(insertBefore = "setAttrInAttributable")
        protected void resetTsp(RAbstractVector x, @SuppressWarnings("unused") RNull rnull,
                        @Cached("createTsp()") RemoveFixedAttributeNode removeTspAttrNode) {
            removeTspAttrNode.execute(x);
        }

        @Specialization(insertBefore = "setAttrInAttributable")
        protected void setTspInVector(RAttributable x, RAbstractDoubleVector newTsp,
                        @Cached("create()") BranchProfile attrNullProfile,
                        @Cached("createTsp()") SetFixedPropertyNode setFixedPropertyNode,
                        @Cached("create()") ShareObjectNode updateRefCountNode) {
            if (x.getAttributes() == null) {
                attrNullProfile.enter();
                x.initAttributes(RAttributesLayout.createTsp(newTsp));
                updateRefCountNode.execute(newTsp);
                return;
            }
            setAttrInAttributable(x, newTsp, attrNullProfile, setFixedPropertyNode, updateRefCountNode);
        }
    }

    public abstract static class GetTspAttributeNode extends GetFixedAttributeNode {

        public static GetTspAttributeNode create() {
            return SpecialAttributesFunctionsFactory.GetTspAttributeNodeGen.create();
        }

        @Override
        protected String getAttributeName() {
            return RRuntime.TSP_ATTR_KEY;
        }

        public RAbstractDoubleVector getTsp(RAttributable x) {
            return (RAbstractDoubleVector) execute(x);
        }

        @Specialization(insertBefore = "getAttrFromAttributable")
        protected Object getVectorTsp(RAbstractContainer x,
                        @Cached("create()") BranchProfile attrNullProfile,
                        @Cached("createTsp()") GetFixedPropertyNode getFixedPropertyNode,
                        @Cached("createBinaryProfile()") ConditionProfile nullTspProfile) {
            Object res = super.getAttrFromAttributable(x, attrNullProfile, getFixedPropertyNode);
            return nullTspProfile.profile(res == null) ? RNull.instance : res;
        }

    }

    public abstract static class SetCommentAttributeNode extends SetSpecialAttributeNode {

        private final ConditionProfile nullCommentProfile = ConditionProfile.createBinaryProfile();
        private final NACheck naCheck = NACheck.create();

        @Child private CastToVectorNode castVector;

        protected SetCommentAttributeNode() {
            super(RRuntime.COMMENT_ATTR_KEY);
        }

        public static SetCommentAttributeNode create() {
            return SpecialAttributesFunctionsFactory.SetCommentAttributeNodeGen.create();
        }

        public void setComment(RAttributable x, Object value) {
            if (nullCommentProfile.profile(value == null)) {
                execute(x, RNull.instance);
            } else {
                Object comment = null;
                if (value == RNull.instance) {
                    comment = value;
                } else if (value instanceof String) {
                    if (castVector == null) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        castVector = insert(CastToVectorNode.create());
                    }
                    comment = castVector.doCast(value);
                } else if (value instanceof RAbstractStringVector) {
                    RAbstractStringVector str = (RAbstractStringVector) value;
                    naCheck.enable(str);
                    for (int j = str.getLength() - 1; j >= 0; j--) {
                        if (!naCheck.check(str.getDataAt(j))) {
                            comment = value;
                            break;
                        }
                    }
                }
                if (comment == null) {
                    throw error(RError.Message.SET_INVALID_ATTR, "comment");
                }
                execute(x, comment);
            }
        }

        @Specialization(insertBefore = "setAttrInAttributable")
        protected void resetComment(RAbstractVector x, @SuppressWarnings("unused") RNull rnull,
                        @Cached("createComment()") RemoveFixedAttributeNode removeCommentAttrNode) {
            removeCommentAttrNode.execute(x);
        }

        @Specialization(insertBefore = "setAttrInAttributable")
        protected void setCommentInVector(RAttributable x, RAbstractVector newComment,
                        @Cached("create()") BranchProfile attrNullProfile,
                        @Cached("createComment()") SetFixedPropertyNode setFixedPropertyNode,
                        @Cached("create()") ShareObjectNode updateRefCountNode) {
            if (x.getAttributes() == null) {
                attrNullProfile.enter();
                x.initAttributes(RAttributesLayout.createComment(newComment));
                updateRefCountNode.execute(newComment);
                return;
            }
            setAttrInAttributable(x, newComment, attrNullProfile, setFixedPropertyNode, updateRefCountNode);
        }
    }

    public abstract static class GetCommentAttributeNode extends GetFixedAttributeNode {

        public static GetCommentAttributeNode create() {
            return SpecialAttributesFunctionsFactory.GetCommentAttributeNodeGen.create();
        }

        @Override
        protected String getAttributeName() {
            return RRuntime.COMMENT_ATTR_KEY;
        }

        public RAbstractStringVector getComment(RAttributable x) {
            return (RAbstractStringVector) execute(x);
        }

        @Specialization(insertBefore = "getAttrFromAttributable")
        protected Object getComment(RAbstractContainer x,
                        @Cached("create()") BranchProfile attrNullProfile,
                        @Cached("createComment()") GetFixedPropertyNode getFixedPropertyNode,
                        @Cached("createBinaryProfile()") ConditionProfile nullCommentProfile) {
            Object res = super.getAttrFromAttributable(x, attrNullProfile, getFixedPropertyNode);
            return nullCommentProfile.profile(res == null) ? RNull.instance : res;
        }

    }

}
