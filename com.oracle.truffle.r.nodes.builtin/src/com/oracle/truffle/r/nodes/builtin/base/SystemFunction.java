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

import java.io.*;
import java.lang.ProcessBuilder.*;
import java.util.*;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "system", kind = RBuiltinKind.INTERNAL, parameterNames = {"command", "intern"})
public abstract class SystemFunction extends RBuiltinNode {
    @Specialization
    @TruffleBoundary
    public Object system(RAbstractStringVector command, byte internLogical) {
        Object result;
        boolean intern = RRuntime.fromLogical(internLogical);
        String shell = REnvVars.get("SHELL");
        if (shell == null) {
            shell = "/bin/sh";
        }
        if (System.getProperty("fastr.logchild") != null) {
            System.out.printf("FastR system: %s -c %s%n", shell, command.getDataAt(0));
        }
        ProcessBuilder pb = new ProcessBuilder(shell, "-c", command.getDataAt(0));
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
            rc = p.waitFor();
            if (intern) {
                String output = readAvailable(os);
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
        RContext.getInstance().setVisible(false);
        return result;
    }

    protected String readAvailable(InputStream output) throws IOException {
        int n = output.available();
        byte[] data = new byte[n];
        output.read(data);
        return new String(data);
    }

    private static void updateEnvironment(ProcessBuilder pb) {
        Map<String, String> pEnv = pb.environment();
        Map<String, String> rEnv = REnvVars.getMap();
        for (Map.Entry<String, String> entry : rEnv.entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();
            if (!pEnv.containsKey(name) || !pEnv.get(name).equals(value)) {
                pEnv.put(name, value);
            }
        }
    }
}
