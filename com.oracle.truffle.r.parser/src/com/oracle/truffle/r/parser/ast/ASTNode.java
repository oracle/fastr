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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.source.SourceSection;

public abstract class ASTNode {

    private final SourceSection source;

    protected ASTNode(SourceSection source) {
        this.source = source;
    }

    public abstract <R> R accept(Visitor<R> v);

    public SourceSection getSource() {
        return source;
    }

    @Override
    @TruffleBoundary
    public final String toString() {
        return source.getCode();
    }
}
