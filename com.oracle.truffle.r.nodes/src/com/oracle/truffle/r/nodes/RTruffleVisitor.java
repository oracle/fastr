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

import java.util.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.access.ReadVariableNode.ReadVariableSuperMaterializedNode;
import com.oracle.truffle.r.nodes.access.UpdateArrayHelperNode.CoerceVector;
import com.oracle.truffle.r.nodes.access.UpdateArrayHelperNodeFactory.CoerceVectorFactory;
import com.oracle.truffle.r.nodes.access.ArrayPositionCastFactory.OperatorConverterNodeFactory;
import com.oracle.truffle.r.nodes.access.ArrayPositionCast.OperatorConverterNode;
import com.oracle.truffle.r.nodes.access.AccessArrayNode.MultiDimPosConverterNode;
import com.oracle.truffle.r.nodes.access.AccessArrayNodeFactory.MultiDimPosConverterNodeFactory;
import com.oracle.truffle.r.nodes.access.UpdateArrayHelperNode.MultiDimPosConverterValueNode;
import com.oracle.truffle.r.nodes.access.UpdateArrayHelperNodeFactory.MultiDimPosConverterValueNodeFactory;
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
    public RNode visit(Formula formula) {
        return ConstantNode.create(new RFormula(formula.getSource(), formula.getResponse().accept(this), formula.getModel().accept(this)));
    }

    @Override
    public RNode visit(FunctionCall callParam) {
        FunctionCall call = callParam;
        Symbol callName = call.isSymbol() ? call.getName() : null;
        SourceSection callSource = call.getSource();

        if (callName != null && callName.toString().equals(".Internal")) {
            // A call .Internal(f(args)) is rewritten as .Internal.f(args).
            // If the first arg is not a function call, then it will be an error,
            // but we must not crash here.
            ASTNode callArg1 = call.getArgs().get(0).getValue();
            if (callArg1 instanceof FunctionCall) {
                FunctionCall internalCall = (FunctionCall) callArg1;
                callName = Symbol.getSymbol(".Internal." + internalCall.getName().toString());
                call = internalCall;
            }
        }
        int index = 0;
        String[] argumentNames = new String[call.getArgs().size()];
        RNode[] nodes = new RNode[call.getArgs().size()];
        for (ArgNode e : call.getArgs()) {
            Symbol argName = e.getName();
            argumentNames[index] = (argName == null ? null : RRuntime.toString(argName));
            ASTNode val = e.getValue();
            if (val != null) {
                // the source must include a value assignment (if there is one) - this is ensured by
                // assigning the source section of the argument node
                val.setSource(e.getSource());
                nodes[index] = val.accept(this);
            }
            index++;
        }
        final CallArgumentsNode aCallArgNode = CallArgumentsNode.create(!callParam.isReplacement(), false, nodes, argumentNames);

        if (callName != null) {
            final String functionName = RRuntime.toString(callName);
            if (!RCmdOptions.DISABLE_GROUP_GENERICS.getValue() && RGroupGenerics.getGroup(functionName) != null) {
                return DispatchedCallNode.create(functionName, RGroupGenerics.RDotGroup, aCallArgNode);
            }
            return RCallNode.createCall(callSource, ReadVariableNode.create(callName, RRuntime.TYPE_FUNCTION, false), aCallArgNode);
        } else {
            RNode lhs = call.getLhsNode().accept(this);
            return RCallNode.createCall(callSource, lhs, aCallArgNode);
        }
    }

    @Override
    public RNode visit(Function func) {
        List<ArgNode> argumentsList = func.getSignature();

        REnvironment.FunctionDefinition funcEnvironment = new REnvironment.FunctionDefinition(environment);
        this.environment = funcEnvironment;
        RootCallTarget callTarget;
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
                for (ArgNode arg : argumentsList) {
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
            FunctionDefinitionNode rootNode = new FunctionDefinitionNode(func.getSource(), funcEnvironment, body, parameterNames, functionBody.substring(0, Math.min(functionBody.length(), 50)), false);
            callTarget = Truffle.getRuntime().createCallTarget(rootNode);
        } finally {
            this.environment = environment.getParent();
        }
        return FunctionExpressionNode.create(callTarget);
    }

    @Override
    public RNode visit(UnaryOperation op) {
        RNode operand = op.getLHS().accept(this);
        final String functionName = op.getPrettyOperator();
        final CallArgumentsNode aCallArgNode = CallArgumentsNode.createUnnamed(false, true, operand);
        if (!RCmdOptions.DISABLE_GROUP_GENERICS.getValue() && RGroupGenerics.getGroup(functionName) != null) {
            return DispatchedCallNode.create(functionName, RGroupGenerics.RDotGroup, aCallArgNode);
        }
        return RCallNode.createStaticCall(op.getSource(), functionName, aCallArgNode);
    }

    @Override
    public RNode visit(BinaryOperation op) {
        RNode left = op.getLHS().accept(this);
        RNode right = op.getRHS().accept(this);
        final String functionName = op.getPrettyOperator();
        final CallArgumentsNode aCallArgNode = CallArgumentsNode.createUnnamed(false, true, left, right);
        if (!RCmdOptions.DISABLE_GROUP_GENERICS.getValue() && RGroupGenerics.getGroup(functionName) != null) {
            return DispatchedCallNode.create(functionName, RGroupGenerics.RDotGroup, aCallArgNode);
        }
        return RCallNode.createStaticCall(op.getSource(), functionName, aCallArgNode);
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
    public RNode visit(ArgNode n) {
        assert n.getValue() != null;
        return n.getValue().accept(this);
    }

    private RNode createPositions(List<ArgNode> argList, int argLength, boolean isSubset, boolean isAssignment) {
        RNode[] positions;
        OperatorConverterNode[] operatorConverters;
        MultiDimPosConverterNode[] multiDimConverters = null;
        MultiDimPosConverterValueNode[] multiDimConvertersValue = null;
        ArrayPositionCast[] castPositions;
        if (argLength == 0) {
            positions = new RNode[]{ConstantNode.create(RMissing.instance)};
            operatorConverters = new OperatorConverterNode[]{OperatorConverterNodeFactory.create(0, 1, isAssignment, isSubset, null, null)};
            castPositions = new ArrayPositionCast[]{ArrayPositionCastFactory.create(0, 1, isAssignment, isSubset, null, null, null)};
        } else {
            positions = new RNode[argLength];
            operatorConverters = new OperatorConverterNode[argLength];
            if (isAssignment) {
                multiDimConvertersValue = new MultiDimPosConverterValueNode[argLength];
            } else {
                multiDimConverters = new MultiDimPosConverterNode[argLength];
            }
            castPositions = new ArrayPositionCast[argLength];
            for (int i = 0; i < argLength; i++) {
                ASTNode node = argList.get(i).getValue();
                positions[i] = (node == null ? ConstantNode.create(RMissing.instance) : node.accept(this));
                if (isAssignment) {
                    multiDimConvertersValue[i] = MultiDimPosConverterValueNodeFactory.create(isSubset, null, null, null);
                } else {
                    multiDimConverters[i] = MultiDimPosConverterNodeFactory.create(isSubset, null, null);
                }
                operatorConverters[i] = OperatorConverterNodeFactory.create(i, positions.length, isAssignment, isSubset, null, null);
                castPositions[i] = ArrayPositionCastFactory.create(i, positions.length, isAssignment, isSubset, null, null, null);
            }
        }
        if (!isAssignment) {
            return new PositionsArrayNode(castPositions, positions, operatorConverters, multiDimConverters);
        } else {
            return new PositionsArrayNodeValue(castPositions, positions, operatorConverters, multiDimConvertersValue);
        }
    }

    @Override
    public RNode visit(AccessVector a) {
        RNode vector = a.getVector().accept(this);
        RNode dropDim = ConstantNode.create(true);
        List<ArgNode> args = a.getArgs();
        int argLength = args.size();
        if (argLength > 0) {
            for (ArgNode e : args) {
                if (e.getName() != null && e.getName().toString().equals(RRuntime.DROP_DIM_ARG_NAME)) {
                    argLength--;
                    break;
                }
            }
            if (argLength < args.size()) {
                dropDim = null;
                List<ArgNode> newArgs = new ArrayList<>();
                for (ArgNode e : args) {
                    if (e.getName() != null && e.getName().toString().equals(RRuntime.DROP_DIM_ARG_NAME) && dropDim == null) {
                        // first occurence of "drop" argument counts - the others are treated as
                        // indexes
                        dropDim = e.getValue().accept(this);
                    } else {
                        newArgs.add(e);
                    }
                }
                args = newArgs;
            }
        }
        RNode castContainer = CastToContainerNodeFactory.create(vector, false, false, false, true);
        RNode positions = createPositions(args, argLength, a.isSubset(), false);
        AccessArrayNode access = AccessArrayNode.create(a.isSubset(), castContainer, (PositionsArrayNode) positions, CastLogicalNodeFactory.create(dropDim, false, false, false));
        access.assignSourceSection(a.getSource());
        return access;
    }

    private static final String varSymbol = "*tmp*";

    private static Object constructReplacementPrefix(RNode[] seq, RNode rhs, RNode replacementArg, WriteVariableNode.Mode rhsWriteMode) {
        //@formatter:off
        // store a - need to use temporary, otherwise there is a failure in case multiple calls to
        // the replacement form are chained:
        // x<-c(1); y<-c(1); dim(x)<-1; dim(y)<-1; attr(x, "dimnames")<-(attr(y, "dimnames")<-list("b"))
        //@formatter:on
        final Object rhsSymbol = new Object();

        WriteVariableNode rhsAssign = WriteVariableNode.create(rhsSymbol, rhs, false, false, rhsWriteMode);

        WriteVariableNode varAssign = WriteVariableNode.create(varSymbol, replacementArg, false, false, WriteVariableNode.Mode.TEMP);

        seq[0] = rhsAssign;
        seq[1] = varAssign;

        return rhsSymbol;
    }

    private static SequenceNode constructReplacementSuffix(RNode[] seq, RNode assignFromTemp, Object rhsSymbol, SourceSection source) {
        // assign var, read rhs
        WriteVariableNode varReset = WriteVariableNode.create(varSymbol, ConstantNode.create(RNull.instance), false, false);
        ReadVariableNode rhsRead = ReadVariableNode.create(rhsSymbol, false);

        // assemble
        seq[2] = assignFromTemp;
        seq[3] = varReset;
        seq[4] = Invisible.create(rhsRead);
        SequenceNode replacement = new SequenceNode(seq);
        replacement.assignSourceSection(source);
        return replacement;
    }

    private RNode constructRecursiveUpdateSuffix(RNode[] seq, RNode updateOp, AccessVector vecAST, SourceSection source, boolean isSuper) {
        seq[2] = updateOp;

        SequenceNode vecUpdate = new SequenceNode(seq);
        vecUpdate.assignSourceSection(source);

        return createVectorUpdate(vecAST, vecUpdate, isSuper, source, true);
    }

    private static SimpleAccessVariable getVectorVariable(AccessVector v) {
        if (v.getVector() instanceof SimpleAccessVariable) {
            return (SimpleAccessVariable) v.getVector();
        } else if (v.getVector() instanceof AccessVector) {
            return getVectorVariable((AccessVector) v.getVector());
        } else {
            Utils.nyi();
            return null;
        }
    }

    private static SimpleAccessVariable getFieldAccessVariable(FieldAccess a) {
        if (a.getLhs() instanceof SimpleAccessVariable) {
            return (SimpleAccessVariable) a.getLhs();
        } else {
            Utils.nyi();
            return null;
        }
    }

    private RNode createVectorUpdate(AccessVector a, RNode rhs, boolean isSuper, SourceSection source, boolean recursive) {
        int argLength = a.getArgs().size();
        if (!recursive) {
            argLength--; // last argument == RHS
        }
        if (a.getVector() instanceof SimpleAccessVariable) {
            SimpleAccessVariable varAST = (SimpleAccessVariable) a.getVector();
            String vSymbol = RRuntime.toString(varAST.getSymbol());

            RNode[] seq = new RNode[5];
            ReadVariableNode v = isSuper ? ReadVariableSuperMaterializedNode.create(varAST.getSource(), vSymbol, RRuntime.TYPE_ANY) : ReadVariableNode.create(varAST.getSource(), vSymbol,
                            RRuntime.TYPE_ANY, varAST.shouldCopyValue());
            final Object rhsSymbol = constructReplacementPrefix(seq, rhs, v, WriteVariableNode.Mode.INVISIBLE);
            RNode rhsAccess = ReadVariableNode.create(null, rhsSymbol, RRuntime.TYPE_ANY, false);
            RNode tmpVarAccess = ReadVariableNode.create(null, varSymbol, RRuntime.TYPE_ANY, false);

            RNode positions = createPositions(a.getArgs(), argLength, a.isSubset(), true);
            CoerceVector coerceVector = CoerceVectorFactory.create(null, null, null);
            UpdateArrayHelperNode updateOp = UpdateArrayHelperNodeFactory.create(a.isSubset(), tmpVarAccess, rhsAccess, ConstantNode.create(0), (PositionsArrayNodeValue) positions, coerceVector);
            RNode assignFromTemp = WriteVariableNode.create(vSymbol, updateOp, false, isSuper, WriteVariableNode.Mode.TEMP);
            return constructReplacementSuffix(seq, assignFromTemp, rhsSymbol, source);
        } else if (a.getVector() instanceof AccessVector) {
            // assign value to the outermost dimension and then the result (recursively) to
            // appropriate position in the lower dimension
            // TODO: it works but perhaps should be revisited

            AccessVector vecAST = (AccessVector) a.getVector();
            SimpleAccessVariable varAST = getVectorVariable(vecAST);
            String vSymbol = RRuntime.toString(varAST.getSymbol());
            RNode[] seq = new RNode[3];

            ReadVariableNode v = isSuper ? ReadVariableSuperMaterializedNode.create(varAST.getSource(), vSymbol, RRuntime.TYPE_ANY) : ReadVariableNode.create(varAST.getSource(), vSymbol,
                            RRuntime.TYPE_ANY, varAST.shouldCopyValue());
            final Object rhsSymbol = constructReplacementPrefix(seq, rhs, v, WriteVariableNode.Mode.INVISIBLE);

            RNode rhsAccess = AccessVariable.create(null, rhsSymbol).accept(this);

            RNode positions = createPositions(a.getArgs(), argLength, a.isSubset(), true);
            CoerceVector coerceVector = CoerceVectorFactory.create(null, null, null);
            UpdateArrayHelperNode updateOp = UpdateArrayHelperNodeFactory.create(a.isSubset(), vecAST.accept(this), rhsAccess, ConstantNode.create(0), (PositionsArrayNodeValue) positions,
                            coerceVector);
            return constructRecursiveUpdateSuffix(seq, updateOp, vecAST, source, isSuper);
        } else if (a.getVector() instanceof FieldAccess) {
            FieldAccess accessAST = (FieldAccess) a.getVector();
            SimpleAccessVariable varAST = getFieldAccessVariable(accessAST);

            String vSymbol = RRuntime.toString(varAST.getSymbol());
            RNode[] seq = new RNode[5];
            ReadVariableNode v = isSuper ? ReadVariableSuperMaterializedNode.create(varAST.getSource(), vSymbol, RRuntime.TYPE_ANY) : ReadVariableNode.create(varAST.getSource(), vSymbol,
                            RRuntime.TYPE_ANY, varAST.shouldCopyValue());
            final Object rhsSymbol = constructReplacementPrefix(seq, rhs, v, WriteVariableNode.Mode.INVISIBLE);
            RNode rhsAccess = ReadVariableNode.create(null, rhsSymbol, RRuntime.TYPE_ANY, false);
            RNode tmpVarAccess = ReadVariableNode.create(null, varSymbol, RRuntime.TYPE_ANY, false);
            UpdateFieldNode ufn = UpdateFieldNodeFactory.create(tmpVarAccess, rhsAccess, RRuntime.toString(accessAST.getFieldName()));
            RNode assignFromTemp = WriteVariableNode.create(vSymbol, ufn, false, isSuper, WriteVariableNode.Mode.TEMP);
            return constructReplacementSuffix(seq, assignFromTemp, rhsSymbol, source);
        } else if (a.getVector() instanceof FunctionCall) {
            FunctionCall callAST = (FunctionCall) a.getVector();
            RNode positions = createPositions(a.getArgs(), argLength, a.isSubset(), true);
            CoerceVector coerceVector = CoerceVectorFactory.create(null, null, null);
            return UpdateArrayHelperNodeFactory.create(a.isSubset(), callAST.accept(this), rhs, ConstantNode.create(0), (PositionsArrayNodeValue) positions, coerceVector);
        } else {
            Utils.nyi();
            return null;
        }
    }

    @Override
    public RNode visit(UpdateVector u) {
        return createVectorUpdate(u.getVector(), u.getRHS().accept(this), u.isSuper(), u.getSource(), false);
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

    private RCallNode prepareReplacementCall(FunctionCall f, List<ArgNode> args, final Object rhsSymbol, boolean simpleReplacement) {
        // massage arguments to replacement function call (replace v with tmp, append a)
        List<ArgNode> rfArgs = new ArrayList<>();
        rfArgs.add(ArgNode.create(null, (Symbol) null, AccessVariable.create(null, varSymbol, false)));
        if (args.size() > 1) {
            for (int i = 1; i < args.size(); ++i) {
                rfArgs.add(args.get(i));
            }
        }
        rfArgs.add(ArgNode.create(null, (Symbol) null, AccessVariable.create(null, rhsSymbol)));

        // replacement function call (use visitor for FunctionCall)
        FunctionCall rfCall = new FunctionCall(null, f.getName(), rfArgs, simpleReplacement);
        return (RCallNode) visit(rfCall);
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
    public RNode visit(Replacement replacement) {
        // preparations
        ASTNode rhsAst = replacement.getExpr();
        RNode rhs = rhsAst.accept(this);
        FunctionCall f = replacement.getBuiltin();
        List<ArgNode> args = f.getArgs();
        ASTNode val = args.get(0).getValue();
        if (val instanceof SimpleAccessVariable) {
            SimpleAccessVariable callArg = (SimpleAccessVariable) val;
            String vSymbol = RRuntime.toString(callArg.getSymbol());
            RNode[] seq = new RNode[5];
            ReadVariableNode replacementCallArg = createReplacementForVariableUsing(callArg, vSymbol, replacement);
            final Object rhsSymbol = constructReplacementPrefix(seq, rhs, replacementCallArg, WriteVariableNode.Mode.COPY);
            RNode replacementCall = prepareReplacementCall(f, args, rhsSymbol, true);
            RNode assignFromTemp = WriteVariableNode.create(vSymbol, replacementCall, false, replacement.isSuper(), WriteVariableNode.Mode.TEMP);
            return constructReplacementSuffix(seq, assignFromTemp, rhsSymbol, replacement.getSource());
        } else if (val instanceof AccessVector) {
            AccessVector callArgAst = (AccessVector) val;
            RNode replacementArg = callArgAst.accept(this);
            RNode[] seq = new RNode[5];
            final Object rhsSymbol = constructReplacementPrefix(seq, rhs, replacementArg, WriteVariableNode.Mode.COPY);
            RNode replacementCall = prepareReplacementCall(f, args, rhsSymbol, false);
            // see AssignVariable.writeVector (number of args must match)
            callArgAst.getArgs().add(ArgNode.create(rhsAst.getSource(), "value", rhsAst));
            RNode assignFromTemp = createVectorUpdate(callArgAst, replacementCall, replacement.isSuper(), replacement.getSource(), false);
            return constructReplacementSuffix(seq, assignFromTemp, rhsSymbol, replacement.getSource());
        } else {
            FieldAccess callArgAst = (FieldAccess) val;
            RNode replacementArg = callArgAst.accept(this);
            RNode[] seq = new RNode[5];
            final Object rhsSymbol = constructReplacementPrefix(seq, rhs, replacementArg, WriteVariableNode.Mode.COPY);
            RNode replacementCall = prepareReplacementCall(f, args, rhsSymbol, false);
            RNode assignFromTemp = createFieldUpdate(callArgAst, replacementCall, replacement.isSuper(), replacement.getSource());
            return constructReplacementSuffix(seq, assignFromTemp, rhsSymbol, replacement.getSource());
        }
    }

    private ReadVariableNode createReplacementForVariableUsing(SimpleAccessVariable simpleAccessVariable,
                                                               String variableSymbol, Replacement replacement) {
        SourceSection argSourceSection = simpleAccessVariable.getSource();
        boolean replacementInSuperEnvironment = replacement.isSuper();
        if (replacementInSuperEnvironment) {
            return ReadVariableSuperMaterializedNode.create(argSourceSection, variableSymbol, RRuntime.TYPE_ANY);
        } else {
            return ReadVariableNode.create(argSourceSection, variableSymbol, RRuntime.TYPE_ANY,
                    simpleAccessVariable.shouldCopyValue());
        }
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

    private static RNode createFieldUpdate(FieldAccess a, RNode rhs, boolean isSuper, SourceSection source) {
        SimpleAccessVariable varAST = null;
        if (a.getLhs() instanceof SimpleAccessVariable) {
            varAST = (SimpleAccessVariable) a.getLhs();
        } else {
            Utils.nyi();
        }
        String vSymbol = RRuntime.toString(varAST.getSymbol());

        RNode[] seq = new RNode[5];
        ReadVariableNode v = isSuper ? ReadVariableSuperMaterializedNode.create(varAST.getSource(), vSymbol, RRuntime.TYPE_ANY) : ReadVariableNode.create(varAST.getSource(), vSymbol,
                        RRuntime.TYPE_ANY, varAST.shouldCopyValue());
        final Object rhsSymbol = constructReplacementPrefix(seq, rhs, v, WriteVariableNode.Mode.INVISIBLE);
        RNode rhsAccess = ReadVariableNode.create(null, rhsSymbol, RRuntime.TYPE_ANY, false);
        RNode tmpVarAccess = ReadVariableNode.create(null, varSymbol, RRuntime.TYPE_ANY, false);
        UpdateFieldNode ufn = UpdateFieldNodeFactory.create(tmpVarAccess, rhsAccess, RRuntime.toString(a.getFieldName()));
        RNode assignFromTemp = WriteVariableNode.create(vSymbol, ufn, false, isSuper, WriteVariableNode.Mode.TEMP);
        return constructReplacementSuffix(seq, assignFromTemp, rhsSymbol, source);
    }

    @Override
    public RNode visit(UpdateField u) {
        FieldAccess a = u.getVector();
        RNode rhs = u.getRHS().accept(this);
        return createFieldUpdate(a, rhs, u.isSuper(), u.getSource());
    }

}
