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

public final class While extends Loop {

    private final ASTNode condition;

    While(SourceSection source, ASTNode condition, ASTNode expression) {
        super(source, expression);
        this.condition = condition;
    }

    public ASTNode getCondition() {
        return condition;
    }

    @Override
    public <R> R accept(Visitor<R> v) {
        return v.visit(this);
    }
}
