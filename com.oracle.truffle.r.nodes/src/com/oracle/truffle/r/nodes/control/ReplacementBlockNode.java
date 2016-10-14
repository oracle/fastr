/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.truffle.r.nodes.control;

import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.access.RemoveAndAnswerNode;
import com.oracle.truffle.r.nodes.access.WriteVariableNode;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.function.RCallSpecialNode;
import com.oracle.truffle.r.nodes.function.RCallSpecialNode.RecursiveSpecialBailout;
import com.oracle.truffle.r.nodes.function.visibility.SetVisibilityNode;
import com.oracle.truffle.r.nodes.unary.GetNonSharedNodeGen;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.builtins.RSpecialFactory.FullCallNeededException;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSourceSectionNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxCall;
import com.oracle.truffle.r.runtime.nodes.RSyntaxConstant;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

/**
 * Represents a replacement block consisting of execution of the RHS, call to the actual replacement
 * sequence and removal of RHS returning the RHS value to the caller. The actual replacement is
 * created lazily. Moreover, we use 'special' fast-path version of replacement where possible with
 * fallback to generic implementation.
 */
public final class ReplacementBlockNode extends RSourceSectionNode implements RSyntaxNode, RSyntaxCall {
    /**
     * Used to initialize {@link #tempNamesStartIndex}. When we are processing a replacement AST, we
     * set this value so that newly created replacements within the original replacement have
     * different temporary variable names. Example {@code x[x[1]<-2]<-3}.
     */
    private static int tempNamesCount = 0;

    /**
     * Should be one of {@link ReplacementNode} or {@link ReplacementNodeSpecial}.
     */
    @Child private ReplacementBase replacementNode;

    @Child private WriteVariableNode storeRhs;
    @Child private RemoveAndAnswerNode removeRhs;
    @Child private SetVisibilityNode visibility = SetVisibilityNode.create();

    private final String operator;
    private final RSyntaxNode lhs;
    private final boolean isSuper;
    private final int tempNamesStartIndex;

    public ReplacementBlockNode(SourceSection src, String operator, RSyntaxNode lhs, RSyntaxNode rhs, boolean isSuper) {
        super(src);
        assert "<-".equals(operator) || "<<-".equals(operator) || "=".equals(operator);
        assert lhs != null && rhs != null;
        tempNamesStartIndex = tempNamesCount;
        storeRhs = WriteVariableNode.createAnonymous(getRhsName(), rhs.asRNode(), WriteVariableNode.Mode.INVISIBLE);
        removeRhs = RemoveAndAnswerNode.create(getRhsName());
        this.operator = operator;
        this.lhs = lhs;
        this.isSuper = isSuper;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        storeRhs.execute(frame);
        getReplacementNode().execute(frame);
        try {
            return removeRhs.execute(frame);
        } finally {
            visibility.execute(frame, false);
        }
    }

    /**
     * Support for syntax tree visitor.
     */
    public RSyntaxNode getLhs() {
        return lhs;
    }

    /**
     * Support for syntax tree visitor.
     */
    public RSyntaxNode getRhs() {
        return storeRhs.getRhs().asRSyntaxNode();
    }

    @Override
    public RSyntaxElement getSyntaxLHS() {
        return RSyntaxLookup.createDummyLookup(null, operator, true);
    }

    @Override
    public ArgumentsSignature getSyntaxSignature() {
        return ArgumentsSignature.empty(2);
    }

    @Override
    public RSyntaxElement[] getSyntaxArguments() {
        return new RSyntaxElement[]{lhs, storeRhs.getRhs().asRSyntaxNode()};
    }

    private RNode getReplacementNode() {
        if (replacementNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            replacementNode = insert(createReplacementNode(true));
        }
        return replacementNode;
    }

    private String getRhsName() {
        return "*tmpr*" + (tempNamesStartIndex - 1);
    }

    private RNode createReplacementNodeWithoutSpecials() {
        return createReplacementNode(false);
    }

