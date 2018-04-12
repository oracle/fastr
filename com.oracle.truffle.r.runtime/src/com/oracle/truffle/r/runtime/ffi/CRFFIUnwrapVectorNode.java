/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.ffi;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.data.StringArrayWrapper;

public abstract class CRFFIUnwrapVectorNode extends Node {

    public abstract Object execute(Object arg);

    @Specialization
    protected Object unwrapRStringVector(StringArrayWrapper wrapper) {
        return wrapper.copyBackFromNative();
    }

    @Specialization
    protected Object unwrapOthers(VectorRFFIWrapper wrapper) {
        return wrapper.getVector();
    }

    public abstract static class CRFFIUnwrapVectorsNode extends Node {

        public abstract Object[] execute(Object[] wrappers);

        protected CRFFIUnwrapVectorNode[] createUnwrapNodes(int length) {
            CRFFIUnwrapVectorNode[] nodes = new CRFFIUnwrapVectorNode[length];
            for (int i = 0; i < nodes.length; i++) {
                nodes[i] = CRFFIUnwrapVectorNodeGen.create();
            }
            return nodes;
        }

        @Specialization(limit = "99", guards = "wrappers.length == cachedLength")
        @ExplodeLoop
        protected Object[] wrapArray(Object[] wrappers,
                        @SuppressWarnings("unused") @Cached("wrappers.length") int cachedLength,
                        @Cached("createUnwrapNodes(wrappers.length)") CRFFIUnwrapVectorNode[] unwrapNodes) {
            Object[] results = new Object[unwrapNodes.length];
            for (int i = 0; i < unwrapNodes.length; i++) {
                results[i] = unwrapNodes[i].execute(wrappers[i]);
            }
            return results;
        }

    }
}
