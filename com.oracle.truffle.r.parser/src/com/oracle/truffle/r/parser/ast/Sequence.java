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

public final class Sequence extends ASTNode {

    private final ASTNode[] expressions;

    private Sequence(SourceSection source, ASTNode[] expressions) {
        super(source);
        this.expressions = expressions; // FIXME or not ... do we need to duplicate this array
    }

    @Override
    public <R> R accept(Visitor<R> v) {
        return v.visit(this);
    }

    public ASTNode[] getExpressions() {
        return expressions;
    }

    @Override
    public <R> List<R> visitAll(Visitor<R> v) {
        List<R> list = new ArrayList<>();
        for (ASTNode e : expressions) {
            list.add(e.accept(v));
        }
        return list;
    }

    public static ASTNode create(SourceSection src, ArrayList<ASTNode> exprs) {
        return new Sequence(src, exprs.toArray(new ASTNode[exprs.size()]));
    }
}
