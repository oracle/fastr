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

public class UpdateVector extends ASTNode {

    final AccessVector vector;
    final ASTNode rhs;
    final boolean isSuper;

    public UpdateVector(boolean isSuper, AccessVector vector, ASTNode rhs) {
        this.vector = updateParent(vector);
        this.rhs = updateParent(rhs);
        this.isSuper = isSuper;
    }

    public AccessVector getVector() {
        return vector;
    }

    public ASTNode getRHS() {
        return rhs;
    }

    public boolean isSuper() {
        return isSuper;
    }

    @Override
    public <R> R accept(Visitor<R> v) {
        return v.visit(this);
    }

    @Override
    public <R> List<R> visitAll(Visitor<R> v) {
        return Arrays.asList(rhs.accept(v), vector.accept(v));
    }
}
