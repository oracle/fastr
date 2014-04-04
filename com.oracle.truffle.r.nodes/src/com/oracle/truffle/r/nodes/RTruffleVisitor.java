/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.access.ReadVariableNode.ReadSuperVariableNode;
import com.oracle.truffle.r.nodes.access.UpdateArrayHelperNode.CoerceOperand;
import com.oracle.truffle.r.nodes.access.UpdateArrayHelperNodeFactory.CoerceOperandFactory;
import com.oracle.truffle.r.nodes.access.ArrayPositionCastFactory.OperatorConverterNodeFactory;
import com.oracle.truffle.r.nodes.access.ArrayPositionCast.OperatorConverterNode;
import com.oracle.truffle.r.nodes.binary.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.nodes.builtin.base.*;
import com.oracle.truffle.r.nodes.control.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.parser.ast.*;
import com.oracle.truffle.r.parser.ast.Constant.ConstantType;
import com.oracle.truffle.r.parser.ast.Repeat;
import com.oracle.truffle.r.parser.tools.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

public final class RTruffleVisitor extends BasicVisitor<RNode> {

    private REnvironment environment;

    public RTruffleVisitor(REnvironment enclosing) {
        this.environment = enclosing;
    }

    public RNode transform(ASTNode ast) {
        return ast.accept(this);
    }

    @Override
    public RNode visit(Constant c) {
        SourceSection src = c.getSource();
        if (c.getType() == ConstantType.NULL) {
            return ConstantNode.create(src, RNull.instance);
        }
        if (c.getValues().length != 1) {
            throw new UnsupportedOperationException();
        }
        switch (c.getType()) {
            case INT:
                return ConstantNode.create(src, RRuntime.string2int(c.getValues()[0]));
            case DOUBLE:
                return ConstantNode.create(src, RRuntime.string2double(c.getValues()[0]));
            case BOOL:
                String value = c.getValues()[0];
                if (value.equals("NA")) {
                    return ConstantNode.create(src, RRuntime.LOGICAL_NA);
                } else if (value.equals("1")) {
                    return ConstantNode.create(src, true);
                } else if (value.equals("0")) {
                    return ConstantNode.create(src, false);
                } else {
                    throw new AssertionError();
                }
            case STRING:
                return ConstantNode.create(src, c.getValues()[0]);
            case COMPLEX:
                return ConstantNode.create(src, RDataFactory.createComplex(0, RRuntime.string2double(c.getValues()[0])));
            default:
                throw new UnsupportedOperationException();
        }
    }

    @Override
    public RNode visit(FunctionCall callParam) {
        FunctionCall call = callParam;
        Symbol callName = call.getName();
        SourceSection callSource = call.getSource();

        if (callName.toString().equals(".Internal")) {
            // A call .Internal(f(args)) is rewritten as .Internal.f(args).
            // If the first arg is not a function call, then it will be an error,
            // but we must not crash here.
            ASTNode callArg1 = call.getArgs().first().getValue();
            if (callArg1 instanceof FunctionCall) {
                FunctionCall internalCall = (FunctionCall) callArg1;
                callName = Symbol.getSymbol(".Internal." + internalCall.getName().toString());
                call = internalCall;
            }
        }
        int index = 0;
        String[] argumentNames = new String[call.getArgs().size()];
        RNode[] nodes = new RNode[call.getArgs().size()];
        for (ArgumentList.Entry e : call.getArgs()) {
            Symbol argName = e.getName();
            argumentNames[index] = (argName == null ? null : RRuntime.toString(argName));
            nodes[index] = e.getValue() != null ? e.getValue().accept(this) : null;
            index++;
        }
        return RCallNode.createCall(callSource, ReadVariableNode.create(callName, RRuntime.TYPE_FUNCTION, false), CallArgumentsNode.create(nodes, argumentNames));
    }

