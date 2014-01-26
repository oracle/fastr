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

public class AccessVector extends Call {

    final ASTNode vector;
    final boolean subset;

    public AccessVector(SourceSection src, ASTNode vector, ArgumentList args, boolean subset) {
        super(args);
        this.source = src;
        this.vector = vector;
        this.subset = subset;
    }

    public ASTNode getVector() {
        return vector;
    }

    public boolean isSubset() {
        return subset;
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
