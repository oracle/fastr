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
import com.oracle.truffle.r.nodes.binary.*;
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

    public RTruffleVisitor() {
        this.environment = new REnvironment();
    }

    public RNode transform(ASTNode ast) {
        return ast.accept(this);
    }

    public REnvironment getEnvironment() {
        return environment;
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
    public RNode visit(FunctionCall call) {
        int index = 0;
        String[] argumentNames = new String[call.getArgs().size()];
        RNode[] nodes = new RNode[call.getArgs().size()];
        for (ArgumentList.Entry e : call.getArgs()) {
            Symbol argName = e.getName();
            argumentNames[index] = (argName == null ? null : argName.toString());
            nodes[index] = e.getValue() != null ? e.getValue().accept(this) : ConstantNode.create(RMissing.instance);
            index++;
        }
        return RCallNode.createCall(call.getSource(), ReadVariableNode.create(call.getName(), true, false), CallArgumentsNode.create(nodes, argumentNames));
    }

    @Override
    public RNode visit(Function func) {
        ArgumentList argumentsList = func.getSignature();

        this.environment = new REnvironment(environment);
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
                    parameterNames[index] = arg.getName().toString();
                    index++;
                }
                init[index] = body;
                body = new SequenceNode(init);
            }

            if (astBody != null && body.getSourceSection() == null) {
                body.assignSourceSection(astBody.getSource());
            }

            String functionBody = func.getSource().getCode();
            FunctionDefinitionNode rootNode = new FunctionDefinitionNode(func.getSource(), environment, body, parameterNames, functionBody.substring(0, Math.min(functionBody.length(), 50)));
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

    @Override
    public RNode visit(AccessVector a) {
        RNode vector = a.getVector().accept(this);
        if (a.getArgs().size() == 1) {
            RNode index = a.getArgs().getNode(0).accept(this);
            AccessVectorNode access = AccessVectorNode.create(vector, index);
            access.setSubset(a.isSubset());
            access.assignSourceSection(a.getSource());
            return access;
        } else if (a.getArgs().size() == 2) {
            ASTNode firstNode = a.getArgs().getNode(0);
            ASTNode secondNode = a.getArgs().getNode(1);
            RNode firstIndex = firstNode == null ? ConstantNode.create(RMissing.instance) : firstNode.accept(this);
            RNode secondIndex = secondNode == null ? ConstantNode.create(RMissing.instance) : secondNode.accept(this);
            AccessMatrixNode access = AccessMatrixNode.create(vector, firstIndex, secondIndex);
            access.setSubset(a.isSubset());
            access.assignSourceSection(a.getSource());
            return access;
        }
        throw new UnsupportedOperationException("Unsupported AST Node " + a.getClass().getName());
    }

    @Override
    public RNode visit(UpdateVector u) {
        AccessVector a = u.getVector();
        RNode rhs = u.getRHS().accept(this);
        RNode vector = a.getVector().accept(this);
        if (a.getArgs().size() == 2) {
            RNode index = a.getArgs().getNode(0).accept(this);
            UpdateVectorHelperNode update = UpdateVectorHelperNodeFactory.create(null, null, index);
            update.setSubset(a.isSubset());
            update.assignSourceSection(u.getSource());
            return CoerceBinaryNodeFactory.create(update, vector, rhs);
        } else if (a.getArgs().size() == 3) {
            RNode firstIndex = a.getArgs().getNode(0).accept(this);
            RNode secondIndex = a.getArgs().getNode(1).accept(this);
            UpdateMatrixHelperNode update = UpdateMatrixHelperNodeFactory.create(null, null, firstIndex, secondIndex);
            update.setSubset(a.isSubset());
            update.assignSourceSection(u.getSource());
            return CoerceBinaryNodeFactory.create(update, vector, rhs);
        }
        throw new UnsupportedOperationException("Unsupported AST Node " + a.getClass().getName());
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
     * We take an anonymous object as the name of the temporary variable and omit the removal, as the
     * anonymous object is unique to this replacement. We also anonymously store a to be able to use it
     * as the value of this expression.
     */
    //@formatter:on
    @Override
    public RNode visit(Replacement n) {
        // preparations
        RNode rhs = n.getExpr().accept(this);
        FunctionCall f = n.getBuiltin();
        ArgumentList args = f.getArgs();
        SimpleAccessVariable vAST = (SimpleAccessVariable) args.first().getValue();
        String vSymbol = vAST.getSymbol().toString();

        //@formatter:off
        // store a - need to use temporary, otherwise there is a failure in case multiple calls to
        // the replacement form are chained: 
        // x<-c(1); y<-c(1); dim(x)<-1; dim(y)<-1; attr(x, "dimnames")<-(attr(y, "dimnames")<-list("b"))
        //@formatter:on

        final Object a = new Object();
        WriteVariableNode aAssign = WriteVariableNode.create(a, rhs, false, false, true);

        // read v, assign tmp
        ReadVariableNode v = n.isSuper() ? ReadSuperVariableNode.create(vAST.getSource(), vSymbol, false, false) : ReadVariableNode.create(vAST.getSource(), vSymbol, false, false);
        final String tmp = "*tmp*";
        WriteVariableNode tmpAssign = WriteVariableNode.create(tmp, v, false, false);

        // massage arguments to replacement function call (replace v with tmp, append a)
        ArgumentList rfArgs = new ArgumentList.Default();
        rfArgs.add(AccessVariable.create(null, tmp, false));
        if (args.size() > 1) {
            for (int i = 1; i < args.size(); ++i) {
                rfArgs.add(args.getNode(i));
            }
        }
        rfArgs.add(AccessVariable.create(null, a));

        // replacement function call (use visitor for FunctionCall)
        FunctionCall rfCall = new FunctionCall(null, f.getName(), rfArgs);
        RCallNode replacementFunctionCall = (RCallNode) visit(rfCall);

        // assign v, read a
        WriteVariableNode vAssign = WriteVariableNode.create(vSymbol, replacementFunctionCall, false, n.isSuper());
        ReadVariableNode aRead = ReadVariableNode.create(a, false, false);

        // assemble
        SequenceNode replacement = new SequenceNode(new RNode[]{aAssign, tmpAssign, vAssign, Invisible.create(aRead)});
        replacement.assignSourceSection(n.getSource());
        return replacement;
    }

    @Override
    public RNode visit(SimpleAccessVariable n) {
        return ReadVariableNode.create(n.getSource(), n.getSymbol(), false, n.shouldCopyValue());
    }

    @Override
    public RNode visit(SimpleAccessTempVariable n) {
        return ReadVariableNode.create(n.getSource(), n.getSymbol(), false, false);
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
        return WhileNode.create(condition, body);
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

}
