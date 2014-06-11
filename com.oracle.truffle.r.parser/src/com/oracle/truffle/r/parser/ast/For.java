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

public class For extends Loop {

    final Symbol cvar;
    final ASTNode range;

    public For(SourceSection src, Symbol cvar, ASTNode range, ASTNode body) {
        super(src, body);
        this.cvar = cvar;
        this.range = range;
    }

    public ASTNode getRange() {
        return range;
    }

    public Symbol getCVar() {
        return cvar;
    }

    @Override
    public <R> List<R> visitAll(Visitor<R> v) {
        return Arrays.asList(getBody().accept(v), range.accept(v));
    }

    @Override
    public <R> R accept(Visitor<R> v) {
        return v.visit(this);
    }
}
