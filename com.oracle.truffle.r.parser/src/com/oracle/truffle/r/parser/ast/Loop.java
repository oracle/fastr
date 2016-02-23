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

public abstract class Loop extends ASTNode {

    private final ASTNode body;

    protected Loop(SourceSection source, ASTNode body) {
        super(source);
        this.body = body;
    }

    public ASTNode getBody() {
        return body;
    }
}
