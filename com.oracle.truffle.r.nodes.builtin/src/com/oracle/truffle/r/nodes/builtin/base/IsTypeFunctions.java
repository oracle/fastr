/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.size;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.attributes.GetFixedAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetDimAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctionsFactory.GetDimAttributeNodeGen;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.helpers.InheritsCheckNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RAttributeProfiles;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RExpression;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RListBase;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RRaw;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

/**
 * Handles all builtin functions of the form {@code is.xxx}, where is {@code xxx} is a "type".
 */
@SuppressWarnings("unused")
public class IsTypeFunctions {

    protected abstract static class MissingAdapter extends RBuiltinNode {

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg("x").conf(c -> c.allowNull().mustNotBeMissing(null, RError.Message.ARGUMENT_MISSING, "x"));
        }

    }

    @RBuiltin(name = "is.array", kind = PRIMITIVE, parameterNames = {"x"}, behavior = PURE)
    public abstract static class IsArray extends MissingAdapter {

        private final ConditionProfile isArrayProfile = ConditionProfile.createBinaryProfile();
        @Child private GetDimAttributeNode getDim = GetDimAttributeNodeGen.create();

        public abstract byte execute(Object value);

        @Specialization
        protected byte isType(RAbstractVector vector) {
            return RRuntime.asLogical(isArrayProfile.profile(getDim.isArray(vector)));
        }

        @Specialization(guards = {"!isRMissing(value)", "!isRAbstractVector(value)"})
        protected byte isType(Object value) {
            return RRuntime.LOGICAL_FALSE;
        }
    }

    @RBuiltin(name = "is.recursive", kind = PRIMITIVE, parameterNames = {"x"}, behavior = PURE)
    public abstract static class IsRecursive extends MissingAdapter {

        @Specialization
        protected byte isRecursive(RNull arg) {
            return RRuntime.LOGICAL_FALSE;
        }

        @Specialization(guards = {"!isRList(arg)", "!isRExpression(arg)"})
        protected byte isRecursive(RAbstractVector arg) {
            return RRuntime.LOGICAL_FALSE;
        }

        @Specialization
        protected byte isRecursive(RListBase arg) {
            return RRuntime.LOGICAL_TRUE;
        }

        protected boolean isListVector(RAbstractVector arg) {
            return arg instanceof RListBase;
        }

        @Fallback
        protected byte isRecursiveFallback(Object value) {
            return RRuntime.LOGICAL_TRUE;
        }
    }

    @RBuiltin(name = "is.atomic", kind = PRIMITIVE, parameterNames = {"x"}, behavior = PURE)
    public abstract static class IsAtomic extends MissingAdapter {

        @Child private InheritsCheckNode inheritsFactorCheck = new InheritsCheckNode(RRuntime.CLASS_FACTOR);

        @Specialization
        protected byte isAtomic(RNull arg) {
            return RRuntime.LOGICAL_TRUE;
        }

        @Specialization(guards = {"!isRList(arg)", "!isRExpression(arg)"})
        protected byte isAtomic(RAbstractVector arg) {
            return RRuntime.LOGICAL_TRUE;
        }

        protected static boolean isNonListVector(Object value) {
            return value instanceof Integer || value instanceof Double || value instanceof RComplex || value instanceof String || value instanceof RRaw ||
                            (value instanceof RAbstractVector && !(value instanceof RListBase));
        }

        protected boolean isFactor(Object value) {
            return inheritsFactorCheck.execute(value);
        }

        @Specialization(guards = {"!isRMissing(value)", "!isRNull(value)", "!isFactor(value)", "!isNonListVector(value)"})
        protected byte isType(Object value) {
            return RRuntime.LOGICAL_FALSE;
        }
    }

    @RBuiltin(name = "is.call", kind = PRIMITIVE, parameterNames = {"x"}, behavior = PURE)
    public abstract static class IsCall extends MissingAdapter {

        @Specialization
        protected byte isType(RLanguage lang) {
            return RRuntime.LOGICAL_TRUE;
        }

        @Specialization(guards = {"!isRMissing(value)", "!isRLanguage(value)"})
        protected byte isType(Object value) {
            return RRuntime.LOGICAL_FALSE;
        }
    }

    @RBuiltin(name = "is.character", kind = PRIMITIVE, parameterNames = {"x"}, behavior = PURE)
    public abstract static class IsCharacter extends MissingAdapter {

        @Specialization
        protected byte isType(RAbstractStringVector value) {
            return RRuntime.LOGICAL_TRUE;
        }

        protected static boolean isAnyCharacter(Object value) {
            return value instanceof String || value instanceof RAbstractStringVector;
        }

        @Specialization(guards = {"!isRMissing(value)", "!isAnyCharacter(value)"})
        protected byte isType(Object value) {
            return RRuntime.LOGICAL_FALSE;
        }
    }

    @RBuiltin(name = "is.complex", kind = PRIMITIVE, parameterNames = {"x"}, behavior = PURE)
    public abstract static class IsComplex extends MissingAdapter {

        @Specialization
        protected byte isType(RAbstractComplexVector value) {
            return RRuntime.LOGICAL_TRUE;
        }

        protected static boolean isAnyComplex(Object value) {
            return value instanceof RComplex || value instanceof RAbstractComplexVector;
        }

        @Specialization(guards = {"!isRMissing(value)", "!isAnyComplex(value)"})
        protected byte isType(Object value) {
            return RRuntime.LOGICAL_FALSE;
        }
    }

    @RBuiltin(name = "is.double", kind = PRIMITIVE, parameterNames = {"x"}, behavior = PURE)
    public abstract static class IsDouble extends MissingAdapter {

        @Specialization
        protected byte isType(RAbstractDoubleVector value) {
            return RRuntime.LOGICAL_TRUE;
        }

        protected static boolean isAnyDouble(Object value) {
            return value instanceof Double || value instanceof RAbstractDoubleVector;
        }

        @Specialization(guards = {"!isRMissing(value)", "!isAnyDouble(value)"})
        protected byte isType(Object value) {
            return RRuntime.LOGICAL_FALSE;
        }
    }

    @RBuiltin(name = "is.expression", kind = PRIMITIVE, parameterNames = {"x"}, behavior = PURE)
    public abstract static class IsExpression extends MissingAdapter {

        @Specialization
        protected byte isType(RExpression expr) {
            return RRuntime.LOGICAL_TRUE;
        }

        @Specialization(guards = {"!isRMissing(value)", "!isRExpression(value)"})
        protected byte isType(Object value) {
            return RRuntime.LOGICAL_FALSE;
        }
    }

    @RBuiltin(name = "is.function", kind = PRIMITIVE, parameterNames = {"x"}, behavior = PURE)
    public abstract static class IsFunction extends MissingAdapter {

        @Specialization
        protected byte isType(RFunction value) {
            return RRuntime.LOGICAL_TRUE;
        }

        @Specialization(guards = {"!isRMissing(value)", "!isRFunction(value)"})
        protected byte isType(Object value) {
            return RRuntime.LOGICAL_FALSE;
        }
    }

    @RBuiltin(name = "is.integer", kind = PRIMITIVE, parameterNames = {"x"}, behavior = PURE)
    public abstract static class IsInteger extends MissingAdapter {

        @Specialization
        protected byte isType(RAbstractIntVector value) {
            return RRuntime.LOGICAL_TRUE;
        }

        protected static boolean isAnyInteger(Object value) {
            return value instanceof Integer || value instanceof RAbstractIntVector;
        }

        @Specialization(guards = {"!isRMissing(value)", "!isAnyInteger(value)"})
        protected byte isType(Object value) {
            return RRuntime.LOGICAL_FALSE;
        }
    }

    @RBuiltin(name = "is.language", kind = PRIMITIVE, parameterNames = {"x"}, behavior = PURE)
    public abstract static class IsLanguage extends MissingAdapter {
        @Specialization
        protected byte isType(RSymbol value) {
            return RRuntime.LOGICAL_TRUE;
        }

        @Specialization
        protected byte isType(RExpression value) {
            return RRuntime.LOGICAL_TRUE;
        }

        @Specialization
        protected byte isType(RLanguage value) {
            return RRuntime.LOGICAL_TRUE;
        }

        @Specialization(guards = {"!isRMissing(value)", "!isRSymbol(value)", "!isRExpression(value)", "!isRLanguage(value)"})
        protected byte isType(Object value) {
            return RRuntime.LOGICAL_FALSE;
        }
    }

    @RBuiltin(name = "is.list", kind = PRIMITIVE, parameterNames = {"x"}, behavior = PURE)
    public abstract static class IsList extends MissingAdapter {

        private final ConditionProfile isListProfile = ConditionProfile.createBinaryProfile();

        public abstract byte execute(Object value);

        @Specialization
        protected byte isType(RList value) {
            return RRuntime.LOGICAL_TRUE;
        }

        @Specialization
        protected byte isType(RPairList pl) {
            return RRuntime.LOGICAL_TRUE;
        }

        @Specialization(guards = {"!isRMissing(value)", "!isRList(value)", "!isRPairList(value)"})
        protected byte isType(Object value) {
            return RRuntime.LOGICAL_FALSE;
        }
    }

    @RBuiltin(name = "is.logical", kind = PRIMITIVE, parameterNames = {"x"}, behavior = PURE)
    public abstract static class IsLogical extends MissingAdapter {

        @Specialization
        protected byte isType(RAbstractLogicalVector value) {
            return RRuntime.LOGICAL_TRUE;
        }

        protected static boolean isAnyLogical(Object value) {
            return value instanceof Byte || value instanceof RAbstractLogicalVector;
        }

        @Specialization(guards = {"!isRMissing(value)", "!isAnyLogical(value)"})
        protected byte isType(Object value) {
            return RRuntime.LOGICAL_FALSE;
        }
    }

    @RBuiltin(name = "is.matrix", kind = PRIMITIVE, parameterNames = {"x"}, behavior = PURE)
    public abstract static class IsMatrix extends MissingAdapter {

        private final ConditionProfile isMatrixProfile = ConditionProfile.createBinaryProfile();
        @Child private GetDimAttributeNode getDim = GetDimAttributeNodeGen.create();

        @Specialization
        protected byte isType(RAbstractVector vector) {
            return RRuntime.asLogical(isMatrixProfile.profile(getDim.isMatrix(vector)));
        }

        @Specialization(guards = {"!isRMissing(value)", "!isRAbstractVector(value)"})
        protected byte isType(Object value) {
            return RRuntime.LOGICAL_FALSE;
        }
    }

    @RBuiltin(name = "is.name", aliases = {"is.symbol"}, kind = PRIMITIVE, parameterNames = {"x"}, behavior = PURE)
    public abstract static class IsName extends MissingAdapter {

        @Specialization
        protected byte isType(RSymbol value) {
            return RRuntime.LOGICAL_TRUE;
        }

        @Specialization(guards = {"!isRMissing(value)", "!isRSymbol(value)"})
        protected byte isType(Object value) {
            return RRuntime.LOGICAL_FALSE;
        }
    }

    @RBuiltin(name = "is.numeric", kind = PRIMITIVE, parameterNames = {"x"}, behavior = PURE)
    public abstract static class IsNumeric extends MissingAdapter {

        @Specialization(guards = "!isFactor(value)")
        protected byte isType(RAbstractIntVector value) {
            return RRuntime.LOGICAL_TRUE;
        }

        @Specialization(guards = "isFactor(value)")
        protected byte isTypeFactor(RAbstractIntVector value) {
            return RRuntime.LOGICAL_FALSE;
        }

        @Specialization
        protected byte isType(RAbstractDoubleVector value) {
            return RRuntime.LOGICAL_TRUE;
        }

        protected static boolean isAnyNumeric(Object value) {
            return value instanceof Integer || value instanceof Double || value instanceof RAbstractIntVector || value instanceof RAbstractDoubleVector;
        }

        @Specialization(guards = {"!isRMissing(value)", "!isAnyNumeric(value)"})
        protected byte isType(Object value) {
            return RRuntime.LOGICAL_FALSE;
        }

        @Child private InheritsCheckNode inheritsCheck = new InheritsCheckNode(RRuntime.CLASS_FACTOR);

        protected boolean isFactor(Object o) {
            return inheritsCheck.execute(o);
        }
    }

    @RBuiltin(name = "is.null", kind = PRIMITIVE, parameterNames = {"x"}, behavior = PURE)
    public abstract static class IsNull extends MissingAdapter {

        @Specialization
        protected byte isType(RNull value) {
            return RRuntime.LOGICAL_TRUE;
        }

        @Specialization(guards = {"!isRMissing(value)", "!isRNull(value)"})
        protected byte isType(Object value) {
            return RRuntime.LOGICAL_FALSE;
        }
    }

    /**
     * The specification is not quite what you might expect. Several builtin types, e.g.,
     * {@code expression} respond to {@code class(e)} but return {@code FALSE} to {@code is.object}.
     * Essentially, this method should only return {@code TRUE} if a {@code class} attribute has
     * been added explicitly to the object. If the attribute is removed, it should return
     * {@code FALSE}.
     */
    @RBuiltin(name = "is.object", kind = PRIMITIVE, parameterNames = {"x"}, behavior = PURE)
    public abstract static class IsObject extends MissingAdapter {

        private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

        public abstract byte execute(Object value);

        @Specialization
        protected byte isObject(RAttributable arg, //
                        @Cached("createClassProfile()") ValueProfile profile) {
            return RRuntime.asLogical(profile.profile(arg).isObject(attrProfiles));
        }

        @Specialization(guards = {"!isRMissing(value)", "!isRAttributable(value)"})
        protected byte isType(Object value) {
            return RRuntime.LOGICAL_FALSE;
        }
    }

    @RBuiltin(name = "is.pairlist", kind = PRIMITIVE, parameterNames = {"x"}, behavior = PURE)
    public abstract static class IsPairList extends MissingAdapter {
        @Specialization
        protected byte isType(RNull value) {
            return RRuntime.LOGICAL_TRUE;
        }

        @Specialization
        protected byte isType(RPairList value) {
            return RRuntime.LOGICAL_TRUE;
        }

        @Specialization(guards = {"!isRMissing(value)", "!isRNull(value)", "!isRPairList(value)"})
        protected byte isType(Object value) {
            return RRuntime.LOGICAL_FALSE;
        }
    }

    @RBuiltin(name = "is.raw", kind = PRIMITIVE, parameterNames = {"x"}, behavior = PURE)
    public abstract static class IsRaw extends MissingAdapter {

        @Specialization
        protected byte isType(RAbstractRawVector value) {
            return RRuntime.LOGICAL_TRUE;
        }

        protected static boolean isAnyRaw(Object value) {
            return value instanceof RRaw || value instanceof RAbstractRawVector;
        }

        @Specialization(guards = {"!isRMissing(value)", "!isAnyRaw(value)"})
        protected byte isType(Object value) {
            return RRuntime.LOGICAL_FALSE;
        }
    }

    @RBuiltin(name = "is.vector", kind = INTERNAL, parameterNames = {"x", "mode"}, behavior = PURE)
    public abstract static class IsVector extends RBuiltinNode {

        private final ConditionProfile attrNull = ConditionProfile.createBinaryProfile();
        private final ConditionProfile attrEmpty = ConditionProfile.createBinaryProfile();
        private final ConditionProfile attrNames = ConditionProfile.createBinaryProfile();
        private final BranchProfile namesAttrProfile = BranchProfile.create();
        @Child private GetFixedAttributeNode namesGetter = GetFixedAttributeNode.createNames();

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg("x").conf(c -> c.allowNull().mustNotBeMissing(null, RError.Message.ARGUMENT_MISSING, "x"));
            casts.arg("mode").defaultError(this, RError.Message.INVALID_ARGUMENT, "mode").mustBe(stringValue()).asStringVector().mustBe(size(1)).findFirst();
        }

        @TruffleBoundary
        protected static RType typeFromMode(String mode) {
            return RType.fromMode(mode);
        }

        @Specialization(limit = "5", guards = "cachedMode == mode")
        protected byte isVectorCached(RAbstractVector x, String mode,
                        @Cached("mode") String cachedMode,
                        @Cached("typeFromMode(mode)") RType type) {
            if (namesOnlyOrNoAttr(x) && (type == RType.Any || x.getRType() == type)) {
                return RRuntime.LOGICAL_TRUE;
            } else {
                return RRuntime.LOGICAL_FALSE;
            }
        }

        @Specialization(contains = "isVectorCached")
        protected byte isVector(RAbstractVector x, String mode) {
            return isVectorCached(x, mode, mode, typeFromMode(mode));
        }

        @Fallback
        protected byte isVector(Object x, Object mode) {
            return RRuntime.LOGICAL_FALSE;
        }

        private boolean namesOnlyOrNoAttr(RAbstractVector x) {
            DynamicObject attributes = x.getAttributes();
            if (attrNull.profile(attributes == null) || attrEmpty.profile(attributes.size() == 0)) {
                return true;
            } else {
                return attributes.size() == 1 && attrNames.profile(namesGetter.execute(attributes) != null);
            }
        }
    }
}
