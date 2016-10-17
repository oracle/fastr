/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.function;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.nodes.access.variables.LocalReadVariableNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.FastROptions;
import com.oracle.truffle.r.runtime.RDeparse;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.builtins.RBuiltinDescriptor;
import com.oracle.truffle.r.runtime.builtins.RSpecialFactory;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxCall;
import com.oracle.truffle.r.runtime.nodes.RSyntaxConstant;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

final class PeekLocalVariableNode extends RNode {

    @Child private LocalReadVariableNode read;

    private final ConditionProfile isPromiseProfile = ConditionProfile.createBinaryProfile();
    private final ValueProfile valueProfile = ValueProfile.createClassProfile();

    PeekLocalVariableNode(String name) {
        this.read = LocalReadVariableNode.create(name, false);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object value = read.execute(frame);
        if (value == null) {
            throw RSpecialFactory.throwFullCallNeeded();
        }
        if (isPromiseProfile.profile(value instanceof RPromise)) {
            RPromise promise = (RPromise) value;
            if (!promise.isEvaluated()) {
                throw RSpecialFactory.throwFullCallNeeded();
            }
            return valueProfile.profile(promise.getValue());
        }
        return valueProfile.profile(value);
    }
}

public final class RCallSpecialNode extends RCallBaseNode implements RSyntaxNode, RSyntaxCall {

    private static final int NO_RECURSIVE_ARGUMENT_INDEX = -1;
    private static final boolean useSpecials = FastROptions.UseSpecials.getBooleanValue();

    // currently cannot be RSourceSectionNode because of TruffleDSL restrictions
    @CompilationFinal private SourceSection sourceSectionR;

    @Override
    public void setSourceSection(SourceSection sourceSection) {
        assert sourceSection != null;
        this.sourceSectionR = sourceSection;
    }

    @Override
    public SourceSection getSourceSection() {
        return sourceSectionR;
    }

    @Child private ForcePromiseNode functionNode;
    @Child private RNode special;

    private final RSyntaxNode[] arguments;
    private final ArgumentsSignature signature;
    private final RFunction expectedFunction;

    /**
     * If this instance is argument of another RCallSpecialNode (parent), then this will be the
     * index into the parent arguments array, otherwise {@link #NO_RECURSIVE_ARGUMENT_INDEX}.
     */
    private int argumentIndex = NO_RECURSIVE_ARGUMENT_INDEX;

    private RCallSpecialNode(SourceSection sourceSection, RNode functionNode, RFunction expectedFunction, RSyntaxNode[] arguments, ArgumentsSignature signature, RNode special) {
        this.sourceSectionR = sourceSection;
        this.expectedFunction = expectedFunction;
        this.special = special;
        this.functionNode = new ForcePromiseNode(functionNode);
        this.arguments = arguments;
        this.signature = signature;
    }

    public static RSyntaxNode createCall(SourceSection sourceSection, RNode functionNode, ArgumentsSignature signature, RSyntaxNode[] arguments) {
        RCallSpecialNode special = null;
        if (useSpecials) {
            special = tryCreate(sourceSection, functionNode, signature, arguments);
        }
        if (special != null) {
            if (sourceSection == RSyntaxNode.EAGER_DEPARSE) {
                RDeparse.ensureSourceSection(special);
            }
            return special;
        } else {
            return RCallNode.createCall(sourceSection, functionNode, signature, arguments);
        }
    }

    private static RCallSpecialNode tryCreate(SourceSection sourceSection, RNode functionNode, ArgumentsSignature signature, RSyntaxNode[] arguments) {
        RSyntaxNode syntaxFunction = functionNode.asRSyntaxNode();
        if (!(syntaxFunction instanceof RSyntaxLookup)) {
            // LHS is not a simple lookup -> bail out
            return null;
        }
        for (RSyntaxNode argument : arguments) {
            if (!(argument instanceof RSyntaxLookup || argument instanceof RSyntaxConstant || argument instanceof RCallSpecialNode)) {
                // argument is not a simple lookup or constant value or another special -> bail out
                return null;
            }
        }
        String name = ((RSyntaxLookup) syntaxFunction).getIdentifier();
        RBuiltinDescriptor builtinDescriptor = RContext.lookupBuiltinDescriptor(name);
        if (builtinDescriptor == null) {
            // no builtint -> bail out
            return null;
        }
        RSpecialFactory specialCall = builtinDescriptor.getSpecialCall();
        if (specialCall == null) {
            // no special call definition -> bail out
            return null;
        }
        RNode[] localArguments = new RNode[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            if (arguments[i] instanceof RSyntaxLookup) {
                localArguments[i] = new PeekLocalVariableNode(((RSyntaxLookup) arguments[i]).getIdentifier());
            } else if (arguments[i] instanceof RSyntaxConstant) {
                localArguments[i] = RContext.getASTBuilder().process(arguments[i]).asRNode();
            } else {
                assert arguments[i] instanceof RCallSpecialNode;
                ((RCallSpecialNode) arguments[i]).setArgumentIndex(i);
                localArguments[i] = arguments[i].asRNode();
            }
        }
        RNode special = specialCall.create(signature, localArguments);
        if (special == null) {
            // the factory refused to create a special call -> bail out
            return null;
        }
        RFunction expectedFunction = RContext.lookupBuiltin(name);
        RInternalError.guarantee(expectedFunction != null);

        return new RCallSpecialNode(sourceSection, functionNode, expectedFunction, arguments, signature, special);
    }

