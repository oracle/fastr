/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.library.utils;

import static com.oracle.truffle.r.nodes.builtin.NodeWithArgumentCasts.Casts.noCasts;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RObjectSize;
import com.oracle.truffle.r.runtime.data.RTypedValue;

/**
 * Similarly to GNU R's version, this is approximate and based, for {@link RTypedValue} instances on
 * {@link RObjectSize#getObjectSize}. As per GNU R the AST size for a closure is included. TODO AST
 * size not included owing to problems sizing it automatically.
 */
public abstract class ObjectSize extends RExternalBuiltinNode.Arg1 {

    static {
        noCasts(ObjectSize.class);
    }

    private static class MyIgnoreObjectHandler implements RObjectSize.IgnoreObjectHandler {
        @Override
        public boolean ignore(Object rootObject, Object obj) {
            return obj == RNull.instance;
        }
    }

    private static final MyIgnoreObjectHandler ignoreObjectHandler = new MyIgnoreObjectHandler();

    @Specialization
    protected int objectSize(@SuppressWarnings("unused") int o) {
        return RObjectSize.INT_SIZE;
    }

    @Specialization
    protected int objectSize(@SuppressWarnings("unused") double o) {
        return RObjectSize.DOUBLE_SIZE;
    }

    @Specialization
    protected int objectSize(@SuppressWarnings("unused") byte o) {
        return RObjectSize.BYTE_SIZE;
    }

    @Fallback
    @TruffleBoundary
    protected int objectSize(Object o) {
        return (int) RObjectSize.getObjectSize(o, ignoreObjectHandler);
    }
}
