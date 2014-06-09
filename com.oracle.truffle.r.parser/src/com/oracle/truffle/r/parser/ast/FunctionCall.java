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
    boolean isAssignment;
    boolean isSuper;

    public FunctionCall(SourceSection src, Object lhsNode, List<ArgNode> args) {
        super(args);
        source = src;
        lhs = lhsNode;
    }

    public FunctionCall(SourceSection src, Symbol funName, List<ArgNode> args) {
        this(src, (Object) funName, args);
    }

    public FunctionCall(SourceSection src, FunctionCall funCall, List<ArgNode> args) {
        this(src, (Object) funCall, args);
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
        assert lhs instanceof Symbol;
        return (Symbol) lhs;
    }

    public FunctionCall getFunctionCall() {
        assert lhs instanceof FunctionCall;
        return (FunctionCall) lhs;
    }

    public boolean isSuper() {
        return isSuper;
    }

    public boolean isAssignment() {
        return isAssignment;
    }

    public void setSuper(boolean value) {
        this.isSuper = value;
    }

    public void setAssignment(boolean value) {
        this.isAssignment = value;
    }

    @Override
    public <R> R accept(Visitor<R> v) {
        return v.visit(this);
    }

}
