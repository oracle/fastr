/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.fastr;

import java.util.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.impl.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.runtime.data.*;

public class FastRSource {

    public static String debugSource(RFunction f) {
        CallTarget ct = f.getTarget();
        if (!(ct instanceof DefaultCallTarget)) {
            return "<no default call target>";
        }

        RootNode root = ((DefaultCallTarget) ct).getRootNode();
        final StringBuilder sb = new StringBuilder();

        sb.append("function source:\n").append(indent(root.getSourceSection().getCode(), 1));
        sb.append("\nnode sources:\n");
        appendChildrenSource(root, sb, 1);

        return sb.toString();
    }

    private static void appendChildrenSource(Node parent, StringBuilder sb, int indentationLevel) {
        for (Node n : parent.getChildren()) {
            if (n == null) {
                continue;
            }
            sb.append(indent(n.getClass().getSimpleName(), indentationLevel)).append(": ");
            if (n.getSourceSection() == null) {
                sb.append("<no source>");
            } else {
                sb.append(n.getSourceSection().getCode());
            }
            sb.append('\n');
            appendChildrenSource(n, sb, indentationLevel + 1);
        }
    }

    private static String indent(String s, int indentationLevel) {
        char[] indentArray = new char[2 * indentationLevel];
        Arrays.fill(indentArray, ' ');
        return s.replaceAll("(?m)^", new String(indentArray));
    }

}
