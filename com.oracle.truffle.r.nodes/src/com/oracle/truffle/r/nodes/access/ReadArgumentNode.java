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
package com.oracle.truffle.r.nodes.access;

import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.instrument.*;
import com.oracle.truffle.r.nodes.instrument.wrappers.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.nodes.*;
import com.oracle.truffle.r.runtime.nodes.instrument.NeedsWrapper;

@NeedsWrapper
public class ReadArgumentNode extends RNode {

    private final int index;

    protected ReadArgumentNode(int index) {
        this.index = index;
    }

    /**
     * for WrapperNode subclass.
     */
    protected ReadArgumentNode() {
        index = 0;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return RArguments.getArgument(frame, index);
    }

    public int getIndex() {
        return index;
    }

    @Override
    public WrapperNode createRWrapperNode() {
        return new ReadArgumentNodeWrapper(this);
    }
}
