/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2012-2013, Purdue University
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.parser.ast;

import java.util.*;

import com.oracle.truffle.api.*;

public abstract class Loop extends ASTNode {

    final ASTNode body;

    public Loop(SourceSection src, ASTNode body) {
        this.source = src;
        this.body = updateParent(body);
    }

    public ASTNode getBody() {
        return body;
    }

    @Override
    public <R> List<R> visitAll(Visitor<R> v) {
        return Arrays.asList(getBody().accept(v));
    }

    public static While create(SourceSection src, ASTNode cond, ASTNode expr) {
        return new While(src, cond, expr);
    }

    public static Repeat create(SourceSection src, ASTNode expr) {
        return new Repeat(src, expr);
    }

    public static For create(SourceSection src, String cvar, ASTNode range, ASTNode body) {
        return new For(src, Symbol.getSymbol(cvar), range, body);
    }
}
