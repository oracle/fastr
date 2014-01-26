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

public class If extends ASTNode {

    final ASTNode cond;
    final ASTNode trueCase;
    final ASTNode falseCase;

    If(SourceSection src, ASTNode cond, ASTNode truecase, ASTNode falsecase) {
        this.source = src;
        this.cond = updateParent(cond);
        this.trueCase = updateParent(truecase);
        this.falseCase = updateParent(falsecase);
    }

    public ASTNode getCond() {
        return cond;
    }

    public ASTNode getTrueCase() {
        return trueCase;
    }

    public ASTNode getFalseCase() {
        return falseCase;
    }

    @Override
    public <R> R accept(Visitor<R> v) {
        return v.visit(this);
    }

    @Override
    public <R> List<R> visitAll(Visitor<R> v) {
        List<R> nodes = new ArrayList<>();
        nodes.add(getCond().accept(v));
        nodes.add(getTrueCase().accept(v));
        if (getFalseCase() != null) {
            nodes.add(getFalseCase().accept(v));
        }
        return nodes;
    }

    public static If create(SourceSection src, ASTNode cond, ASTNode trueBranch) {
        return create(src, cond, trueBranch, null);
    }

    public static If create(SourceSection src, ASTNode cond, ASTNode trueBranch, ASTNode falseBranch) {
        return new If(src, cond, trueBranch, falseBranch);
    }
}