    private ReplacementBase createReplacementNode(boolean useSpecials) {
        CompilerAsserts.neverPartOfCompilation();

        /*
         * Collect all the function calls in this replacement. For "a(b(x)) <- z", this would be
         * "a(...)" and "b(...)".
         */
        List<RSyntaxCall> calls = new ArrayList<>();
        RSyntaxElement current = lhs;
        boolean onlySpecials = true;
        while (!(current instanceof RSyntaxLookup)) {
            if (!(current instanceof RSyntaxCall)) {
                if (current instanceof RSyntaxConstant && ((RSyntaxConstant) current).getValue() == RNull.instance) {
                    throw RError.error(this, RError.Message.INVALID_NULL_LHS);
                } else {
                    throw RError.error(this, RError.Message.NON_LANG_ASSIGNMENT_TARGET);
                }
            }
            RSyntaxCall call = (RSyntaxCall) current;
            calls.add(call);

            RSyntaxElement syntaxLHS = call.getSyntaxLHS();
            if (call.getSyntaxArguments().length == 0 || !(syntaxLHS instanceof RSyntaxLookup || isNamespaceLookupCall(syntaxLHS))) {
                throw RError.error(this, RError.Message.INVALID_NULL_LHS);
            }
            onlySpecials &= call instanceof RCallSpecialNode;
            current = call.getSyntaxArguments()[0];
        }
        RSyntaxLookup variable = (RSyntaxLookup) current;
        SourceSection source = getSourceSection();

        // Note: if specials are turned off in FastR, onlySpecials will never be true
        if (onlySpecials && useSpecials && !isSuper) {
            return createSpecialReplacement(source, calls, variable);
        }

        return createGenericReplacement(source, calls, variable);
    }

    /**
     * Creates a replacement that consists only of {@link RCallSpecialNode} calls.
     */
    private ReplacementBase createSpecialReplacement(SourceSection source, List<RSyntaxCall> calls, RSyntaxLookup variable) {
        RNode extractFunc = createReplacementForVariableUsing(variable, isSuper);
        for (int i = calls.size() - 1; i >= 1; i--) {
            extractFunc = createSpecialFunctionQuery(extractFunc.asRSyntaxNode(), calls.get(i));
        }
        RNode updateFunc = createFunctionUpdate(source, extractFunc.asRSyntaxNode(), ReadVariableNode.create(getRhsName()), calls.get(0));
        assert updateFunc instanceof RCallSpecialNode : "should be only specials";
        return new ReplacementNodeSpecial((RCallSpecialNode) updateFunc);
    }

    /**
     * When there are more than two function calls in LHS, then we save some function calls by
     * saving the intermediate results into temporary variables and reusing them.
     */
    private ReplacementBase createGenericReplacement(SourceSection source, List<RSyntaxCall> calls, RSyntaxLookup variable) {
        List<RNode> instructions = new ArrayList<>();
        int tempNamesIndex = tempNamesStartIndex;
        tempNamesCount += calls.size() + 1;

        /*
         * Create the calls that extract inner components - only needed for complex replacements
         * like "a(b(x)) <- z" (where we would extract "b(x)").
         */
        for (int i = calls.size() - 1; i >= 1; i--) {
            ReadVariableNode newLhs = ReadVariableNode.create("*tmp*" + (tempNamesIndex + i + 1));
            RNode update = createSpecialFunctionQuery(newLhs, calls.get(i));
            instructions.add(WriteVariableNode.createAnonymous("*tmp*" + (tempNamesIndex + i), update, WriteVariableNode.Mode.INVISIBLE));
        }
        /*
         * Create the update calls, for "a(b(x)) <- z", this would be `a<-` and `b<-`.
         */
        for (int i = 0; i < calls.size(); i++) {
            RNode update = createFunctionUpdate(source, ReadVariableNode.create("*tmp*" + (tempNamesIndex + i + 1)), ReadVariableNode.create("*tmpr*" + (tempNamesIndex + i - 1)),
                            calls.get(i));
            if (i < calls.size() - 1) {
                instructions.add(WriteVariableNode.createAnonymous("*tmpr*" + (tempNamesIndex + i), update, WriteVariableNode.Mode.INVISIBLE));
            } else {
                instructions.add(WriteVariableNode.createAnonymous(variable.getIdentifier(), update, WriteVariableNode.Mode.REGULAR, isSuper));
            }
        }

        ReadVariableNode variableValue = createReplacementForVariableUsing(variable, isSuper);
        ReplacementNode newReplacementNode = new ReplacementNode(variableValue, "*tmp*" + (tempNamesIndex + calls.size()), instructions);

        tempNamesCount -= calls.size() + 1;
        return newReplacementNode;
    }

