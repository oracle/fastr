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

import java.util.Collections;
import java.util.List;

import com.oracle.truffle.api.source.*;

public final class AccessVariable extends ASTNode {

    private final String variable;

    private AccessVariable(SourceSection source, String variable) {
        super(source);
        this.variable = variable;
    }

    public String getVariable() {
        return variable;
    }

    @Override
    public int getPrecedence() {
        return Operation.MAX_PRECEDENCE;
    }

    @Override
    public <R> R accept(Visitor<R> v) {
        return v.visit(this);
    }

    @Override
    public <R> List<R> visitAll(Visitor<R> v) {
        return Collections.emptyList();
    }

    public static ASTNode create(SourceSection src, String name) {
        return new AccessVariable(src, name);
    }
}
