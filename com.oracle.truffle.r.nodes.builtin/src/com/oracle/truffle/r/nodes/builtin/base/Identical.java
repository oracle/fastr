/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import java.util.Iterator;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.IdenticalFactory.IdenticalInternalNodeGen;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.CharSXPWrapper;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RAttributesLayout;
import com.oracle.truffle.r.runtime.data.RExternalPtr;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RInteropScalar;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RS4Object;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.RTypes;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary;
import com.oracle.truffle.r.runtime.data.model.RAbstractListBaseVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.attributes.GetAttributeNode;
import com.oracle.truffle.r.runtime.data.nodes.attributes.IterableAttributeNode;
import com.oracle.truffle.r.runtime.data.nodes.attributes.IterableAttributeNodeGen;
import com.oracle.truffle.r.runtime.data.nodes.attributes.SpecialAttributesFunctions.GetRowNamesAttributeNode;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.nodes.IdenticalVisitor;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

/**
 * Internal part of {@code identical}. The default values for args after {@code x} and {@code y} are
 * all default to {@code TRUE/FALSE} in the R wrapper.
 *
 * TODO Implement the full set of types. This will require refactoring the code so that a generic
 * "identical" function can be called recursively to handle lists and language objects (and
 * closures) GnuR compares attributes also. The general case is therefore slow but the fast path
 * needs to be fast! The five defaulted logical arguments are supposed to be cast to logical and
 * checked for NA (regardless of whether they are used).
 *
 * TODO implement {@code ignoreSrcref}.
 *
 * N.B. GNU R allows all the {@code ignoreXXX} options to be optional. However, the only call to the
 * {@code .Internal} in the GNU R code base is from the closure wrapper, which passes all of them.
 * There may be some package that calls the {@code .Internal} directly with less, however.
 */
@RBuiltin(name = "identical", kind = INTERNAL, parameterNames = {"x", "y", "num.eq", "single.NA", "attrib.as.set", "ignore.bytecode", "ignore.environment", "ignore.srcref"}, behavior = PURE)
public final class Identical extends RBuiltinNode.Arg8 {

    private static final int MAX_DEPTH = 5;

    static boolean isCached(int depth) {
        return depth < DSLConfig.getCacheSize(MAX_DEPTH);
    }

    @Child private IdenticalInternal identicalInternalNode = isCached(0) ? IdenticalInternal.create() : IdenticalInternalNodeGen.getUncached();

    @Override
    public Object execute(VirtualFrame frame, Object x, Object y, Object numEq, Object singleNA, Object attribAsSet, Object ignoreBytecode, Object ignoreEnvironment,
                    Object ignoreSrcref) {
        return identicalInternalNode.executeByte(x, y, numEq, singleNA, attribAsSet, ignoreBytecode, ignoreEnvironment, ignoreSrcref, 0);
    }

    public byte executeByte(Object x, Object y, boolean numEq, boolean singleNA, boolean attribAsSet, boolean ignoreBytecode, boolean ignoreEnvironment, boolean ignoreSrcref) {
        return identicalInternalNode.executeByte(x, y, numEq, singleNA, attribAsSet, ignoreBytecode, ignoreEnvironment, ignoreSrcref, 0);
    }

    // Note: the execution of the recursive cases is not done directly and not through RCallNode or
    // similar, this means that the visibility handling is left to us.

    public static final class IdenticalRecursiveAttrNode extends RBaseNode {

        @Child private IdenticalInternal identicalRecursiveAttr;
        @Child private IterableAttributeNode attrIterNodeX;
        @Child private IterableAttributeNode attrIterNodeY;

        private static final IdenticalRecursiveAttrNode UNCACHED = new IdenticalRecursiveAttrNode(false);

