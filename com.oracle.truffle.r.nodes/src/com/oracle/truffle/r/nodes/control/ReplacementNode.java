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
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.nodes.EmptyTypeSystemFlatLayout;
import com.oracle.truffle.r.nodes.access.RemoveAndAnswerNode;
import com.oracle.truffle.r.nodes.access.WriteVariableNode;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.function.RCallSpecialNode;
import com.oracle.truffle.r.nodes.function.RCallSpecialNode.RecursiveSpecialBailout;
import com.oracle.truffle.r.nodes.unary.GetNonSharedNodeGen;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.builtins.RSpecialFactory.FullCallNeededException;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.nodes.RCodeBuilder.CodeBuilderContext;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxCall;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

@NodeChild(value = "target", type = RNode.class)
@TypeSystemReference(EmptyTypeSystemFlatLayout.class)
abstract class ReplacementNode extends Node {

    private final int tempNamesStartIndex;
    private final SourceSection source;
    private final List<RSyntaxCall> calls;
    private final String targetVarName;
    private final String rhsVarName;
    private final boolean isSuper;

    ReplacementNode(SourceSection source, List<RSyntaxCall> calls, String rhsVarName, String targetVarName, boolean isSuper, int tempNamesStartIndex) {
        this.source = source;
        this.calls = calls;
        this.rhsVarName = rhsVarName;
        this.targetVarName = targetVarName;
        this.isSuper = isSuper;
        this.tempNamesStartIndex = tempNamesStartIndex;
    }

    public abstract void execute(VirtualFrame frame);

    @Specialization
    protected void doRObject(VirtualFrame frame, Object target,
                    @Cached("createTargetTmpWrite()") WriteVariableNode targetTmpWrite,
                    @Cached("createTargetTmpRemove()") RemoveAndAnswerNode targetTmpRemove,
                    @Cached("createTargetWrite()") WriteVariableNode targetWrite,
                    @Cached("createReplacementNode()") ReplacementBase replacement) {
        targetTmpWrite.execute(frame, target);
        replacement.execute(frame);
        targetWrite.execute(frame, targetTmpRemove.execute(frame));
    }

    protected final WriteVariableNode createTargetTmpWrite() {
        return WriteVariableNode.createAnonymous(getTargetTmpName(), null, WriteVariableNode.Mode.INVISIBLE);
    }

    protected final WriteVariableNode createTargetWrite() {
        return WriteVariableNode.createAnonymous(targetVarName, null, WriteVariableNode.Mode.INVISIBLE, isSuper);
    }

    protected final RemoveAndAnswerNode createTargetTmpRemove() {
        return RemoveAndAnswerNode.create(getTargetTmpName());
    }

    protected final ReplacementBase createReplacementNode() {
        return createReplacementNode(true);
    }

    private ReplacementBase createReplacementNodeWithoutSpecials() {
        return createReplacementNode(false);
    }

    private ReplacementBase createReplacementNode(boolean useSpecials) {
        CompilerAsserts.neverPartOfCompilation();
        // Note: if specials are turned off in FastR, onlySpecials will never be true
        boolean createSpecial = hasOnlySpecialCalls() && useSpecials;
        return createSpecial ? createSpecialReplacement(source, calls) : createGenericReplacement(source, calls);
    }

    private String getTargetTmpName() {
        return "*tmp*" + tempNamesStartIndex;
    }

