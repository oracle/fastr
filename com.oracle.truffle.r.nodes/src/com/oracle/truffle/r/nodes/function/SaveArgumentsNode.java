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

import com.oracle.truffle.r.nodes.RNode;
import com.oracle.truffle.r.nodes.control.SequenceNode;
import com.oracle.truffle.r.runtime.env.REnvironment;

/**
 * Encapsulates the nodes that save the incoming function arguments into the frame. Functionally a
 * pass-through, but provides structure that assists instrumentation. This <b>always</b> exists even
 * if the function has no formal arguments.
 */
public class SaveArgumentsNode extends SequenceNode {

    public static final SaveArgumentsNode NO_ARGS = new SaveArgumentsNode(RNode.EMTPY_RNODE_ARRAY);

    public SaveArgumentsNode(RNode[] sequence) {
        super(sequence);
    }

    @Override
    public RNode substitute(REnvironment env) {
        SequenceNode seqSub = (SequenceNode) super.substitute(env);
        return new SaveArgumentsNode(seqSub.getSequence());
    }

}
