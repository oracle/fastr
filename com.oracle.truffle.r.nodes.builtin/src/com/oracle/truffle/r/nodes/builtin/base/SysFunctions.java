/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.eq;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.gte;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.notEmpty;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.size;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.RVisibility.CUSTOM;
import static com.oracle.truffle.r.runtime.RVisibility.OFF;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.IO;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.MODIFIES_STATE;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.READS_STATE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinPackages;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.REnvVars;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.ffi.BaseRFFI;
import com.oracle.truffle.r.runtime.ffi.BaseRFFI.UtsName;

public class SysFunctions {

    @RBuiltin(name = "Sys.getpid", kind = INTERNAL, parameterNames = {}, behavior = READS_STATE)
    public abstract static class SysGetpid extends RBuiltinNode {
        @Child private BaseRFFI.GetpidNode getpidNode = BaseRFFI.GetpidNode.create();

        @Specialization
        @TruffleBoundary
        protected Object sysGetPid() {
            int pid = getpidNode.execute();
            return RDataFactory.createIntVectorFromScalar(pid);
        }
    }

    @RBuiltin(name = "Sys.getenv", kind = INTERNAL, parameterNames = {"x", "unset"}, behavior = READS_STATE)
    public abstract static class SysGetenv extends RBuiltinNode {
        private final ConditionProfile zeroLengthProfile = ConditionProfile.createBinaryProfile();

        static {
            Casts casts = new Casts(SysGetenv.class);
            casts.arg("x").mustBe(stringValue(), RError.Message.ARGUMENT_WRONG_TYPE);
            casts.arg("unset").mustBe(stringValue()).asStringVector().mustBe(size(1)).findFirst();
        }