    @Override
    public RNode visit(Function func) {
        ArgumentList argumentsList = func.getSignature();

        REnvironment.FunctionDefinition funcEnvironment = new REnvironment.FunctionDefinition(environment);
        this.environment = funcEnvironment;
        CallTarget callTarget;
        try {

            ASTNode astBody = func.getBody();

            RNode body;
            if (astBody != null) {
                body = astBody.accept(this);
            } else {
                body = new SequenceNode(RNode.EMTPY_RNODE_ARRAY);
            }

            Object[] parameterNames = new Object[argumentsList.size()];
            if (!argumentsList.isEmpty()) {
                RNode[] init = new RNode[argumentsList.size() + 1];
                int index = 0;
                for (ArgumentList.Entry arg : argumentsList) {
                    RNode defaultValue = arg.getValue() != null ? arg.getValue().accept(this) : ConstantNode.create(RMissing.instance);
                    init[index] = WriteVariableNode.create(arg.getName(), new AccessArgumentNode(index, defaultValue), true, false);
                    parameterNames[index] = RRuntime.toString(arg.getName());
                    index++;
                }
                init[index] = body;
                body = new SequenceNode(init);
            }

            if (astBody != null && body.getSourceSection() == null) {
                body.assignSourceSection(astBody.getSource());
            }

            String functionBody = func.getSource().getCode();
            FunctionDefinitionNode rootNode = new FunctionDefinitionNode(func.getSource(), funcEnvironment, body, parameterNames, functionBody.substring(0, Math.min(functionBody.length(), 50)));
            callTarget = Truffle.getRuntime().createCallTarget(rootNode);

        } finally {
            this.environment = environment.getParent();
        }
        return FunctionExpressionNode.create(callTarget);
    }

    @Override
    public RNode visit(UnaryOperation op) {
        RNode operand = op.getLHS().accept(this);
        return RCallNode.createStaticCall(op.getSource(), op.getPrettyOperator(), CallArgumentsNode.createUnnamed(operand));
    }

    @Override
    public RNode visit(BinaryOperation op) {
        RNode left = op.getLHS().accept(this);
        RNode right = op.getRHS().accept(this);
        return RCallNode.createStaticCall(op.getSource(), op.getPrettyOperator(), CallArgumentsNode.createUnnamed(left, right));
    }

    @Override
    public RNode visit(Sequence seq) {
        ASTNode[] exprs = seq.getExprs();
        RNode[] rexprs = new RNode[exprs.length];
        for (int i = 0; i < exprs.length; i++) {
            rexprs[i] = exprs[i].accept(this);
        }
        if (rexprs.length == 1) {
            return rexprs[0];
        } else {
            return new SequenceNode(seq.getSource(), rexprs);
        }
    }

    @Override
    public RNode visit(ASTNode n) {
        throw new UnsupportedOperationException("Unsupported AST Node " + n.getClass().getName());
    }

    private RNode createPositions(ArgumentList argList, int argLength, boolean isSubset, boolean isAssignment) {
        RNode[] positions;
        OperatorConverterNode[] operatorConverters;
        ArrayPositionCast[] castPositions;
        if (argLength == 0) {
            positions = new RNode[]{ConstantNode.create(RMissing.instance)};
            operatorConverters = new OperatorConverterNode[]{OperatorConverterNodeFactory.create(0, 1, isAssignment, isSubset, null, null, null)};
            castPositions = new ArrayPositionCast[]{ArrayPositionCastFactory.create(0, 1, isAssignment, isSubset, null, null, null, null)};
        } else {
            positions = new RNode[argLength];
            operatorConverters = new OperatorConverterNode[argLength];
            castPositions = new ArrayPositionCast[argLength];
            for (int i = 0; i < argLength; i++) {
                ASTNode node = argList.getNode(i);
                positions[i] = (node == null ? ConstantNode.create(RMissing.instance) : node.accept(this));
                operatorConverters[i] = OperatorConverterNodeFactory.create(i, positions.length, isAssignment, isSubset, null, null, null);
                castPositions[i] = ArrayPositionCastFactory.create(i, positions.length, isAssignment, isSubset, null, null, null, null);
            }
        }
        if (!isAssignment) {
            return new PositionsArrayNode(castPositions, positions, operatorConverters);
        } else {
            return new PositionsArrayNodeValue(castPositions, positions, operatorConverters);
        }
    }

    @Override
    public RNode visit(AccessVector a) {
        RNode vector = a.getVector().accept(this);
        int argLength = a.getArgs().size();
        RNode castVector = CastToVectorNodeFactory.create(vector, false, false, true);
        RNode positions = createPositions(a.getArgs(), argLength, a.isSubset(), false);
        AccessArrayNode access = AccessArrayNode.create(a.isSubset(), castVector, (PositionsArrayNode) positions);
        access.assignSourceSection(a.getSource());
        return access;
    }

