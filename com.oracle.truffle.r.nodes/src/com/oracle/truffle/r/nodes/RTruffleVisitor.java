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
package com.oracle.truffle.r.nodes;

import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.nodes.access.AccessArgumentNode;
import com.oracle.truffle.r.nodes.access.ConstantNode;
import com.oracle.truffle.r.nodes.access.ReadVariadicComponentNode;
import com.oracle.truffle.r.nodes.access.WriteLocalFrameVariableNode;
import com.oracle.truffle.r.nodes.access.WriteVariableNode;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.control.BlockNode;
import com.oracle.truffle.r.nodes.control.BreakNode;
import com.oracle.truffle.r.nodes.control.ForNode;
import com.oracle.truffle.r.nodes.control.IfNode;
import com.oracle.truffle.r.nodes.control.NextNode;
import com.oracle.truffle.r.nodes.control.ReplacementNode;
import com.oracle.truffle.r.nodes.control.WhileNode;
import com.oracle.truffle.r.nodes.function.FormalArguments;
import com.oracle.truffle.r.nodes.function.FunctionBodyNode;
import com.oracle.truffle.r.nodes.function.FunctionDefinitionNode;
import com.oracle.truffle.r.nodes.function.FunctionExpressionNode;
import com.oracle.truffle.r.nodes.function.FunctionStatementsNode;
import com.oracle.truffle.r.nodes.function.GroupDispatchNode;
import com.oracle.truffle.r.nodes.function.PostProcessArgumentsNode;
import com.oracle.truffle.r.nodes.function.RCallNode;
import com.oracle.truffle.r.nodes.function.SaveArgumentsNode;
import com.oracle.truffle.r.nodes.function.WrapDefaultArgumentNode;
import com.oracle.truffle.r.nodes.unary.GetNonSharedNodeGen;
import com.oracle.truffle.r.parser.ast.ASTNode;
import com.oracle.truffle.r.parser.ast.AccessVariable;
import com.oracle.truffle.r.parser.ast.AccessVariadicComponent;
import com.oracle.truffle.r.parser.ast.ArgNode;
import com.oracle.truffle.r.parser.ast.AssignVariable;
import com.oracle.truffle.r.parser.ast.BinaryOperation;
import com.oracle.truffle.r.parser.ast.Break;
import com.oracle.truffle.r.parser.ast.Call;
import com.oracle.truffle.r.parser.ast.Constant;
import com.oracle.truffle.r.parser.ast.For;
import com.oracle.truffle.r.parser.ast.Formula;
import com.oracle.truffle.r.parser.ast.Function;
import com.oracle.truffle.r.parser.ast.If;
import com.oracle.truffle.r.parser.ast.Missing;
import com.oracle.truffle.r.parser.ast.Next;
import com.oracle.truffle.r.parser.ast.Repeat;
import com.oracle.truffle.r.parser.ast.Replacement;
import com.oracle.truffle.r.parser.ast.Sequence;
import com.oracle.truffle.r.parser.ast.UnaryOperation;
import com.oracle.truffle.r.parser.ast.Visitor;
import com.oracle.truffle.r.parser.ast.While;
import com.oracle.truffle.r.parser.tools.EvaluatedArgumentsVisitor;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.FastROptions;
import com.oracle.truffle.r.runtime.RGroupGenerics;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.FastPathFactory;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.REmpty;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

public final class RTruffleVisitor implements Visitor<RSyntaxNode> {

    private int tempNamesCount;

    public RSyntaxNode transform(ASTNode ast) {
        return ast.accept(this);
    }

    public RFunction transformFunction(String name, Function func, MaterializedFrame enclosingFrame) {
        if (func.getDebugName() == null && name != null && !name.isEmpty()) {
            func.setDebugName(name);
        }
        RootCallTarget callTarget = createFunctionCallTarget(func);
        FastPathFactory fastPath = EvaluatedArgumentsVisitor.process(func);
        FrameSlotChangeMonitor.initializeEnclosingFrame(callTarget.getRootNode().getFrameDescriptor(), enclosingFrame);
        return RDataFactory.createFunction(name, callTarget, null, enclosingFrame, fastPath, ((FunctionDefinitionNode) callTarget.getRootNode()).containsDispatch());
    }

