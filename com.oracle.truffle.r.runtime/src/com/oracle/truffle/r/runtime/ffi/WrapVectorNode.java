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
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

public abstract class WrapVectorNode extends Node {

    @Child FFIWrapNode wrapNode = FFIWrapNode.create();

    public abstract VectorRFFIWrapper execute(Object vector);

    public final VectorRFFIWrapper dispatch(Object vector) {
        return execute(wrapNode.execute(vector));
    }

    protected boolean isTemporary(Object vector) {
        // if the vector is temporary, we can re-use it. We turn it into native memory backed
        // vector, keep it so and reuse it as the result.
        return (vector instanceof RVector<?> && ((RVector<?>) vector).isTemporary());
    }

    @Specialization(guards = "isTemporary(vector)")
    protected VectorRFFIWrapper temporaryToNative(TruffleObject vector) {
        return VectorRFFIWrapper.get(vector);
    }

    @Specialization(guards = "!isTemporary(vector)")
    protected VectorRFFIWrapper nonTemporaryToNative(RAbstractVector vector) {
        return VectorRFFIWrapper.get(vector.copy());
    }

    @Fallback
    protected VectorRFFIWrapper fallback(@SuppressWarnings("unused") Object vector) {
        throw RInternalError.shouldNotReachHere("Unimplemented native conversion of argument");
    }

    public abstract static class WrapVectorsNode extends Node {

        public abstract VectorRFFIWrapper[] execute(Object[] vectors);

        protected WrapVectorNode[] createWrapNodes(int length) {
            WrapVectorNode[] nodes = new WrapVectorNode[length];
            for (int i = 0; i < nodes.length; i++) {
                nodes[i] = WrapVectorNodeGen.create();
            }
            return nodes;
        }

        @Specialization(limit = "99", guards = "vectors.length == cachedLength")
        @ExplodeLoop
        protected VectorRFFIWrapper[] wrapArray(Object[] vectors,
                        @SuppressWarnings("unused") @Cached("vectors.length") int cachedLength,
                        @Cached("createWrapNodes(vectors.length)") WrapVectorNode[] wrapNodes) {
            VectorRFFIWrapper[] results = new VectorRFFIWrapper[wrapNodes.length];
            for (int i = 0; i < wrapNodes.length; i++) {
                results[i] = wrapNodes[i].dispatch(vectors[i]);
            }
            return results;
        }

    }

}
