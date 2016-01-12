/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.parser.tools;

import java.io.*;

import com.oracle.truffle.r.parser.ast.*;

public class TreePrinter extends BasicVisitor<Void> {

    private final PrintStream out;
    private int level;

    public TreePrinter(PrintStream stream) {
        out = stream;
    }

    public void print(ASTNode n) {
        n.accept(this);
    }

    public void println(ASTNode n) {
        n.accept(this);
        out.println();
    }

    @Override
    public Void visit(ASTNode n) {
        for (int i = 0; i < level; i++) {
            out.append("  ");
        }
        out.print(n.getClass().getSimpleName());
        if (n instanceof BinaryOperation) {
            out.print(" " + ((BinaryOperation) n).getOperator().getName());
        } else if (n instanceof AccessVariable) {
            out.print(" " + ((AccessVariable) n).getVariable());
        } else if (n instanceof Call) {
            Call call = (Call) n;
            out.print(" " + call.getLhs());
        }
        out.println();
        level++;
        super.visit(n);
        level--;
        return null;
    }
}
