/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.helpers;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;

public abstract class MaterializeNode extends Node {

    protected static final int LIMIT = 10;

    public abstract Object execute(VirtualFrame frame, Object arg);

    @Specialization(limit = "LIMIT", guards = {"vec.getClass() == cachedClass"})
    protected RAbstractContainer doAbstractContainerCached(RAbstractContainer vec,
                    @SuppressWarnings("unused") @Cached("vec.getClass()") Class<?> cachedClass) {
        return vec.materialize();
    }

    @Specialization(replaces = "doAbstractContainerCached")
    protected RAbstractContainer doAbstractContainer(RAbstractContainer vec) {
        return vec.materialize();
    }

    @Fallback
    protected Object doGeneric(Object o) {
        return o;
    }

    public static MaterializeNode create() {
        return MaterializeNodeGen.create();
    }

}