        IdenticalRecursiveAttrNode(boolean cached) {
            identicalRecursiveAttr = cached ? IdenticalInternal.create() : IdenticalInternalNodeGen.getUncached();
            attrIterNodeX = cached ? IterableAttributeNodeGen.create() : IterableAttributeNodeGen.getUncached();
            attrIterNodeY = cached ? IterableAttributeNodeGen.create() : IterableAttributeNodeGen.getUncached();
        }

        public static IdenticalRecursiveAttrNode createOrGet(int depth) {
            return isCached(depth) ? new IdenticalRecursiveAttrNode(true) : UNCACHED;
        }

        public static IdenticalRecursiveAttrNode getUncached() {
            return UNCACHED;
        }

        public byte execute(RAttributable x, RAttributable y, boolean numEq, boolean singleNA, boolean attribAsSet, boolean ignoreBytecode, boolean ignoreEnvironment, boolean ignoreSrcref,
                        int depth) {
            DynamicObject xAttributes = x.getAttributes();
            DynamicObject yAttributes = y.getAttributes();
            int xSize = xAttributes == null ? 0 : xAttributes.getShape().getPropertyCount();
            int ySize = yAttributes == null ? 0 : yAttributes.getShape().getPropertyCount();
            if (xSize == 0 && ySize == 0) {
                return RRuntime.LOGICAL_TRUE;
            } else if (xSize != ySize) {
                return RRuntime.LOGICAL_FALSE;
            } else {
                return identicalAttrInternal(numEq, singleNA, attribAsSet, ignoreBytecode, ignoreEnvironment, ignoreSrcref, xAttributes, yAttributes, depth);
            }
        }

        @TruffleBoundary
        private byte identicalAttrInternal(boolean numEq, boolean singleNA, boolean attribAsSet, boolean ignoreBytecode, boolean ignoreEnvironment, boolean ignoreSrcref, DynamicObject xAttributes,
                        DynamicObject yAttributes, int depth) {
            if (attribAsSet) {
                // make sure all attributes from x are in y, with identical values
                Iterator<RAttributesLayout.RAttribute> xIter = attrIterNodeX.execute(xAttributes).iterator();
                while (xIter.hasNext()) {
                    RAttributesLayout.RAttribute xAttr = xIter.next();
                    Object yValue = DynamicObjectLibrary.getUncached().getOrDefault(yAttributes, xAttr.getName(), null);
                    if (yValue == null) {
                        return RRuntime.LOGICAL_FALSE;
                    }
                    byte res = identicalRecursiveAttr(xAttr.getName(), xAttr.getValue(), yValue, numEq, singleNA, attribAsSet, ignoreBytecode, ignoreEnvironment, ignoreSrcref, depth);
                    if (res == RRuntime.LOGICAL_FALSE) {
                        return RRuntime.LOGICAL_FALSE;
                    }
                }
                // make sure all attributes from y are in x
                Iterator<RAttributesLayout.RAttribute> yIter = attrIterNodeY.execute(yAttributes).iterator();
                while (xIter.hasNext()) {
                    RAttributesLayout.RAttribute yAttr = yIter.next();
                    if (!DynamicObjectLibrary.getUncached().containsKey(xAttributes, yAttr.getName())) {
                        return RRuntime.LOGICAL_FALSE;
                    }
                }
            } else {
                Iterator<RAttributesLayout.RAttribute> xIter = attrIterNodeX.execute(xAttributes).iterator();
                Iterator<RAttributesLayout.RAttribute> yIter = attrIterNodeY.execute(yAttributes).iterator();
                while (xIter.hasNext()) {
                    RAttributesLayout.RAttribute xAttr = xIter.next();
                    RAttributesLayout.RAttribute yAttr = yIter.next();
                    if (!xAttr.getName().equals(yAttr.getName())) {
                        return RRuntime.LOGICAL_FALSE;
                    }
                    byte res = identicalRecursiveAttr(xAttr.getName(), xAttr.getValue(), yAttr.getValue(), numEq, singleNA, attribAsSet, ignoreBytecode, ignoreEnvironment, ignoreSrcref, depth);
                    if (res == RRuntime.LOGICAL_FALSE) {
                        return RRuntime.LOGICAL_FALSE;
                    }
                }
            }
            return RRuntime.LOGICAL_TRUE;
        }

