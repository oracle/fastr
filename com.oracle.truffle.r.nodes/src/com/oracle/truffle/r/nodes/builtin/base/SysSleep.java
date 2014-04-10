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

@RBuiltin(".Internal.Sys.sleep")
public abstract class SysSleep extends RInvisibleBuiltinNode {

    @Specialization(order = 0)
    public Object sysSleep(double seconds) {
        controlVisibility();
        sleep(convertToMillis(seconds));
        return RNull.instance;
    }

    @Specialization(order = 1)
    public Object sysSleep(String secondsString) {
        controlVisibility();
        long millis = convertToMillis(checkValidString(secondsString));
        sleep(millis);
        return RNull.instance;
    }

    @Specialization(order = 2, guards = "lengthOne")
    public Object sysSleep(RStringVector secondsVector) {
        controlVisibility();
        long millis = convertToMillis(checkValidString(secondsVector.getDataAt(0)));
        sleep(millis);
        return RNull.instance;
    }

    public static boolean lengthOne(RStringVector vec) {
        return vec.getLength() == 1;
    }

    @Specialization(order = 100)
    public Object sysSleep(@SuppressWarnings("unused") Object arg) throws RError {
        controlVisibility();
        throw invalid();
    }

    private RError invalid() throws RError {
        throw RError.getGenericError(getEncapsulatingSourceSection(), "invalid 'time' value");
    }

    private static long convertToMillis(double d) {
        return (long) (d * 1000);
    }

    private double checkValidString(String s) throws RError {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException ex) {
            throw invalid();
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            // ignore
        }
    }
}
