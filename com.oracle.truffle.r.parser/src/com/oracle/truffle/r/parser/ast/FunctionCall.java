/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates
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
    private final boolean isReplacement;
    private boolean tempSuppressReplacement;

    public FunctionCall(SourceSection source, Object lhs, List<ArgNode> arguments, boolean isReplacement) {
        super(source, arguments);
        this.lhs = lhs;
        this.isReplacement = isReplacement;
    }

    FunctionCall(SourceSection src, String funName, List<ArgNode> arguments) {
        this(src, funName, arguments, false);
    }

    FunctionCall(SourceSection src, FunctionCall funCall, List<ArgNode> arguments) {
        this(src, funCall, arguments, false);
    }

    public Object getLhs() {
        return lhs;
    }

    public void setSymbol(String symbol) {
        lhs = symbol;
    }

    public boolean isSymbol() {
        return lhs instanceof String;
    }

    public String getName() {
        String result = (String) lhs;
        if (tempSuppressReplacement) {
            return result.replace("<-", "");
        }
        return result;
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

    /**
     * Calls to getName will suppress the "<-" suffix when {@code on == true}.
     */
    public void tempSuppressReplacementSuffix(boolean on) {
        tempSuppressReplacement = on;
    }

    @Override
    public <R> R accept(Visitor<R> v) {
        return v.visit(this);
    }
}
