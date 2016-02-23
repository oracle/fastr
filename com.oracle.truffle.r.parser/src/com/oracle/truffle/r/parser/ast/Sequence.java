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

import com.oracle.truffle.api.source.SourceSection;

public final class Sequence extends ASTNode {

    private final ASTNode[] expressions;

    public Sequence(SourceSection source, ASTNode[] expressions) {
        super(source);
        this.expressions = expressions;
    }

    @Override
    public <R> R accept(Visitor<R> v) {
        return v.visit(this);
    }

    public ASTNode[] getExpressions() {
        return expressions;
    }
}
