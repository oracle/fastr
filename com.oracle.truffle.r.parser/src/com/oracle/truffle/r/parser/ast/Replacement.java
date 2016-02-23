/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.parser.ast;

import com.oracle.truffle.api.source.SourceSection;

public final class Replacement extends ASTNode {

    private final boolean isSuper;
    private final ASTNode lhs;
    private final ASTNode rhs;
    private final SourceSection opSource;

    Replacement(SourceSection source, SourceSection opSource, boolean isSuper, ASTNode lhs, ASTNode rhs) {
        super(source);
        this.opSource = opSource;
        this.isSuper = isSuper;
        this.lhs = lhs;
        this.rhs = rhs;
    }

    public SourceSection getOpSource() {
        return opSource;
    }

    public boolean isSuper() {
        return isSuper;
    }

    public ASTNode getLhs() {
        return lhs;
    }

    public ASTNode getRhs() {
        return rhs;
    }

    @Override
    public <R> R accept(Visitor<R> v) {
        return v.visit(this);
    }
}
