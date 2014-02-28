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

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.ffi.*;

@RBuiltin("Sys.sleep")
public abstract class SysSleep extends RBuiltinNode {

    @Specialization(order = 0)
    public Object sysSleep(double seconds) {
        sleep((int) seconds);
        return RInvisible.INVISIBLE_NULL;
    }

    @Specialization(order = 1)
    public Object sysSleep(String secondsString) {
        int seconds = checkIntString(secondsString);
        sleep(seconds);
        return RInvisible.INVISIBLE_NULL;
    }

    @Specialization(order = 2)
    public Object sysSleep(RStringVector secondsVector) {
        if (secondsVector.getLength() != 1) {
            throw invalid();
        }
        int seconds = checkIntString(secondsVector.getDataAt(0));
        sleep(seconds);
        return RInvisible.INVISIBLE_NULL;
    }

    @Generic
    public Object sysSleep(@SuppressWarnings("unused") Object arg) throws RError {
        throw invalid();
    }

    private RError invalid() throws RError {
        throw RError.getGenericError(getSourceSection(), "invalid 'time' value");
    }

    private int checkIntString(String s) throws RError {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException ex) {
            throw invalid();
        }
    }

    private static void sleep(int seconds) {
        BaseRFFIFactory.getRFFI().sleep(seconds);
    }
}
