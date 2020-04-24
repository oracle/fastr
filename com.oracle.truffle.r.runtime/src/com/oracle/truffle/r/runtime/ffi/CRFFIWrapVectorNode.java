/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RBaseObject;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.ffi.interop.StringArrayWrapper;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

public abstract class CRFFIWrapVectorNode extends Node {

    @Child protected FFIMaterializeNode materializeNode = FFIMaterializeNode.create();

    protected abstract Object execute(Object vector);

    public final Object dispatch(Object vector) {
        return execute(materializeNode.materialize(vector));
    }

    protected boolean isTemporary(Object vector) {
        // if the vector is temporary, we can re-use it. We turn it into native memory backed
        // vector, keep it so and reuse it as the result.
        return RRuntime.isMaterializedVector(vector) && ((RAbstractVector) vector).isTemporary();
    }

    protected static boolean isStringVector(Object vector) {
        return vector instanceof RStringVector;
    }

    @Specialization
    protected Object temporaryToNative(RStringVector vector) {
        return new StringArrayWrapper(vector);
    }

    @Specialization(guards = {"isTemporary(vector)", "!isStringVector(vector)"})
    protected Object temporaryToNative(RAbstractVector vector) {
        return RObjectDataPtr.get(vector);
    }

    @Specialization(guards = {"!isTemporary(vector)", "!isStringVector(vector)"})
    protected Object nonTemporaryToNative(RAbstractVector vector) {
        return RObjectDataPtr.get(vector.copy());
    }

    @Specialization
    protected Object toNativeMirror(RBaseObject obj,
                    @Cached() FFIToNativeMirrorNode ffiToNativeMirrorNode) {
        return ffiToNativeMirrorNode.execute(obj);
    }

    @Fallback
    protected Object fallback(Object obj) {
        return fallbackError(obj);
    }

    @TruffleBoundary
    private static Object fallbackError(Object vector) {
        throw RInternalError.shouldNotReachHere("Unimplemented native conversion of argument " + vector +
                        " of class " + (vector != null ? vector.getClass() : "<null>"));
    }

    public abstract static class CRFFIWrapVectorsNode extends Node {

        public abstract Object[] execute(Object[] vectors);

        protected CRFFIWrapVectorNode[] createWrapNodes(int length) {
            CRFFIWrapVectorNode[] nodes = new CRFFIWrapVectorNode[length];
            for (int i = 0; i < nodes.length; i++) {
                nodes[i] = CRFFIWrapVectorNodeGen.create();
            }
            return nodes;
        }

        @Specialization(limit = "99", guards = "vectors.length == cachedLength")
        @ExplodeLoop
        protected Object[] wrapArray(Object[] vectors,
                        @SuppressWarnings("unused") @Cached("vectors.length") int cachedLength,
                        @Cached("createWrapNodes(vectors.length)") CRFFIWrapVectorNode[] wrapNodes) {
            Object[] results = new Object[wrapNodes.length];
            for (int i = 0; i < wrapNodes.length; i++) {
                results[i] = wrapNodes[i].dispatch(vectors[i]);
            }
            return results;
        }

    }

}
