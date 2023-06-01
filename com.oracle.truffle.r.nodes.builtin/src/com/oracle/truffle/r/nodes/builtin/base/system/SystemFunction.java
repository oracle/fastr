/*
 * Copyright (c) 2014, 2023, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base.system;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.gte;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.RError.NO_CALLER;
import static com.oracle.truffle.r.runtime.RError.SHOW_CALLER;
import static com.oracle.truffle.r.runtime.RLogger.LOGGER_SYSTEM_FUNCTION;
import static com.oracle.truffle.r.runtime.RVisibility.CUSTOM;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.api.TruffleLogger;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.LanguageInfo;
import com.oracle.truffle.llvm.api.Toolchain;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.function.visibility.SetVisibilityNode;
import com.oracle.truffle.r.runtime.ProcessOutputManager;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RLogger;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RStringVector;

@RBuiltin(name = "system", visibility = CUSTOM, kind = INTERNAL, parameterNames = {"command", "intern", "timeout"}, behavior = COMPLEX)
public abstract class SystemFunction extends RBuiltinNode.Arg3 {
    @Child private SetVisibilityNode visibility = SetVisibilityNode.create();

    static {
        Casts casts = new Casts(SystemFunction.class);
        casts.arg("command").mustBe(stringValue(), RError.Message.SYSTEM_CHAR_ARG).asStringVector().findFirst();
        casts.arg("intern").asLogicalVector().findFirst().mustNotBeNA(RError.Message.SYSTEM_INTERN_NOT_NA).map(toBoolean());
        casts.arg("timeout").asIntegerVector().findFirst().mustNotBeNA().mustBe(gte(0));
    }

    @Specialization
    protected Object system(VirtualFrame frame, String command, boolean intern, int timeout) {
        Object result = executeCommand(command.trim(), intern, timeout);
        visibility.execute(frame, intern);
        return result;
    }

    private static final String[] LLVM_TOOLCHAIN_TOOLS = new String[]{
                    "CC",
                    "CXX",
                    "LD",
                    "AR",
                    "NM",
                    "OBJCOPY",
                    "OBJDUMP",
                    "RANLIB",
                    "READELF",
                    "READOBJ",
                    "STRIP"};

    @TruffleBoundary
    public static Object executeCommand(String command, boolean intern, int timeoutSecs) {
        Object result;
        // GNU R uses popen which always invokes /bin/sh
        String shell = "/bin/sh";
        log(String.format("%s -c \"%s\"", shell, command));
        ProcessBuilder pb = new ProcessBuilder(shell, "-c", command);
        updateEnvironment(pb);
        pb.redirectInput(Redirect.INHERIT);
        if (intern) {
            pb.redirectErrorStream(true);
        } else {
            pb.redirectOutput(Redirect.INHERIT);
            pb.redirectError(Redirect.INHERIT);
        }
        int rc;
        try {
            Process p = pb.start();
            InputStream os = p.getInputStream();
            ProcessOutputManager.OutputThreadVariable readThread = null;
            if (intern) {
                readThread = new ProcessOutputManager.OutputThreadVariable("system", os);
                readThread.start();
            }
            if (timeoutSecs > 0) {
                boolean exited = p.waitFor(timeoutSecs, TimeUnit.SECONDS);
                if (!exited) {
                    p.destroy();
                    RError.warning(SHOW_CALLER, Message.COMMAND_TIMED_OUT, command, timeoutSecs);
                }
                rc = exited ? p.exitValue() : 127;
            } else {
                rc = p.waitFor();
            }

            if (intern) {
                // capture output in character vector
                String output = new String(readThread.getData(), 0, readThread.getTotalRead());
                RStringVector vec;
                if (output.length() == 0) {
                    vec = RDataFactory.createEmptyStringVector();
                } else {
                    String[] data = output.split("\n");
                    vec = RDataFactory.createStringVector(data, RDataFactory.COMPLETE_VECTOR);
                }
                if (rc != 0) {
                    vec.setAttr("status", RDataFactory.createIntVectorFromScalar(rc));
                }
                result = vec;
            } else {
                result = rc;
            }
        } catch (InterruptedException | IOException ex) {
            result = 127;
        }
        return result;
    }

    /**
     * Any environment variables that have been added to this session must be forwarded to the
     * process (Java does not provide a {@code setenv} call, so {@code Sys.setenv} calls only affect
     * {@code stateEnvVars}. Any explicit settings in the command call (arising from the {@code env}
     * argument to the {@code system2} call, will override these by virtue of being explicitly set
     * in the new shell.
     */
    private static void updateEnvironment(ProcessBuilder pb) {
        Map<String, String> pEnv = pb.environment();
        Map<String, String> rEnv = RContext.getInstance().stateREnvVars.getMap();
        for (Map.Entry<String, String> entry : rEnv.entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();
            if (!pEnv.containsKey(name) || !pEnv.get(name).equals(value)) {
                pEnv.put(name, value);
            }
        }

        putToolChainVars(pEnv);
    }

    /**
     * The variables being set in this method are required by {@code llvm-cc} and {@code llvm-c++}
     * scripts.
     */
    private static void putToolChainVars(Map<String, String> pEnv) {
        LanguageInfo llvmInfo = RContext.getInstance().getEnv().getInternalLanguages().get("llvm");
        if (llvmInfo == null) {
            toolchainWarning(null);
            return;
        }

        Toolchain toolchain = RContext.getInstance().getEnv().lookup(llvmInfo, Toolchain.class);
        if (toolchain == null) {
            toolchainWarning(null);
            return;
        }

        // exports, e.g., LABS_TOOLCHAIN_CC, which can then be used in etc/Makeconf
        ArrayList<String> missingTools = null;
        for (String tool : LLVM_TOOLCHAIN_TOOLS) {
            TruffleFile path = toolchain.getToolPath(tool);
            if (path != null) {
                pEnv.put("LABS_TOOLCHAIN_" + tool, path.toString());
            } else {
                if (missingTools == null) {
                    missingTools = new ArrayList<>();
                }
                missingTools.add(tool);
            }
        }
        if (missingTools != null) {
            toolchainWarning(String.join(", ", missingTools));
        }
    }

    private static void toolchainWarning(String missingTools) {
        String start;
        if (missingTools == null) {
            start = "Labs LLVM Toolchain services are not available. ";
        } else {
            start = "Some Labs LLVM Toolchain tools are missing (" + missingTools + "). ";
        }
        RError.warning(NO_CALLER, Message.GENERIC, start +
                        "If the current $FASTR_HOME/etc/Makeconf is configured to use those services (uses variables starting with 'LABS_TOOLCHAIN_'), " +
                        "then the compilation will fail. Please make sure you have Labs LLVM Toolchain installed in GraalVM, " +
                        "or update etc/Makeconf to use your system compilers (see ?fastr.setToolchain). " +
                        "Note: the LLVM back-end will not work for packages compiled with system compilers.");
    }

    private static final TruffleLogger LOGGER = RLogger.getLogger(LOGGER_SYSTEM_FUNCTION);

    static void log(String command) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "FastR system: {1}", new Object[]{command});
        }
    }
}
