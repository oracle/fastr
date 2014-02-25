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

@Precedence(Precedence.MAX)
public class SimpleAccessVariable extends AccessVariable {

    Symbol symbol;
    boolean shouldCopyValue;

    public SimpleAccessVariable(SourceSection src, Symbol sym, boolean copy) {
        source = src;
        symbol = sym;
        shouldCopyValue = copy;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    public boolean shouldCopyValue() {
        return shouldCopyValue;
    }

    @Override
    public <R> R accept(Visitor<R> v) {
        return v.visit(this);
    }

    @Override
    public <R> List<R> visitAll(Visitor<R> v) {
        return Collections.emptyList();
    }
}
