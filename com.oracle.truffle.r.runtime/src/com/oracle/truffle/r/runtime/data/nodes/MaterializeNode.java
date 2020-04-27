/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data.nodes;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.data.AbstractContainerLibrary;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

/**
 * Profiled version of slow path operation {@link RAbstractVector#materialize()}.
 */
@ImportStatic(DSLConfig.class)
@GenerateUncached
public abstract class MaterializeNode extends Node {

    protected static final int LIMIT = 10;

    public abstract Object execute(Object arg);

    // few simple fast-paths for common types:

    @Specialization
    protected RList doList(RList vec) {
        return vec;
    }

    @Specialization
    protected RIntVector doList(RIntVector vec) {
        return vec;
    }

    @Specialization
    protected RDoubleVector doList(RDoubleVector vec) {
        return vec;
    }

    @Specialization
    protected RStringVector doList(RStringVector vec) {
        return vec;
    }

    @Specialization(limit = "getTypedVectorDataLibraryCacheSize()")
    protected Object doAbstractVectorCached(RAbstractVector vec,
                    @CachedLibrary("vec") AbstractContainerLibrary vecLib) {
        return vecLib.materialize(vec);
    }

    @Fallback
    protected Object doGeneric(Object o) {
        return o;
    }

    public static MaterializeNode create() {
        return MaterializeNodeGen.create();
    }

}
