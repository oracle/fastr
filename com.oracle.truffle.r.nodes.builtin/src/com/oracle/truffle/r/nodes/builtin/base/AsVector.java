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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.singleElement;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.runtime.RDispatch.INTERNAL_GENERIC;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.AsVectorNodeGen.AsVectorInternalNodeGen;
import com.oracle.truffle.r.nodes.builtin.base.AsVectorNodeGen.AsVectorInternalNodeGen.CastPairListNodeGen;
import com.oracle.truffle.r.nodes.builtin.base.AsVectorNodeGen.AsVectorInternalNodeGen.DropAttributesNodeGen;
import com.oracle.truffle.r.nodes.function.CallMatcherNode;
import com.oracle.truffle.r.nodes.function.ClassHierarchyNode;
import com.oracle.truffle.r.nodes.function.ClassHierarchyNodeGen;
import com.oracle.truffle.r.nodes.function.S3FunctionLookupNode;
import com.oracle.truffle.r.nodes.function.S3FunctionLookupNode.Result;
import com.oracle.truffle.r.nodes.unary.CastComplexNode;
import com.oracle.truffle.r.nodes.unary.CastDoubleNode;
import com.oracle.truffle.r.nodes.unary.CastExpressionNode;
import com.oracle.truffle.r.nodes.unary.CastIntegerNode;
import com.oracle.truffle.r.nodes.unary.CastListNodeGen;
import com.oracle.truffle.r.nodes.unary.CastLogicalNode;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.nodes.unary.CastRawNode;
import com.oracle.truffle.r.nodes.unary.CastStringNode;
import com.oracle.truffle.r.nodes.unary.CastSymbolNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RSharingAttributeStorage;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.model.RAbstractAtomicVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.interop.ForeignArray2R;
import com.oracle.truffle.r.runtime.interop.ForeignArray2RNodeGen;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

@RBuiltin(name = "as.vector", kind = INTERNAL, parameterNames = {"x", "mode"}, dispatch = INTERNAL_GENERIC, behavior = COMPLEX)
public abstract class AsVector extends RBuiltinNode.Arg2 {

    @Child private AsVectorInternal internal = AsVectorInternalNodeGen.create();
    @Child private ClassHierarchyNode classHierarchy = ClassHierarchyNodeGen.create(false, false);

    @Child private S3FunctionLookupNode lookup;
    @Child private CallMatcherNode callMatcher;

    private final ConditionProfile hasClassProfile = ConditionProfile.createBinaryProfile();

    static {
        Casts casts = new Casts(AsVector.class);
        casts.arg("mode").mustBe(stringValue()).asStringVector().mustBe(singleElement()).findFirst();
    }

    protected static AsVectorInternal createInternal() {
        return AsVectorInternalNodeGen.create();
    }

    private static final ArgumentsSignature SIGNATURE = ArgumentsSignature.get("x", "mode");

    @Specialization
    protected Object asVector(VirtualFrame frame, Object x, String mode) {
        // TODO given dispatch = INTERNAL_GENERIC, this should not be necessary
        // However, INTERNAL_GENERIC is not handled for INTERNAL builtins
        RStringVector clazz = classHierarchy.execute(x);
        if (hasClassProfile.profile(clazz != null)) {
            // Note: this dispatch takes care of factor, because there is as.vector.factor
            // specialization in R
            if (lookup == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                lookup = insert(S3FunctionLookupNode.create(false, false));
            }
            Result lookupResult = lookup.execute(frame, "as.vector", clazz, null, frame.materialize(), null);
            if (lookupResult != null) {
                if (callMatcher == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    callMatcher = insert(CallMatcherNode.create(false));
                }
                return callMatcher.execute(frame, SIGNATURE, new Object[]{x, mode}, lookupResult.function, lookupResult.targetFunctionName, lookupResult.createS3Args(frame));
            }
        }
        return internal.execute(x, mode);
    }

    @ImportStatic(RRuntime.class)
    public abstract static class AsVectorInternal extends Node {

        public abstract Object execute(Object x, String mode);

        private final ConditionProfile indirectMatchProfile = ConditionProfile.createBinaryProfile();

        protected static CastNode createCast(RType type) {
            if (type != null) {
                switch (type) {
                    case Any:
                        return null;
                    case Character:
                        return CastStringNode.createNonPreserving();
                    case Complex:
                        return CastComplexNode.createNonPreserving();
                    case Double:
                        return CastDoubleNode.createNonPreserving();
                    case Expression:
                        return CastExpressionNode.createNonPreserving();
                    case Function:
                        throw RInternalError.unimplemented("as.vector cast to 'function'");
                    case Integer:
                        return CastIntegerNode.createNonPreserving();
                    case List:
                        return CastListNodeGen.create(true, false, false);
                    case Logical:
                        return CastLogicalNode.createNonPreserving();
                    case PairList:
                        return CastPairListNodeGen.create();
                    case Raw:
                        return CastRawNode.createNonPreserving();
                    case Symbol:
                        return CastSymbolNode.createNonPreserving();
                }
            }
            throw RError.error(RError.SHOW_CALLER, Message.INVALID_ARGUMENT, "mode");
        }

