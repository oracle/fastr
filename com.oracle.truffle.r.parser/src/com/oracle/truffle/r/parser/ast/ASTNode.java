/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.parser.ast;

import java.util.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.SlowPath;

public abstract class ASTNode {

    ASTNode parent;

    SourceSection source;

    public abstract <R> R accept(Visitor<R> v);

    public abstract <R> List<R> visitAll(Visitor<R> v);

    public void setSource(SourceSection src) {
        source = src;
    }

    public SourceSection getSource() {
        return source;
    }

    @Override
    @SlowPath
    public final String toString() {
        return source.getCode();
    }

    public int getPrecedence() {
        Class<?> clazz = getClass();
        Precedence prec = clazz.getAnnotation(Precedence.class);
        return prec == null ? Precedence.MIN : prec.value();
    }

    // FIXME: do we still need these Truffle-like methods for the AST tree?
    public ASTNode getParent() {
        return parent;
    }

    public void setParent(ASTNode node) {
        parent = node;
    }

    protected <T extends ASTNode> T[] updateParent(T[] children) {
        for (T node : children) {
            updateParent(node);
        }
        return children;
    }

    protected <T extends ASTNode> T updateParent(T child) {
        if (child != null) {
            child.setParent(this);
        }
        return child;
    }
}