    @Override
    public RSyntaxNode visit(Constant c) {
        return ConstantNode.create(c.getSource(), c.getValue());
    }

    @Override
    public RSyntaxNode visit(Formula formula) {
        // response may be omitted
        RSyntaxNode response = formula.getResponse() == null ? null : formula.getResponse().accept(this);
        RSyntaxNode model = formula.getModel().accept(this);
        RSyntaxNode[] tildeArgs = new RSyntaxNode[response == null ? 1 : 2];
        int ix = 0;
        if (response != null) {
            tildeArgs[ix++] = response;
        }
        tildeArgs[ix++] = model;
        SourceSection formulaSrc = formula.getSource();
        String formulaCode = formulaSrc.getCode();
        int tildeIndex = formulaCode.indexOf('~');
        SourceSection tildeSrc = ASTNode.adjustedSource(formulaSrc, formulaSrc.getCharIndex() + tildeIndex, 1);
        RCallNode call = RCallNode.createOpCall(formulaSrc, tildeSrc, "~", tildeArgs);
        return call;
    }

    @Override
    public RSyntaxNode visit(Missing m) {
        return ConstantNode.create(REmpty.instance);
    }

    @Override
    public RSyntaxNode visit(Call call) {
        SourceSection callSource = call.getSource();
        List<ArgNode> arguments = call.getArguments();

        String[] argumentNames = arguments.stream().map(arg -> arg.getName()).toArray(String[]::new);
        ArgumentsSignature signature = ArgumentsSignature.get(argumentNames);
        RSyntaxNode[] nodes = arguments.stream().map(arg -> arg.getValue() == null ? null : arg.getValue().accept(this)).toArray(RSyntaxNode[]::new);

        RNode lhs;
        if (call.isSymbol()) {
            String callName = call.getName();
            if (RGroupGenerics.isGroupGeneric(callName)) {
                return GroupDispatchNode.create(callName, callSource, signature, nodes);
            }
            SourceSection varSource = ASTNode.adjustedSource(callSource, callSource.getCharIndex(), callName.length());
            lhs = ReadVariableNode.createForcedFunctionLookup(varSource, callName);
        } else {
            lhs = call.getLhsNode().accept(this).asRNode();
        }
        return RCallNode.createCall(callSource, lhs, signature, nodes);
    }

    @Override
    public RSyntaxNode visit(Function func) {
        RootCallTarget callTarget = null;
        try {
            callTarget = createFunctionCallTarget(func);
            FastPathFactory fastPath = EvaluatedArgumentsVisitor.process(func);
            return FunctionExpressionNode.create(func.getSource(), callTarget, fastPath);
        } catch (Throwable err) {
            throw new RInternalError(err, "visit(Function)");
        }
    }

