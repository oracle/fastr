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

import static com.oracle.truffle.r.nodes.builtin.RBuiltinKind.*;
import java.io.*;
import java.util.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ffi.*;

public class SysFunctions {

    @RBuiltin(name = "Sys.getpid", kind = INTERNAL)
    public abstract static class SysGetpid extends RBuiltinNode {

        @Specialization
        public Object sysGetPid() {
            controlVisibility();
            int pid = RFFIFactory.getRFFI().getBaseRFFI().getpid();
            return RDataFactory.createIntVectorFromScalar(pid);
        }
    }

    @RBuiltin(name = "Sys.getenv", kind = INTERNAL)
    public abstract static class SysGetenv extends RBuiltinNode {

        @Specialization
        public Object sysGetEnv(RAbstractStringVector x, String unset) {
            controlVisibility();
            Map<String, String> envMap = REnvVars.getMap();
            int len = x.getLength();
            String[] data = new String[len == 0 ? envMap.size() : len];
            if (len == 0) {
                // all
                int i = 0;
                for (Map.Entry<String, String> entry : envMap.entrySet()) {
                    data[i++] = entry.getKey() + '=' + entry.getValue();
                }
                return RDataFactory.createStringVector(data, true);
            } else {
                // just those in 'x' without the 'name=' which is handled in the R snippet
                boolean complete = RDataFactory.COMPLETE_VECTOR;
                for (int i = 0; i < len; i++) {
                    String name = x.getDataAt(i);
                    String value = envMap.get(name);
                    if (value != null) {
                        data[i] = value;
                    } else {
                        data[i] = unset;
                        if (unset == RRuntime.STRING_NA) {
                            complete = RDataFactory.INCOMPLETE_VECTOR;
                        }
                    }
                }
                return RDataFactory.createStringVector(data, complete);
            }
        }

        @Specialization(order = 100)
        public Object sysGetEnvGeneric(@SuppressWarnings("unused") Object x, @SuppressWarnings("unused") Object unset) {
            controlVisibility();
            throw RError.getWrongTypeOfArgument(getEncapsulatingSourceSection());
        }

    }

    @RBuiltin(name = "Sys.sleep", kind = INTERNAL)
    public abstract static class SysSleep extends RInvisibleBuiltinNode {

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

    /**
     * TODO: Handle ~ expansion which is not handled by POSIX.
     */
    @RBuiltin(name = "Sys.readlink", kind = INTERNAL)
    public abstract static class SysReadlink extends RBuiltinNode {

        @Specialization(order = 0)
        public Object sysReadLink(String path) {
            controlVisibility();
            return RDataFactory.createStringVector(doSysReadLink(path));
        }

        @Specialization(order = 1)
        public Object sysReadlink(RStringVector vector) {
            controlVisibility();
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

        private static String doSysReadLink(String path) {
            String s;
            try {
                s = RFFIFactory.getRFFI().getBaseRFFI().readlink(path);
                if (s == null) {
                    s = "";
                }
            } catch (IOException ex) {
                s = RRuntime.STRING_NA;
            }
            return s;
        }

        @Specialization(order = 100)
        public Object sysReadlinkGeneric(@SuppressWarnings("unused") Object path) {
            controlVisibility();
            throw RError.getGenericError(getEncapsulatingSourceSection(), "invalid 'paths' argument");
        }
    }

}
