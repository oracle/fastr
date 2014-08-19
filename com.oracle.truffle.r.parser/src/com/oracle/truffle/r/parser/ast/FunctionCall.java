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

public class FunctionCall extends Call {

    // LHS of a call does not need to be a symbol, it can be a lambda expression (FunctionCall)
    private Object lhs;
    private boolean isAssignment;
    private boolean isSuper;
    private boolean isReplacement;

    public FunctionCall(SourceSection source, Object lhs, List<ArgNode> arguments, boolean isReplacement) {
        super(source, arguments);
        this.lhs = lhs;
        this.isReplacement = isReplacement;
    }

    public FunctionCall(SourceSection src, Symbol funName, List<ArgNode> arguments) {
        this(src, funName, arguments, false);
    }

    public FunctionCall(SourceSection src, FunctionCall funCall, List<ArgNode> arguments) {
        this(src, funCall, arguments, false);
    }

    public Object getLhs() {
        return lhs;
    }

    public void setSymbol(Symbol symbol) {
        lhs = symbol;
    }

    public boolean isSymbol() {
        return lhs instanceof Symbol;
    }

    public Symbol getName() {
        return (Symbol) lhs;
    }

    public FunctionCall getFunctionCall() {
        return (FunctionCall) lhs;
    }

    public ASTNode getLhsNode() {
        return (ASTNode) lhs;
    }

    public boolean isSuper() {
        return isSuper;
    }

    public void setSuper(boolean value) {
        this.isSuper = value;
    }

    public boolean isAssignment() {
        return isAssignment;
    }

    public void setAssignment(boolean value) {
        this.isAssignment = value;
    }

    public boolean isReplacement() {
        return this.isReplacement;
    }

    @Override
    public <R> R accept(Visitor<R> v) {
        return v.visit(this);
    }
}