    private RootCallTarget createFunctionCallTarget(Function func) {
        // Parse function statements
        ASTNode astBody = func.getBody();
        FunctionStatementsNode statements;
        if (astBody != null) {
            statements = new FunctionStatementsNode(astBody.getSource(), astBody.accept(this));
        } else {
            statements = new FunctionStatementsNode();
        }

        // Parse argument list
        List<ArgNode> argumentsList = func.getSignature();
        String[] argumentNames = new String[argumentsList.size()];
        RNode[] defaultValues = new RNode[argumentsList.size()];
        SaveArgumentsNode saveArguments;
        AccessArgumentNode[] argAccessNodes = new AccessArgumentNode[argumentsList.size()];
        PostProcessArgumentsNode argPostProcess;
        if (!argumentsList.isEmpty()) {
            RNode[] init = new RNode[argumentsList.size()];
            int index = 0;
            for (ArgNode arg : argumentsList) {
                // Parse argument's default value
                RNode defaultValue;
                ASTNode defaultValNode = arg.getValue();
                if (defaultValNode != null) {
                    // default argument initialization is, in a sense, quite similar to local
                    // variable write and thus should do appropriate state transition and/or
                    // RShareable copy if need be
                    defaultValue = WrapDefaultArgumentNode.create(arg.getValue().accept(this).asRNode());
                } else {
                    defaultValue = null;
                }

                // Create an initialization statement
                AccessArgumentNode accessArg = AccessArgumentNode.create(index);
                argAccessNodes[index] = accessArg;
                init[index] = WriteVariableNode.createArgSave(arg.getName(), accessArg);

                // Store formal arguments
                argumentNames[index] = arg.getName();
                defaultValues[index] = defaultValue;

                index++;
            }

            saveArguments = new SaveArgumentsNode(init);
            if (FastROptions.NewStateTransition.getBooleanValue() && !FastROptions.RefCountIncrementOnly.getBooleanValue()) {
                argPostProcess = PostProcessArgumentsNode.create(argumentsList.size());
            } else {
                argPostProcess = null;
            }
        } else {
            saveArguments = new SaveArgumentsNode(RNode.EMTPY_RNODE_ARRAY);
            argPostProcess = null;
        }

        // Maintain SourceSection
        if (astBody != null && statements.getSourceSection() == null) {
            statements.assignSourceSection(astBody.getSource());
        }
        FormalArguments formals = FormalArguments.createForFunction(defaultValues, ArgumentsSignature.get(argumentNames));
        for (AccessArgumentNode access : argAccessNodes) {
            access.setFormals(formals);
        }

        FrameDescriptor descriptor = new FrameDescriptor();
        String description = getFunctionDescription(func);
        FrameSlotChangeMonitor.initializeFunctionFrameDescriptor(description != null && !description.isEmpty() ? description : "<function>", descriptor);
        FunctionDefinitionNode rootNode = new FunctionDefinitionNode(func.getSource(), descriptor, new FunctionBodyNode(saveArguments, statements), formals, description, false, argPostProcess);
        return Truffle.getRuntime().createCallTarget(rootNode);
    }

    private static String getFunctionDescription(Function func) {
        if (func.getDebugName() != null) {
            return func.getDebugName();
        } else {
            String functionBody = func.getSource().getCode();
            return functionBody.substring(0, Math.min(functionBody.length(), 40)).replace("\n", "\\n");
        }
    }

    @Override
    public RSyntaxNode visit(UnaryOperation op) {
        String functionName = op.getOperator().getName();
        assert RGroupGenerics.getGroup(functionName) == RGroupGenerics.Ops : "unexpected group: " + RGroupGenerics.getGroup(functionName) + "(" + functionName + ")";

        RSyntaxNode operand = op.getLHS().accept(this);
        return GroupDispatchNode.create(functionName, op.getSource(), ArgumentsSignature.empty(1), operand);
    }

    @Override
    public RSyntaxNode visit(BinaryOperation op) {
        RSyntaxNode left = op.getLHS().accept(this);
        RSyntaxNode right = op.getRHS().accept(this);
        String functionName = op.getOperator().getName();
        if (RGroupGenerics.isGroupGeneric(functionName)) {
            return GroupDispatchNode.create(functionName, op.getSource(), ArgumentsSignature.empty(2), left, right);
        }
        // create a SourceSection for the operator
        SourceSection opSrc = op.getSource();
        String code = opSrc.getCode();
        String opName = op.getOperator().getName();
        int charIndex = code.indexOf(opName);
        SourceSection opNameSrc = opSrc.getSource().createSection(opSrc.getIdentifier(), opSrc.getCharIndex() + charIndex, opName.length());
        return RCallNode.createOpCall(op.getSource(), opNameSrc, functionName, left, right);
    }

    @Override
    public RSyntaxNode visit(Sequence seq) {
        ASTNode[] exprs = seq.getExpressions();
        RNode[] rexprs = new RNode[exprs.length];
        for (int i = 0; i < exprs.length; i++) {
            rexprs[i] = exprs[i].accept(this).asRNode();
        }
        return new BlockNode(seq.getSource(), rexprs);
    }

    @Override
    public RSyntaxNode visit(ArgNode n) {
        assert n.getValue() != null;
        return n.getValue().accept(this);
    }

    @Override
    public RSyntaxNode visit(AssignVariable n) {
        if (n.getExpr() instanceof Function) {
            ((Function) n.getExpr()).setDebugName(n.getVariable().toString());
        }
        RSyntaxNode expression = n.getExpr().accept(this);
        return (RSyntaxNode) WriteVariableNode.create(n.getSource(), n.getVariable(), expression.asRNode(), n.isSuper());
    }

