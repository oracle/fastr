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

import static com.oracle.truffle.r.runtime.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.AsVectorNodeGen.AsVectorInternalNodeGen;
import com.oracle.truffle.r.nodes.function.ClassHierarchyNode;
import com.oracle.truffle.r.nodes.function.ClassHierarchyNodeGen;
import com.oracle.truffle.r.nodes.function.S3FunctionLookupNode;
import com.oracle.truffle.r.nodes.function.UseMethodInternalNode;
import com.oracle.truffle.r.nodes.unary.CastComplexNode;
import com.oracle.truffle.r.nodes.unary.CastDoubleNode;
import com.oracle.truffle.r.nodes.unary.CastExpressionNode;
import com.oracle.truffle.r.nodes.unary.CastIntegerNode;
import com.oracle.truffle.r.nodes.unary.CastListNode;
import com.oracle.truffle.r.nodes.unary.CastListNodeGen;
import com.oracle.truffle.r.nodes.unary.CastLogicalNode;
import com.oracle.truffle.r.nodes.unary.CastRawNode;
import com.oracle.truffle.r.nodes.unary.CastSymbolNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RAttributeProfiles;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDouble;
import com.oracle.truffle.r.runtime.data.RFactor;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RInteger;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RLogical;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RRaw;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.RTypes;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

@RBuiltin(name = "as.vector", kind = INTERNAL, parameterNames = {"x", "mode"})
public abstract class AsVector extends RBuiltinNode {

    @Child private AsVectorInternal internal = AsVectorInternalNodeGen.create();
    @Child private ClassHierarchyNode classHierarchy = ClassHierarchyNodeGen.create(false, false);
    @Child private UseMethodInternalNode useMethod;

