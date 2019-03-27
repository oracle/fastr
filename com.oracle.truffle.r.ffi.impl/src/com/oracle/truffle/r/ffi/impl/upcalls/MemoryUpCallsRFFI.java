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
package com.oracle.truffle.r.ffi.impl.upcalls;

import com.oracle.truffle.r.ffi.processor.RFFICpointer;

public interface MemoryUpCallsRFFI {
    // Checkstyle: stop method name check

    Object R_MakeWeakRef(Object key, Object val, Object fin, long onexit);

    Object R_MakeWeakRefC(Object key, Object val, long fin, long onexit);

    Object R_WeakRefKey(Object w);

    Object R_WeakRefValue(Object w);

    void R_PreserveObject(Object obj);

    void R_ReleaseObject(Object obj);

    Object Rf_protect(Object x);

    void Rf_unprotect(int x);

    int R_ProtectWithIndex(Object x);

    void R_Reprotect(Object x, int y);

    void Rf_unprotect_ptr(Object x);

    @RFFICpointer
    Object R_alloc(int n, int size);
}
