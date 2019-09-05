/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.r.runtime.data.RBaseObject;

/**
 * A final step before an object is sent to native code or LLVM interpreter.
 * 
 * The resulting object must be able to live on the native side, i.e., {@link RBaseObject} that can
 * have its native mirror.
 *
 * The life-cycle of the resulting Java object should be tied to the argument, which is performance
 * (caching the result) and defensive measure. It is a defensive measure since any RFFI function
 * whose result's life-cycle should be tied with some other R object (e.g. it is retrieved from an
 * environment) should make sure it is tied by replacing the original compact representation with
 * the materialized one inside the referer.
 *
 * We cannot tie the life-cycle of wrappers of primitive values, they are at least protected with
 * {@link RFFIContext#registerReferenceUsedInNative(Object)} elsewhere.
 *
 * See documentation/dev/ffi.md for more details.
 */
public final class FFIWrap {
    /**
     * Hold the materialized values for as long, as the FFIWrap instance exists.
     */
    public final Object[] materialized;

    public FFIWrap() {
        materialized = new Object[1];
    }

    public FFIWrap(int length) {
        materialized = new Object[length];
    }

    public Object wrapUncached(Object arg) {
        assert materialized.length == 1;
        materialized[0] = FFIMaterializeNode.getUncached().materialize(arg);
        return FFIToNativeMirrorNode.getUncached().execute(materialized[0]);
    }

    public Object wrap(Object arg, FFIMaterializeNode ffiMateralizeNodes, FFIToNativeMirrorNode ffiToNativeMirrorNode) {
        assert materialized.length == 1;
        materialized[0] = ffiMateralizeNodes.materialize(arg);
        return ffiToNativeMirrorNode.execute(materialized[0]);
    }

    @ExplodeLoop
    public void wrap(Object[] args, FFIMaterializeNode[] ffiMateralizeNodes, FFIToNativeMirrorNode[] ffiToNativeMirrorNodes) {
        assert ffiMateralizeNodes.length == ffiToNativeMirrorNodes.length;
        assert ffiMateralizeNodes.length == materialized.length;
        CompilerAsserts.compilationConstant(ffiMateralizeNodes.length);
        for (int i = 0; i < ffiMateralizeNodes.length; i++) {
            materialized[i] = ffiMateralizeNodes[i].materialize(args[i]);
            args[i] = ffiToNativeMirrorNodes[i].execute(materialized[i]);
        }
    }
}
