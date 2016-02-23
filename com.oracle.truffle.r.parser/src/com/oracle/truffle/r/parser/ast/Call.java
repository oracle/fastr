/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.parser.ast;

import java.util.List;

import com.oracle.truffle.api.source.SourceSection;

public final class Call extends ASTNode {

    // LHS of a call does not need to be a symbol, it can be a lambda expression (FunctionCall)
    private final Object lhs;
    private final SourceSection lhsSource;
    private final List<ArgNode> arguments;

    private Call(SourceSection source, SourceSection lhsSource, Object lhs, List<ArgNode> arguments) {
        super(source);
        this.lhs = lhs;
        this.lhsSource = lhsSource;
        this.arguments = arguments;
    }

    public Object getLhs() {
        return lhs;
    }

    public boolean isSymbol() {
        return lhs instanceof String;
    }

    public SourceSection getLhsSource() {
        return lhsSource;
    }

    public String getName() {
        String result = (String) lhs;
        return result;
    }

    public Call getFunctionCall() {
        return (Call) lhs;
    }

    public ASTNode getLhsNode() {
        return (ASTNode) lhs;
    }

    @Override
    public <R> R accept(Visitor<R> v) {
        return v.visit(this);
    }

    public List<ArgNode> getArguments() {
        return arguments;
    }

    static ASTNode create(SourceSection src, SourceSection lhsSource, ASTNode call, List<ArgNode> arguments) {
        if (call instanceof AccessVariable) {
            return new Call(src, lhsSource, ((AccessVariable) call).getVariable(), arguments);
        } else {
            return new Call(src, lhsSource, call, arguments);
        }
    }
}
