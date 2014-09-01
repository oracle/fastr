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
package com.oracle.truffle.r.parser.ast;

import java.util.*;

import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.runtime.*;

public abstract class AssignVariable extends ASTNode {

    private final boolean isSuper;
    private final ASTNode rhs;

    protected AssignVariable(SourceSection source, boolean isSuper, ASTNode expr) {
        super(source);
        this.isSuper = isSuper;
        this.rhs = expr;
    }

    @Override
    public <R> List<R> visitAll(Visitor<R> v) {
        return Arrays.asList(getExpr().accept(v));
    }

    public ASTNode getExpr() {
        return rhs;
    }

    public boolean isSuper() {
        return isSuper;
    }

    public static ASTNode create(SourceSection src, boolean isSuper, ASTNode lhs, ASTNode rhs) {
        if (lhs instanceof SimpleAccessVariable) {
            return writeVariable(src, isSuper, ((SimpleAccessVariable) lhs).getVariable(), rhs);
        } else if (lhs instanceof SimpleAccessVariadicComponent) {
            // assigning to ..N indeed creates a local variable of that name
            return writeVariable(src, isSuper, ((SimpleAccessVariadicComponent) lhs).getName(), rhs);
        } else if (lhs instanceof AccessVector) {
            return writeVector(src, isSuper, (AccessVector) lhs, rhs);
        } else if (lhs instanceof FieldAccess) {
            return writeField(src, isSuper, (FieldAccess) lhs, rhs);
        } else if (lhs instanceof FunctionCall) {
            return writeFunction(src, isSuper, (FunctionCall) lhs, rhs);
        } else if (lhs instanceof Constant) {
            Constant c = (Constant) lhs;
            assert c.getType() == Constant.ConstantType.STRING;
            assert c.getValues().length == 1;
            String value = c.getValues()[0];
            return writeVariable(src, isSuper, Symbol.getSymbol(value), rhs);
        }
        Utils.nyi();
        return null;
    }

    public static ASTNode writeVariable(SourceSection src, boolean isSuper, Symbol name, ASTNode rhs) {
        return new SimpleAssignVariable(src, isSuper, name, rhs);
    }

    public static ASTNode writeVector(@SuppressWarnings("unused") SourceSection src, boolean isSuper, AccessVector lhs, ASTNode rhs) {
        ASTNode first = lhs.getVector();
        if (first instanceof SimpleAccessVariable) {
            SimpleAccessVariable simpleAccessVariable = (SimpleAccessVariable) first;
            AccessVector newLhs = lhs;
            if (!isSuper) {
                // ensure the vector is copied from an enclosing frame if it is not found in the
                // current frame
                SimpleAccessVariable newAccessVector = new SimpleAccessVariable(simpleAccessVariable.getSource(), simpleAccessVariable.getVariable(), true);
                newLhs = new AccessVector(lhs.getSource(), newAccessVector, lhs.getArguments(), lhs.isSubset());
            }
            UpdateVector update = new UpdateVector(isSuper, newLhs, rhs);
            lhs.getArguments().add(ArgNode.create(rhs.getSource(), "value", rhs));
            return update;
        } else if (first instanceof AccessVector || first instanceof FieldAccess) {
            UpdateVector update = new UpdateVector(isSuper, lhs, rhs);
            lhs.getArguments().add(ArgNode.create(rhs.getSource(), "value", rhs));
            return update;
        } else if (first instanceof FunctionCall) {
            FunctionCall replacementFunc = (FunctionCall) first;
            FunctionCall func = new FunctionCall(replacementFunc.getSource(), replacementFunc.getLhs(), replacementFunc.getArguments(), false);
            AccessVector newLhs = new AccessVector(func.getSource(), func, lhs.getArguments(), lhs.isSubset());
            UpdateVector update = new UpdateVector(isSuper, newLhs, rhs);
            lhs.getArguments().add(ArgNode.create(rhs.getSource(), "value", rhs));
            return writeFunction(lhs.getSource(), isSuper, replacementFunc, update);
        } else {
            Utils.nyi(); // TODO here we need to flatten complex assignments
            return null;
        }
    }

    public static ASTNode writeField(SourceSection src, boolean isSuper, FieldAccess lhs, ASTNode rhs) {
        return new UpdateField(src, isSuper, lhs, rhs);
    }

    public static ASTNode writeFunction(SourceSection src, boolean isSuper, FunctionCall lhs, ASTNode rhs) {
        // FIXME Probably we need a special node, for now all assign function should return value
        if (lhs.isSymbol()) {
            String builtinName = lhs.getName().pretty() + "<-";
            lhs.setSymbol(Symbol.getSymbol(builtinName));
        } else {
            assert false;
        }
        lhs.setAssignment(true);
        lhs.setSuper(isSuper);
        if (lhs.getArguments().size() > 0) {
            ASTNode first = lhs.getArguments().get(0).getValue();
            if (first instanceof SimpleAccessVariable || first instanceof AccessVector || first instanceof FieldAccess) {
                return new Replacement(src, isSuper, lhs, rhs);
            } else {
                Utils.nyi(); // TODO here we need to flatten complex assignments
            }
        }
        return lhs;
    }

}
