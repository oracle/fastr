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

import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.CompilerDirectives.SlowPath;

public abstract class ASTNode {

    private SourceSection source;

    protected ASTNode(SourceSection source) {
        this.source = source;
    }

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
}
