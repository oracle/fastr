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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.INTERNAL;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinPackages;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.REnvVars;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RVisibility;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.ffi.BaseRFFI.UtsName;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;

public class SysFunctions {

    @RBuiltin(name = "Sys.getpid", kind = INTERNAL, parameterNames = {})
    public abstract static class SysGetpid extends RBuiltinNode {

        @Specialization
        @TruffleBoundary
        protected Object sysGetPid() {
            int pid = RFFIFactory.getRFFI().getBaseRFFI().getpid();
            return RDataFactory.createIntVectorFromScalar(pid);
        }
    }

    @RBuiltin(name = "Sys.getenv", kind = INTERNAL, parameterNames = {"x", "unset"})
    public abstract static class SysGetenv extends RBuiltinNode {
        private final ConditionProfile zeroLengthProfile = ConditionProfile.createBinaryProfile();

        @Specialization
        @TruffleBoundary
        protected Object sysGetEnv(RAbstractStringVector x, RAbstractStringVector unset) {
            Map<String, String> envMap = RContext.getInstance().stateREnvVars.getMap();
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
                        data[i] = unset.getDataAt(0);
                        if (RRuntime.isNA(unset.getDataAt(0))) {
                            complete = RDataFactory.INCOMPLETE_VECTOR;
                        }
                    }
                }
                return RDataFactory.createStringVector(data, complete);
            }
        }

        @Specialization
        protected Object sysGetEnvGeneric(@SuppressWarnings("unused") Object x, @SuppressWarnings("unused") Object unset) {
            CompilerDirectives.transferToInterpreter();
            throw RError.error(this, RError.Message.WRONG_TYPE);
        }
    }

    /**
     * HACK ALERT: To interpose on the end of namespace loading to support overriding some of the R
     * code, we check for the symbol _R_NS_LOAD_, see attachNamespace in namespace.R. Evidently this
     * code depends critically in the current implementation which sets/unsets this environment
     * variable around a call to loadNamespace/attachNamespace.
     */
    protected abstract static class LoadNamespaceAdapter extends RBuiltinNode {
        private static final String NS_LOAD = "_R_NS_LOAD_";
        private static final String LOADNAMESPACE = "loadNamespace";

        protected void checkNSLoad(VirtualFrame frame, RAbstractStringVector names, RAbstractStringVector values, boolean setting) {
            if (names.getLength() == 1 && names.getDataAt(0).equals(NS_LOAD)) {
                Frame caller = Utils.getCallerFrame(frame, FrameAccess.READ_ONLY);
                RFunction func = RArguments.getFunction(caller);
                if (func.toString().equals(LOADNAMESPACE)) {
                    if (setting) {
                        RContext.getInstance().setNamespaceName(values.getDataAt(0));
                    } else {
                        // Now we can run the overrides
                        RBuiltinPackages.loadDefaultPackageOverrides(RContext.getInstance().getNamespaceName());
                    }
                    System.console();
                }
            }

        }
    }

    @RBuiltin(name = "Sys.setenv", visibility = RVisibility.OFF, kind = INTERNAL, parameterNames = {"nm", "values"})
    public abstract static class SysSetEnv extends LoadNamespaceAdapter {

        @Specialization
        protected RLogicalVector doSysSetEnv(VirtualFrame frame, RAbstractStringVector names, RAbstractStringVector values) {
            checkNSLoad(frame, names, values, true);
            return doSysSetEnv(names, values);
        }

        @TruffleBoundary
        private static RLogicalVector doSysSetEnv(RAbstractStringVector names, RAbstractStringVector values) {
            byte[] data = new byte[names.getLength()];
            REnvVars stateREnvVars = RContext.getInstance().stateREnvVars;
            for (int i = 0; i < data.length; i++) {
                stateREnvVars.put(names.getDataAt(i), values.getDataAt(i));
                data[i] = RRuntime.LOGICAL_TRUE;
            }
            return RDataFactory.createLogicalVector(data, RDataFactory.COMPLETE_VECTOR);
        }
    }

    @RBuiltin(name = "Sys.unsetenv", visibility = RVisibility.OFF, kind = INTERNAL, parameterNames = {"x"})
    public abstract static class SysUnSetEnv extends LoadNamespaceAdapter {

        @Specialization
        protected RLogicalVector doSysUnSetEnv(VirtualFrame frame, RAbstractStringVector names) {
            checkNSLoad(frame, names, null, false);
            return doSysUnSetEnv(names);
        }

        @TruffleBoundary
        protected static RLogicalVector doSysUnSetEnv(RAbstractStringVector names) {
            byte[] data = new byte[names.getLength()];
            REnvVars stateREnvVars = RContext.getInstance().stateREnvVars;
            for (int i = 0; i < data.length; i++) {
                data[i] = RRuntime.asLogical(stateREnvVars.unset(names.getDataAt(i)));
            }
            RLogicalVector result = RDataFactory.createLogicalVector(data, RDataFactory.COMPLETE_VECTOR);
            return result;
        }
    }

    @RBuiltin(name = "Sys.sleep", visibility = RVisibility.OFF, kind = INTERNAL, parameterNames = {"time"})
    public abstract static class SysSleep extends RBuiltinNode {

        @Specialization
        @TruffleBoundary
        protected Object sysSleep(double seconds) {
            sleep(convertToMillis(seconds));
            return RNull.instance;
        }

        @Specialization
        @TruffleBoundary
        protected Object sysSleep(String secondsString) {
            long millis = convertToMillis(checkValidString(secondsString));
            sleep(millis);
            return RNull.instance;
        }

        @Specialization(guards = "lengthOne(secondsVector)")
        @TruffleBoundary
        protected Object sysSleep(RStringVector secondsVector) {
            long millis = convertToMillis(checkValidString(secondsVector.getDataAt(0)));
            sleep(millis);
            return RNull.instance;
        }

        protected static boolean lengthOne(RStringVector vec) {
            return vec.getLength() == 1;
        }

        @Specialization
        @TruffleBoundary
        protected Object sysSleep(@SuppressWarnings("unused") Object arg) {
            throw RError.error(this, RError.Message.INVALID_VALUE, "time");
        }

        private static long convertToMillis(double d) {
            return (long) (d * 1000);
        }

        private double checkValidString(String s) {
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException ex) {
                throw RError.error(this, RError.Message.INVALID_VALUE, "time");
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
            return RDataFactory.createStringVector(doSysReadLink(path));
        }

        @Specialization
        @TruffleBoundary
        protected Object sysReadlink(RStringVector vector) {
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
            CompilerDirectives.transferToInterpreter();
            throw RError.error(this, RError.Message.INVALID_ARGUMENT, "paths");
        }
    }

    // TODO implement
    @RBuiltin(name = "Sys.chmod", visibility = RVisibility.OFF, kind = INTERNAL, parameterNames = {"paths", "octmode", "use_umask"})
    public abstract static class SysChmod extends RBuiltinNode {
        @Specialization
        @TruffleBoundary
        protected RLogicalVector sysChmod(RAbstractStringVector pathVec, RAbstractIntVector octmode, @SuppressWarnings("unused") byte useUmask) {
            byte[] data = new byte[pathVec.getLength()];
            for (int i = 0; i < data.length; i++) {
                String path = Utils.tildeExpand(pathVec.getDataAt(i));
                if (path.length() == 0 || RRuntime.isNA(path)) {
                    continue;
                }
                int result = RFFIFactory.getRFFI().getBaseRFFI().chmod(path, octmode.getDataAt(i % octmode.getLength()));
                data[i] = RRuntime.asLogical(result == 0);
            }
            return RDataFactory.createLogicalVector(data, RDataFactory.COMPLETE_VECTOR);
        }
    }

    // TODO implement
    @RBuiltin(name = "Sys.umask", visibility = RVisibility.CUSTOM, kind = INTERNAL, parameterNames = {"octmode"})
    public abstract static class SysUmask extends RBuiltinNode {
        @SuppressWarnings("unused")
        @Specialization
        @TruffleBoundary
        protected Object sysChmod(Object octmode) {
            throw RError.nyi(this, "Sys.umask");
        }
    }

    @RBuiltin(name = "Sys.time", kind = INTERNAL, parameterNames = {})
    public abstract static class SysTime extends RBuiltinNode {
        @Specialization
        @TruffleBoundary
        protected double sysTime() {
            return ((double) System.currentTimeMillis()) / 1000;
        }
    }

    @RBuiltin(name = "Sys.info", kind = INTERNAL, parameterNames = {})
    public abstract static class SysInfo extends RBuiltinNode {
        private static final String[] NAMES = new String[]{"sysname", "release", "version", "nodename", "machine", "login", "user", "effective_user"};
        private static final RStringVector NAMES_ATTR = RDataFactory.createStringVector(NAMES, RDataFactory.COMPLETE_VECTOR);

        @Specialization
        @TruffleBoundary
        protected Object sysTime() {
            UtsName utsname = RFFIFactory.getRFFI().getBaseRFFI().uname();
            String[] data = new String[NAMES.length];
            data[0] = utsname.sysname();
            data[1] = utsname.release();
            data[2] = utsname.version();
            data[3] = utsname.nodename();
            data[4] = utsname.machine();
            // Need more RFFI support for these, and "unknown" is an ok value
            data[5] = "unknown";
            data[6] = "unknown";
            data[7] = "unknown";
            RStringVector result = RDataFactory.createStringVector(data, RDataFactory.COMPLETE_VECTOR, NAMES_ATTR);
            return result;
        }
    }

    @RBuiltin(name = "Sys.glob", kind = INTERNAL, parameterNames = {"paths", "dirmask"})
    public abstract static class SysGlob extends RBuiltinNode {

        @Specialization
        @TruffleBoundary
        protected Object sysGlob(RAbstractStringVector pathVec, @SuppressWarnings("unused") byte dirMask) {
            ArrayList<String> matches = new ArrayList<>();
            // Sys.glob closure already called path.expand
            for (int i = 0; i < pathVec.getLength(); i++) {
                String pathPattern = pathVec.getDataAt(i);
                if (pathPattern.length() == 0 || RRuntime.isNA(pathPattern)) {
                    continue;
                }
                ArrayList<String> pathPatternMatches = RFFIFactory.getRFFI().getBaseRFFI().glob(pathPattern);
                matches.addAll(pathPatternMatches);
            }
            String[] data = new String[matches.size()];
            matches.toArray(data);
            return RDataFactory.createStringVector(data, RDataFactory.COMPLETE_VECTOR);
        }
    }
}
