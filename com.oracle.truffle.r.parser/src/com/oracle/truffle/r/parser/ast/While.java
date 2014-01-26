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

public class While extends Loop {

    ASTNode cond;

    public While(SourceSection src, ASTNode cond, ASTNode expr) {
        super(src, expr);
        setCond(cond);
    }

    public ASTNode getCond() {
        return cond;
    }

    public void setCond(ASTNode cond) {
        this.cond = updateParent(cond);
    }

    @Override
    public <R> List<R> visitAll(Visitor<R> v) {
        return Arrays.asList(getCond().accept(v), getBody().accept(v));
    }

    @Override
    public <R> R accept(Visitor<R> v) {
        return v.visit(this);
    }
}