    private static final String varSymbol = "*tmp*";

    private static Object constructReplacementPrefix(RNode[] seq, RNode rhs, String vSymbol, SimpleAccessVariable vAST, boolean isSuper) {
        //@formatter:off
        // store a - need to use temporary, otherwise there is a failure in case multiple calls to
        // the replacement form are chained:
        // x<-c(1); y<-c(1); dim(x)<-1; dim(y)<-1; attr(x, "dimnames")<-(attr(y, "dimnames")<-list("b"))
        //@formatter:on
        final Object rhsSymbol = new Object();

        WriteVariableNode rhsAssign = WriteVariableNode.create(rhsSymbol, rhs, false, false, true);

        ReadVariableNode v = isSuper ? ReadSuperVariableNode.create(vAST.getSource(), vSymbol, RRuntime.TYPE_ANY, false) : ReadVariableNode.create(vAST.getSource(), vSymbol, RRuntime.TYPE_ANY, false);
        WriteVariableNode varAssign = WriteVariableNode.create(varSymbol, v, false, false);

        seq[0] = rhsAssign;
        seq[1] = varAssign;

        return rhsSymbol;
    }

    private static SequenceNode constructReplacementSuffix(RNode[] seq, RNode op, String vSymbol, Object rhsSymbol, SourceSection source, boolean isSuper) {
        // assign var, read rhs
        WriteVariableNode vAssign = WriteVariableNode.create(vSymbol, op, false, isSuper);
        ReadVariableNode rhsRead = ReadVariableNode.create(rhsSymbol, false);

        // assemble
        seq[2] = vAssign;
        seq[3] = Invisible.create(rhsRead);
        SequenceNode replacement = new SequenceNode(seq);
        replacement.assignSourceSection(source);
        return replacement;
    }

    @Override
    public RNode visit(UpdateVector u) {
        AccessVector a = u.getVector();
        RNode rhs = u.getRHS().accept(this);
        SimpleAccessVariable vAST = null;
        if (a.getVector() instanceof SimpleAccessVariable) {
            vAST = (SimpleAccessVariable) a.getVector();
        } else {
            Utils.nyi();
        }
        String vSymbol = RRuntime.toString(vAST.getSymbol());

        RNode[] seq = new RNode[4];
        final Object rhsSymbol = constructReplacementPrefix(seq, rhs, vSymbol, vAST, u.isSuper());
        RNode rhsAccess = AccessVariable.create(null, rhsSymbol).accept(this);
        RNode varAccess = AccessVariable.create(null, varSymbol).accept(this);
        int argLength = a.getArgs().size() - 1; // last argument == RHS
        RNode positions = createPositions(a.getArgs(), argLength, a.isSubset(), true);
        CoerceOperand coerceOperand = CoerceOperandFactory.create(null, null);
        UpdateArrayHelperNode updateOp = UpdateArrayHelperNodeFactory.create(a.isSubset(), varAccess, rhsAccess, coerceOperand, ConstantNode.create(0), (PositionsArrayNodeValue) positions);
        return constructReplacementSuffix(seq, updateOp, vSymbol, rhsSymbol, u.getSource(), u.isSuper());
    }

    @Override
    public RNode visit(Colon c) {
        // TODO convert to function call
        RNode left = c.getLHS().accept(this);
        RNode right = c.getRHS().accept(this);
        return ColonNode.create(c.getSource(), left, right);
    }

    @Override
    public RNode visit(SimpleAssignVariable n) {
        RNode expression = n.getExpr().accept(this);
        return WriteVariableNode.create(n.getSource(), n.getSymbol(), expression, false, n.isSuper());
    }

