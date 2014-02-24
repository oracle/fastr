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

import com.oracle.truffle.api.*;

public class SimpleAssignVariable extends AssignVariable {

    Symbol variable;

    public SimpleAssignVariable(SourceSection src, boolean isSuper, Symbol var, ASTNode rhs) {
        super(isSuper, rhs);
        source = src;
        variable = var;
    }

    public Symbol getSymbol() {
        return variable;
    }

    @Override
    public <R> R accept(Visitor<R> v) {
        return v.visit(this);
    }
}
