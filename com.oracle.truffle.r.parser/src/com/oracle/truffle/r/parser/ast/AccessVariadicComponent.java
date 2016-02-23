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

public final class AccessVariadicComponent extends ASTNode {

    private final int index;
    private final String name;

    AccessVariadicComponent(SourceSection source, String name) {
        super(source);
        index = getVariadicComponentIndex(name);
        this.name = name;
    }

    public static int getVariadicComponentIndex(String symbol) {
        if (symbol.length() > 2 && symbol.charAt(0) == '.' && symbol.charAt(1) == '.') {
            for (int i = 2; i < symbol.length(); i++) {
                if (symbol.charAt(i) < '\u0030' || symbol.charAt(i) > '\u0039') {
                    return -1;
                }
            }
            return Integer.parseInt(symbol.substring(2));
        }
        return -1;
    }

    public int getIndex() {
        return index;
    }

    public String getName() {
        return name;
    }

    @Override
    public <R> R accept(Visitor<R> v) {
        return v.visit(this);
    }
}
