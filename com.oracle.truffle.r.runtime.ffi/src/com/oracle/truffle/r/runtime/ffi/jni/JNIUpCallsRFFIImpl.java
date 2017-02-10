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
package com.oracle.truffle.r.runtime.ffi.jni;

import static com.oracle.truffle.r.nodes.ffi.RFFIUtils.guaranteeInstanceOf;

import com.oracle.truffle.r.nodes.ffi.JavaUpCallsRFFIImpl;
import com.oracle.truffle.r.runtime.RErrorHandling;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.ffi.CharSXPWrapper;

/**
 * Some additional methods to support the native JNI side.
 */
public final class JNIUpCallsRFFIImpl extends JavaUpCallsRFFIImpl {

    /**
     * Helper function for {@code R_TopLevelExec}, see {@link #R_ToplevelExec()}, called after C
     * function returns.
     */
    public static void R_ToplevelExecRestoreErrorHandlerStacks(Object stacks) {
        RErrorHandling.HandlerStacks handlerStacks = guaranteeInstanceOf(stacks, RErrorHandling.HandlerStacks.class);
        RErrorHandling.restoreHandlerStacks(handlerStacks);
    }

    /**
     * Called to possibly update the "complete" status on {@code x}. N.B. {@code x} may not be an
     * object with a concrete {@code setComplete} method, e.g. see {@link #INTEGER(Object)}.
     */
    public static void setComplete(Object x, boolean complete) {
        // only care about concrete vectors
        if (x instanceof RVector) {
            ((RVector<?>) x).setComplete(complete);
        }
    }

    /**
     * Called when a {@link CharSXPWrapper} is expected and not found.
     */
    public static void logNotCharSXPWrapper(Object x) {
        System.out.println("object " + x);
        System.out.println("class " + x.getClass());
    }

}
