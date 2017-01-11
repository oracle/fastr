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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.singleElement;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.runtime.RDispatch.INTERNAL_GENERIC;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.AsVectorNodeGen.AsVectorInternalNodeGen;
import com.oracle.truffle.r.nodes.builtin.base.AsVectorNodeGen.AsVectorInternalNodeGen.CastPairListNodeGen;
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
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RAttributeProfiles;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RExpression;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

@RBuiltin(name = "as.vector", kind = INTERNAL, parameterNames = {"x", "mode"}, dispatch = INTERNAL_GENERIC, behavior = COMPLEX)
public abstract class AsVector extends RBuiltinNode {

    @Child private AsVectorInternal internal = AsVectorInternalNodeGen.create();
    @Child private ClassHierarchyNode classHierarchy = ClassHierarchyNodeGen.create(false, false);

    @Child private S3FunctionLookupNode lookup;
    @Child private CallMatcherNode callMatcher;

    private final ConditionProfile hasClassProfile = ConditionProfile.createBinaryProfile();

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.arg("mode").mustBe(stringValue()).asStringVector().mustBe(singleElement()).findFirst();
    }

    protected static AsVectorInternal createInternal() {
        return AsVectorInternalNodeGen.create();
    }

    private static final ArgumentsSignature SIGNATURE = ArgumentsSignature.get("x", "mode");

    @Specialization
    protected Object asVector(VirtualFrame frame, Object x, String mode) {
        // TODO given dispatch = INTERNAL_GENERIC, this should not be necessary
        // However, removing it causes unit test failures
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

    public abstract static class AsVectorInternal extends Node {

        public abstract Object execute(Object x, String mode);

        private final ConditionProfile hasAttributes = ConditionProfile.createBinaryProfile();
        private final ConditionProfile indirectMatchProfile = ConditionProfile.createBinaryProfile();
        private final BranchProfile vectorProfile = BranchProfile.create();
        private final ConditionProfile listProfile = ConditionProfile.createBinaryProfile();
        private final BranchProfile languageProfile = BranchProfile.create();
        private final BranchProfile symbolProfile = BranchProfile.create();
        private final BranchProfile expressionProfile = BranchProfile.create();

        private Object dropAttributesIfNeeded(Object o) {
            Object res = o;
            if (res instanceof RAttributable && hasAttributes.profile(((RAttributable) res).getAttributes() != null)) {
                // the assertion should hold because of how cast works and it's only used for
                // vectors (as per as.vector docs)
                if (res instanceof RExpression) {
                    expressionProfile.enter();
                    return res;
                } else if (res instanceof RAbstractVector) {
                    vectorProfile.enter();
                    if (listProfile.profile(res instanceof RAbstractListVector)) {
                        // attributes are not dropped for list results
                        return res;
                    } else {
                        return ((RAbstractVector) res).copyDropAttributes();
                    }
                } else if (res instanceof RLanguage) {
                    languageProfile.enter();
                    return RDataFactory.createLanguage(((RLanguage) res).getRep());
                } else if (res instanceof RSymbol) {
                    symbolProfile.enter();
                    return RDataFactory.createSymbol(((RSymbol) res).getName());
                } else {
                    CompilerDirectives.transferToInterpreter();
                    throw RInternalError.unimplemented("drop attributes for " + res.getClass().getSimpleName());
                }
            }
            return res;
        }

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

        // there should never be more than ~12 specializations
        @SuppressWarnings("unused")
        @Specialization(limit = "99", guards = "matchesMode(mode, cachedMode)")
        protected Object asVector(Object x, String mode,
                        @Cached("mode") String cachedMode,
                        @Cached("fromMode(cachedMode)") RType type,
                        @Cached("createCast(type)") CastNode cast) {
            return dropAttributesIfNeeded(cast == null ? x : cast.execute(x));
        }

        protected abstract static class CastPairListNode extends CastNode {

            @Specialization
            @TruffleBoundary
            protected Object castPairlist(RAbstractListVector x) {
                // TODO implement non-empty element list conversion; this is a placeholder for type
                // test
                if (x.getLength() == 0) {
                    return RNull.instance;
                } else {
                    Object list = RNull.instance;
                    RStringVector names = x.getNames();
                    for (int i = x.getLength() - 1; i >= 0; i--) {
                        Object name = names == null ? RNull.instance : RDataFactory.createSymbolInterned(names.getDataAt(i));
                        Object data = x.getDataAt(i);
                        list = RDataFactory.createPairList(data, list, name);
                    }
                    return list;
                }
            }

            @Fallback
            protected Object castPairlist(@SuppressWarnings("unused") Object x) {
                throw RInternalError.unimplemented("non-list casts to pairlist");
            }
        }
    }
}
