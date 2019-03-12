/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.logicalValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.nodes.builtin.casts.fluent.CastNodeBuilder.newCastBuilder;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.SUBSTITUTE;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.RRootNode;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.abstractVectorValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.typeName;

import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetNamesAttributeNode;
import com.oracle.truffle.r.nodes.builtin.base.MatchArgFastPathNodeGen.MatchArgInternalNodeGen;
import com.oracle.truffle.r.nodes.function.ClassHierarchyNode;
import com.oracle.truffle.r.nodes.function.ClassHierarchyNodeGen;
import com.oracle.truffle.r.nodes.function.FormalArguments;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RAttributesLayout;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RTypes;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractAtomicVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RFastPathNode;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

@RBuiltin(name = "match.arg", kind = SUBSTITUTE, parameterNames = {"arg", "choices", "several.ok"}, nonEvalArgs = {0}, behavior = COMPLEX)
public abstract class MatchArgFastPath extends RFastPathNode {

    @TypeSystemReference(RTypes.class)
    protected abstract static class MatchArgInternal extends Node {

        @Child private PMatch pmatch = PMatchNodeGen.create();
        @Child private Identical identical = IdenticalNodeGen.create();

        @Child private CastNode argCast = newCastBuilder().defaultError(Message.MUST_BE_NULL_OR_STRING, "arg").allowNull().mustBe(stringValue()).buildCastNode();
        @Child private CastNode choicesCast = newCastBuilder().allowNull().mustBe(abstractVectorValue(), Message.CANNOT_COERCE, typeName(), "character").asVector().buildCastNode();
        @Child private CastNode severalOKCast = newCastBuilder().mustBe(logicalValue()).asLogicalVector().findFirst().map(toBoolean()).buildCastNode();
        @Child private AsCharacter asCharacterNode;

        public abstract Object execute(VirtualFrame frame, Object arg, Object choices, Object severalOK);

        public final Object castAndExecute(VirtualFrame frame, Object arg, Object choices, Object severalOK) {
            return execute(frame, argCast.doCast(arg), choicesCast.doCast(choices), severalOKCast.doCast(severalOK));
        }

        @Specialization
        protected Object match(@SuppressWarnings("unused") RNull arg, @SuppressWarnings("unused") RNull choices,
                        @SuppressWarnings("unused") boolean severalOK) {
            return RNull.instance;
        }

        @Specialization
        protected Object matchArgNULL(@SuppressWarnings("unused") RNull arg, RList choices,
                        @SuppressWarnings("unused") boolean severalOK,
                        @Cached("createBinaryProfile()") ConditionProfile isEmptyProfile) {
            return isEmptyProfile.profile(choices.getLength() == 0) ? RDataFactory.createList(new Object[]{RNull.instance}) : RDataFactory.createList(new Object[]{choices.getDataAtAsObject(0)});
        }

        @Specialization
        protected Object matchArgNULL(@SuppressWarnings("unused") RNull arg, RAbstractAtomicVector choices,
                        @SuppressWarnings("unused") boolean severalOK,
                        @Cached("createBinaryProfile()") ConditionProfile isEmptyProfile) {
            return isEmptyProfile.profile(choices.getLength() == 0) ? RRuntime.STRING_NA : choices.getDataAtAsObject(0);
        }

        private void checkEmpty(RAbstractStringVector choices, int count) {
            if (count == 0) {
                CompilerDirectives.transferToInterpreter();
                StringBuilder choicesString = new StringBuilder();
                for (int i = 0; i < choices.getLength(); i++) {
                    choicesString.append(i == 0 ? "" : ", ").append(RRuntime.escapeString(choices.getDataAt(i), false, true, "“", "”"));
                }
                throw RError.error(this, Message.ARG_ONE_OF, "arg", choicesString);
            }
        }

        private static int count(RIntVector matched) {
            int count = 0;
            for (int i = 0; i < matched.getLength(); i++) {
                if (matched.getDataAt(i) != -1) {
                    count++;
                }
            }
            return count;
        }

