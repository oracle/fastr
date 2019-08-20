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
package com.oracle.truffle.r.runtime.ffi.base;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.r.runtime.data.RTruffleObject;
import com.oracle.truffle.r.runtime.ffi.BaseRFFI.UtsName;

@ExportLibrary(InteropLibrary.class)
public final class UnameResult implements UtsName, RTruffleObject {
    private String sysname;
    private String release;
    private String version;
    private String machine;
    private String nodename;

    @SuppressWarnings("static-method")
    @ExportMessage
    boolean isExecutable() {
        return true;
    }

    @ExportMessage
    Object execute(Object[] arguments) {
        setResult((String) arguments[0], (String) arguments[1], (String) arguments[2], (String) arguments[3], (String) arguments[4]);
        return this;
    }

    public void setResult(String sysnameA, String releaseA, String versionA, String machineA, String nodenameA) {
        sysname = sysnameA;
        release = releaseA;
        version = versionA;
        machine = machineA;
        nodename = nodenameA;
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
}
