/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;

import com.oracle.truffle.api.source.*;

public final class ArgNode extends ASTNode {

    private final Symbol name;
    private final ASTNode value;

    private ArgNode(SourceSection source, Symbol name, ASTNode value) {
        super(source);
        this.name = name;
        this.value = value;
    }

    public static ArgNode create(SourceSection source, String name, ASTNode value) {
        return new ArgNode(source, Symbol.getSymbol(name), value);
    }

    public static ArgNode create(SourceSection source, Symbol name, ASTNode value) {
        return new ArgNode(source, name, value);
    }

    public Symbol getName() {
        return name;
    }

    public ASTNode getValue() {
        return value;
    }

    @Override
    public <R> R accept(Visitor<R> v) {
        return v.visit(this);
    }

    @Override
    public <R> List<R> visitAll(Visitor<R> v) {
        return Arrays.asList(getValue().accept(v));
    }
}
