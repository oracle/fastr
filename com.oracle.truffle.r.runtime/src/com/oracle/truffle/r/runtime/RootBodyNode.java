/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.ProbeNode;
import com.oracle.truffle.api.instrumentation.StandardTags.RootTag;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.r.runtime.nodes.RInstrumentableNode;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.instrumentation.RRootBodyNodeWrapper;

/**
 * Marks a node that represents the body of a {@link com.oracle.truffle.api.nodes.RootNode}, such
 * nodes are tagged with {@link com.oracle.truffle.api.instrumentation.StandardTags.RootTag} and
 * should inherit {@link RNode}.
 *
 * The {@link RootBodyNode} task is to save the arguments from frame's arguments array to the local
 * variables and then invoke the actual body statement accessible via {@link #getBody()}.
 */
public interface RootBodyNode extends RInstrumentableNode {
    RNode getBody();

    Object visibleExecute(VirtualFrame frame);

    @Override
    default boolean isInstrumentable() {
        return true;
    }

    @Override
    default boolean hasTag(Class<? extends Tag> tag) {
        return tag == RootTag.class;
    }

    @Override
    default WrapperNode createWrapper(ProbeNode probe) {
        return new RRootBodyNodeWrapper(this, probe);
    }
}
