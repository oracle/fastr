/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base.system;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ProcessBuilder.Redirect;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.runtime.ProcessOutputManager;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RStringVector;

public class ProcessSystemFunctionFactory extends SystemFunctionFactory {
    /**
     * Temporary support for (test) processes that hang.
     */
    private static final String TIMEOUT = "FASTR_PROCESS_TIMEOUT";

    @Override
    public Object execute(VirtualFrame frame, String command, boolean intern) {
        return execute(command, intern);
    }

    @TruffleBoundary
    private Object execute(String command, boolean intern) {
        Object result;
        // GNU R uses popen which always invokes /bin/sh
        String shell = "/bin/sh";
        log(String.format("%s -c \"%s\"", shell, command), "Process");
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
            String timeoutVar = System.getenv(TIMEOUT);
            if (timeoutVar != null) {
                long timeout;
                try {
                    timeout = Integer.parseInt(timeoutVar);
                } catch (NumberFormatException ex) {
                    timeout = 5;
                }
                boolean exited = p.waitFor(timeout, TimeUnit.MINUTES);
                rc = exited ? 0 : 127;
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
    }
}