        private byte identicalRecursiveAttr(String attrName, Object xx, Object yy, boolean numEq, boolean singleNA, boolean attribAsSet, boolean ignoreBytecode, boolean ignoreEnvironment,
                        boolean ignoreSrcref, int depth) {
            Object x = xx;
            Object y = yy;
            if (GetAttributeNode.isRowNamesAttr(attrName)) {
                x = GetRowNamesAttributeNode.convertRowNamesToSeq(x);
                y = GetRowNamesAttributeNode.convertRowNamesToSeq(y);
            }
            return identicalRecursiveAttr.executeByte(x, y, numEq, singleNA, attribAsSet, ignoreBytecode, ignoreEnvironment, ignoreSrcref, depth);
        }

    }

    static {
        Casts casts = new Casts(Identical.class);
        casts.arg("num.eq").asLogicalVector().findFirst(RRuntime.LOGICAL_FALSE).map(toBoolean());
        casts.arg("single.NA").asLogicalVector().findFirst(RRuntime.LOGICAL_FALSE).map(toBoolean());
        casts.arg("attrib.as.set").asLogicalVector().findFirst(RRuntime.LOGICAL_FALSE).map(toBoolean());
        casts.arg("ignore.bytecode").asLogicalVector().findFirst(RRuntime.LOGICAL_FALSE).map(toBoolean());
        casts.arg("ignore.environment").asLogicalVector().findFirst(RRuntime.LOGICAL_FALSE).map(toBoolean());
        casts.arg("ignore.srcref").asLogicalVector().findFirst(RRuntime.LOGICAL_FALSE).map(toBoolean());
    }

    @GenerateUncached
    @TypeSystemReference(RTypes.class)
    public abstract static class IdenticalInternal extends RBaseNode {

        public abstract byte executeByte(Object x, Object y, Object numEq, Object singleNA, Object attribAsSet, Object ignoreBytecode, Object ignoreEnvironment,
                        Object ignoreSrcref, int depth);

