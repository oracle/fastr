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

public class SimpleAccessVariable extends AccessVariable {

    private final String variable;
    private final boolean shouldCopyValue;

    SimpleAccessVariable(SourceSection source, String variable, boolean shouldCopyValue) {
        super(source);
        this.variable = variable;
        this.shouldCopyValue = shouldCopyValue;
    }

    public String getVariable() {
        return variable;
    }

    @Override
    public int getPrecedence() {
        return Operation.MAX_PRECEDENCE;
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