        @Specialization(guards = {"!severalOK", "!hasS3Class(classHierarchy, choices)"})
        protected Object matchArg(VirtualFrame frame, RAbstractStringVector arg, RAbstractVector choices, @SuppressWarnings("unused") boolean severalOK,
                        @Cached("create()") GetNamesAttributeNode getNamesNode,
                        @SuppressWarnings("unused") @Cached("createClassHierarchyNode()") ClassHierarchyNode classHierarchy) {
            if (identical.executeByte(arg, choices, true, true, true, true, true, true) == RRuntime.LOGICAL_TRUE) {
                return choices.getDataAtAsObject(0);
            }
            if (arg.getLength() != 1) {
                CompilerDirectives.transferToInterpreter();
                throw RError.error(this, Message.MUST_BE_SCALAR, "arg");
            }

            RAbstractStringVector choicesStringVector = toStringVector(frame, choices);

            RIntVector matched = pmatch.execute(arg, choicesStringVector, -1, true);
            int count = count(matched);
            checkEmpty(choicesStringVector, count);
            if (count > 1) {
                CompilerDirectives.transferToInterpreter();
                throw RError.error(this, Message.MORE_THAN_ONE_MATCH, "match.arg");
            }

            RVector<?> resultVector = choices.createEmptySameType(count, true);
            int matchedIdx = matched.getDataAt(0) - 1;
            resultVector.transferElementSameType(0, choices, matchedIdx);
            RStringVector names = getNamesNode.getNames(choices);
            if (names != null) {
                resultVector.initAttributes(RAttributesLayout.createNames(RDataFactory.createStringVector(names.getDataAt(matchedIdx))));
            }
            return resultVector;
        }

        @Specialization(guards = {"severalOK", "!hasS3Class(classHierarchy, choices)"})
        protected Object matchArgSeveral(VirtualFrame frame, RAbstractStringVector arg, RAbstractVector choices, @SuppressWarnings("unused") boolean severalOK,
                        @Cached("create()") GetNamesAttributeNode getNamesNode,
                        @SuppressWarnings("unused") @Cached("createClassHierarchyNode()") ClassHierarchyNode classHierarchy) {
            if (arg.getLength() == 0) {
                CompilerDirectives.transferToInterpreter();
                throw RError.error(this, Message.MUST_BE_GE_ONE, "arg");
            }

            RAbstractStringVector choicesStringVector = toStringVector(frame, choices);

            RIntVector matched = pmatch.execute(arg, choicesStringVector, -1, true);
            int count = count(matched);
            checkEmpty(choicesStringVector, count);
            RVector<?> resultVector = choices.createEmptySameType(count, true);
            if (count == 1) {
                for (int i = 0; i < matched.getLength(); i++) {
                    int matchedIdx = matched.getDataAt(i) - 1;
                    if (matchedIdx >= 0) {
                        resultVector.transferElementSameType(0, choices, matchedIdx);
                        RStringVector names = getNamesNode.getNames(choices);
                        if (names != null) {
                            resultVector.initAttributes(RAttributesLayout.createNames(RDataFactory.createStringVector(names.getDataAt(matchedIdx))));
                        }
                        return resultVector;
                    }
                }
                assert false;
            }
            RStringVector namesVector = getNamesNode.getNames(choices);
            String[] names = namesVector != null ? new String[count] : null;
            int resultIdx = -1;
            for (int i = 0; i < matched.getLength(); i++) {
                int matchedIdx = matched.getDataAt(i) - 1;
                if (matchedIdx >= 0) {
                    resultIdx++;
                    resultVector.transferElementSameType(resultIdx, choices, matchedIdx);
                    if (names != null) {
                        names[resultIdx] = namesVector.getDataAt(matchedIdx);
                    }
                }
            }
            if (names != null) {
                resultVector.initAttributes(RAttributesLayout.createNames(RDataFactory.createStringVector(names, false)));
            }
            return resultVector;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "hasS3Class(classHierarchy, choices)")
        protected Object matchArgS3(RAbstractStringVector arg, RAbstractVector choices, boolean severalOK,
                        @Cached("createClassHierarchyNode()") ClassHierarchyNode classHierarchy) {
            return null;
        }

