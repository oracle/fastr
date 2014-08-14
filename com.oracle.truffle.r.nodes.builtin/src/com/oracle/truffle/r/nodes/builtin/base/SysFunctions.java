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

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import java.io.*;
import java.util.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
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

    @RBuiltin(name = "Sys.getenv", kind = INTERNAL, parameterNames = {"x", "unset", "names"})
    public abstract static class SysGetenv extends RBuiltinNode {

        @Override
        public RNode[] getParameterValues() {
            return new RNode[]{ConstantNode.create(RNull.instance), ConstantNode.create(""), ConstantNode.create(RRuntime.LOGICAL_NA)};
        }

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
                        if (RRuntime.isNA(unset)) {
                            complete = RDataFactory.INCOMPLETE_VECTOR;
                        }
                    }
                }
                return RDataFactory.createStringVector(data, complete);
            }
        }

        @Specialization(order = 100)
        public Object sysGetEnvGeneric(VirtualFrame frame, @SuppressWarnings("unused") Object x, @SuppressWarnings("unused") Object unset) {
            controlVisibility();
            throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.WRONG_TYPE);
        }

    }

    @RBuiltin(name = "Sys.setenv", kind = SUBSTITUTE, parameterNames = {"..."})
    // TODO INTERNAL when argument names available in list(...)
    public abstract static class SysSetEnv extends RInvisibleBuiltinNode {

        @Override
        public RNode[] getParameterValues() {
            return new RNode[]{ConstantNode.create(RMissing.instance)};
        }

        @Specialization
        public RLogicalVector doSysSetEnv(VirtualFrame frame, RAbstractStringVector argVec) {
            return doSysSetEnv(frame, new Object[]{argVec.getDataAt(0)});
        }

        @Specialization
        public RLogicalVector doSysSetEnv(VirtualFrame frame, Object[] args) {
            controlVisibility();
            String[] argNames = getSuppliedArgsNames();
            validateArgNames(frame, argNames);
            byte[] data = new byte[args.length];
            for (int i = 0; i < args.length; i++) {
                REnvVars.put(argNames[i], (String) args[i]);
                data[i] = RRuntime.LOGICAL_TRUE;
            }
            return RDataFactory.createLogicalVector(data, RDataFactory.COMPLETE_VECTOR);
        }

        private void validateArgNames(VirtualFrame frame, String[] argNames) throws RError {
            boolean ok = argNames != null;
            if (argNames != null) {
                for (int i = 0; i < argNames.length; i++) {
                    if (argNames[i] == null) {
                        ok = false;
                    }
                }
            }
            if (!ok) {
                throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.ARGS_MUST_BE_NAMED);
            }
        }
    }

    @RBuiltin(name = "Sys.unsetenv", kind = INTERNAL, parameterNames = {"x"})
    public abstract static class SysUnSetEnv extends RInvisibleBuiltinNode {

        @Specialization
        public RLogicalVector doSysSetEnv(RAbstractStringVector argVec) {
            byte[] data = new byte[argVec.getLength()];
            for (int i = 0; i < data.length; i++) {
                data[i] = RRuntime.asLogical(REnvVars.unset(argVec.getDataAt(i)));
            }
            return RDataFactory.createLogicalVector(data, RDataFactory.COMPLETE_VECTOR);
        }
    }

    @RBuiltin(name = "Sys.sleep", kind = INTERNAL, parameterNames = {"time"})
    public abstract static class SysSleep extends RInvisibleBuiltinNode {

        @Specialization(order = 0)
        public Object sysSleep(double seconds) {
            controlVisibility();
            sleep(convertToMillis(seconds));
            return RNull.instance;
        }

        @Specialization(order = 1)
        public Object sysSleep(VirtualFrame frame, String secondsString) {
            controlVisibility();
            long millis = convertToMillis(checkValidString(frame, secondsString));
            sleep(millis);
            return RNull.instance;
        }

        @Specialization(order = 2, guards = "lengthOne")
        public Object sysSleep(VirtualFrame frame, RStringVector secondsVector) {
            controlVisibility();
            long millis = convertToMillis(checkValidString(frame, secondsVector.getDataAt(0)));
            sleep(millis);
            return RNull.instance;
        }

        public static boolean lengthOne(RStringVector vec) {
            return vec.getLength() == 1;
        }

        @Specialization(order = 100)
        public Object sysSleep(VirtualFrame frame, @SuppressWarnings("unused") Object arg) throws RError {
            controlVisibility();
            throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.INVALID_VALUE, "time");
        }

        private static long convertToMillis(double d) {
            return (long) (d * 1000);
        }

        private double checkValidString(VirtualFrame frame, String s) {
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException ex) {
                throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.INVALID_VALUE, "time");
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
    @RBuiltin(name = "Sys.readlink", kind = INTERNAL, parameterNames = {"paths"})
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
                if (RRuntime.isNA(path)) {
                    paths[i] = path;
                } else {
                    paths[i] = doSysReadLink(path);
                }
                if (RRuntime.isNA(paths[i])) {
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
        public Object sysReadlinkGeneric(VirtualFrame frame, @SuppressWarnings("unused") Object path) {
            controlVisibility();
            throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.INVALID_ARGUMENT, "paths");
        }
    }

}
