/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.access.array.read.*;
import com.oracle.truffle.r.nodes.access.array.write.*;
import com.oracle.truffle.r.nodes.access.variables.*;
import com.oracle.truffle.r.nodes.binary.*;
import com.oracle.truffle.r.nodes.control.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.parser.ast.*;
import com.oracle.truffle.r.parser.ast.Constant.ConstantType;
import com.oracle.truffle.r.parser.ast.Operation.*;
import com.oracle.truffle.r.parser.tools.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.env.frame.*;

public final class RTruffleVisitor extends BasicVisitor<RSyntaxNode> {

    public RSyntaxNode transform(ASTNode ast) {
        return ast.accept(this);
    }

    @Override
    public RSyntaxNode visit(Constant c) {
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
                switch (c.getValues()[0]) {
                    case "NA":
                        return ConstantNode.create(src, RRuntime.LOGICAL_NA);
                    case "1":
                        return ConstantNode.create(src, true);
                    case "0":
                        return ConstantNode.create(src, false);
                    default:
                        throw new AssertionError();
                }
            case STRING:
                return ConstantNode.create(src, c.getValues()[0]);
            case COMPLEX:
                if (c.getValues()[0].equals("NA_complex_")) {
                    return ConstantNode.create(src, RDataFactory.createComplex(RRuntime.COMPLEX_NA_REAL_PART, RRuntime.COMPLEX_NA_IMAGINARY_PART));
                } else {
                    return ConstantNode.create(src, RDataFactory.createComplex(0, RRuntime.string2double(c.getValues()[0])));
                }
            default:
                throw new UnsupportedOperationException();
        }
    }

    @Override
    public RSyntaxNode visit(Formula formula) {
        return ConstantNode.create(RDataFactory.createFormula(formula.getSource(), formula.getResponse().accept(this), formula.getModel().accept(this)));
    }

    @Override
    public RSyntaxNode visit(Missing m) {
        return ConstantNode.create(REmpty.instance);
    }

    @Override
    public RSyntaxNode visit(FunctionCall callParam) {
        FunctionCall call = callParam;
        String callName = call.isSymbol() ? call.getName() : null;
        SourceSection callSource = call.getSource();
        int argsCharLength = 0;
        int index = 0;
        String[] argumentNames = new String[call.getArguments().size()];
        RNode[] nodes = new RNode[call.getArguments().size()];
        for (ArgNode e : call.getArguments()) {
            argumentNames[index] = e.getName();
            ASTNode val = e.getValue();
            if (val != null) {
                // the source must include a value assignment (if there is one) - this is ensured by
                // assigning the source section of the argument node
                val.setSource(e.getSource());
                nodes[index] = val.accept(this).asRNode();
            }
            if (e.getSource() != null) {
                argsCharLength += e.getSource().getCharLength();
            } else {
                // happens for Replacement FIXME
            }
            index++;
        }

        RSyntaxNode lhs = null;
        int lhsLength;
        if (callName != null) {
            lhsLength = callName.length();
        } else {
            lhs = call.getLhsNode().accept(this);
            lhsLength = lhs.getSourceSection().getCharLength();
        }
        SourceSection argsSource = ASTNode.adjustedSource(callSource, callSource.getCharIndex() + lhsLength, argsCharLength);
        CallArgumentsNode aCallArgNode = CallArgumentsNode.create(argsSource, !call.isReplacement(), nodes, ArgumentsSignature.get(argumentNames));

        if (callName != null) {
            String functionName = callName;
            if (RGroupGenerics.isGroupGeneric(functionName)) {
                return GroupDispatchNode.create(functionName, aCallArgNode, callSource);
            }
            SourceSection varSource = ASTNode.adjustedSource(callSource, callSource.getCharIndex(), lhsLength);
            return RCallNode.createCall(callSource, ReadVariableNode.createForced(varSource, functionName, RType.Function), aCallArgNode, callParam);
        } else {
            return RCallNode.createCall(callSource, lhs.asRNode(), aCallArgNode, callParam);
        }
    }

    @Override
    public RSyntaxNode visit(Function func) {
        RootCallTarget callTarget = null;
        try {
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
            if (!argumentsList.isEmpty()) {
                RNode[] init = new RNode[argumentsList.size()];
                int index = 0;
                for (ArgNode arg : argumentsList) {
                    // Parse argument's default value
                    RNode defaultValue;
                    ASTNode defaultValNode = arg.getValue();
                    if (defaultValNode != null) {
                        defaultValue = arg.getValue().accept(this).asRNode();
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
            } else {
                saveArguments = new SaveArgumentsNode(RNode.EMTPY_RNODE_ARRAY);
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
            FrameSlotChangeMonitor.initializeFunctionFrameDescriptor(descriptor);
            String description = getFunctionDescription(func);
            FunctionDefinitionNode rootNode = new FunctionDefinitionNode(func.getSource(), descriptor, new FunctionBodyNode(saveArguments, statements), formals, description, false);
            callTarget = Truffle.getRuntime().createCallTarget(rootNode);
            return FunctionExpressionNode.create(func.getSource(), callTarget);
        } catch (Throwable err) {
            throw new RInternalError(err, "visit(Function)");
        }
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
        RNode operand = op.getLHS().accept(this).asRNode();
        String functionName = op.getOperator().getName();
        CallArgumentsNode aCallArgNode = CallArgumentsNode.createUnnamed(false, true, operand);
        if (RGroupGenerics.isGroupGeneric(functionName)) {
            assert RGroupGenerics.getGroup(functionName) == RGroupGenerics.Ops;
            return GroupDispatchNode.create(functionName, aCallArgNode, op.getSource());
        }
        return RCallNode.createOpCall(op.getSource(), null, functionName, aCallArgNode, op);
    }

    @Override
    public RSyntaxNode visit(BinaryOperation op) {
        RNode left = op.getLHS().accept(this).asRNode();
        RNode right = op.getRHS().accept(this).asRNode();
        if (op.getOperator() == Operator.COLON) {
            return ColonNode.create(op.getSource(), left, right);
        } else {
            String functionName = op.getOperator().getName();
            CallArgumentsNode aCallArgNode = CallArgumentsNode.createUnnamed(false, true, left, right);
            if (RGroupGenerics.isGroupGeneric(functionName)) {
                return GroupDispatchNode.create(functionName, aCallArgNode, op.getSource());
            }
            // create a SourceSection for the operator
            SourceSection opSrc = op.getSource();
            String code = opSrc.getCode();
            String opName = op.getOperator().getName();
            int charIndex = code.indexOf(opName);
            SourceSection opNameSrc = opSrc.getSource().createSection(opSrc.getIdentifier(), opSrc.getCharIndex() + charIndex, opName.length());
            return RCallNode.createOpCall(op.getSource(), opNameSrc, functionName, aCallArgNode, op);
        }
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
    public RSyntaxNode visit(ASTNode n) {
        throw new UnsupportedOperationException("Unsupported AST Node " + n.getClass().getName());
    }

    @Override
    public RSyntaxNode visit(ArgNode n) {
        assert n.getValue() != null;
        return n.getValue().accept(this);
    }

    private RSyntaxNode createPositionsForSyntaxUpdate(List<ArgNode> argList, int argLength, boolean isSubset, RNode vector, RNode rhs) {
        RNode[] positions;
        boolean varArgFound = false;
        if (argLength == 0) {
            positions = new RNode[]{ConstantNode.create(RMissing.instance)};
        } else {
            int newArgLength = argLength;
            positions = new RNode[newArgLength];
            int ind = 0;
            for (int i = 0; i < argLength; i++) {
                ArgNode argNode = argList.get(i);
                ASTNode node = argNode.getValue();
                if (node instanceof SimpleAccessVariable && ((SimpleAccessVariable) node).getVariable().equals(ArgumentsSignature.VARARG_NAME)) {
                    varArgFound = true;
                }
                positions[ind++] = (node == null ? ConstantNode.create(RMissing.instance) : node.accept(this).asRNode());
            }
        }
        PositionsArrayNodeValue posArrayNodeValue = new PositionsArrayNodeValue(isSubset, positions, varArgFound);
        /*
         * UpdateArrayHelperNode insists on a CoerceVector, which is unecessary for the syntax AST,
         * but we don't want to change that class.
         */
        CoerceVector coerceVector = CoerceVectorNodeGen.create(null, vector, null);
        return UpdateArrayHelperNodeGen.create(isSubset, true, 0, vector, rhs, posArrayNodeValue, coerceVector);
    }

    private RSyntaxNode createPositions(List<ArgNode> argList, int argLength, boolean isSubset, RNode castContainer, RNode tmpVarAccess, RNode rhsAccess, CoerceVector coerceVector,
                    boolean isAssignment) {
        RNode[] positions;
        RNode exact = ConstantNode.create(RRuntime.LOGICAL_TRUE);
        boolean exactInSource = false;
        RNode drop = ConstantNode.create(RMissing.instance);
        boolean dropInSource = false;
        boolean varArgFound = false;
        if (argLength == 0) {
            positions = new RNode[]{ConstantNode.create(RMissing.instance)};
        } else {
            int newArgLength = argLength;
            if (!isAssignment) {
                for (int i = 0; i < argLength; i++) {
                    ArgNode argNode = argList.get(i);
                    ASTNode node = argNode.getValue();
                    String name = argNode.getName();
                    if (name != null && name.toString() != null) {
                        String s = name.toString();
                        if (!exactInSource && !isSubset && s.equals("exact")) {
                            exact = (node == null ? ConstantNode.create(RMissing.instance) : CastLogicalNodeGen.create(node.accept(this).asRNode(), false, false, false));
                            exactInSource = true;
                            newArgLength--;
                        }
                        if (!dropInSource && s.equals("drop")) {
                            // apparently, even though "drop" is only defined for [] operator in the
                            // docs, it's ignored for the [[]] operator as well instead of
                            // contributing to indexes
                            drop = (node == null ? ConstantNode.create(RMissing.instance) : CastLogicalNodeGen.create(node.accept(this).asRNode(), false, false, false));
                            dropInSource = true;
                            newArgLength--;
                        }
                    }
                }
            }
            positions = new RNode[newArgLength];
            int ind = 0;
            boolean exactFound = false;
            boolean dropFound = false;
            for (int i = 0; i < argLength; i++) {
                ArgNode argNode = argList.get(i);
                ASTNode node = argNode.getValue();
                if (node instanceof SimpleAccessVariable && ((SimpleAccessVariable) node).getVariable().equals(ArgumentsSignature.VARARG_NAME)) {
                    varArgFound = true;
                }
                if (!isAssignment) {
                    String name = argNode.getName();
                    if (name != null && name.toString() != null) {
                        String s = name.toString();
                        if (!exactFound && !isSubset && s.equals("exact")) {
                            exactFound = true;
                            continue;
                        }
                        if (!dropFound && s.equals("drop")) {
                            dropFound = true;
                            continue;
                        }
                    }
                }
                positions[ind++] = (node == null ? ConstantNode.create(RMissing.instance) : node.accept(this).asRNode());
            }
        }
        if (!isAssignment) {
            assert castContainer != null;
            PositionsArrayNode posArrayNode = new PositionsArrayNode(isSubset, positions, varArgFound);
            return AccessArrayNode.create(isSubset, true, exactInSource, dropInSource, castContainer, exact, posArrayNode, drop);
        } else {
            PositionsArrayNodeValue posArrayNodeValue = new PositionsArrayNodeValue(isSubset, positions, varArgFound);
            assert tmpVarAccess != null;
            assert rhsAccess != null;
            assert coerceVector != null;
            return UpdateArrayHelperNodeGen.create(isSubset, true, tmpVarAccess, rhsAccess, ConstantNode.create(0), posArrayNodeValue, coerceVector);
        }
    }

    /**
     * The sequence created for a {@linkplain #visit(Replacement) replacement} consists of the
     * following elements:
     * <ol>
     * <li>(prefix) store the right-hand side in an anonymous slot,
     * <li>(prefix) assign the left-hand side to {@code *tmp*},
     * <li>(suffix) assign from the replacement call,
     * <li>(suffix) remove *tmp*,
     * <li>(suffix) remove the anonymous right-hand side slot and answer its value.
     * <ol>
     */
    private static RNode[] createReplacementSequence() {
        return new RNode[2];
    }

    private static String createTempName() {
        //@formatter:off
        // store a - need to use temporary, otherwise there is a failure in case multiple calls to
        // the replacement form are chained:
        // x<-c(1); y<-c(1); dim(x)<-1; dim(y)<-1; attr(x, "dimnames")<-(attr(y, "dimnames")<-list("b"))
        //@formatter:on
        return new Object().toString();
    }

    private static ReplacementNode constructReplacementSuffix(RNode rhs, RNode v, boolean copyRhs, RNode assignFromTemp, String tmpSymbol, String rhsSymbol, SourceSection source) {
        return new ReplacementNode(source, rhs, v, copyRhs, assignFromTemp, tmpSymbol, rhsSymbol);
    }

    private static RecursiveReplacementNode createUpdateSequence(String rhsSymbol, RNode rhs, String tmpSymbol, RNode v, RNode updateOp, SourceSection source) {
        return new RecursiveReplacementNode(source, rhsSymbol, rhs, tmpSymbol, v, updateOp);
    }

    private static SimpleAccessVariable getVectorVariable(AccessVector v) {
        if (v.getVector() instanceof SimpleAccessVariable) {
            return (SimpleAccessVariable) v.getVector();
        } else if (v.getVector() instanceof AccessVector) {
            return getVectorVariable((AccessVector) v.getVector());
        } else if (v.getVector() instanceof FieldAccess) {
            return getFieldAccessVariable((FieldAccess) v.getVector());
        } else if (v.getVector() instanceof FunctionCall) {
            return null;
        } else {
            throw RInternalError.unimplemented();
        }
    }

    private static SimpleAccessVariable getFieldAccessVariable(FieldAccess a) {
        if (a.getLhs() instanceof SimpleAccessVariable) {
            return (SimpleAccessVariable) a.getLhs();
        } else if (a.getLhs() instanceof FunctionCall) {
            return null;
        } else {
            throw RInternalError.unimplemented();
        }
    }

    private RSyntaxNode createVectorUpdate(AccessVector a, RNode rhs, boolean isSuper, SourceSection source, boolean recursive) {
        RSyntaxNode syntaxAST = null;
        int argLength = a.getIndexes().size();
        // If recursive no need to set syntaxAST as already handled at top-level
        if (!recursive) {
            argLength--; // last argument == RHS
            RNode theVector = a.getVector().accept(this).asRNode();
            syntaxAST = createPositionsForSyntaxUpdate(a.getIndexes(), argLength, a.isSubset(), theVector, rhs);
        }
        if (a.getVector().getClass() == FunctionCall.class) {
            // N.B. This is the only branch that does not set result to a ReplacementNode
            FunctionCall callAST = (FunctionCall) a.getVector();
            CoerceVector coerceVector = CoerceVectorNodeGen.create(null, null, null);
            return createPositions(a.getIndexes(), argLength, a.isSubset(), null, callAST.accept(this).asRNode(), rhs, coerceVector, true);
        } else {
            RSyntaxNode result;
            String tmpSymbol = createTempName();
            String rhsSymbol = createTempName();
            if (a.getVector() instanceof SimpleAccessVariable) {
                SimpleAccessVariable varAST = (SimpleAccessVariable) a.getVector();
                String vSymbol = varAST.getVariable();

                ReadVariableNode v = isSuper ? ReadVariableNode.createSuperLookup(varAST.getSource(), vSymbol) : ReadVariableNode.create(varAST.getSource(), vSymbol, varAST.shouldCopyValue());
                String rhsSymbolString = RRuntime.toString(rhsSymbol);
                RNode rhsAccess = ReadVariableNode.create(rhsSymbolString, false);
                RNode tmpVarAccess = ReadVariableNode.create(tmpSymbol, false);

                CoerceVector coerceVector = CoerceVectorNodeGen.create(null, null, null);
                RSyntaxNode updateOp = createPositions(a.getIndexes(), argLength, a.isSubset(), null, tmpVarAccess, rhsAccess, coerceVector, true);
                RNode assignFromTemp = WriteVariableNode.createAnonymous(vSymbol, updateOp.asRNode(), WriteVariableNode.Mode.INVISIBLE, isSuper);
                result = constructReplacementSuffix(rhs, v, false, assignFromTemp, tmpSymbol, rhsSymbol, source);
            } else if (a.getVector() instanceof AccessVector) {
                // assign value to the outermost dimension and then the result (recursively) to
                // appropriate position in the lower dimension
                // TODO: it works but perhaps should be revisited

                AccessVector vecAST = (AccessVector) a.getVector();
                SimpleAccessVariable varAST = getVectorVariable(vecAST);
                RNode v;
                if (varAST != null) {
                    String vSymbol = varAST.getVariable();
                    v = isSuper ? ReadVariableNode.createSuperLookup(varAST.getSource(), vSymbol) : ReadVariableNode.create(varAST.getSource(), vSymbol, varAST.shouldCopyValue());
                } else {
                    v = vecAST.getVector().accept(this).asRNode();
                }
                RNode rhsAccess = AccessVariable.create(null, rhsSymbol).accept(this).asRNode();
                CoerceVector coerceVector = CoerceVectorNodeGen.create(null, null, null);
                RSyntaxNode updateOp = createPositions(a.getIndexes(), argLength, a.isSubset(), null, vecAST.accept(this).asRNode(), rhsAccess, coerceVector, true);
                RecursiveReplacementNode vecUpdate = createUpdateSequence(rhsSymbol, rhs, tmpSymbol, v, updateOp.asRNode(), source);
                result = createVectorUpdate(vecAST, vecUpdate, isSuper, source, true);
            } else if (a.getVector() instanceof FieldAccess) {
                FieldAccess accessAST = (FieldAccess) a.getVector();
                SimpleAccessVariable varAST = getFieldAccessVariable(accessAST);

                RNode v;
                if (varAST != null) {
                    String vSymbol = varAST.getVariable();
                    v = isSuper ? ReadVariableNode.createSuperLookup(varAST.getSource(), vSymbol) : ReadVariableNode.create(varAST.getSource(), vSymbol, varAST.shouldCopyValue());
                } else {
                    v = accessAST.getLhs().accept(this).asRNode();
                }
                RSyntaxNode rhsAccess = AccessVariable.create(null, rhsSymbol).accept(this);

                CoerceVector coerceVector = CoerceVectorNodeGen.create(null, null, null);
                RSyntaxNode updateOp = createPositions(a.getIndexes(), argLength, a.isSubset(), null, accessAST.accept(this).asRNode(), rhsAccess.asRNode(), coerceVector, true);
                RecursiveReplacementNode fieldUpdate = createUpdateSequence(rhsSymbol, rhs, tmpSymbol, v, updateOp.asRNode(), source);
                result = createFieldUpdate(accessAST, fieldUpdate, isSuper, source);
            } else {
                throw RInternalError.unimplemented();
            }
            if (result instanceof ReplacementNode) {
                ((ReplacementNode) result).setSyntaxAST(syntaxAST);
            }
            return result;
        }
    }

    @Override
    public RSyntaxNode visit(UpdateVector u) {
        return createVectorUpdate(u.getVector(), u.getRHS().accept(this).asRNode(), u.isSuper(), u.getSource(), false);
    }

    @Override
    public RSyntaxNode visit(SimpleAssignVariable n) {
        if (n.getExpr() instanceof Function) {
            ((Function) n.getExpr()).setDebugName(n.getVariable().toString());
        }
        RSyntaxNode expression = n.getExpr().accept(this);
        return (RSyntaxNode) WriteVariableNode.create(n.getSource(), n.getVariable(), expression.asRNode(), n.isSuper());
    }

    private RCallNode prepareReplacementCall(FunctionCall f, List<ArgNode> args, String tmpSymbol, String rhsSymbol, boolean simpleReplacement) {
        // massage arguments to replacement function call (replace v with tmp, append a)
        List<ArgNode> rfArgs = new ArrayList<>();
        rfArgs.add(ArgNode.create(null, null, AccessVariable.create(null, tmpSymbol, false)));
        if (args.size() > 1) {
            for (int i = 1; i < args.size(); i++) {
                rfArgs.add(args.get(i));
            }
        }
        rfArgs.add(ArgNode.create(null, null, AccessVariable.create(null, rhsSymbol)));

        // replacement function call (use visitor for FunctionCall)
        FunctionCall rfCall = new FunctionCall(f.getSource(), f.getName(), rfArgs, simpleReplacement);
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
     * replacement. This value must be stored as it is the result of the entire replacement expression.
     */
    //@formatter:on
    @Override
    public RSyntaxNode visit(Replacement replacement) {
        // preparations
        ASTNode rhsAst = replacement.getExpr();
        RNode rhs = rhsAst.accept(this).asRNode();
        FunctionCall fAst = replacement.getReplacementFunctionCall();
        // fAst has the function name as "x<-" already; we don't want that in the syntaxAST
        fAst.tempSuppressReplacementSuffix(true);
        RNode f = fAst.accept(this).asRNode();
        fAst.tempSuppressReplacementSuffix(false);
        RSyntaxNode syntaxAST = new WriteReplacementNode((RCallNode) f, rhs);
        List<ArgNode> args = fAst.getArguments();
        ASTNode val = args.get(0).getValue();
        String tmpSymbol = createTempName();
        RNode assignFromTemp;
        RNode replacementArg;
        String rhsSymbol = createTempName();
        if (val instanceof SimpleAccessVariable) {
            SimpleAccessVariable callArg = (SimpleAccessVariable) val;
            String vSymbol = callArg.getVariable();
            replacementArg = createReplacementForVariableUsing(callArg, vSymbol, replacement);
            RNode replacementCall = prepareReplacementCall(fAst, args, tmpSymbol, rhsSymbol, true);
            if (replacement.isSuper()) {
                assignFromTemp = WriteSuperFrameVariableNode.create(vSymbol, replacementCall, WriteVariableNode.Mode.INVISIBLE);
            } else {
                assignFromTemp = WriteLocalFrameVariableNode.createAnonymous(vSymbol, replacementCall, WriteVariableNode.Mode.INVISIBLE);
            }
        } else if (val instanceof AccessVector) {
            AccessVector callArgAst = (AccessVector) val;
            replacementArg = callArgAst.accept(this).asRNode();
            RNode replacementCall = prepareReplacementCall(fAst, args, tmpSymbol, rhsSymbol, false);
            // see AssignVariable.writeVector (number of args must match)
            callArgAst.getArguments().add(ArgNode.create(rhsAst.getSource(), "value", rhsAst));
            assignFromTemp = createVectorUpdate(callArgAst, replacementCall, replacement.isSuper(), replacement.getSource(), false).asRNode();
        } else {
            FieldAccess callArgAst = (FieldAccess) val;
            replacementArg = callArgAst.accept(this).asRNode();
            RSyntaxNode replacementCall = prepareReplacementCall(fAst, args, tmpSymbol, rhsSymbol, false);
            assignFromTemp = createFieldUpdate(callArgAst, replacementCall.asRNode(), replacement.isSuper(), replacement.getSource()).asRNode();
        }
        RSyntaxNode result = constructReplacementSuffix(rhs, replacementArg, true, assignFromTemp, tmpSymbol, rhsSymbol, replacement.getSource());
        ((ReplacementNode) result).setSyntaxAST(syntaxAST);
        return result;
    }

    private static ReadVariableNode createReplacementForVariableUsing(SimpleAccessVariable simpleAccessVariable, String variableSymbol, Replacement replacement) {
        SourceSection argSourceSection = simpleAccessVariable.getSource();
        boolean replacementInSuperEnvironment = replacement.isSuper();
        if (replacementInSuperEnvironment) {
            return ReadVariableNode.createSuperLookup(argSourceSection, variableSymbol);
        } else {
            return ReadVariableNode.create(argSourceSection, variableSymbol, simpleAccessVariable.shouldCopyValue());
        }
    }

    @Override
    public RSyntaxNode visit(SimpleAccessVariable n) {
        return ReadVariableNode.create(n.getSource(), n.getVariable(), n.shouldCopyValue());
    }

    @Override
    public RSyntaxNode visit(SimpleAccessTempVariable n) {
        String symbol = RRuntime.toString(n.getSymbol());
        return ReadVariableNode.create(n.getSource(), symbol, false);
    }

    @Override
    public RSyntaxNode visit(SimpleAccessVariadicComponent n) {
        int ind = n.getIndex();
        return new ReadVariadicComponentNode(ind > 0 ? ind - 1 : ind);
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
        return matchSources(WhileNode.create(condition, body, false), loop);
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
        return matchSources(WhileNode.create(ConstantNode.create(true), body, true), loop);
    }

    private static RSyntaxNode matchSources(RSyntaxNode truffleNode, ASTNode astNode) {
        truffleNode.asRNode().assignSourceSection(astNode.getSource());
        return truffleNode;
    }

    @Override
    public RSyntaxNode visit(For loop) {
        WriteVariableNode cvar = WriteVariableNode.create(loop.getSource(), loop.getVariable(), null, false);
        RSyntaxNode range = loop.getRange().accept(this);
        RSyntaxNode body = loop.getBody().accept(this);
        return matchSources(ForNode.create(cvar, range, new BlockNode(loop.getBody().getSource(), body)), loop);
    }

    @Override
    public RSyntaxNode visit(FieldAccess n) {
        AccessFieldNode afn = AccessFieldNodeGen.create(n.getLhs().accept(this).asRNode(), n.getFieldName());
        afn.assignSourceSection(n.getSource());
        return afn;
    }

    private RSyntaxNode createFieldUpdate(FieldAccess a, RNode rhs, boolean isSuper, SourceSection source) {
        UpdateFieldNode syntaxAST = UpdateFieldNodeGen.create(a.getLhs().accept(this).asRNode(), rhs, a.getFieldName());
        syntaxAST.assignSourceSection(source);
        RSyntaxNode result = null;

        if (a.getLhs().getClass() == FunctionCall.class) {
            FunctionCall callAST = (FunctionCall) a.getLhs();
            CoerceVector coerceVector = CoerceVectorNodeGen.create(null, null, null);
            List<ArgNode> arguments = new ArrayList<>(2);
            arguments.add(ArgNode.create(null, (String) null, Constant.createStringConstant(null, new String[]{a.getFieldName().toString()})));
            return createPositions(arguments, arguments.size(), false, null, callAST.accept(this).asRNode(), rhs, coerceVector, true);
        } else {
            String tmpSymbol = createTempName();
            String rhsSymbol = createTempName();
            if (a.getLhs() instanceof SimpleAccessVariable) {
                SimpleAccessVariable varAST = (SimpleAccessVariable) a.getLhs();
                String vSymbol = varAST.getVariable();

                ReadVariableNode v = isSuper ? ReadVariableNode.createSuperLookup(varAST.getSource(), vSymbol) : ReadVariableNode.create(varAST.getSource(), vSymbol, varAST.shouldCopyValue());
                RNode rhsAccess = ReadVariableNode.create(rhsSymbol, false);
                RNode tmpVarAccess = ReadVariableNode.create(tmpSymbol, false);
                UpdateFieldNode ufn = UpdateFieldNodeGen.create(tmpVarAccess, rhsAccess, a.getFieldName());
                RNode assignFromTemp = WriteSuperFrameVariableNode.createAnonymous(vSymbol, ufn, WriteVariableNode.Mode.INVISIBLE, isSuper);
                result = constructReplacementSuffix(rhs, v, false, assignFromTemp, tmpSymbol, rhsSymbol, source);
            } else if (a.getLhs() instanceof AccessVector) {
                AccessVector vecAST = (AccessVector) a.getLhs();
                SimpleAccessVariable varAST = getVectorVariable(vecAST);
                RNode v;
                if (varAST != null) {
                    String vSymbol = varAST.getVariable();
                    v = isSuper ? ReadVariableNode.createSuperLookup(varAST.getSource(), vSymbol) : ReadVariableNode.create(varAST.getSource(), vSymbol, varAST.shouldCopyValue());
                } else {
                    v = vecAST.getVector().accept(this).asRNode();
                }
                RSyntaxNode rhsAccess = AccessVariable.create(null, rhsSymbol).accept(this);

                List<ArgNode> arguments = new ArrayList<>(2);
                arguments.add(ArgNode.create(null, (String) null, Constant.createStringConstant(null, new String[]{a.getFieldName().toString()})));
                CoerceVector coerceVector = CoerceVectorNodeGen.create(null, null, null);
                RSyntaxNode updateOp = createPositions(arguments, arguments.size(), false, null, vecAST.accept(this).asRNode(), rhsAccess.asRNode(), coerceVector, true);
                RecursiveReplacementNode vecUpdate = createUpdateSequence(rhsSymbol, rhs, tmpSymbol, v, updateOp.asRNode(), source);
                result = createVectorUpdate(vecAST, vecUpdate, isSuper, source, true);
            } else if (a.getLhs() instanceof FieldAccess) {
                FieldAccess accessAST = (FieldAccess) a.getLhs();
                SimpleAccessVariable varAST = getFieldAccessVariable(accessAST);

                String vSymbol = varAST.getVariable();
                ReadVariableNode v = isSuper ? ReadVariableNode.createSuperLookup(varAST.getSource(), vSymbol) : ReadVariableNode.create(varAST.getSource(), vSymbol, varAST.shouldCopyValue());
                RSyntaxNode rhsAccess = AccessVariable.create(null, rhsSymbol).accept(this);
                UpdateFieldNode ufn = UpdateFieldNodeGen.create(accessAST.accept(this).asRNode(), rhsAccess.asRNode(), a.getFieldName());
                RecursiveReplacementNode fieldUpdate = createUpdateSequence(rhsSymbol, rhs, tmpSymbol, v, ufn, source);
                result = createFieldUpdate(accessAST, fieldUpdate, isSuper, source);
            } else {
                throw RInternalError.unimplemented();
            }
            ((ReplacementNode) result).setSyntaxAST(syntaxAST);
            return result;
        }
    }

    @Override
    public RSyntaxNode visit(UpdateField u) {
        FieldAccess a = u.getVector();
        RSyntaxNode rhs = u.getRHS().accept(this);
        return createFieldUpdate(a, rhs.asRNode(), u.isSuper(), u.getSource());
    }

}
