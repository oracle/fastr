/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.utilities.ConditionProfile;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ffi.*;

public class SysFunctions {

    @RBuiltin(name = "Sys.getpid", kind = INTERNAL, parameterNames = {})
    public abstract static class SysGetpid extends RBuiltinNode {

        @Specialization
        @TruffleBoundary
        protected Object sysGetPid() {
            controlVisibility();
            int pid = RFFIFactory.getRFFI().getBaseRFFI().getpid();
            return RDataFactory.createIntVectorFromScalar(pid);
        }
    }

    @RBuiltin(name = "Sys.getenv", kind = INTERNAL, parameterNames = {"x", "unset", "names"})
    public abstract static class SysGetenv extends RBuiltinNode {
        private final ConditionProfile zeroLengthProfile = ConditionProfile.createBinaryProfile();

        @Override
        public RNode[] getParameterValues() {
            return new RNode[]{ConstantNode.create(RNull.instance), ConstantNode.create(""), ConstantNode.create(RRuntime.LOGICAL_NA)};
        }

        @Specialization
        @TruffleBoundary
        protected Object sysGetEnv(RAbstractStringVector x, String unset) {
            controlVisibility();
            Map<String, String> envMap = REnvVars.getMap();
            int len = x.getLength();
            if (zeroLengthProfile.profile(len == 0)) {
                String[] data = new String[envMap.size()];
                // all
                int i = 0;
                for (Map.Entry<String, String> entry : envMap.entrySet()) {
                    data[i++] = entry.getKey() + '=' + entry.getValue();
                }
                return RDataFactory.createStringVector(data, true);
            } else {
                String[] data = new String[len];
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

        @Specialization
        protected Object sysGetEnvGeneric(@SuppressWarnings("unused") Object x, @SuppressWarnings("unused") Object unset) {
            controlVisibility();
            CompilerDirectives.transferToInterpreter();
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.WRONG_TYPE);
        }

    }

    @RBuiltin(name = "Sys.setenv", kind = INTERNAL, parameterNames = {"nm", "values"})
    public abstract static class SysSetEnv extends RInvisibleBuiltinNode {

        @Specialization()
        @TruffleBoundary
        protected RLogicalVector doSysSetEnv(RStringVector names, RStringVector values) {
            byte[] data = new byte[names.getLength()];
            for (int i = 0; i < data.length; i++) {
                REnvVars.put(names.getDataAt(i), values.getDataAt(i));
                data[i] = RRuntime.LOGICAL_TRUE;
            }
            return RDataFactory.createLogicalVector(data, RDataFactory.COMPLETE_VECTOR);
        }

    }

    @RBuiltin(name = "Sys.unsetenv", kind = INTERNAL, parameterNames = {"x"})
    public abstract static class SysUnSetEnv extends RInvisibleBuiltinNode {

        @Specialization
        @TruffleBoundary
        protected RLogicalVector doSysSetEnv(RAbstractStringVector argVec) {
            byte[] data = new byte[argVec.getLength()];
            for (int i = 0; i < data.length; i++) {
                data[i] = RRuntime.asLogical(REnvVars.unset(argVec.getDataAt(i)));
            }
            return RDataFactory.createLogicalVector(data, RDataFactory.COMPLETE_VECTOR);
        }
    }

    @RBuiltin(name = "Sys.sleep", kind = INTERNAL, parameterNames = {"time"})
    public abstract static class SysSleep extends RInvisibleBuiltinNode {

        @Specialization
        @TruffleBoundary
        protected Object sysSleep(double seconds) {
            controlVisibility();
            sleep(convertToMillis(seconds));
            return RNull.instance;
        }

        @Specialization
        @TruffleBoundary
        protected Object sysSleep(String secondsString) {
            controlVisibility();
            long millis = convertToMillis(checkValidString(secondsString));
            sleep(millis);
            return RNull.instance;
        }

        @Specialization(guards = "lengthOne")
        @TruffleBoundary
        protected Object sysSleep(RStringVector secondsVector) {
            controlVisibility();
            long millis = convertToMillis(checkValidString(secondsVector.getDataAt(0)));
            sleep(millis);
            return RNull.instance;
        }

        public static boolean lengthOne(RStringVector vec) {
            return vec.getLength() == 1;
        }

        @Specialization
        @TruffleBoundary
        protected Object sysSleep(@SuppressWarnings("unused") Object arg) {
            controlVisibility();
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_VALUE, "time");
        }

        private static long convertToMillis(double d) {
            return (long) (d * 1000);
        }

        private double checkValidString(String s) {
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException ex) {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_VALUE, "time");
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