    /**
     * Creates a call that looks like {@code fun} but has the first argument replaced with
     * {@code newLhs}.
     */
    private RCallNode createFunctionQuery(RSyntaxNode newLhs, Call fun) {
        assert fun.isSymbol();
        List<ArgNode> arguments = fun.getArguments();

        RSyntaxNode[] argNodes = new RSyntaxNode[arguments.size()];
        String[] names = new String[argNodes.length];
        for (int i = 0; i < arguments.size(); i++) {
            names[i] = arguments.get(i).getName();
            argNodes[i] = i == 0 ? newLhs : visit(arguments.get(i));
        }

        ReadVariableNode function = ReadVariableNode.createForcedFunctionLookup(null, fun.getName());
        return RCallNode.createCall(fun.getSource(), function, ArgumentsSignature.get(names), argNodes);
    }

    /**
     * Creates a call that looks like {@code fun}, but has its first argument replaced with
     * {@code newLhs}, its target turned into an update function ("foo<-"), with the given value
     * added to the arguments list.
     */
    private RCallNode createFunctionUpdate(SourceSection source, RSyntaxNode newLhs, RSyntaxNode rhs, Call fun) {
        assert fun.isSymbol();
        List<ArgNode> arguments = fun.getArguments();

        RSyntaxNode[] argNodes = new RSyntaxNode[arguments.size() + 1];
        String[] names = new String[argNodes.length];
        for (int i = 0; i < arguments.size(); i++) {
            names[i] = arguments.get(i).getName();
            argNodes[i] = i == 0 ? newLhs : visit(arguments.get(i));
        }
        argNodes[argNodes.length - 1] = rhs;
        names[argNodes.length - 1] = "value";

        if (fun.getName().equals("slot") || fun.getName().equals("@")) {
            // this is pretty gross, but at this point seems like the only way to get setClass to
            // work properly
            argNodes[0] = GetNonSharedNodeGen.create(argNodes[0].asRNode());
        }
        ReadVariableNode function = ReadVariableNode.createForcedFunctionLookup(null, fun.getName() + "<-");
        return RCallNode.createCall(source, function, ArgumentsSignature.get(names), argNodes);
    }

    /**
     * This method builds the sequence of calls needed to represent a replacement.<br/>
     * For example, the replacement {@code a(b(c(x),i),j) <- z} should be decomposed into the
     * following statements:
     *
     * <pre>
     * t3 <- x
     * t2 <- c(t3)
     * t1 <- b(t2,i)
     * tt0 <- z
     * tt1 <- `a<-`(t1, j, tt0) // b(...) with a replaced
     * tt2 <- `b<-`(t2, i, tt1) // a(...) with b replaced
     * x <- `c<-`(t3, tt2) // x with c replaced
     * </pre>
     */
    public RSyntaxNode visit(Replacement replacement) {
        ASTNode lhsAst = replacement.getLhs();
        ASTNode rhsAst = replacement.getRhs();

        /*
         * Collect all the function calls in this replacement. For "a(b(x)) <- z", this would be
         * "a(...)" and "b(...)".
         */
        List<Call> calls = new ArrayList<>();
        ASTNode current = lhsAst;
        while (!(current instanceof AccessVariable)) {
            assert current instanceof Call;
            Call call = (Call) current;
            calls.add(call);

            if (call.getArguments().isEmpty() || !call.isSymbol()) {
                // TODO: this should only be signaled when run, not when parsed
                throw RInternalError.unimplemented("proper error message for RError.INVALID_LHS");
            }
            current = call.getArguments().get(0).getValue();
        }
        AccessVariable variable = (AccessVariable) current;

        List<RNode> instructions = new ArrayList<>();
        int tempNamesIndex = tempNamesCount;
        tempNamesCount += calls.size() + 1;

        /*
         * Create the calls that extract inner components - only needed for complex replacements
         * like "a(b(x)) <- z" (where we would extract "b(x)").
         */
        for (int i = calls.size() - 1; i >= 1; i--) {
            RCallNode update = createFunctionQuery(ReadVariableNode.create("*tmp*" + (tempNamesIndex + i + 1)), calls.get(i));
            instructions.add(WriteVariableNode.createAnonymous("*tmp*" + (tempNamesIndex + i), update, WriteVariableNode.Mode.INVISIBLE));
        }
        /*
         * Create the update calls, for "a(b(x)) <- z", this would be `a<-` and `b<-`.
         */
        for (int i = 0; i < calls.size(); i++) {
            RCallNode update = createFunctionUpdate(replacement.getSource(), ReadVariableNode.create("*tmp*" + (tempNamesIndex + i + 1)), ReadVariableNode.create("*tmpr*" + (tempNamesIndex + i - 1)),
                            calls.get(i));
            if (i < calls.size() - 1) {
                instructions.add(WriteVariableNode.createAnonymous("*tmpr*" + (tempNamesIndex + i), update, WriteVariableNode.Mode.INVISIBLE));
            } else {
                instructions.add(WriteLocalFrameVariableNode.createAnonymous(variable.getVariable(), update, WriteVariableNode.Mode.INVISIBLE, replacement.isSuper()));
            }
        }

        ReadVariableNode variableValue = createReplacementForVariableUsing(variable, replacement.isSuper());
        ReplacementNode newReplacementNode = new ReplacementNode(replacement.getSource(), replacement.isSuper(), lhsAst.accept(this), rhsAst.accept(this), "*tmpr*" + (tempNamesIndex - 1),
                        variableValue, "*tmp*" + (tempNamesIndex + calls.size()), instructions);

        tempNamesCount -= calls.size() + 1;
        return newReplacementNode;
    }

