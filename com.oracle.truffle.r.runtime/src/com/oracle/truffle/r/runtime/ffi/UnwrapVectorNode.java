/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.ffi;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

public abstract class UnwrapVectorNode extends Node {

    public abstract Object execute(VectorRFFIWrapper arg);

    protected static boolean isRStringVector(VectorRFFIWrapper wrapper) {
        return wrapper.getVector() instanceof RStringVector;
    }

    @Specialization(guards = "isRStringVector(wrapper)")
    protected Object unwrapRStringVector(VectorRFFIWrapper wrapper) {
        return ((RStringVector) wrapper.getVector()).copyBackFromNative();
    }

    @Specialization(guards = "!isRStringVector(wrapper)")
    protected Object unwrapOthers(VectorRFFIWrapper wrapper) {
        return wrapper.getVector();
    }

    public abstract static class UnwrapVectorsNode extends Node {

        public abstract Object[] execute(VectorRFFIWrapper[] wrappers);

        protected UnwrapVectorNode[] createUnwrapNodes(int length) {
            UnwrapVectorNode[] nodes = new UnwrapVectorNode[length];
            for (int i = 0; i < nodes.length; i++) {
                nodes[i] = UnwrapVectorNodeGen.create();
            }
            return nodes;
        }

        @Specialization(limit = "99", guards = "wrappers.length == cachedLength")
        @ExplodeLoop
        protected Object[] wrapArray(VectorRFFIWrapper[] wrappers,
                        @SuppressWarnings("unused") @Cached("wrappers.length") int cachedLength,
                        @Cached("createUnwrapNodes(wrappers.length)") UnwrapVectorNode[] unwrapNodes) {
            Object[] results = new Object[unwrapNodes.length];
            for (int i = 0; i < unwrapNodes.length; i++) {
                results[i] = unwrapNodes[i].execute(wrappers[i]);
            }
            return results;
        }

    }
}
