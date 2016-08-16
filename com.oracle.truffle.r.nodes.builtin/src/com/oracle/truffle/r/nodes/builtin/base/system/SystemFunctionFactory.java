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
import java.nio.file.FileSystems;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.runtime.REnvVars;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.context.RContext;

abstract class SystemFunctionFactory {
    private static String kind;
    private static SystemFunctionFactory theInstance;

    static {
        kind = System.getProperty("fastr.systemfunction.factory.kind", "Process");
        String className = "com.oracle.truffle.r.nodes.builtin.base.system." + kind + "SystemFunctionFactory";
        try {
            theInstance = (SystemFunctionFactory) Class.forName(className).newInstance();
        } catch (Exception ex) {
            // CheckStyle: stop system..print check
            Utils.rSuicide("Failed to instantiate class: " + className);
        }
    }

    @TruffleBoundary
    public static SystemFunctionFactory getInstance() {
        return theInstance;
    }

    /**
     * Implements the system {@code .Internal}. If {@code intern} is {@code true} the result is a
     * character vector containing the output of the process, with a {@code status} attribute
     * carrying the return code, else it is just the return code.
     *
     * {@code command} is a string with args separated by spaces with the first element enclosed in
     * single quotes.
     */
    abstract Object execute(VirtualFrame frame, String command, boolean intern);

    @TruffleBoundary
    protected void log(String command, String useKind) {
        if (RContext.getInstance().stateREnvVars.getMap().get("FASTR_LOG_SYSTEM") != null) {
            System.out.printf("FastR system (%s): %s%n", useKind, command);
        }

    }

    @TruffleBoundary
    protected void log(String command) {
        log(command, kind);
    }

    /**
     * Returns {@code true} iff, {@code command} is {@code R} or {@code Rscript}.
     */
    protected static String isFastR(String command) {
        // strip off quotes
        String xc = Utils.unShQuote(command);
        if (xc.equals("R") || xc.equals("Rscript")) {
            return xc;
        }
        // often it is an absolute path
        String rhome = REnvVars.rHome();
        if (isFullPath(rhome, "Rscript", xc)) {
            return "Rscript";
        }
        if (isFullPath(rhome, "R", xc)) {
            return "R";
        }
        return null;
    }

    private static boolean isFullPath(String rhome, String rcmd, String command) {
        try {
            String rpath = FileSystems.getDefault().getPath(rhome, "bin", rcmd).toString();
            String cpath = FileSystems.getDefault().getPath(command).toRealPath().toString();
            if (cpath.equals(rpath)) {
                return true;
            }
        } catch (IOException ex) {
            // should not happen but just return false
        }
        return false;
    }

}