        @Specialization
        @TruffleBoundary
        protected Object sysGetEnv(RAbstractStringVector x, String unset) {
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
                        data[i] = unset;
                        if (RRuntime.isNA(unset)) {
                            complete = RDataFactory.INCOMPLETE_VECTOR;
                        }
                    }
                }
                return RDataFactory.createStringVector(data, complete);
            }
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
            if (names.getLength() == 1 && NS_LOAD.equals(names.getDataAt(0))) {
                doCheckNSLoad(frame.materialize(), values, setting);
            }
        }

        @TruffleBoundary
        private static void doCheckNSLoad(MaterializedFrame frame, RAbstractStringVector values, boolean setting) {
            Frame caller = Utils.getCallerFrame(frame, FrameAccess.READ_ONLY);
            RFunction func = RArguments.getFunction(caller);
            if (func.toString().equals(LOADNAMESPACE)) {
                if (setting) {
                    RContext.getInstance().setNamespaceName(values.getDataAt(0));
                } else {
                    // Now we can run the overrides
                    RBuiltinPackages.loadDefaultPackageOverrides(RContext.getInstance().getNamespaceName());
                }
            }
        }
    }

    @RBuiltin(name = "Sys.setenv", visibility = OFF, kind = INTERNAL, parameterNames = {"nm", "values"}, behavior = MODIFIES_STATE)
    public abstract static class SysSetEnv extends LoadNamespaceAdapter {

        static {
            Casts casts = new Casts(SysSetEnv.class);
            casts.arg("nm").mustBe(stringValue(), RError.Message.ARGUMENT_WRONG_TYPE);
            casts.arg("values").mustBe(stringValue(), RError.Message.ARGUMENT_WRONG_TYPE);
        }

        @Specialization
        protected RLogicalVector doSysSetEnv(VirtualFrame frame, RAbstractStringVector names, RAbstractStringVector values) {
            if (names.getLength() != values.getLength()) {
                throw error(RError.Message.ARGUMENT_WRONG_LENGTH);
            }
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

    @RBuiltin(name = "Sys.unsetenv", visibility = OFF, kind = INTERNAL, parameterNames = {"x"}, behavior = READS_STATE)
    public abstract static class SysUnSetEnv extends LoadNamespaceAdapter {

        static {
            Casts casts = new Casts(SysUnSetEnv.class);
            casts.arg("x").mustBe(stringValue(), RError.Message.ARGUMENT_WRONG_TYPE);
        }

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

    @RBuiltin(name = "Sys.sleep", visibility = OFF, kind = INTERNAL, parameterNames = {"time"}, behavior = COMPLEX)
    public abstract static class SysSleep extends RBuiltinNode {

        static {
            Casts casts = new Casts(SysSleep.class);
            casts.arg("time").asDoubleVector().findFirst().mustBe(gte(0.0).and(eq(Double.NaN).not()));
        }

        @Specialization
        @TruffleBoundary
        protected Object sysSleep(double seconds) {
            sleep(convertToMillis(seconds));
            return RNull.instance;
        }

        private static long convertToMillis(double d) {
            return (long) (d * 1000);
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
    @RBuiltin(name = "Sys.readlink", kind = INTERNAL, parameterNames = {"paths"}, behavior = IO)
    public abstract static class SysReadlink extends RBuiltinNode {

        static {
            Casts casts = new Casts(SysReadlink.class);
            casts.arg("paths").mustBe(stringValue());
        }

        @Specialization
        @TruffleBoundary
        protected Object sysReadlink(RAbstractStringVector vector,
                        @Cached("create()") BaseRFFI.ReadlinkNode readlinkNode) {
            String[] paths = new String[vector.getLength()];
            boolean complete = RDataFactory.COMPLETE_VECTOR;
            for (int i = 0; i < paths.length; i++) {
                String path = vector.getDataAt(i);
                if (RRuntime.isNA(path)) {
                    paths[i] = path;
                } else {
                    paths[i] = doSysReadLink(path, readlinkNode);
                }
                if (RRuntime.isNA(paths[i])) {
                    complete = RDataFactory.INCOMPLETE_VECTOR;
                }
            }
            return RDataFactory.createStringVector(paths, complete);
        }

        @TruffleBoundary
        private static String doSysReadLink(String path, BaseRFFI.ReadlinkNode readlinkNode) {
            String s;
            try {
                s = readlinkNode.execute(path);
                if (s == null) {
                    s = "";
                }
            } catch (IOException ex) {
                s = RRuntime.STRING_NA;
            }
            return s;
        }
    }

    @RBuiltin(name = "Sys.chmod", visibility = OFF, kind = INTERNAL, parameterNames = {"paths", "octmode", "use_umask"}, behavior = IO)
    public abstract static class SysChmod extends RBuiltinNode {

        static {
            Casts casts = new Casts(SysChmod.class);
            casts.arg("paths").mustBe(stringValue());
            casts.arg("octmode").asIntegerVector().mustBe(notEmpty(), RError.Message.MODE_LENGTH_ONE);
            casts.arg("use_umask").asLogicalVector().findFirst().mustNotBeNA().map(toBoolean());
        }

        @Specialization
        @TruffleBoundary
        protected RLogicalVector sysChmod(RAbstractStringVector pathVec, RAbstractIntVector octmode, @SuppressWarnings("unused") boolean useUmask,
                        @Cached("create()") BaseRFFI.ChmodNode chmodNode) {
            byte[] data = new byte[pathVec.getLength()];
            for (int i = 0; i < data.length; i++) {
                String path = Utils.tildeExpand(pathVec.getDataAt(i));
                if (path.length() == 0 || RRuntime.isNA(path)) {
                    continue;
                }
                int result = chmodNode.execute(path, octmode.getDataAt(i % octmode.getLength()));
                data[i] = RRuntime.asLogical(result == 0);
            }
            return RDataFactory.createLogicalVector(data, RDataFactory.COMPLETE_VECTOR);
        }
    }

    // TODO implement
    @RBuiltin(name = "Sys.umask", visibility = CUSTOM, kind = INTERNAL, parameterNames = {"octmode"}, behavior = COMPLEX)
    public abstract static class SysUmask extends RBuiltinNode {

        static {
            Casts casts = new Casts(SysUmask.class);
            casts.arg("octmode").asIntegerVector().findFirst();
        }

        @SuppressWarnings("unused")
        @Specialization
        @TruffleBoundary
        protected Object sysUmask(int octmode) {
            throw RError.nyi(this, "Sys.umask");
        }
    }

    @RBuiltin(name = "Sys.time", kind = INTERNAL, parameterNames = {}, behavior = IO)
    public abstract static class SysTime extends RBuiltinNode {
        @Specialization
        @TruffleBoundary
        protected double sysTime() {
            return ((double) System.currentTimeMillis()) / 1000;
        }
    }

    @RBuiltin(name = "Sys.info", kind = INTERNAL, parameterNames = {}, behavior = READS_STATE)
    public abstract static class SysInfo extends RBuiltinNode {
        private static final String[] NAMES = new String[]{"sysname", "release", "version", "nodename", "machine", "login", "user", "effective_user"};
        private static final RStringVector NAMES_ATTR = RDataFactory.createStringVector(NAMES, RDataFactory.COMPLETE_VECTOR);

        @Child private BaseRFFI.UnameNode unameNode = BaseRFFI.UnameNode.create();

        @Specialization
        @TruffleBoundary
        protected Object sysTime() {
            UtsName utsname = unameNode.execute();
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

    @RBuiltin(name = "Sys.glob", kind = INTERNAL, parameterNames = {"paths", "dirmask"}, behavior = IO)
    public abstract static class SysGlob extends RBuiltinNode {

        static {
            Casts casts = new Casts(SysGlob.class);
            casts.arg("paths").mustBe(stringValue()).asStringVector();
            casts.arg("dirmask").asLogicalVector().findFirst().mustNotBeNA().map(toBoolean());
        }

        @Specialization
        @TruffleBoundary
        protected Object sysGlob(RAbstractStringVector pathVec, @SuppressWarnings("unused") boolean dirMask,
                        @Cached("create()") BaseRFFI.GlobNode globNode) {
            ArrayList<String> matches = new ArrayList<>();
            // Sys.glob closure already called path.expand
            for (int i = 0; i < pathVec.getLength(); i++) {
                String pathPattern = pathVec.getDataAt(i);
                if (pathPattern.length() == 0 || RRuntime.isNA(pathPattern)) {
                    continue;
                }
                ArrayList<String> pathPatternMatches = globNode.glob(pathPattern);
                matches.addAll(pathPatternMatches);
            }
            String[] data = new String[matches.size()];
            matches.toArray(data);
            return RDataFactory.createStringVector(data, RDataFactory.COMPLETE_VECTOR);
        }
    }

    @RBuiltin(name = "setFileTime", kind = INTERNAL, parameterNames = {"path", "time"}, visibility = OFF, behavior = IO)
    public abstract static class SysSetFileTime extends RBuiltinNode {

        static {
            Casts casts = new Casts(SysSetFileTime.class);
            casts.arg("path").mustBe(stringValue()).asStringVector().findFirst();
            casts.arg("time").asIntegerVector().findFirst().mustNotBeNA();
        }

        @Specialization
        @TruffleBoundary
        protected byte sysSetFileTime(String path, int time) {
            try {
                Files.setLastModifiedTime(FileSystems.getDefault().getPath(path), FileTime.from(time, TimeUnit.SECONDS));
                return RRuntime.LOGICAL_TRUE;
            } catch (IOException ex) {
                return RRuntime.LOGICAL_FALSE;
            }
        }
    }
}