    private final ConditionProfile hasClassProfile = ConditionProfile.createBinaryProfile();

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.firstStringWithError(1, RError.Message.INVALID_ARGUMENT, "mode");
    }

    protected static AsVectorInternal createInternal() {
        return AsVectorInternalNodeGen.create();
    }

    private static final ArgumentsSignature SIGNATURE = ArgumentsSignature.get("x", "mode");

    @Specialization
    protected Object asVector(VirtualFrame frame, Object x, String mode) {
        controlVisibility();
        RStringVector clazz = classHierarchy.execute(x);
        if (hasClassProfile.profile(clazz != null)) {
            if (useMethod == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                useMethod = insert(new UseMethodInternalNode("as.vector", SIGNATURE, false));
            }
            try {
                return useMethod.execute(frame, clazz, new Object[]{x, mode});
            } catch (S3FunctionLookupNode.NoGenericMethodException e) {
                // fallthrough
            }
        }
        return internal.execute(x, mode);
    }

    @TypeSystemReference(RTypes.class)
    public abstract static class AsVectorInternal extends Node {

        public abstract Object execute(Object x, String mode);

        private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

        @Specialization(guards = "castToString(mode)")
        protected Object asVectorString(Object x, @SuppressWarnings("unused") String mode, //
                        @Cached("create()") AsCharacter asCharacter) {
            return asCharacter.execute(x);
        }

        @Specialization(guards = "castToInt(x, mode)")
        protected Object asVectorInt(RAbstractContainer x, @SuppressWarnings("unused") String mode, //
                        @Cached("createNonPreserving()") CastIntegerNode cast) {
            return cast.execute(x);
        }

        @Specialization(guards = "castToDouble(x, mode)")
        protected Object asVectorDouble(RAbstractContainer x, @SuppressWarnings("unused") String mode, //
                        @Cached("createNonPreserving()") CastDoubleNode cast) {
            return cast.execute(x);
        }

        @Specialization(guards = "castToComplex(x, mode)")
        protected Object asVectorComplex(RAbstractContainer x, @SuppressWarnings("unused") String mode, //
                        @Cached("createNonPreserving()") CastComplexNode cast) {
            return cast.execute(x);
        }

        @Specialization(guards = "castToLogical(x, mode)")
        protected Object asVectorLogical(RAbstractContainer x, @SuppressWarnings("unused") String mode, //
                        @Cached("createNonPreserving()") CastLogicalNode cast) {
            return cast.execute(x);
        }

        @Specialization(guards = "castToRaw(x, mode)")
        protected Object asVectorRaw(RAbstractContainer x, @SuppressWarnings("unused") String mode, //
                        @Cached("createNonPreserving()") CastRawNode cast) {
            return cast.execute(x);
        }

        protected static CastListNode createListCast() {
            return CastListNodeGen.create(true, false, false);
        }

        @Specialization(guards = "castToList(mode)")
        protected Object asVectorList(RAbstractContainer x, @SuppressWarnings("unused") String mode, //
                        @Cached("createListCast()") CastListNode cast) {
            return cast.execute(x);
        }

        @Specialization(guards = "castToSymbol(x, mode)")
        protected Object asVectorSymbol(RAbstractContainer x, @SuppressWarnings("unused") String mode, //
                        @Cached("createNonPreserving()") CastSymbolNode cast) {
            return cast.execute(x);
        }

        @Specialization(guards = "castToExpression(mode)")
        protected Object asVectorExpression(Object x, @SuppressWarnings("unused") String mode, //
                        @Cached("createNonPreserving()") CastExpressionNode cast) {
            return cast.execute(x);
        }

        @Specialization(guards = "castToList(mode)")
        protected RAbstractVector asVectorList(@SuppressWarnings("unused") RNull x, @SuppressWarnings("unused") String mode) {
            return RDataFactory.createList();
        }

        @Specialization(guards = "isSymbol(x, mode)")
        protected RSymbol asVectorSymbol(RSymbol x, @SuppressWarnings("unused") String mode) {
            String sName = x.getName();
            return RDataFactory.createSymbol(sName);
        }

        protected boolean isSymbol(@SuppressWarnings("unused") RSymbol x, String mode) {
            return RType.Symbol.getName().equals(mode);
        }

        @Specialization(guards = "modeIsAny(mode)")
        protected RAbstractVector asVector(RList x, @SuppressWarnings("unused") String mode) {
            RList result = x.copyWithNewDimensions(null);
            result.copyNamesFrom(attrProfiles, x);
            return result;
        }

        @Specialization(guards = "modeIsAny(mode)")
        protected RAbstractVector asVector(RFactor x, @SuppressWarnings("unused") String mode) {
            RVector levels = x.getLevels(attrProfiles);
            RVector result = levels.createEmptySameType(x.getLength(), RDataFactory.COMPLETE_VECTOR);
            RIntVector factorData = x.getVector();
            for (int i = 0; i < result.getLength(); i++) {
                result.transferElementSameType(i, levels, factorData.getDataAt(i) - 1);
            }
            return result;
        }

        @Specialization(guards = "modeIsAny(mode)")
        protected RNull asVector(RNull x, @SuppressWarnings("unused") String mode) {
            return x;
        }

        @Specialization(guards = "modeIsPairList(mode)")
        protected Object asVectorPairList(RList x, @SuppressWarnings("unused") String mode) {
            // TODO implement non-empty element list conversion; this is a placeholder for type test
            if (x.getLength() == 0) {
                return RNull.instance;
            } else {
                throw RError.nyi(RError.SHOW_CALLER, "non-empty lists");
            }
        }

        @Specialization(guards = "modeIsAny(mode)")
        protected RAbstractVector asVectorAny(RAbstractVector x, @SuppressWarnings("unused") String mode) {
            return x.copyWithNewDimensions(null);
        }

        @Specialization(guards = "modeMatches(x, mode)")
        protected RAbstractVector asVector(RAbstractVector x, @SuppressWarnings("unused") String mode) {
            return x.copyWithNewDimensions(null);
        }

        protected boolean castToInt(RAbstractContainer x, String mode) {
            return x.getElementClass() != RInteger.class && RType.Integer.getName().equals(mode);
        }

        protected boolean castToDouble(RAbstractContainer x, String mode) {
            return x.getElementClass() != RDouble.class && (RType.Double.getClazz().equals(mode) || RType.Double.getName().equals(mode));
        }

        protected boolean castToComplex(RAbstractContainer x, String mode) {
            return x.getElementClass() != RComplex.class && RType.Complex.getName().equals(mode);
        }

        protected boolean castToLogical(RAbstractContainer x, String mode) {
            return x.getElementClass() != RLogical.class && RType.Logical.getName().equals(mode);
        }

        protected boolean castToString(String mode) {
            return RType.Character.getName().equals(mode);
        }

        protected boolean castToRaw(RAbstractContainer x, String mode) {
            return x.getElementClass() != RRaw.class && RType.Raw.getName().equals(mode);
        }

        protected boolean castToList(String mode) {
            return RType.List.getName().equals(mode);
        }

        protected boolean castToSymbol(RAbstractContainer x, String mode) {
            return x.getElementClass() != Object.class && RType.Symbol.getName().equals(mode);
        }

        protected boolean castToExpression(String mode) {
            return RType.Expression.getName().equals(mode);
        }

        protected boolean modeMatches(RAbstractVector x, String mode) {
            return RRuntime.classToString(x.getElementClass()).equals(mode) || x.getElementClass() == RDouble.class && RType.Double.getName().equals(mode);
        }

        protected boolean modeIsAny(String mode) {
            return RType.Any.getName().equals(mode);
        }

        protected boolean modeIsPairList(String mode) {
            return RType.PairList.getName().equals(mode);
        }

        @SuppressWarnings("unused")
        @Fallback
        @TruffleBoundary
        protected RAbstractVector asVectorWrongMode(Object x, String mode) {
            throw RError.error(RError.SHOW_CALLER, RError.Message.INVALID_ARGUMENT, "mode");
        }
    }
}
