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
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.MatchArgNodeGen.MatchArgInternalNodeGen;
import com.oracle.truffle.r.nodes.function.FormalArguments;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RTypes;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

@RBuiltin(name = "match.arg", kind = SUBSTITUTE, parameterNames = {"arg", "choices", "several.ok"}, nonEvalArgs = {0}, behavior = COMPLEX)
public abstract class MatchArg extends RBuiltinNode.Arg3 {

    static {
        Casts.noCasts(MatchArg.class);
    }

    @Override
    public Object[] getDefaultParameterValues() {
        return new Object[]{RMissing.instance, RMissing.instance, RRuntime.LOGICAL_FALSE};
    }

    @TypeSystemReference(RTypes.class)
    protected abstract static class MatchArgInternal extends Node {

        @Child private PMatch pmatch = PMatchNodeGen.create();
        @Child private Identical identical = IdenticalNodeGen.create();

        @Child private CastNode argCast = newCastBuilder().asStringVector().buildCastNode();
        @Child private CastNode choicesCast = newCastBuilder().allowMissing().mustBe(stringValue()).asStringVector().buildCastNode();
        @Child private CastNode severalOKCast = newCastBuilder().mustBe(logicalValue()).asLogicalVector().findFirst().map(toBoolean()).buildCastNode();

        public abstract Object execute(Object arg, Object choices, Object severalOK);

        public final Object castAndExecute(Object arg, Object choices, Object severalOK) {
            return execute(argCast.doCast(arg), choicesCast.doCast(choices), severalOKCast.doCast(severalOK));
        }

        @Specialization
        protected String matchArgNULL(@SuppressWarnings("unused") RNull arg, RAbstractStringVector choices, @SuppressWarnings("unused") boolean severalOK,
                        @Cached("createBinaryProfile()") ConditionProfile isEmptyProfile) {
            return isEmptyProfile.profile(choices.getLength() == 0) ? RRuntime.STRING_NA : choices.getDataAt(0);
        }

        private void checkEmpty(RAbstractStringVector choices, int count) {
            if (count == 0) {
                CompilerDirectives.transferToInterpreter();
                StringBuilder choicesString = new StringBuilder();
                for (int i = 0; i < choices.getLength(); i++) {
                    choicesString.append(i == 0 ? "" : ", ").append(RRuntime.escapeString(choices.getDataAt(i), false, true));
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

        @Specialization(guards = "!severalOK")
        protected String matchArg(RAbstractStringVector arg, RAbstractStringVector choices, @SuppressWarnings("unused") boolean severalOK) {
            if (identical.executeByte(arg, choices, true, true, true, true, true) == RRuntime.LOGICAL_TRUE) {
                return choices.getDataAt(0);
            }
            if (arg.getLength() != 1) {
                CompilerDirectives.transferToInterpreter();
                throw RError.error(this, Message.MUST_BE_SCALAR, "arg");
            }
            RIntVector matched = pmatch.execute(arg, choices, -1, true);
            int count = count(matched);
            checkEmpty(choices, count);
            if (count > 1) {
                CompilerDirectives.transferToInterpreter();
                throw RError.error(this, Message.MORE_THAN_ONE_MATCH, "match.arg");
            }
            return choices.getDataAt(matched.getDataAt(0) - 1);
        }

        @Specialization(guards = "severalOK")
        protected Object matchArgSeveral(RAbstractStringVector arg, RAbstractStringVector choices, @SuppressWarnings("unused") boolean severalOK) {
            if (arg.getLength() == 0) {
                CompilerDirectives.transferToInterpreter();
                throw RError.error(this, Message.MUST_BE_GE_ONE, "arg");
            }
            RIntVector matched = pmatch.execute(arg, choices, -1, true);
            int count = count(matched);
            if (count == 1) {
                return choices.getDataAt(matched.getDataAt(0) - 1);
            }
            checkEmpty(choices, count);
            String[] result = new String[count];
            for (int i = 0; i < matched.getLength(); i++) {
                result[i] = choices.getDataAt(matched.getDataAt(i) - 1);
            }
            return RDataFactory.createStringVector(result, choices.isComplete());
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
                if (symbol == arguments.getSignature().getName(i)) {
                    RNode defaultArg = arguments.getDefaultArgument(i);
                    if (defaultArg == null) {
                        this.value = RContext.getASTBuilder().constant(RSyntaxNode.INTERNAL, RDataFactory.createEmptyStringVector()).asRNode();
                    }
                    this.value = RContext.getASTBuilder().process(defaultArg.asRSyntaxNode()).asRNode();
                    return;
                }
            }
            throw RError.error(RError.SHOW_CALLER, Message.INVALID_USE, "match.arg");
        }

        public boolean isSupported(VirtualFrame frame, RPromise arg) {
            return RArguments.getFunction(frame).getTarget() == target && arg.getClosure().asSymbol() == symbol;
        }

        public Object execute(VirtualFrame frame) {
            return value.execute(frame);
        }
    }

    protected static MatchArgInternal createInternal() {
        return MatchArgInternalNodeGen.create();
    }

    @Specialization(limit = "3", guards = "choicesValue.isSupported(frame, arg)")
    protected Object matchArg(VirtualFrame frame, RPromise arg, @SuppressWarnings("unused") RMissing choices, Object severalOK,
                    @Cached("new(frame, arg)") MatchArgChoices choicesValue,
                    @Cached("createInternal()") MatchArgInternal internal,
                    @Cached("new()") PromiseHelperNode promiseHelper) {
        return internal.castAndExecute(promiseHelper.evaluate(frame, arg), choicesValue.execute(frame), severalOK);
    }

    protected static boolean isRMissing(Object value) {
        return value instanceof RMissing;
    }

    @Specialization(guards = "!isRMissing(choices)")
    protected Object matchArg(VirtualFrame frame, RPromise arg, Object choices, Object severalOK,
                    @Cached("createInternal()") MatchArgInternal internal,
                    @Cached("new()") PromiseHelperNode promiseHelper) {
        return internal.castAndExecute(promiseHelper.evaluate(frame, arg), choices, severalOK);
    }

    @SuppressWarnings("unused")
    @Fallback
    protected Object matchArgFallback(Object arg, Object choices, Object severalOK) {
        throw error(Message.GENERIC, "too many different names in match.arg");
    }
}
