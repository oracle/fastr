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

public class FunctionCall extends Call {

    // FIXME: LHS of a call does not need to be a symbol, it can be a lambda expression
    Symbol name;
    boolean isAssignment;
    boolean isSuper;

    public FunctionCall(SourceSection src, Symbol funName, List<ArgNode> args) {
        super(args);
        source = src;
        name = funName;
    }

    public Symbol getName() {
        return name;
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