    private static ReadVariableNode createReplacementForVariableUsing(AccessVariable simpleAccessVariable, boolean isSuper) {
        SourceSection argSourceSection = simpleAccessVariable.getSource();
        if (isSuper) {
            return ReadVariableNode.createSuperLookup(argSourceSection, simpleAccessVariable.getVariable());
        } else {
            return ReadVariableNode.create(argSourceSection, simpleAccessVariable.getVariable(), !isSuper);
        }
    }

    @Override
    public RSyntaxNode visit(AccessVariable n) {
        return ReadVariableNode.create(n.getSource(), n.getVariable(), false);
    }

    @Override
    public RSyntaxNode visit(AccessVariadicComponent n) {
        int ind = n.getIndex();
        return new ReadVariadicComponentNode(n.getSource(), ind > 0 ? ind - 1 : ind);
    }

    @Override
    public RSyntaxNode visit(If n) {
        RSyntaxNode condition = n.getCondition().accept(this);
        RSyntaxNode thenPart = n.getTrueCase().accept(this);
        RSyntaxNode elsePart = n.getFalseCase() != null ? n.getFalseCase().accept(this) : null;
        return IfNode.create(n.getSource(), condition, BlockNode.ensureBlock(n.getTrueCase().getSource(), thenPart),
                        BlockNode.ensureBlock(n.getFalseCase() == null ? null : n.getFalseCase().getSource(), elsePart));
    }

    @Override
    public RSyntaxNode visit(While loop) {
        RSyntaxNode condition = loop.getCondition().accept(this);
        RSyntaxNode body = BlockNode.ensureBlock(loop.getBody().getSource(), loop.getBody().accept(this));
        return WhileNode.create(loop.getSource(), condition, body, false);
    }

    @Override
    public RSyntaxNode visit(Break n) {
        return new BreakNode(n.getSource());
    }

    @Override
    public RSyntaxNode visit(Next n) {
        return new NextNode(n.getSource());
    }

    @Override
    public RSyntaxNode visit(Repeat loop) {
        RSyntaxNode body = BlockNode.ensureBlock(loop.getBody().getSource(), loop.getBody().accept(this));
        return WhileNode.create(loop.getSource(), ConstantNode.create(RRuntime.LOGICAL_TRUE), body, true);
    }

    @Override
    public RSyntaxNode visit(For loop) {
        WriteVariableNode cvar = WriteVariableNode.create(loop.getSource(), loop.getVariable(), null, false);
        RSyntaxNode range = loop.getRange().accept(this);
        RSyntaxNode body = loop.getBody().accept(this);
        return ForNode.create(loop.getSource(), cvar, range, new BlockNode(loop.getBody().getSource(), body));
    }
}