        private RAbstractStringVector toStringVector(VirtualFrame frame, RAbstractVector choices) {
            if (asCharacterNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                asCharacterNode = insert(AsCharacterNodeGen.create());
            }
            return (RAbstractStringVector) asCharacterNode.call(frame, choices, RArgsValuesAndNames.EMPTY);
        }

        protected ClassHierarchyNode createClassHierarchyNode() {
            return ClassHierarchyNodeGen.create(false, false);
        }

        protected boolean hasS3Class(ClassHierarchyNode chn, Object obj) {
            return chn.execute(obj) != null;
        }
    }

    protected static final class MatchArgChoices extends Node {

        private final CallTarget target;
        private final String symbol;

        @Child private RNode value;

        public MatchArgChoices(VirtualFrame frame, RPromise arg) {
            CompilerAsserts.neverPartOfCompilation();

            RFunction function = RArguments.getFunction(frame);
            assert function.getRBuiltin() == null;

            this.symbol = arg.getClosure().asSymbol();
            if (symbol == null) {
                throw RError.error(this, Message.INVALID_USE, "match.arg");
            }

            RRootNode def = (RRootNode) function.getRootNode();
            this.target = function.getTarget();
            FormalArguments arguments = def.getFormalArguments();

            for (int i = 0; i < arguments.getLength(); i++) {
                assert symbol == arguments.getSignature().getName(i) || !symbol.equals(arguments.getSignature().getName(i));
                if (Utils.identityEquals(symbol, arguments.getSignature().getName(i))) {
                    RNode defaultArg = arguments.getDefaultArgument(i);
                    if (defaultArg == null) {
                        this.value = RContext.getASTBuilder().constant(RSyntaxNode.INTERNAL, RDataFactory.createEmptyStringVector()).asRNode();
                    } else {
                        this.value = RContext.getASTBuilder().process(defaultArg.asRSyntaxNode()).asRNode();
                    }
                    return;
                }
            }
            throw RError.error(RError.SHOW_CALLER, Message.INVALID_USE, "match.arg");
        }

        public boolean isSupported(VirtualFrame frame, RPromise arg) {
            return RArguments.getFunction(frame).getTarget() == target && Utils.identityEquals(arg.getClosure().asSymbol(), symbol);
        }

        public Object execute(VirtualFrame frame) {
            return value.execute(frame);
        }
    }

    protected static MatchArgInternal createInternal() {
        return MatchArgInternalNodeGen.create();
    }

    @Specialization(limit = "1", guards = "cache.choicesValue.isSupported(frame, arg)")
    protected Object matchArg(VirtualFrame frame, RPromise arg, @SuppressWarnings("unused") RMissing choices, Object severalOK,
                    @Cached("new(frame, arg)") MatchArgNode cache) {
        return cache.internal.castAndExecute(frame, cache.promiseHelper.evaluate(frame, arg), cache.choicesValue.execute(frame), severalOK == RMissing.instance ? RRuntime.LOGICAL_FALSE : severalOK);
    }

    public static final class MatchArgNode extends Node {
        @Child public MatchArgChoices choicesValue;
        @Child public MatchArgInternal internal;
        @Child public PromiseHelperNode promiseHelper;

        public MatchArgNode(VirtualFrame frame, RPromise arg) {
            this.choicesValue = new MatchArgChoices(frame, arg);
            this.internal = MatchArgInternalNodeGen.create();
            this.promiseHelper = new PromiseHelperNode();
        }
    }

    protected static boolean isRMissing(Object value) {
        return value instanceof RMissing;
    }

    @Specialization(guards = "!isRMissing(choices)")
    protected Object matchArg(VirtualFrame frame, RPromise arg, Object choices, Object severalOK,
                    @Cached("createInternal()") MatchArgInternal internal,
                    @Cached("new()") PromiseHelperNode promiseHelper) {
        return internal.castAndExecute(frame, promiseHelper.evaluate(frame, arg), choices, severalOK == RMissing.instance ? RRuntime.LOGICAL_FALSE : severalOK);
    }

    @SuppressWarnings("unused")
    @Fallback
    protected Object matchArgFallback(Object arg, Object choices, Object severalOK) {
        throw error(Message.GENERIC, "too many different names in match.arg");
    }
}