        protected boolean matchesMode(String mode, String cachedMode) {
            return mode == cachedMode || indirectMatchProfile.profile(cachedMode.equals(mode));
        }

        protected ForeignArray2R createForeignArray2R() {
            return ForeignArray2RNodeGen.create();
        }

        // there should never be more than ~12 specializations
        @SuppressWarnings("unused")
        @Specialization(limit = "99", guards = "matchesMode(mode, cachedMode)")
        protected Object asVector(Object x, String mode,
                        @Cached("mode") String cachedMode,
                        @Cached("fromMode(cachedMode)") RType type,
                        @Cached("createCast(type)") CastNode cast,
                        @Cached("create()") DropAttributesNode drop,
                        @Cached("createForeignArray2R()") ForeignArray2R foreignArray2R) {
            if (RRuntime.isForeignObject(x)) {
                Object o = foreignArray2R.execute(x, true);
                if (!RRuntime.isForeignObject(o)) {
                    return cast == null ? o : cast.doCast(o);
                }
                if (type == RType.List) {
                    throw RError.error(RError.SHOW_CALLER, RError.Message.CANNOT_COERCE_EXTERNAL_OBJECT_TO_VECTOR, "list");
                } else {
                    throw RError.error(RError.SHOW_CALLER, RError.Message.CANNOT_COERCE_EXTERNAL_OBJECT_TO_VECTOR, "vector");
                }
            }
            return drop.execute(cast == null ? x : cast.doCast(x));
        }

        public abstract static class DropAttributesNode extends RBaseNode {

            public abstract Object execute(Object o);

            public static DropAttributesNode create() {
                return DropAttributesNodeGen.create();
            }

            protected static boolean hasAttributes(Class<? extends RAbstractAtomicVector> clazz, RAbstractAtomicVector o) {
                return clazz.cast(o).getAttributes() != null;
            }

            @Specialization(guards = "o.getAttributes() == null")
            protected static RSharingAttributeStorage drop(RSharingAttributeStorage o) {
                // quickly reject any RSharingAttributeStorage without attributes
                return o;
            }

            @Specialization(guards = "o.getClass() == oClass")
            protected RAbstractVector dropCached(RAbstractAtomicVector o,
                            @Cached("o.getClass()") Class<? extends RAbstractAtomicVector> oClass,
                            @Cached("createBinaryProfile()") ConditionProfile profile) {
                return profile.profile(hasAttributes(oClass, o)) ? oClass.cast(o).copyDropAttributes() : o;
            }

            @Specialization(replaces = "dropCached")
            protected RAbstractVector drop(RAbstractAtomicVector o,
                            @Cached("createBinaryProfile()") ConditionProfile profile) {
                return profile.profile(o.getAttributes() != null) ? o.copyDropAttributes() : o;
            }

            @Specialization(guards = "o.getAttributes() != null")
            protected static RLanguage drop(RLanguage o) {
                return RDataFactory.createLanguage(o.getRep());
            }

            @Specialization(guards = "o.getAttributes() != null")
            protected static RSymbol drop(RSymbol o) {
                return RDataFactory.createSymbol(o.getName());
            }

            @Fallback
            protected Object drop(Object o) {
                // includes RAbstractListVector, RExpression, RPairList
                return o;
            }
        }

        protected abstract static class CastPairListNode extends CastNode {

            @Specialization
            @TruffleBoundary
            protected Object castPairlist(RAbstractVector x) {
                // TODO implement non-empty element list conversion; this is a placeholder for type
                // test
                if (x.getLength() == 0) {
                    return RNull.instance;
                } else {
                    Object list = RNull.instance;
                    RStringVector names = x.getNames();
                    for (int i = x.getLength() - 1; i >= 0; i--) {
                        Object name = names == null ? RNull.instance : RDataFactory.createSymbolInterned(names.getDataAt(i));
                        Object data = x.getDataAtAsObject(i);
                        list = RDataFactory.createPairList(data, list, name);
                    }
                    return list;
                }
            }

            @Specialization
            protected Object doRNull(@SuppressWarnings("unused") RNull value) {
                return RNull.instance;
            }

            @Fallback
            protected Object castPairlist(Object x) {
                throw RInternalError.unimplemented("non-list casts to pairlist for " + x.getClass().getSimpleName());
            }
        }
    }
}
