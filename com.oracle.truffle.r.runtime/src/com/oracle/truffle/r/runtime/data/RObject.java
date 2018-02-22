/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data;

public abstract class RObject {

    private Object nativeMirror;
    /**
     * It maintains the <code>1-?</code> relationship between this object and its native wrapper
     * through which the native code accesses it. For instance, Sulong implements the "pointer"
     * equality of two objects that are not pointers (i.e. <code>IS_POINTER</code> returns
     * <code>false</code>) as the reference equality of the objects. It follows that the pointer
     * comparison would fail if the same <code>RObject</code> instance were wrapped by two different
     * native wrappers.
     */
    private Object nativeWrapper;

    public final void setNativeMirror(Object mirror) {
        this.nativeMirror = mirror;
    }

    public final Object getNativeMirror() {
        return nativeMirror;
    }

    public void setNativeWrapper(Object wrapper) {
        this.nativeWrapper = wrapper;
    }

    public Object getNativeWrapper() {
        return this.nativeWrapper;
    }
}
