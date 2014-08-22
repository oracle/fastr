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

public class Function extends ASTNode {

    private final List<ArgNode> signature;
    private final ASTNode body;

    private Function(SourceSection source, List<ArgNode> signature, ASTNode body) {
        super(source);
        this.signature = signature;
        this.body = body;
    }

    public List<ArgNode> getSignature() {
        return signature;
    }

    public ASTNode getBody() {
        return body;
    }

    @Override
    public <R> R accept(Visitor<R> v) {
        return v.visit(this);
    }

    @Override
    public <R> List<R> visitAll(Visitor<R> v) {
        return Arrays.asList(body.accept(v));
    }

    public static ASTNode create(SourceSection source, List<ArgNode> signature, ASTNode body) {
        return new Function(source, signature, body);
    }
}
