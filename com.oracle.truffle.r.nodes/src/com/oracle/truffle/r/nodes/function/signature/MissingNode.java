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
package com.oracle.truffle.r.nodes.function.signature;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.nodes.access.variables.LocalReadVariableNode;
import com.oracle.truffle.r.nodes.control.OperatorNode;
import com.oracle.truffle.r.nodes.function.GetMissingValueNode;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode;
import com.oracle.truffle.r.nodes.function.RMissingHelper;
import com.oracle.truffle.r.nodes.function.signature.MissingNodeFactory.MissingCheckCacheNodeGen;
import com.oracle.truffle.r.nodes.function.visibility.SetVisibilityNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RPromise.EagerPromise;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;

public final class MissingNode extends OperatorNode {

    public abstract static class MissingCheckCache extends Node {

        protected static final int CACHE_LIMIT = 3;

        private final int level;

        protected MissingCheckCache(int level) {
            this.level = level;
        }

        public static MissingCheckCache create(int level) {
            return MissingCheckCacheNodeGen.create(level);
        }

        public abstract boolean execute(Frame frame, String symbol);

        protected MissingCheckLevel createNodeForRep(String symbol) {
            return new MissingCheckLevel(symbol, level);
        }

        @Specialization(limit = "CACHE_LIMIT", guards = "cachedSymbol == symbol")
        public static boolean checkCached(Frame frame, @SuppressWarnings("unused") String symbol,
                        @SuppressWarnings("unused") @Cached("symbol") String cachedSymbol,
                        @Cached("createNodeForRep(symbol)") MissingCheckLevel node) {
            return node.execute(frame);
        }

        @Specialization(replaces = "checkCached")
        public static boolean check(Frame frame, String symbol) {
            return RMissingHelper.isMissingArgument(frame, symbol);
        }
    }

    protected static class MissingCheckLevel extends Node {

        @Child private GetMissingValueNode getMissingValue;
        @Child private MissingCheckCache recursive;
        @Child private PromiseHelperNode promiseHelper;

        @CompilationFinal private FrameDescriptor recursiveDesc;

        private final ConditionProfile isNullProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile isMissingProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile isPromiseProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile isSymbolNullProfile = ConditionProfile.createBinaryProfile();
        private final int level;

        MissingCheckLevel(String symbol, int level) {
            this.level = level;
            this.getMissingValue = GetMissingValueNode.create(symbol);
        }

        public boolean execute(Frame frame) {
            // Read symbols value directly
            Object value = getMissingValue.execute(frame);
            if (isNullProfile.profile(value == null)) {
                // In case we are not able to read the symbol in current frame: This is not an
                // argument and thus return false
                return false;
            }

            if (isMissingProfile.profile(RMissingHelper.isMissing(value))) {
                return true;
            }

            // This might be a promise...
            if (isPromiseProfile.profile(value instanceof RPromise)) {
                RPromise promise = (RPromise) value;
                if (promiseHelper == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    promiseHelper = insert(new PromiseHelperNode());
                    recursiveDesc = !promise.isEvaluated() && promise.getFrame() != null ? promise.getFrame().getFrameDescriptor() : null;
                }
                if (level == 0 && promiseHelper.isDefaultArgument(promise)) {
                    return true;
                }
                if (promiseHelper.isEvaluated(promise)) {
                    if (level > 0) {
                        return false;
                    }
                } else {
                    // Check: If there is a cycle, return true. (This is done like in GNU R)
                    if (promiseHelper.isUnderEvaluation(promise)) {
                        return true;
                    }
                }
                String symbol = promise.getClosure().asSymbol();
                if (isSymbolNullProfile.profile(symbol == null)) {
                    return false;
                } else {
                    if (promise instanceof EagerPromise && !((EagerPromise) promise).isDeoptimized()) {
                        return false;
                    }
                    if (recursiveDesc != null) {
                        promiseHelper.materialize(promise); // Ensure that promise holds a frame
                    }
                    if (recursiveDesc == null || recursiveDesc != promise.getFrame().getFrameDescriptor()) {
                        if (promiseHelper.isEvaluated(promise)) {
                            return false;
                        } else {
                            return RMissingHelper.isMissingName(promise);
                        }
                    } else {
                        if (recursiveDesc == null) {
                            promiseHelper.materialize(promise); // Ensure that promise holds a frame
                        }
                        try {
                            promise.setUnderEvaluation();
                            if (recursive == null) {
                                CompilerDirectives.transferToInterpreterAndInvalidate();
                                recursive = insert(MissingCheckCache.create(level + 1));
                            }
                            return recursive.execute(promise.getFrame(), symbol);
                        } finally {
                            promise.resetUnderEvaluation();
                        }
                    }
                }
            }
            return false;
        }
    }

    @Child private MissingCheckLevel level;
    @Child private LocalReadVariableNode readVarArgs;

    /**
     * We need to set the visibility ourselves, because this node is created directly in RASTBuilder
     * without being wrapped by BuiltinCallNode. This node must set the visibility value, because it
     * can be the only statement of a function.
     */
    @Child private SetVisibilityNode visibility = SetVisibilityNode.create();

    private final ArgumentsSignature signature;
    private final RSyntaxElement[] args;

    public MissingNode(SourceSection source, RSyntaxLookup operator, ArgumentsSignature signature, RSyntaxElement[] args) {
        super(source, operator);
        this.signature = signature;
        this.args = args;
    }

    @Override
    public Byte execute(VirtualFrame frame) {
        if (level == null && readVarArgs == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            if (args.length != 1) {
                throw error(Message.ARGUMENTS_PASSED, args.length, "'missing'", 1);
            }
            RSyntaxElement arg = args[0];
            if (!(arg instanceof RSyntaxLookup)) {
                throw error(Message.INVALID_USE, "missing");
            }
            String identifier = ((RSyntaxLookup) arg).getIdentifier();
            if (ArgumentsSignature.VARARG_NAME.equals(identifier)) {
                readVarArgs = insert(LocalReadVariableNode.create(ArgumentsSignature.VARARG_NAME, false));
            } else {
                level = insert(new MissingCheckLevel(identifier, 0));
            }
        }
        if (level != null) {
            return createResult(frame, level.execute(frame));
        }
        if (readVarArgs != null) {
            RArgsValuesAndNames varArgs = (RArgsValuesAndNames) readVarArgs.execute(frame);
            if (varArgs == null) {
                CompilerDirectives.transferToInterpreter();
                throw error(Message.MISSING_ARGUMENTS);
            }
            return createResult(frame, varArgs.getLength() == 0);
        }
        throw RInternalError.shouldNotReachHere();
    }

    @Override
    public ArgumentsSignature getSyntaxSignature() {
        return signature;
    }

    @Override
    public RSyntaxElement[] getSyntaxArguments() {
        return args;
    }

    private Byte createResult(Frame frame, boolean result) {
        visibility.execute(frame, true);
        return RRuntime.asLogical(result);
    }
}