        @SuppressWarnings("unused")
        @Specialization(guards = "isRNull(x) || isRNull(y)")
        protected byte doInternalIdenticalNull(Object x, Object y, boolean numEq, boolean singleNA, boolean attribAsSet, boolean ignoreBytecode, boolean ignoreEnvironment, boolean ignoreSrcref,
                        int depth) {
            return RRuntime.asLogical(x == y);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "isRMissing(x) || isRMissing(y)")
        protected byte doInternalIdenticalMissing(Object x, Object y, boolean numEq, boolean singleNA, boolean attribAsSet, boolean ignoreBytecode, boolean ignoreEnvironment, boolean ignoreSrcref,
                        int depth) {
            return RRuntime.asLogical(x == y);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected byte doInternalIdentical(byte x, byte y, boolean numEq, boolean singleNA, boolean attribAsSet, boolean ignoreBytecode, boolean ignoreEnvironment, boolean ignoreSrcref, int depth) {
            return RRuntime.asLogical(x == y);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected byte doInternalIdentical(String x, String y, boolean numEq, boolean singleNA, boolean attribAsSet, boolean ignoreBytecode, boolean ignoreEnvironment, boolean ignoreSrcref,
                        int depth) {
            return RRuntime.asLogical(x.equals(y));
        }

        @SuppressWarnings("unused")
        @Specialization
        protected byte doInternalIdentical(CharSXPWrapper x, CharSXPWrapper y, boolean numEq, boolean singleNA, boolean attribAsSet, boolean ignoreBytecode, boolean ignoreEnvironment,
                        boolean ignoreSrcref, int depth) {
            return RRuntime.asLogical(x.equals(y));
        }

        @SuppressWarnings("unused")
        @Specialization
        protected byte doInternalIdentical(double x, double y, boolean numEq, boolean singleNA, boolean attribAsSet, boolean ignoreBytecode, boolean ignoreEnvironment, boolean ignoreSrcref,
                        int depth) {
            return identical(x, y, numEq, singleNA);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected byte doInternalIdentical(REnvironment x, REnvironment y, boolean numEq, boolean singleNA, boolean attribAsSet, boolean ignoreBytecode, boolean ignoreEnvironment,
                        boolean ignoreSrcref,
                        int depth) {
            // reference equality for environments
            return RRuntime.asLogical(x == y);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected byte doInternalIdentical(RSymbol x, RSymbol y, boolean numEq, boolean singleNA, boolean attribAsSet, boolean ignoreBytecode, boolean ignoreEnvironment, boolean ignoreSrcref,
                        int depth) {
            return RRuntime.asLogical(x == y);
        }

        @Specialization
        byte doInternalIdentical(RFunction x, RFunction y, boolean numEq, boolean singleNA, boolean attribAsSet, boolean ignoreBytecode, boolean ignoreEnvironment, boolean ignoreSrcref, int depth,
                        @Cached(value = "createOrGet(depth)", uncached = "getUncached()") IdenticalRecursiveAttrNode identicalRecursiveAttrNode) {
            if (x == y) {
                // trivial case
                return RRuntime.LOGICAL_TRUE;
            } else {
                return doInternalIdenticalSlowpath(x, y, numEq, singleNA, attribAsSet, ignoreBytecode, ignoreEnvironment, ignoreSrcref, identicalRecursiveAttrNode, depth);
            }
        }

        @TruffleBoundary
        private static byte doInternalIdenticalSlowpath(RFunction x, RFunction y, boolean numEq, boolean singleNA, boolean attribAsSet, boolean ignoreBytecode, boolean ignoreEnvironment,
                        boolean ignoreSrcref,
                        IdenticalRecursiveAttrNode identicalRecursiveAttrNode, int depth) {
            boolean xb = x.isBuiltin();
            boolean yb = y.isBuiltin();
            if ((xb && !yb) || (yb && !xb)) {
                return RRuntime.LOGICAL_FALSE;
            }

            if (xb && yb) {
                // equal if the factories are
                return RRuntime.asLogical(x.getRBuiltin() == y.getRBuiltin());
            }

            // compare the structure
            if (!new IdenticalVisitor().accept((RSyntaxNode) x.getRootNode(), (RSyntaxNode) y.getRootNode())) {
                return RRuntime.LOGICAL_FALSE;
            }
            // The environments have to match unless ignoreEnvironment == false
            if (!ignoreEnvironment) {
                if (x.getEnclosingFrame() != y.getEnclosingFrame()) {
                    return RRuntime.LOGICAL_FALSE;
                }
            }
            // finally check attributes
            return identicalRecursiveAttrNode.execute(x, y, numEq, singleNA, attribAsSet, ignoreBytecode, ignoreEnvironment, ignoreSrcref, depth + 1);
        }

        @Specialization(guards = "!vectorsLists(x, y)", limit = "getGenericDataLibraryCacheSize()")
        protected static byte doInternalIdenticalGeneric(RAbstractVector x, RAbstractVector y, boolean numEq, boolean singleNA, boolean attribAsSet, boolean ignoreBytecode, boolean ignoreEnvironment,
                        boolean ignoreSrcref, int depth,
                        @CachedLibrary("x.getData()") VectorDataLibrary xDataLib,
                        @CachedLibrary("y.getData()") VectorDataLibrary yDataLib,
                        @Cached("createClassProfile()") ValueProfile xClassProfile,
                        @Cached("createClassProfile()") ValueProfile yClassProfile,
                        @Cached("createBinaryProfile()") ConditionProfile vecLengthProfile,
                        @Cached("createBinaryProfile()") ConditionProfile differentTypesProfile,
                        @Cached("createBinaryProfile()") ConditionProfile isDoubleProfile,
                        @Cached(value = "createOrGet(depth)", uncached = "getUncached()") IdenticalRecursiveAttrNode identicalRecursiveAttrNode) {
            RAbstractVector xProfiled = xClassProfile.profile(x);
            RAbstractVector yProfiled = yClassProfile.profile(y);
            Object xData = xProfiled.getData();
            Object yData = yProfiled.getData();
            int xLen = xDataLib.getLength(xData);
            if (vecLengthProfile.profile(xLen != yDataLib.getLength(yData)) || differentTypesProfile.profile(xProfiled.getRType() != yProfiled.getRType())) {
                return RRuntime.LOGICAL_FALSE;
            } else {
                for (int i = 0; i < xLen; i++) {
                    Object xValue = xDataLib.getDataAtAsObject(xData, i);
                    Object yValue = yDataLib.getDataAtAsObject(yData, i);
                    if (isDoubleProfile.profile(xValue instanceof Double)) {
                        if (identical((double) xValue, (double) yValue, numEq, singleNA) == RRuntime.LOGICAL_FALSE) {
                            return RRuntime.LOGICAL_FALSE;
                        }
                    } else if (!xValue.equals(yValue)) {
                        return RRuntime.LOGICAL_FALSE;
                    }
                }
            }
            return identicalRecursiveAttrNode.execute(xProfiled, yProfiled, numEq, singleNA, attribAsSet, ignoreBytecode, ignoreEnvironment, ignoreSrcref, depth + 1);
        }

        static IdenticalInternal createOrGet(int depth) {
            return isCached(depth) ? IdenticalInternal.create() : IdenticalInternalNodeGen.getUncached();
        }

        @Specialization(limit = "getTypedVectorDataLibraryCacheSize()")
        protected byte doInternalIdenticalGeneric(RAbstractListBaseVector x, RAbstractListBaseVector y, boolean numEq, boolean singleNA, boolean attribAsSet, boolean ignoreBytecode,
                        boolean ignoreEnvironment, boolean ignoreSrcref, int depth,
                        @CachedLibrary("x.getData()") VectorDataLibrary xDataLib,
                        @CachedLibrary("y.getData()") VectorDataLibrary yDataLib,
                        @Cached("createClassProfile()") ValueProfile xClassProfile,
                        @Cached("createClassProfile()") ValueProfile yClassProfile,
                        @Cached("createBinaryProfile()") ConditionProfile vecLengthProfile,
                        @Cached("createBinaryProfile()") ConditionProfile differentTypesProfile,
                        @Cached(value = "createOrGet(depth)", uncached = "getUncached()") IdenticalInternal identicalRecursiveNode,
                        @Cached(value = "createOrGet(depth)", uncached = "getUncached()") IdenticalRecursiveAttrNode identicalRecursiveAttrNode) {
            RAbstractVector xProfiled = xClassProfile.profile(x);
            RAbstractVector yProfiled = yClassProfile.profile(y);
            Object xData = xProfiled.getData();
            Object yData = yProfiled.getData();
            int xLen = xDataLib.getLength(xData);
            if (vecLengthProfile.profile(xLen != yDataLib.getLength(yData)) || differentTypesProfile.profile(xProfiled.getRType() != yProfiled.getRType())) {
                return RRuntime.LOGICAL_FALSE;
            }
            for (int i = 0; i < xLen; i++) {
                byte res = identicalRecursiveNode.executeByte(xDataLib.getElementAt(xData, i), yDataLib.getElementAt(yData, i), numEq, singleNA, attribAsSet, ignoreBytecode, ignoreEnvironment,
                                ignoreSrcref, depth + 1);
                if (res == RRuntime.LOGICAL_FALSE) {
                    return RRuntime.LOGICAL_FALSE;
                }
            }
            return identicalRecursiveAttrNode.execute(xProfiled, yProfiled, numEq, singleNA, attribAsSet, ignoreBytecode, ignoreEnvironment, ignoreSrcref, depth + 1);
        }

        @Specialization
        protected byte doInternalIdenticalGeneric(RS4Object x, RS4Object y, boolean numEq, boolean singleNA, boolean attribAsSet, boolean ignoreBytecode, boolean ignoreEnvironment,
                        boolean ignoreSrcref,
                        int depth,
                        @Cached(value = "createOrGet(depth)", uncached = "getUncached()") IdenticalRecursiveAttrNode identicalRecursiveAttrNode) {
            if (x.isS4() != y.isS4()) {
                return RRuntime.LOGICAL_FALSE;
            }
            return identicalRecursiveAttrNode.execute(x, y, numEq, singleNA, attribAsSet, ignoreBytecode, ignoreEnvironment, ignoreSrcref, depth + 1);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected byte doInternalIdenticalGeneric(RExternalPtr x, RExternalPtr y, boolean numEq, boolean singleNA, boolean attribAsSet, boolean ignoreBytecode, boolean ignoreEnvironment,
                        boolean ignoreSrcref, int depth) {
            return RRuntime.asLogical(x.getAddr() == y.getAddr());
        }

        @Specialization
        @TruffleBoundary
        protected byte doInternalIdenticalGeneric(RPairList x, RPairList y, boolean numEq, boolean singleNA, boolean attribAsSet, boolean ignoreBytecode, boolean ignoreEnvironment,
                        boolean ignoreSrcref,
                        int depth,
                        @Cached(value = "createOrGet(depth)", uncached = "getUncached()") IdenticalInternal identicalRecursiveNode,
                        @Cached(value = "createOrGet(depth)", uncached = "getUncached()") IdenticalRecursiveAttrNode identicalRecursiveAttrNode) {
            if (x == y) {
                return RRuntime.LOGICAL_TRUE;
            }
            boolean xHasClosure = x.hasClosure();
            boolean yHasClosure = y.hasClosure();
            try {
                if (identicalRecursiveNode.executeByte(x.car(), y.car(), numEq, singleNA, attribAsSet, ignoreBytecode, ignoreEnvironment, ignoreSrcref, depth + 1) == RRuntime.LOGICAL_FALSE) {
                    return RRuntime.LOGICAL_FALSE;
                }
                Object tmpXCdr = x.cdr();
                Object tmpYCdr = y.cdr();
                while (true) {
                    if (RRuntime.isNull(tmpXCdr) && RRuntime.isNull(tmpYCdr)) {
                        break;
                    } else if (RRuntime.isNull(tmpXCdr) || RRuntime.isNull(tmpYCdr)) {
                        return RRuntime.LOGICAL_FALSE;
                    } else {
                        RPairList xSubList = (RPairList) tmpXCdr;
                        RPairList ySubList = (RPairList) tmpYCdr;
                        Object tagX = xSubList.getTag();
                        if (tagX instanceof RSymbol && "".equals(((RSymbol) tagX).getName())) {
                            tagX = RNull.instance;
                        }
                        Object tagY = ySubList.getTag();
                        if (tagY instanceof RSymbol && "".equals(((RSymbol) tagY).getName())) {
                            tagY = RNull.instance;
                        }

                        if (RRuntime.isNull(tagX) && RRuntime.isNull(tagY)) {
                            // continue
                        } else if (RRuntime.isNull(tagX) || RRuntime.isNull(tagY)) {
                            return RRuntime.LOGICAL_FALSE;
                        } else {
                            if (tagX instanceof RSymbol && tagY instanceof RSymbol) {
                                if (xSubList.getTag() != ySubList.getTag()) {
                                    return RRuntime.LOGICAL_FALSE;
                                }
                            } else {
                                throw RInternalError.unimplemented("non-RNull and non-RSymbol pairlist tags are not currently supported");
                            }
                        }
                        if (identicalRecursiveNode.executeByte(xSubList.car(), ySubList.car(), numEq, singleNA, attribAsSet, ignoreBytecode, ignoreEnvironment, ignoreSrcref,
                                        depth + 1) == RRuntime.LOGICAL_FALSE) {
                            return RRuntime.LOGICAL_FALSE;
                        }
                        if (xSubList.getAttributes() != null || ySubList.getAttributes() != null) {
                            throw RInternalError.unimplemented("attributes of internal pairlists are not currently supported");
                        }
                        tmpXCdr = ((RPairList) tmpXCdr).cdr();
                        tmpYCdr = ((RPairList) tmpYCdr).cdr();
                    }
                }
                return identicalRecursiveAttrNode.execute(x, y, numEq, singleNA, attribAsSet, ignoreBytecode, ignoreEnvironment, ignoreSrcref, depth + 1);
            } finally {
                // if they were closures before, they can still be afterwards
                if (xHasClosure) {
                    x.allowClosure();
                }
                if (yHasClosure) {
                    y.allowClosure();
                }
            }
        }

        @SuppressWarnings("unused")
        @Specialization
        protected byte doInternalIdenticalForeignObject(RInteropScalar x, RInteropScalar y, boolean numEq, boolean singleNA, boolean attribAsSet, boolean ignoreBytecode, boolean ignoreEnvironment,
                        boolean ignoreSrcref, int depth) {
            return RRuntime.asLogical(x == y);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "areForeignObjects(x, y)")
        protected byte doInternalIdenticalForeignObject(TruffleObject x, TruffleObject y, boolean numEq, boolean singleNA, boolean attribAsSet, boolean ignoreBytecode, boolean ignoreEnvironment,
                        boolean ignoreSrcref, int depth) {
            return RRuntime.asLogical(x == y);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected byte doInternalIdenticalForeignObject(RPromise x, RPromise y, boolean numEq, boolean singleNA, boolean attribAsSet, boolean ignoreBytecode, boolean ignoreEnvironment,
                        boolean ignoreSrcref, int depth) {
            return RRuntime.asLogical(x == y);
        }

        @SuppressWarnings("unused")
        @Fallback
        protected byte doInternalIdenticalWrongTypes(Object x, Object y, Object numEq, Object singleNA, Object attribAsSet, Object ignoreBytecode, Object ignoreEnvironment, Object ignoreSrcref,
                        int depth) {
            assert x.getClass() != y.getClass();
            return RRuntime.LOGICAL_FALSE;
        }

        protected static boolean areForeignObjects(TruffleObject x, TruffleObject y) {
            return RRuntime.isForeignObject(x) && RRuntime.isForeignObject(y);
        }

        protected static boolean vectorsLists(RAbstractVector x, RAbstractVector y) {
            return x instanceof RAbstractListBaseVector && y instanceof RAbstractListBaseVector;
        }

        public static IdenticalInternal create() {
            return IdenticalInternalNodeGen.create();
        }

        private static byte identical(double x, double y, boolean numEq, boolean singleNA) {
            if (singleNA) {
                if (RRuntime.isNA(x)) {
                    return RRuntime.asLogical(RRuntime.isNA(y));
                } else if (RRuntime.isNA(y)) {
                    return RRuntime.LOGICAL_FALSE;
                } else if (Double.isNaN(x)) {
                    return RRuntime.asLogical(Double.isNaN(y));
                } else if (Double.isNaN(y)) {
                    return RRuntime.LOGICAL_FALSE;
                }
            }
            if (numEq) {
                if (!singleNA) {
                    if (Double.isNaN(x) || Double.isNaN(y)) {
                        return RRuntime.asLogical(Double.doubleToRawLongBits(x) == Double.doubleToRawLongBits(y));
                    }
                }
                return RRuntime.asLogical(x == y);
            }
            return RRuntime.asLogical(Double.doubleToRawLongBits(x) == Double.doubleToRawLongBits(y));
        }

    }

    public static Identical create() {
        return new Identical();
    }

}
