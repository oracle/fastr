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
package com.oracle.truffle.r.ffi.impl.interop.base;

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.r.runtime.data.RTruffleObject;
import com.oracle.truffle.r.runtime.ffi.BaseRFFI;

public final class UnameResult implements BaseRFFI.UtsName, RTruffleObject {
    private String sysname;
    private String release;
    private String version;
    private String machine;
    private String nodename;

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

    @Override
    public ForeignAccess getForeignAccess() {
        return UnameResultMRForeign.ACCESS;
    }
}