    @Override
    public Object execute(VirtualFrame frame, Object function) {
        try {
            if (function != expectedFunction) {
                // the actual function differs from the expected function
                throw RSpecialFactory.throwFullCallNeeded();
            }
            return special.execute(frame);
        } catch (RecursiveSpecialBailout bailout) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throwOnRecursiveSpecial();
            return replace(getRCallNode(rewriteSpecialArgument(bailout))).execute(frame, function);
        } catch (RSpecialFactory.FullCallNeededException e) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throwOnRecursiveSpecial();
            return replace(getRCallNode()).execute(frame, function);
        }
    }

    private void throwOnRecursiveSpecial() {
        if (isRecursiveSpecial()) {
            throw new RecursiveSpecialBailout(argumentIndex);
        }
    }

    private RSyntaxNode[] rewriteSpecialArgument(RecursiveSpecialBailout bailout) {
        // Note: other arguments that may be specials too, stay specials, their parent node will be
        // changed in createRCallNode, but we are never going to use the original parent, which is
        // the 'this.special' node that bailed out.
        // Note 2: we have to make a copy of the array, because this node may have been shallow
        // copied and the other copy will keep its copied parent, i.e. bailout exception from here
        // will not reach the copied parent, but we would rewrite one of its arguments to full-blown
        // RCallNode. It seems that bailing out happens less frequently than Node.copy, so we do the
        // copying here.
        RSyntaxNode[] newArguments = Arrays.copyOf(arguments, arguments.length);
        RCallSpecialNode arg = (RCallSpecialNode) arguments[bailout.argumentIndex];
        newArguments[bailout.argumentIndex] = arg.getRCallNode();
        return newArguments;
    }

    private boolean isRecursiveSpecial() {
        // Note: we need to check the parent's parent, because it might have been rewritten by
        // bailout of some of its other arguments. If parent is special node, then its parent must
        // be RCallSpecialNode
        return argumentIndex != NO_RECURSIVE_ARGUMENT_INDEX && getParent() != null && getParent().getParent() instanceof RCallSpecialNode;
    }

    private RCallNode getRCallNode(RSyntaxNode[] arguments) {
        return RCallNode.createCall(sourceSectionR, functionNode == null ? null : functionNode.getValueNode(), signature, arguments);
    }

    private RCallNode getRCallNode() {
        return getRCallNode(arguments);
    }

    private void setArgumentIndex(int index) {
        argumentIndex = index;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return execute(frame, functionNode.execute(frame));
    }

    @Override
    public RSyntaxElement getSyntaxLHS() {
        ForcePromiseNode func = functionNode;
        return func == null || func.getValueNode() == null ? RSyntaxLookup.createDummyLookup(RSyntaxNode.LAZY_DEPARSE, "FUN", true) : func.getValueNode().asRSyntaxNode();
    }

    @Override
    public ArgumentsSignature getSyntaxSignature() {
        return signature == null ? ArgumentsSignature.empty(1) : signature;
    }

    @Override
    public RSyntaxElement[] getSyntaxArguments() {
        return arguments == null ? new RSyntaxElement[]{RSyntaxLookup.createDummyLookup(RSyntaxNode.LAZY_DEPARSE, "...", false)} : arguments;
    }

    @SuppressWarnings("serial")
    private static final class RecursiveSpecialBailout extends RuntimeException {
        public final int argumentIndex;

        RecursiveSpecialBailout(int argumentIndex) {
            this.argumentIndex = argumentIndex;
        }

        @Override
        public synchronized Throwable fillInStackTrace() {
            return null;
        }
    }
}
