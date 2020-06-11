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
package com.oracle.truffle.r.runtime.ffi;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.r.runtime.RInternalError;

/**
 *
 * In general, two steps are necessary before an object is sent to native code or LLVM interpreter.
 * <ol>
 * <li>Ensure the object is <b>materialized</b>. Done via {@link FFIMaterializeNode}</li>
 * <li>Convert to an object, that is able to <b>live on the native side</b>. Done via
 * {@link FFIToNativeMirrorNode}</li>
 * </ol>
 *
 * <p>
 * As a result, two additional objects are created, and so we have now three objects - the original
 * argument, the materialized (via FFIMaterializeNode) version of the original argument and the
 * NativeMirror.
 * </p>
 * <br>
 *
 * The life-cycle of both resulting Java objects should be tied to the original argument, which is a
 * performance (caching the result) and a defensive measure.
 *
 * <ol>
 * <li><b>Performance measure</b> - the NativeMirror life cycle is trivially tied to the
 * materialized version of the arg since it's going to be stored in its nativeMirror field, but what
 * is important is that the materialized arg life-cycle is tied to the original argument, that is,
 * e.g., why sequences cache and reuse the materialized vector.</li>
 *
 * <li><b>Defensive measure</b> - any RFFI function whose result's life-cycle should be tied with
 * some other R object (e.g. it is retrieved from an environment or list) should make sure it is
 * tied by replacing the original compact representation with the materialized one inside the
 * referer. For return values of upcalls, the upcall itself should already have called
 * FFIMaterializeNode and, for example, saved the materialized object to the list from which it had
 * retrieved it original non-materialized object, but for arguments of downcalls this doesn't apply
 * and calling FFIMaterializeNode is not a defensive measure, but necessity and it has to be ensured
 * that the materialized value stays alive until the downcall returns.</li>
 * </ol>
 *
 * <p>
 * We cannot tie the life-cycle of wrappers of primitive values, they are at least protected with
 * {@link RFFIContext#registerReferenceUsedInNative(Object)} elsewhere.
 * </p>
 * <br>
 * See documentation/dev/ffi.md for more details.
 */
public class FFIWrap {

    public static final class FFIDownCallWrap implements AutoCloseable {
        /**
         * Hold the materialized values for as long, as the FFIWrap instance exists.
         */
        private final Object[] materialized;

        public FFIDownCallWrap() {
            materialized = new Object[1];
        }

        public FFIDownCallWrap(int length) {
            materialized = new Object[length];
        }

        public Object wrapUncached(Object arg) {
            assert materialized.length == 1;
            materialized[0] = FFIMaterializeNode.getUncached().materialize(arg);
            return FFIToNativeMirrorNode.getUncached().execute(materialized[0]);
        }

        public Object[] wrapAll(Object[] args, FFIMaterializeNode[] ffiMateralizeNodes, FFIToNativeMirrorNode[] ffiToNativeMirrorNodes) {
            return wrapSome(args, ffiMateralizeNodes, ffiToNativeMirrorNodes, null);
        }

        @ExplodeLoop
        public Object[] wrapSome(Object[] args, FFIMaterializeNode[] ffiMaterializeNodes, FFIToNativeMirrorNode[] ffiToNativeMirrorNodes, boolean[] whichArgToWrap) {
            assert ffiMaterializeNodes.length == ffiToNativeMirrorNodes.length;
            assert ffiMaterializeNodes.length == materialized.length;
            CompilerAsserts.compilationConstant(ffiMaterializeNodes.length);
            Object[] wrappedArgs = new Object[args.length];
            for (int i = 0; i < ffiMaterializeNodes.length; i++) {
                if (whichArgToWrap == null || whichArgToWrap[i]) {
                    materialized[i] = ffiMaterializeNodes[i].materialize(args[i]);
                    wrappedArgs[i] = ffiToNativeMirrorNodes[i].execute(materialized[i]);
                } else {
                    materialized[i] = null;
                    wrappedArgs[i] = args[i];
                }
            }
            return wrappedArgs;
        }

        @Override
        public void close() {
            if (materialized == null) {
                throw RInternalError.shouldNotReachHere();
            }
        }
    }

    public static final class FFIUpCallWrap {
        public static class FFIWrapResult {
            public Object materialized;
            public Object nativeMirror;
        }

        public static FFIWrapResult wrap(Object arg, FFIMaterializeNode ffiMateralizeNodes, FFIToNativeMirrorNode ffiToNativeMirrorNode) {
            FFIWrapResult result = new FFIWrapResult();
            result.materialized = ffiMateralizeNodes.materialize(arg);
            result.nativeMirror = ffiToNativeMirrorNode.execute(result.materialized);
            return result;
        }

    }
}
