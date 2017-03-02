/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.ffi.impl.upcalls;

import com.oracle.truffle.api.frame.Frame;

/**
 * Additional upcalls created for supporting FastR in RStudio. These mainly relate to the GNU R
 * notion of a "context", which corresponds somewhat to a Truffle {@link Frame}.
 */
public interface IDEUpCallsRFFI {
    // Checkstyle: stop method name check
    Object R_GlobalContext();

    Object R_getGlobalFunctionContext();

    Object R_getParentFunctionContext(Object c);

    Object R_getContextEnv(Object c);

    Object R_getContextFun(Object c);

    Object R_getContextCall(Object c);

    Object R_getContextSrcRef(Object c);

    int R_insideBrowser();

    int R_isGlobal(Object c);

    int R_isEqual(Object x, Object y);

}
