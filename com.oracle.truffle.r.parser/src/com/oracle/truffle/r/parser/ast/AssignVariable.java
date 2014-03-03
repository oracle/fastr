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

import com.oracle.truffle.api.*;
import com.oracle.truffle.r.runtime.*;

public abstract class AssignVariable extends ASTNode {

    final boolean isSuper;
    ASTNode rhs;

    AssignVariable(boolean isSuper, ASTNode expr) {
        this.isSuper = isSuper;
        rhs = updateParent(expr);
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
            return writeVariable(src, isSuper, ((SimpleAccessVariable) lhs).symbol, rhs);
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
        if (!(first instanceof SimpleAccessVariable)) {
            Utils.nyi(); // TODO here we need to flatten complex assignments
        }
        SimpleAccessVariable simpleAccessVariable = (SimpleAccessVariable) first;
        AccessVector newLhs = lhs;
        if (!isSuper) {
            // ensure the vector is copied from an enclosing frame if it is not found in the current
            // frame
            SimpleAccessVariable newAccessVector = new SimpleAccessVariable(simpleAccessVariable.getSource(), simpleAccessVariable.getSymbol(), true);
            newLhs = new AccessVector(lhs.getSource(), newAccessVector, lhs.getArgs(), lhs.isSubset());
            newLhs.setParent(lhs.getParent());
        }
        UpdateVector update = new UpdateVector(isSuper, newLhs, rhs);
        lhs.args.add("value", rhs);
        return update;
    }

    public static ASTNode writeField(SourceSection src, boolean isSuper, FieldAccess lhs, ASTNode rhs) {
        return new UpdateField(src, isSuper, lhs, rhs);
    }

    public static ASTNode writeFunction(SourceSection src, boolean isSuper, FunctionCall lhs, ASTNode rhs) {
        // FIXME Probably we need a special node, for now all assign function should return value
        String builtinName = lhs.name.pretty() + "<-";
        lhs.name = Symbol.getSymbol(builtinName);
        lhs.setAssignment(true);
        lhs.setSuper(isSuper);
        if (lhs.args.size() > 0) {
            ASTNode first = lhs.args.first().getValue();
            if (!(first instanceof SimpleAccessVariable)) {
                Utils.nyi(); // TODO here we need to flatten complex assignments
            } else {
                return new Replacement(src, isSuper, lhs, rhs);
            }
        }
        return lhs;
    }

}