    private boolean hasOnlySpecialCalls() {
        for (int i = 0; i < calls.size(); i++) {
            if (!(calls.get(i) instanceof RCallSpecialNode)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Creates a replacement that consists only of {@link RCallSpecialNode} calls.
     */
    private SpecialReplacementNode createSpecialReplacement(SourceSection source, List<RSyntaxCall> calls) {
        CodeBuilderContext codeBuilderContext = new CodeBuilderContext(tempNamesStartIndex + 2);
        RNode extractFunc = ReadVariableNode.create(getTargetTmpName());
        for (int i = calls.size() - 1; i >= 1; i--) {
            extractFunc = createSpecialFunctionQuery(extractFunc.asRSyntaxNode(), calls.get(i), codeBuilderContext);
        }
        RNode updateFunc = createFunctionUpdate(source, extractFunc.asRSyntaxNode(), ReadVariableNode.create(rhsVarName), calls.get(0), codeBuilderContext);
        assert updateFunc instanceof RCallSpecialNode : "should be only specials";
        return new SpecialReplacementNode((RCallSpecialNode) updateFunc);
    }

    /**
     * When there are more than two function calls in LHS, then we save some function calls by
     * saving the intermediate results into temporary variables and reusing them.
     */
    private GenericReplacementNode createGenericReplacement(SourceSection source, List<RSyntaxCall> calls) {
        List<RNode> instructions = new ArrayList<>();
        CodeBuilderContext codeBuilderContext = new CodeBuilderContext(tempNamesStartIndex + calls.size() + 1);

        /*
         * Create the calls that extract inner components - only needed for complex replacements
         * like "a(b(x)) <- z" (where we would extract "b(x)"). The extracted values are saved into
         * temporary variables *tmp*{index} indexed from tempNamesIndex to (tempNamesIndex +
         * calls.size()-1), the first such temporary variable holds the "target" of the replacement,
         * 'x' in our example (the assignment from 'x' is not done in this loop).
         */
        for (int i = calls.size() - 1, tmpIndex = 0; i >= 1; i--, tmpIndex++) {
            ReadVariableNode newLhs = ReadVariableNode.create("*tmp*" + (tempNamesStartIndex + tmpIndex));
            RNode update = createSpecialFunctionQuery(newLhs, calls.get(i), codeBuilderContext);
            instructions.add(WriteVariableNode.createAnonymous("*tmp*" + (tempNamesStartIndex + tmpIndex + 1), update, WriteVariableNode.Mode.INVISIBLE));
        }
        /*
         * Create the update calls, for "a(b(x)) <- z", this would be `a<-` and `b<-`, the
         * intermediate results are stored to temporary variables *tmpr*{index}.
         */
        for (int i = 0; i < calls.size(); i++) {
            int tmpIndex = tempNamesStartIndex + calls.size() - i - 1;
            String tmprName = i == 0 ? rhsVarName : "*tmpr*" + (tempNamesStartIndex + i - 1);
            RNode update = createFunctionUpdate(source, ReadVariableNode.create("*tmp*" + tmpIndex), ReadVariableNode.create(tmprName), calls.get(i), codeBuilderContext);
            if (i < calls.size() - 1) {
                instructions.add(WriteVariableNode.createAnonymous("*tmpr*" + (tempNamesStartIndex + i), update, WriteVariableNode.Mode.INVISIBLE));
            } else {
                instructions.add(WriteVariableNode.createAnonymous(getTargetTmpName(), update, WriteVariableNode.Mode.REGULAR));
            }
        }

        GenericReplacementNode newReplacementNode = new GenericReplacementNode(instructions);
        return newReplacementNode;
    }

    /**
     * Creates a call that looks like {@code fun} but has the first argument replaced with
     * {@code newLhs}.
     */
    private static RNode createSpecialFunctionQuery(RSyntaxNode newLhs, RSyntaxCall fun, CodeBuilderContext codeBuilderContext) {
        RSyntaxElement[] arguments = fun.getSyntaxArguments();

        RSyntaxNode[] argNodes = new RSyntaxNode[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            argNodes[i] = i == 0 ? newLhs : process(arguments[i], codeBuilderContext);
        }

        return RCallSpecialNode.createCallInReplace(fun.getSourceSection(), process(fun.getSyntaxLHS(), codeBuilderContext).asRNode(), fun.getSyntaxSignature(), argNodes).asRNode();
    }

    /**
     * Creates a call that looks like {@code fun}, but has its first argument replaced with
     * {@code newLhs}, its target turned into an update function ("foo<-"), with the given value
     * added to the arguments list.
     */
    private static RNode createFunctionUpdate(SourceSection source, RSyntaxNode newLhs, RSyntaxNode rhs, RSyntaxCall fun, CodeBuilderContext codeBuilderContext) {
        RSyntaxElement[] arguments = fun.getSyntaxArguments();

        ArgumentsSignature signature = fun.getSyntaxSignature();
        RSyntaxNode[] argNodes = new RSyntaxNode[arguments.length + 1];
        String[] names = new String[argNodes.length];
        for (int i = 0; i < arguments.length; i++) {
            names[i] = signature.getName(i);
            argNodes[i] = i == 0 ? newLhs : process(arguments[i], codeBuilderContext);
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

    private static RSyntaxNode process(RSyntaxElement original, CodeBuilderContext codeBuilderContext) {
        return RContext.getASTBuilder().process(original, codeBuilderContext);
    }

    private static RSyntaxNode lookup(SourceSection source, String symbol, boolean functionLookup) {
        return RContext.getASTBuilder().lookup(source, symbol, functionLookup);
    }

    static RLanguage getLanguage(WriteVariableNode wvn) {
        Node parent = wvn.getParent();
        if (parent instanceof ReplacementBase) {
            Node replacementBlock = ((ReplacementBase) parent).getReplacementNodeParent().getParent();
            assert replacementBlock instanceof ReplacementDispatchNode;
            return RDataFactory.createLanguage((RNode) replacementBlock);
        }
        return null;
    }

    /**
     * Base class for nodes implementing the actual replacement.
     */
    protected abstract static class ReplacementBase extends Node {

        public abstract void execute(VirtualFrame frame);

        protected final ReplacementNode getReplacementNodeParent() {
            // Note: new DSL puts another node in between ReplacementBase instance and
            // ReplacementNode, to be flexible we traverse the parents until we reach it
            Node current = this;
            do {
                current = current.getParent();
            } while (!(current instanceof ReplacementNode));
            return (ReplacementNode) current;
        }
    }

    /**
     * Replacement that is made of only special calls, if one of the special calls falls back to
     * full version, the replacement also falls back to {@link ReplacementNode}.
     */
    private static final class SpecialReplacementNode extends ReplacementBase {

        @Child private RCallSpecialNode replaceCall;

        SpecialReplacementNode(RCallSpecialNode replaceCall) {
            this.replaceCall = replaceCall;
            this.replaceCall.setPropagateFullCallNeededException(true);
        }

        @Override
        public void execute(VirtualFrame frame) {
            try {
                // Note: the very last call is the actual assignment, e.g. [[<-, if this call's
                // argument is shared, it bails out. Moreover, if that call's argument is not
                // shared, it could not be extracted from a shared container (list), so we should be
                // OK with not calling any other update function and just update the value directly.
                replaceCall.execute(frame);
            } catch (FullCallNeededException | RecursiveSpecialBailout e) {
                replace(getReplacementNodeParent().createReplacementNodeWithoutSpecials()).execute(frame);
            }
        }
    }

    /**
     * Holds the sequence of nodes created for R's replacement assignment.
     */
    private static final class GenericReplacementNode extends ReplacementBase {
        @Children private final RNode[] updates;

        GenericReplacementNode(List<RNode> updates) {
            this.updates = updates.toArray(new RNode[updates.size()]);
        }

        @Override
        @ExplodeLoop
        public void execute(VirtualFrame frame) {
            for (RNode update : updates) {
                update.execute(frame);
            }
        }
    }
}
