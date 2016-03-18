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
package com.oracle.truffle.r.nodes.access.variables;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

/**
 * Used when creating an {@code RCallNode} with a function that has a "name". This could subclass
 * {@link ReadVariableNode} but that is currently final.
 */
public final class NamedRNode extends RNode {
    private final RNode original;
    public final String name;

    public NamedRNode(RNode original, String name) {
        this.original = original;
        this.name = name;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return original.execute(frame);
    }

    @Override
    public RSyntaxNode getRSyntaxNode() {
        return original.asRSyntaxNode();
    }
}