    private static ReadVariableNode createReplacementForVariableUsing(RSyntaxLookup var, boolean isSuper) {
        if (isSuper) {
            return ReadVariableNode.createSuperLookup(var.getSourceSection(), var.getIdentifier());
        } else {
            return ReadVariableNode.create(var.getSourceSection(), var.getIdentifier(), true);
        }
    }

    /*
     * Determines if syntax call is of the form foo::bar
     */
    private static boolean isNamespaceLookupCall(RSyntaxElement e) {
        if (e instanceof RSyntaxCall) {
            RSyntaxCall call = (RSyntaxCall) e;
            // check for syntax nodes as this will be required to recreate a call during
            // replacement form construction in createFunctionUpdate
            if (call.getSyntaxLHS() instanceof RSyntaxLookup && call.getSyntaxLHS() instanceof RSyntaxNode) {
                if (((RSyntaxLookup) call.getSyntaxLHS()).getIdentifier().equals("::")) {
                    RSyntaxElement[] args = call.getSyntaxArguments();
                    if (args.length == 2 && args[0] instanceof RSyntaxLookup && args[0] instanceof RSyntaxNode && args[1] instanceof RSyntaxLookup && args[1] instanceof RSyntaxNode) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Creates a call that looks like {@code fun} but has the first argument replaced with
     * {@code newLhs}.
     */
    private static RNode createSpecialFunctionQuery(RSyntaxNode newLhs, RSyntaxCall fun) {
        RSyntaxElement[] arguments = fun.getSyntaxArguments();

        RSyntaxNode[] argNodes = new RSyntaxNode[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            argNodes[i] = i == 0 ? newLhs : process(arguments[i]);
        }

        return RCallSpecialNode.createCallInReplace(fun.getSourceSection(), process(fun.getSyntaxLHS()).asRNode(), fun.getSyntaxSignature(), argNodes).asRNode();
    }

    /**
     * Creates a call that looks like {@code fun}, but has its first argument replaced with
     * {@code newLhs}, its target turned into an update function ("foo<-"), with the given value
     * added to the arguments list.
     */
    private static RNode createFunctionUpdate(SourceSection source, RSyntaxNode newLhs, RSyntaxNode rhs, RSyntaxCall fun) {
        RSyntaxElement[] arguments = fun.getSyntaxArguments();

        ArgumentsSignature signature = fun.getSyntaxSignature();
        RSyntaxNode[] argNodes = new RSyntaxNode[arguments.length + 1];
        String[] names = new String[argNodes.length];
        for (int i = 0; i < arguments.length; i++) {
            names[i] = signature.getName(i);
            argNodes[i] = i == 0 ? newLhs : process(arguments[i]);
        }
        argNodes[argNodes.length - 1] = rhs;
        names[argNodes.length - 1] = "value";

        RSyntaxElement syntaxLHS = fun.getSyntaxLHS();
        RSyntaxNode newSyntaxLHS;
        if (syntaxLHS instanceof RSyntaxLookup) {
            RSyntaxLookup lookupLHS = (RSyntaxLookup) syntaxLHS;
            String symbol = lookupLHS.getIdentifier();
            if ("slot".equals(symbol) || "@".equals(symbol)) {
                // this is pretty gross, but at this point seems like the only way to get setClass
                // to work properly
                argNodes[0] = GetNonSharedNodeGen.create(argNodes[0].asRNode());
            }
            newSyntaxLHS = lookup(lookupLHS.getSourceSection(), symbol + "<-", true);
        } else {
            // data types (and lengths) are verified in isNamespaceLookupCall
            RSyntaxCall callLHS = (RSyntaxCall) syntaxLHS;
            RSyntaxElement[] oldArgs = callLHS.getSyntaxArguments();
            RSyntaxNode[] newArgs = new RSyntaxNode[2];
            newArgs[0] = (RSyntaxNode) oldArgs[0];
            newArgs[1] = lookup(oldArgs[1].getSourceSection(), ((RSyntaxLookup) oldArgs[1]).getIdentifier() + "<-", true);
            newSyntaxLHS = RCallSpecialNode.createCall(callLHS.getSourceSection(), ((RSyntaxNode) callLHS.getSyntaxLHS()).asRNode(), callLHS.getSyntaxSignature(), newArgs);
        }
        return RCallSpecialNode.createCall(source, newSyntaxLHS.asRNode(), ArgumentsSignature.get(names), argNodes).asRNode();
    }

    private static RSyntaxNode process(RSyntaxElement original) {
        return RContext.getASTBuilder().process(original);
    }

    private static RSyntaxNode lookup(SourceSection source, String symbol, boolean functionLookup) {
        return RContext.getASTBuilder().lookup(source, symbol, functionLookup);
    }

    /*
     * Encapsulates check for the specific structure of replacements, to display the replacement
     * instead of the "internal" form (with *tmp*, etc.) of the update call.
     */
    public static RLanguage getRLanguage(RLanguage language) {
        RSyntaxNode sn = (RSyntaxNode) language.getRep();
        Node parent = RASTUtils.unwrapParent(sn.asNode());
        if (parent instanceof WriteVariableNode) {
            WriteVariableNode wvn = (WriteVariableNode) parent;
            if (wvn.getParent() instanceof ReplacementBase) {
                // the parent of Replacement interface is always ReplacementBlockNode
                return RDataFactory.createLanguage((RNode) wvn.getParent().getParent());
            }
        }

        return null;
    }

    /**
     * Base class for nodes implementing the actual replacement.
     */
    private abstract static class ReplacementBase extends RNode {
    }

    /**
     * Replacement that is made of only special calls, if one of the special calls falls back to
     * full version, the replacement also falls back to {@link ReplacementNode}.
     */
    private static final class ReplacementNodeSpecial extends ReplacementBase {

        @Child private RCallSpecialNode replaceCall;

        ReplacementNodeSpecial(RCallSpecialNode replaceCall) {
            this.replaceCall = replaceCall;
            this.replaceCall.setPropagateFullCallNeededException(true);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            try {
                // Note: the very last call is the actual assignment, e.g. [[<-, if this call's
                // argument is shared, it bails out. Moreover, if that call's argument is not
                // shared, it could not be extracted from a shared container (list), so we should be
                // OK with not calling any other update function and just update the value directly.
                replaceCall.execute(frame);
            } catch (FullCallNeededException | RecursiveSpecialBailout e) {
                assert getParent() instanceof ReplacementBlockNode;
                RNode newReplacement = ((ReplacementBlockNode) getParent()).createReplacementNodeWithoutSpecials();
                return replace(newReplacement).execute(frame);
            }
            return null;
        }
    }

    /**
     * Holds the sequence of nodes created for R's replacement assignment.
     */
    public static final class ReplacementNode extends ReplacementBase {

        @Child private WriteVariableNode storeValue;
        @Children private final RNode[] updates;
        @Child private RemoveAndAnswerNode removeTemp;

        public ReplacementNode(RNode variable, String tmpSymbol, List<RNode> updates) {
            this.storeValue = WriteVariableNode.createAnonymous(tmpSymbol, variable, WriteVariableNode.Mode.INVISIBLE);
            this.updates = updates.toArray(new RNode[updates.size()]);
            this.removeTemp = RemoveAndAnswerNode.create(tmpSymbol);
        }

        @Override
        @ExplodeLoop
        public Object execute(VirtualFrame frame) {
            storeValue.execute(frame);
            for (RNode update : updates) {
                update.execute(frame);
            }
            removeTemp.execute(frame);
            return null;
        }
    }

    /**
     * Used by the parser for assignments that miss a left hand side. This node will raise an error
     * once executed.
     */
    public static final class LHSError extends RSourceSectionNode implements RSyntaxNode, RSyntaxCall {

        private final String operator;
        private final RSyntaxElement lhs;
        private final RSyntaxElement rhs;

        public LHSError(SourceSection sourceSection, String operator, RSyntaxElement lhs, RSyntaxElement rhs) {
            super(sourceSection);
            this.operator = operator;
            this.lhs = lhs;
            this.rhs = rhs;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            CompilerDirectives.transferToInterpreter();
            throw RError.error(this, RError.Message.INVALID_LHS, "do_set");
        }

        @Override
        public RSyntaxElement getSyntaxLHS() {
            return RSyntaxLookup.createDummyLookup(null, operator, true);
        }

        @Override
        public RSyntaxElement[] getSyntaxArguments() {
            return new RSyntaxElement[]{lhs, rhs};
        }

        @Override
        public ArgumentsSignature getSyntaxSignature() {
            return ArgumentsSignature.empty(2);
        }
    }
}
