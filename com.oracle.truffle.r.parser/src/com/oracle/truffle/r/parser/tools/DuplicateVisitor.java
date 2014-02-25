/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.parser.tools;

import com.oracle.truffle.r.parser.ast.*;
import com.oracle.truffle.r.runtime.*;

// WARNING: the duplication only duplicates nodes that have child nodes (e.g. for node rewriting tricks)
// WARNING: it does not duplicate notes with no state or with a state that does not include nodes ! (see result = n below)
public class DuplicateVisitor extends BasicVisitor<ASTNode> {

    protected ASTNode result;

    public ASTNode duplicate(ASTNode orig) {
        result = null;
        orig.accept(this);
        return result;
    }

    protected ASTNode d(ASTNode n) {
        if (n == null) {
            return null;
        }
        n.accept(this);
        return result;
    }

    private ASTNode[] d(ASTNode[] nodes) {
        ASTNode[] newNodes = new ASTNode[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            newNodes[i] = d(nodes[i]);
        }
        return newNodes;
    }

    private AccessVector d(AccessVector n) {
        return new AccessVector(n.getSource(), d(n.getVector()), d(n.getArgs()), n.isSubset());
    }

    protected ArgumentList d(ArgumentList l) {
        ArgumentList newList = new ArgumentList.Default();
        for (ArgumentList.Entry e : l) {
            newList.add(e.getName(), d(e.getValue()));
        }
        return newList;
    }

    @Override
    public ASTNode visit(Sequence n) {
        return Sequence.create(n.getSource(), d(n.getExprs()));
    }

    @Override
    public ASTNode visit(If n) {
        return If.create(n.getSource(), d(n.getCond()), d(n.getTrueCase()), d(n.getFalseCase()));
    }

    @Override
    public ASTNode visit(Repeat n) {
        return new Repeat(n.getSource(), d(n.getBody()));
    }

    @Override
    public ASTNode visit(While n) {
        return new While(n.getSource(), d(n.getCond()), d(n.getBody()));
    }

    @Override
    public ASTNode visit(For n) {
        return new For(n.getSource(), n.getCVar(), d(n.getRange()), d(n.getBody()));
    }

    @Override
    public ASTNode visit(Next n) {
        return n;
    }

    @Override
    public ASTNode visit(Break n) {
        return n;
    }

    @Override
    public ASTNode visit(EQ n) {
        return new EQ(n.getSource(), d(n.getLHS()), d(n.getRHS()));
    }

    @Override
    public ASTNode visit(NE n) {
        return new NE(n.getSource(), d(n.getLHS()), d(n.getRHS()));
    }

    @Override
    public ASTNode visit(LE n) {
        return new LE(n.getSource(), d(n.getLHS()), d(n.getRHS()));
    }

    @Override
    public ASTNode visit(GE n) {
        return new GE(n.getSource(), d(n.getLHS()), d(n.getRHS()));
    }

    @Override
    public ASTNode visit(LT n) {
        return new LT(n.getSource(), d(n.getLHS()), d(n.getRHS()));
    }

    @Override
    public ASTNode visit(GT n) {
        return new GT(n.getSource(), d(n.getLHS()), d(n.getRHS()));
    }

    @Override
    public ASTNode visit(Sub n) {
        return new Sub(n.getSource(), d(n.getLHS()), d(n.getRHS()));
    }

    @Override
    public ASTNode visit(MatMult n) {
        return new MatMult(n.getSource(), d(n.getLHS()), d(n.getRHS()));
    }

    @Override
    public ASTNode visit(OuterMult n) {
        return new OuterMult(n.getSource(), d(n.getLHS()), d(n.getRHS()));
    }

    @Override
    public ASTNode visit(IntegerDiv n) {
        return new IntegerDiv(n.getSource(), d(n.getLHS()), d(n.getRHS()));
    }

    @Override
    public ASTNode visit(In n) {
        return new In(n.getSource(), d(n.getLHS()), d(n.getRHS()));
    }

    @Override
    public ASTNode visit(Mod n) {
        return new Mod(n.getSource(), d(n.getLHS()), d(n.getRHS()));
    }

    @Override
    public ASTNode visit(Pow n) {
        return new Pow(n.getSource(), d(n.getLHS()), d(n.getRHS()));
    }

    @Override
    public ASTNode visit(Div n) {
        return new Div(n.getSource(), d(n.getLHS()), d(n.getRHS()));
    }

    @Override
    public ASTNode visit(Colon n) {
        return new Colon(n.getSource(), d(n.getLHS()), d(n.getRHS()));
    }

    @Override
    public ASTNode visit(And n) {
        return new And(n.getSource(), d(n.getLHS()), d(n.getRHS()));
    }

    @Override
    public ASTNode visit(ElementwiseAnd n) {
        return new ElementwiseAnd(n.getSource(), d(n.getLHS()), d(n.getRHS()));
    }

    @Override
    public ASTNode visit(Or n) {
        return new Or(n.getSource(), d(n.getLHS()), d(n.getRHS()));
    }

    @Override
    public ASTNode visit(ElementwiseOr n) {
        return new ElementwiseOr(n.getSource(), d(n.getLHS()), d(n.getRHS()));
    }

    @Override
    public ASTNode visit(Add n) {
        return new Add(n.getSource(), d(n.getLHS()), d(n.getRHS()));
    }

    @Override
    public ASTNode visit(Mult n) {
        return new Mult(n.getSource(), d(n.getLHS()), d(n.getRHS()));
    }

    @Override
    public ASTNode visit(Not n) {
        return new Not(n.getSource(), d(n.getLHS()));
    }

    @Override
    public ASTNode visit(UnaryMinus n) {
        return new UnaryMinus(n.getSource(), d(n.getLHS()));
    }

    @Override
    public ASTNode visit(Constant n) {
        return n;
    }

    @Override
    public ASTNode visit(SimpleAccessVariable n) {
        return n;
    }

    @Override
    public ASTNode visit(FieldAccess n) {
        return new FieldAccess(n.getSource(), d(n.lhs()), n.getFieldName());
    }

    @Override
    public ASTNode visit(SimpleAssignVariable n) {
        return new SimpleAssignVariable(n.getSource(), n.isSuper(), n.getSymbol(), d(n.getExpr()));
    }

    @Override
    public ASTNode visit(UpdateVector n) {
        return new UpdateVector(n.isSuper(), d(n.getVector()), d(n.getRHS()));
    }

    @Override
    public ASTNode visit(UpdateField n) {
        return new UpdateField(n.getSource(), n.isSuper(), (FieldAccess) d(n.getVector()), d(n.getRHS()));
    }

    @Override
    public ASTNode visit(FunctionCall n) {
        return new FunctionCall(n.getSource(), n.getName(), d(n.getArgs()));
    }

    @Override
    public ASTNode visit(AccessVector n) {
        return new AccessVector(n.getSource(), d(n.getVector()), d(n.getArgs()), n.isSubset());
    }

    @Override
    public ASTNode visit(Function n) {
        return Function.create(d(n.getSignature()), d(n.getBody()), n.getSource());
    }

    @Override
    public ASTNode visit(ASTNode n) {
        Utils.nyi("todo: support " + n.getClass());
        return null;
    }

}
