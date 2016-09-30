/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.r.runtime.ffi.BaseRFFI.UtsName;

public class JNI_UtsName implements UtsName {
    String sysname;
    String release;
    String version;
    String machine;
    String nodename;

    private static JNI_UtsName singleton;

    public static UtsName get() {
        if (singleton == null) {
            singleton = new JNI_UtsName();
        }
        singleton.getutsname();
        return singleton;
    }

    @Override
    public String sysname() {
        return sysname;
    }

    @Override
    public String release() {
        return release;
    }

    @Override
    public String version() {
        return version;
    }

    @Override
    public String machine() {
        return machine;
    }

    @Override
    public String nodename() {
        return nodename;
    }

    private native void getutsname();

}