    //@formatter:off
    /**
     * Handle an assignment of the form {@code xxx(v) <- a} (or similar, with additional arguments).
     * These are called "replacements".
     *
     * According to the R language specification, this corresponds to the following code:
     * <pre>
     * '*tmp*' <- v
     * v <- `xxx<-`('*tmp*', a)
     * rm('*tmp*')
     * </pre>
     *
     * We take an anonymous object to store a, as the anonymous object is unique to this
     * replacement. We omit the removal of *tmp*.
     */
    //@formatter:on
    @Override
    public RNode visit(Replacement n) {
        // preparations
        RNode rhs = n.getExpr().accept(this);
        FunctionCall f = n.getBuiltin();
        ArgumentList args = f.getArgs();
        SimpleAccessVariable vAST = (SimpleAccessVariable) args.first().getValue();
        String vSymbol = RRuntime.toString(vAST.getSymbol());

        RNode[] seq = new RNode[4];
        final Object rhsSymbol = constructReplacementPrefix(seq, rhs, vSymbol, vAST, n.isSuper());

        // massage arguments to replacement function call (replace v with tmp, append a)
        ArgumentList rfArgs = new ArgumentList.Default();
        rfArgs.add(AccessVariable.create(null, varSymbol, false));
        if (args.size() > 1) {
            for (int i = 1; i < args.size(); ++i) {
                rfArgs.add(args.getNode(i));
            }
        }
        rfArgs.add(AccessVariable.create(null, rhsSymbol));

        // replacement function call (use visitor for FunctionCall)
        FunctionCall rfCall = new FunctionCall(null, f.getName(), rfArgs);
        RCallNode replacementFunctionCall = (RCallNode) visit(rfCall);

        return constructReplacementSuffix(seq, replacementFunctionCall, vSymbol, rhsSymbol, n.getSource(), n.isSuper());
    }

    @Override
    public RNode visit(SimpleAccessVariable n) {
        return ReadVariableNode.create(n.getSource(), n.getSymbol(), RRuntime.TYPE_ANY, n.shouldCopyValue());
    }

    @Override
    public RNode visit(SimpleAccessTempVariable n) {
        return ReadVariableNode.create(n.getSource(), n.getSymbol(), RRuntime.TYPE_ANY, false);
    }

    @Override
    public RNode visit(If n) {
        RNode condition = n.getCond().accept(this);
        RNode thenPart = n.getTrueCase().accept(this);
        RNode elsePart = n.getFalseCase() != null ? n.getFalseCase().accept(this) : null;
        return IfNode.create(n.getSource(), condition, thenPart, elsePart);
    }

    @Override
    public RNode visit(While loop) {
        RNode condition = loop.getCond().accept(this);
        RNode body = loop.getBody().accept(this);
        return WhileNode.create(loop.getSource(), condition, body);
    }

    @Override
    public RNode visit(Break n) {
        return new BreakNode();
    }

    @Override
    public RNode visit(Next n) {
        return new NextNode();
    }

    @Override
    public RNode visit(Repeat loop) {
        RNode body = loop.getBody().accept(this);
        return WhileNode.create(loop.getSource(), ConstantNode.create(true), body);
    }

    @Override
    public RNode visit(For loop) {
        WriteVariableNode cvar = WriteVariableNode.create(loop.getCVar(), null, false, false);
        RNode range = loop.getRange().accept(this);
        RNode body = loop.getBody().accept(this);
        return ForNode.create(cvar, range, body);
    }

    @Override
    public RNode visit(FieldAccess n) {
        AccessFieldNode afn = AccessFieldNodeFactory.create(n.getLhs().accept(this), RRuntime.toString(n.getFieldName()));
        afn.assignSourceSection(n.getSource());
        return afn;
    }

    @Override
    public RNode visit(UpdateField u) {
        FieldAccess a = u.getVector();
        RNode rhs = u.getRHS().accept(this);
        SimpleAccessVariable vAST = null;
        if (a.getLhs() instanceof SimpleAccessVariable) {
            vAST = (SimpleAccessVariable) a.getLhs();
        } else {
            Utils.nyi();
        }
        String vSymbol = RRuntime.toString(vAST.getSymbol());

        RNode[] seq = new RNode[4];
        final Object rhsSymbol = constructReplacementPrefix(seq, rhs, vSymbol, vAST, u.isSuper());
        RNode rhsAccess = AccessVariable.create(null, rhsSymbol).accept(this);
        RNode varAccess = AccessVariable.create(null, varSymbol).accept(this);
        UpdateFieldNode ufn = UpdateFieldNodeFactory.create(varAccess, rhsAccess, RRuntime.toString(a.getFieldName()));
        return constructReplacementSuffix(seq, ufn, vSymbol, rhsSymbol, u.getSource(), u.isSuper());
    }

}
