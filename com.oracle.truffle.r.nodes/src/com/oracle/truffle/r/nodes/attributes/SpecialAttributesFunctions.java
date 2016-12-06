/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctionsFactory.GetDimAttributeNodeGen;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RInteger;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
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
     * A factory method for creating a node handling the given special attribute.
     *
     * @param name the special attribute name
     * @return the node
     */
    public static SetSpecialAttributeNode createSpecialAttributeNode(String name) {
        assert name.intern() == name;
        if (name == RRuntime.NAMES_ATTR_KEY) {
            return SetNamesAttributeNode.create();
        } else if (name == RRuntime.DIM_ATTR_KEY) {
            return SetDimAttributeNode.create();
        } else if (name == RRuntime.DIMNAMES_ATTR_KEY) {
            return SpecialAttributesFunctions.SetDimNamesAttributeNode.create();
        } else if (name == RRuntime.ROWNAMES_ATTR_KEY) {
            return SpecialAttributesFunctions.SetRowNamesAttributeNode.create();
        } else if (name == RRuntime.CLASS_ATTR_KEY) {
            throw RInternalError.unimplemented("The \"class\" attribute should be set using a separate method");
        } else {
            throw RInternalError.shouldNotReachHere();
        }
    }

    /**
     * The base class for the nodes setting values to special attributes.
     */
    public abstract static class SetSpecialAttributeNode extends RBaseNode {

        public abstract void execute(RAttributable x, Object attrValue);

    }

    public abstract static class SetNamesAttributeNode extends SetSpecialAttributeNode {

        public static SetNamesAttributeNode create() {
            return SpecialAttributesFunctionsFactory.SetNamesAttributeNodeGen.create();
        }

        @Specialization
        protected void setNamesInContainer(RAbstractContainer x, RStringVector names, @Cached("createClassProfile()") ValueProfile contClassProfile) {
            RAbstractContainer xProfiled = contClassProfile.profile(x);
            xProfiled.setNames(names);
        }
    }

    public abstract static class SetDimAttributeNode extends SetSpecialAttributeNode {

        public static SetDimAttributeNode create() {
            return SpecialAttributesFunctionsFactory.SetDimAttributeNodeGen.create();
        }

        @Specialization
        protected void setOneDimInContainer(RAbstractContainer x, Integer dim, @Cached("createClassProfile()") ValueProfile contClassProfile) {
            RAbstractContainer xProfiled = contClassProfile.profile(x);
            // xProfiled.setDimensions(new int[]{dim});
            xProfiled.setAttr(RRuntime.DIM_ATTR_KEY, new int[]{dim});
        }

        @Specialization
        protected void setDimsInContainer(RAbstractContainer x, RAbstractIntVector dims, @Cached("createClassProfile()") ValueProfile contClassProfile) {
            RAbstractContainer xProfiled = contClassProfile.profile(x);
            xProfiled.setDimensions(dims.materialize().getDataCopy());
        }

    }

    public abstract static class GetDimAttributeNode extends GetFixedAttributeNode {

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
            RIntVector dims = (RIntVector) execute(x);
            return nullDimsProfile.profile(dims == null) ? null : dims.getInternalStore();
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

        public static SetDimNamesAttributeNode create() {
            return SpecialAttributesFunctionsFactory.SetDimNamesAttributeNodeGen.create();
        }

        @Specialization
        protected void setDimNamesInContainer(RAbstractContainer x, RList dimNames, @Cached("createClassProfile()") ValueProfile contClassProfile) {
            RAbstractContainer xProfiled = contClassProfile.profile(x);
            xProfiled.setDimNames(dimNames);
        }

    }

    public abstract static class SetRowNamesAttributeNode extends SetSpecialAttributeNode {

        public static SetRowNamesAttributeNode create() {
            return SpecialAttributesFunctionsFactory.SetRowNamesAttributeNodeGen.create();
        }

        @Specialization
        protected void setRowNamesInContainer(RAbstractContainer x, RAbstractVector rowNames, @Cached("createClassProfile()") ValueProfile contClassProfile) {
            RAbstractContainer xProfiled = contClassProfile.profile(x);
            xProfiled.setRowNames(rowNames);
        }

    }

    public abstract static class SetClassAttributeNode extends SetSpecialAttributeNode {

        public static SetClassAttributeNode create() {
            return SpecialAttributesFunctionsFactory.SetClassAttributeNodeGen.create();
        }

        public void reset(RAttributable x) {
            execute(x, RNull.instance);
        }

        @Specialization
        protected <T> void handleVectorNullClass(RVector<T> vector, @SuppressWarnings("unused") RNull classAttr,
                        @Cached("createClass()") RemoveFixedAttributeNode removeClassAttrNode,
                        @Cached("createClass()") SetFixedAttributeNode setClassAttrNode,
                        @Cached("create()") BranchProfile nullAttrProfile,
                        @Cached("createBinaryProfile()") ConditionProfile nullClassProfile,
                        @Cached("createBinaryProfile()") ConditionProfile notNullClassProfile) {
            handleVector(vector, null, removeClassAttrNode, setClassAttrNode, nullAttrProfile, nullClassProfile, notNullClassProfile);
        }

        @Specialization
        protected <T> void handleVector(RVector<T> vector, RStringVector classAttr,
                        @Cached("createClass()") RemoveFixedAttributeNode removeClassAttrNode,
                        @Cached("createClass()") SetFixedAttributeNode setClassAttrNode,
                        @Cached("create()") BranchProfile nullAttrProfile,
                        @Cached("createBinaryProfile()") ConditionProfile nullClassProfile,
                        @Cached("createBinaryProfile()") ConditionProfile notNullClassProfile) {

            DynamicObject attrs = vector.getAttributes();
            if (attrs == null && classAttr != null && classAttr.getLength() != 0) {
                nullAttrProfile.enter();
                attrs = vector.initAttributes();
            }
            if (nullClassProfile.profile(attrs != null && (classAttr == null || classAttr.getLength() == 0))) {
                removeAttributeMapping(vector, attrs, removeClassAttrNode);
            } else if (notNullClassProfile.profile(classAttr != null && classAttr.getLength() != 0)) {
                for (int i = 0; i < classAttr.getLength(); i++) {
                    String attr = classAttr.getDataAt(i);
                    if (RRuntime.CLASS_FACTOR.equals(attr)) {
                        // TODO: Isn't this redundant when the same operation is done after the
                        // loop?
                        setClassAttrNode.execute(attrs, classAttr);
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
                setClassAttrNode.execute(attrs, classAttr);
            }
        }

        @Specialization
        protected void handleAttributable(RAttributable x, @SuppressWarnings("unused") RNull classAttr) {
            x.setClassAttr(null);
        }

        @Specialization
        protected void handleAttributable(RAttributable x, RStringVector classAttr) {
            x.setClassAttr(classAttr);
        }

        private static void removeAttributeMapping(RAttributable x, DynamicObject attrs, RemoveFixedAttributeNode removeClassAttrNode) {
            if (attrs != null) {
                removeClassAttrNode.execute(attrs);
                if (attrs.isEmpty()) {
                    x.initAttributes(null);
                }
            }
        }

    }

}
