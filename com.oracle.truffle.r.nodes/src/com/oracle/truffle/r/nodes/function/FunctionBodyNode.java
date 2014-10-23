/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.function;

import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.RNode;
import com.oracle.truffle.r.nodes.control.SequenceNode;

/**
 * Denotes a function body, which is a special variant of {@link SequenceNode}
 * that supports instrumentation. A function body always has exactly two
 * elements: a {@link SaveArgumentsNode} followed by a
 * {@link FunctionStatementsNode} (which could be made explicit as children
 * rather than subclassing {@link SequenceNode}).
 *
 */
public class FunctionBodyNode extends SequenceNode {

    public FunctionBodyNode(SaveArgumentsNode saveArgs, FunctionStatementsNode statements) {
        super(new RNode[]{saveArgs, statements});
    }
    
    public FunctionDefinitionNode getFunctionDefinitionNode() {
        return (FunctionDefinitionNode) RASTUtils.unwrapParent(this);
    }

    @Override
    public boolean isInstrumentable() {
        return true;
    }
}
