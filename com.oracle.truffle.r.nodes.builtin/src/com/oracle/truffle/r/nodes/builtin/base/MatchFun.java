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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.SUBSTITUTE;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.MatchFunNodeGen.MatchFunInternalNodeGen;
import com.oracle.truffle.r.nodes.function.GetCallerFrameNode;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode;
import com.oracle.truffle.r.runtime.RDeparse;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.RTypes;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

@RBuiltin(name = "match.fun", kind = SUBSTITUTE, parameterNames = {"fun", "descend"}, nonEvalArgs = 0, behavior = COMPLEX)
public abstract class MatchFun extends RBuiltinNode.Arg2 {

    @Override
    public Object[] getDefaultParameterValues() {
        return new Object[]{RMissing.instance, RRuntime.LOGICAL_TRUE};
    }

    static {
        Casts casts = new Casts(MatchFun.class);
        casts.arg("descend").asLogicalVector().findFirst().map(toBoolean());
    }

    @Specialization
    protected static RFunction matchFun(VirtualFrame frame, RPromise funPromise, boolean descend,
                    @Cached("new()") PromiseHelperNode promiseHelper,
                    @Cached("createInternal()") MatchFunInternal internal) {
        Object funValue = promiseHelper.evaluate(frame, funPromise);
        return internal.execute(frame, funPromise, funValue, descend);
    }

    protected MatchFunInternal createInternal() {
        return MatchFunInternalNodeGen.create(this);
    }

    @TypeSystemReference(RTypes.class)
    abstract static class MatchFunInternal extends RBaseNode {

        protected static final int LIMIT = 3;
        private final MatchFun outer;

        private final BranchProfile needsMaterialize = BranchProfile.create();
        @Child private GetCallerFrameNode getCallerFrame = new GetCallerFrameNode();

        MatchFunInternal(MatchFun outer) {
            this.outer = outer;
        }

        public abstract RFunction execute(VirtualFrame frame, RPromise funPromise, Object funValue, boolean descend);

        @Specialization
        protected static RFunction matchfun(@SuppressWarnings("unused") RPromise funPromise, RFunction funValue, @SuppressWarnings("unused") boolean descend) {
            return funValue;
        }

        protected static ReadVariableNode createLookup(String name, boolean descend) {
            return descend ? ReadVariableNode.createFunctionLookup(RSyntaxNode.INTERNAL, name) : ReadVariableNode.create(RSyntaxNode.INTERNAL, name, false);
        }

        protected static String firstString(RAbstractStringVector vec) {
            return vec.getDataAt(0);
        }

        protected static String firstString(RSymbol symbol) {
            return symbol.getName();
        }

        private RFunction checkResult(Object result) {
            if (result instanceof RFunction) {
                return (RFunction) result;
            } else {
                CompilerDirectives.transferToInterpreter();
                throw outer.error(RError.Message.NON_FUNCTION, RDeparse.deparse(result));
            }
        }

        @SuppressWarnings("unused")
        @Specialization(limit = "LIMIT", guards = {"funValue.getLength() == 1", "funValue.getDataAt(0) == cachedName", "getCallerFrameDescriptor(frame) == cachedCallerFrameDescriptor"})
        protected RFunction matchfunCached(VirtualFrame frame, RPromise funPromise, RAbstractStringVector funValue, boolean descend,
                        @Cached("firstString(funValue)") String cachedName,
                        @Cached("getCallerFrameDescriptor(frame)") FrameDescriptor cachedCallerFrameDescriptor,
                        @Cached("createLookup(cachedName, descend)") ReadVariableNode lookup) {
            return checkResult(lookup.execute(frame, getCallerFrame.execute(frame)));
        }

        @Specialization(replaces = "matchfunCached", guards = {"funValue.getLength() == 1"})
        protected RFunction matchfunGeneric(VirtualFrame frame, @SuppressWarnings("unused") RPromise funPromise, RAbstractStringVector funValue, boolean descend) {
            return checkResult(slowPathLookup(funValue.getDataAt(0), getCallerFrame.execute(frame), descend));
        }

        @SuppressWarnings("unused")
        @Specialization(limit = "LIMIT", guards = {"funValue.getName() == cachedName", "getCallerFrameDescriptor(frame) == cachedCallerFrameDescriptor"})
        protected RFunction matchfunCached(VirtualFrame frame, RPromise funPromise, RSymbol funValue, boolean descend,
                        @Cached("firstString(funValue)") String cachedName,
                        @Cached("getCallerFrameDescriptor(frame)") FrameDescriptor cachedCallerFrameDescriptor,
                        @Cached("createLookup(cachedName, descend)") ReadVariableNode lookup) {
            return checkResult(lookup.execute(frame, getCallerFrame.execute(frame)));
        }

        @Specialization(replaces = "matchfunCached")
        protected RFunction matchfunGeneric(VirtualFrame frame, @SuppressWarnings("unused") RPromise funPromise, RSymbol funValue, boolean descend) {
            return checkResult(slowPathLookup(funValue.getName(), getCallerFrame.execute(frame), descend));
        }

        @TruffleBoundary
        private Object slowPathLookup(String name, MaterializedFrame frame, boolean descend) {
            Object result = descend ? ReadVariableNode.lookupFunction(name, frame) : ReadVariableNode.lookupAny(name, frame, false);
            if (result == null) {
                throw outer.error(descend ? RError.Message.UNKNOWN_FUNCTION : RError.Message.UNKNOWN_OBJECT, name);
            }
            return result;
        }

        @Fallback
        protected RFunction matchfunFallback(VirtualFrame frame, RPromise funPromise, @SuppressWarnings("unused") Object funValue, boolean descend) {
            RSyntaxElement rep = getPromiseRep(funPromise);
            String lookupName = null;
            if (rep instanceof RSyntaxLookup) {
                needsMaterialize.enter();
                RSyntaxLookup lookup = (RSyntaxLookup) rep;
                lookupName = lookup.getIdentifier();
                Object value = lookupLocal(frame.materialize(), lookupName);
                if (value instanceof RPromise) {
                    lookupName = null;
                    rep = getPromiseRep((RPromise) value);
                    if (rep instanceof RSyntaxLookup) {
                        lookup = (RSyntaxLookup) rep;
                        lookupName = lookup.getIdentifier();
                    }
                }
            }
            if (lookupName != null) {
                return checkResult(slowPathLookup(lookupName, getCallerFrame.execute(frame), descend));
            } else {
                CompilerDirectives.transferToInterpreter();
                throw outer.error(RError.Message.NOT_FUNCTION, RDeparse.deparseSyntaxElement(rep));
            }
        }

        @TruffleBoundary
        private static RSyntaxNode getPromiseRep(RPromise funPromise) {
            return funPromise.getRep().asRSyntaxNode();
        }

        @TruffleBoundary
        private static Object lookupLocal(MaterializedFrame frame, String lookupName) {
            FrameSlot slot = frame.getFrameDescriptor().findFrameSlot(lookupName);
            if (slot == null) {
                return null;
            } else {
                return FrameSlotChangeMonitor.getValue(slot, frame);
            }
        }

        protected FrameDescriptor getCallerFrameDescriptor(VirtualFrame frame) {
            return getCallerFrame.execute(frame).getFrameDescriptor();
        }
    }
}
