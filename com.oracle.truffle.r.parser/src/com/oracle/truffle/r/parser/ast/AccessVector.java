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

public final class AccessVector extends FunctionCall {

    private final ASTNode vector;
    private final boolean subset;

    AccessVector(SourceSection source, ASTNode vector, List<ArgNode> arguments, boolean subset) {
        super(source, subset ? "[" : "[[", arguments);
        this.vector = vector;
        this.subset = subset;
    }

    public ASTNode getVector() {
        return vector;
    }

    public boolean isSubset() {
        return subset;
    }

    public List<ArgNode> getIndexes() {
        return super.getArguments().subList(1, super.getArguments().size());
    }

    @Override
    public <R> R accept(Visitor<R> v) {
        return v.visit(this);
    }

    @Override
    public <R> List<R> visitAll(Visitor<R> v) {
        return Arrays.asList(vector.accept(v));
    }
}
