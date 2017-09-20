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
package com.oracle.truffle.r.ffi.impl.nfi;

import java.util.ArrayList;

import com.oracle.truffle.r.ffi.impl.common.LibPaths;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.RContext.ContextState;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.DLLRFFI;
import com.oracle.truffle.r.runtime.ffi.RFFIContext;
import com.oracle.truffle.r.runtime.ffi.UnsafeAdapter;

class NFIContext extends RFFIContext {
    /**
     * Memory allocated using Rf_alloc, which should be reclaimed at every down-call exit. Note:
     * this is less efficient than GNUR's version, we may need to implement it properly should the
     * performance be a problem.
     */
    public final ArrayList<Long> transientAllocations = new ArrayList<>();

    @Override
    public ContextState initialize(RContext context) {
        String librffiPath = LibPaths.getBuiltinLibPath("R");
        if (context.isInitial()) {
            DLL.loadLibR(librffiPath);
        } else {
            // force initialization of NFI
            DLLRFFI.DLOpenRootNode.create(context).call(librffiPath, false, false);
        }
        return this;
    }

    @Override
    public void afterDowncall() {
        for (Long ptr : transientAllocations) {
            UnsafeAdapter.UNSAFE.freeMemory(ptr);
        }
        transientAllocations.clear();
    }
}