        @Specialization
        @TruffleBoundary
        protected Object sysReadlink(String path) {
            controlVisibility();
            return RDataFactory.createStringVector(doSysReadLink(path));
        }

        @Specialization
        @TruffleBoundary
        protected Object sysReadlink(RStringVector vector) {
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

        @TruffleBoundary
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

        @Specialization
        protected Object sysReadlinkGeneric(@SuppressWarnings("unused") Object path) {
            controlVisibility();
            CompilerDirectives.transferToInterpreter();
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_ARGUMENT, "paths");
        }
    }

    // TODO implement
    @RBuiltin(name = "Sys.chmod", kind = INTERNAL, parameterNames = {"paths", "octmode", "use_umask"})
    public abstract static class SysChmod extends RBuiltinNode {
        @SuppressWarnings("unused")
        @Specialization
        @TruffleBoundary
        protected Object sysChmod(RAbstractStringVector pathVec, Object octmode, byte useUmask) {
            controlVisibility();
            throw RError.nyi(getEncapsulatingSourceSection(), " Sys.chmod");
        }

    }

    // TODO implement
    @RBuiltin(name = "Sys.umask", kind = INTERNAL, parameterNames = {"octmode"})
    public abstract static class SysUmask extends RBuiltinNode {
        @SuppressWarnings("unused")
        @Specialization
        @TruffleBoundary
        protected Object sysChmod(Object octmode) {
            controlVisibility();
            throw RError.nyi(getEncapsulatingSourceSection(), " Sys.umask");
        }

    }

    @RBuiltin(name = "Sys.time", kind = INTERNAL, parameterNames = {})
    public abstract static class SysTime extends RBuiltinNode {
        @Specialization
        @TruffleBoundary
        protected double sysChmod() {
            controlVisibility();
            return ((double) System.currentTimeMillis()) / 1000000;
        }

    }

    @RBuiltin(name = "Sys.glob", kind = INTERNAL, parameterNames = {"paths", "dirmask"})
    public abstract static class SysGlob extends RBuiltinNode {
        private static final char[] GLOBCHARS = new char[]{'*', '?', '['};

        @Specialization
        @TruffleBoundary
        protected Object sysGlob(RAbstractStringVector pathVec, @SuppressWarnings("unused") byte dirMask) {
            controlVisibility();
            ArrayList<String> matches = new ArrayList<>();
            FileSystem fileSystem = FileSystems.getDefault();
            for (int i = 0; i < pathVec.getLength(); i++) {
                String pathPattern = pathVec.getDataAt(i);
                if (pathPattern.length() == 0) {
                    continue;
                }
                @SuppressWarnings("unused")
                int firstGlobChar;
                if ((firstGlobChar = containsGlobChar(pathPattern)) >= 0) {
                    Path path = fileSystem.getPath(pathPattern);
                    ArrayList<Path> components = components(path);
                    if (components.size() > 1) {
                        // No easy way to do this in a single shot
                        throw RError.nyi(getEncapsulatingSourceSection(), " glob on dir");
                    } else {
                        try (Stream<Path> stream = Files.find(fileSystem.getPath(""), 1, new FileMatcher(pathPattern))) {
                            Iterator<Path> iter = stream.iterator();
                            while (iter.hasNext()) {
                                String s = iter.next().getFileName().toString();
                                if (s.length() > 0) {
                                    matches.add(s);
                                }
                            }
                        } catch (IOException ex) {
                            // ignored
                        }
                    }
                } else {
                    matches.add(pathPattern);
                }
            }
            String[] data = new String[matches.size()];
            matches.toArray(data);
            return RDataFactory.createStringVector(data, RDataFactory.COMPLETE_VECTOR);
        }

        private static int containsGlobChar(String pathPattern) {
            for (int i = 0; i < pathPattern.length(); i++) {
                char ch = pathPattern.charAt(i);
                for (int j = 0; j < GLOBCHARS.length; j++) {
                    if (ch == GLOBCHARS[j]) {
                        return i;
                    }
                }
            }
            return -1;
        }

        private static ArrayList<Path> components(Path path) {
            ArrayList<Path> list = new ArrayList<>();
            Iterator<Path> iter = path.iterator();
            while (iter.hasNext()) {
                list.add(iter.next());
            }
            return list;
        }

        private static class FileMatcher implements BiPredicate<Path, BasicFileAttributes> {
            PathMatcher pathMatcher;

            FileMatcher(String pathPattern) {
                pathMatcher = FileSystems.getDefault().getPathMatcher("glob:" + pathPattern);
            }

            public boolean test(Path path, BasicFileAttributes u) {
                boolean result = pathMatcher.matches(path);
                return result;
            }
        }
    }

}
