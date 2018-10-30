/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.nodes.instrumentation;

import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.r.runtime.RootBodyNode;

public class RSyntaxTags {

    public final class LoopTag extends Tag {
        private LoopTag() {
            // no instances
        }
    }

    /**
     * Marks a block of statements that is the body of a function, the difference to
     * {@link com.oracle.truffle.api.instrumentation.StandardTags.RootTag} is that the
     * {@code RootTag} is supposed to save arguments and then invoke the actual body tagged with
     * this tag.
     *
     * More technically, this tag tags {@link com.oracle.truffle.r.nodes.control.BlockNode}s that
     * have parent of type {@link RootBodyNode}.
     */
    @Tag.Identifier("FUNCTIONBODYBLOCK")
    public static final class FunctionBodyBlockTag extends Tag {
        private FunctionBodyBlockTag() {
            // no instances
        }
    }

    @SuppressWarnings("unchecked") public static final Class<? extends Tag>[] ALL_TAGS = (Class<? extends Tag>[]) new Class<?>[]{StandardTags.CallTag.class, StandardTags.StatementTag.class,
                    StandardTags.RootTag.class, LoopTag.class, FunctionBodyBlockTag.class};
}
