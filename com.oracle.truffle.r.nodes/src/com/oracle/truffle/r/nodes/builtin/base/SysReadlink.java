/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base;

import java.io.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.ffi.*;

/**
 * TODO: Handle ~ expansion which is not handled by POSIX.
 */
@RBuiltin("Sys.readlink")
public abstract class SysReadlink extends RBuiltinNode {

    @Specialization(order = 1)
    public Object sysReadlink(RStringVector vector) {
        String[] paths = new String[vector.getLength()];
        boolean complete = RDataFactory.COMPLETE_VECTOR;
        for (int i = 0; i < paths.length; i++) {
            String path = vector.getDataAt(i);
            if (path == RRuntime.STRING_NA) {
                paths[i] = path;
            } else {
                paths[i] = doSysReadLink(path);
            }
            if (paths[i] == RRuntime.STRING_NA) {
                complete = RDataFactory.INCOMPLETE_VECTOR;
            }
        }
        return RDataFactory.createStringVector(paths, complete);
    }

    @Specialization(order = 0)
    public Object sysReadLink(String path) {
        return RDataFactory.createStringVector(doSysReadLink(path));
    }

    private static String doSysReadLink(String path) {
        String s;
        try {
            s = BaseRFFIFactory.getRFFI().readlink(path);
            if (s == null) {
                s = "";
            }
        } catch (IOException ex) {
            s = RRuntime.STRING_NA;
        }
        return s;
    }

    @Generic
    public Object sysReadlinkGeneric(@SuppressWarnings("unused") Object path) {
        throw RError.getGenericError(getEncapsulatingSourceSection(), "invalid 'paths' argument");
    }
}
