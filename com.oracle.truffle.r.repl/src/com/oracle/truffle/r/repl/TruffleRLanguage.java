/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.repl;

import java.io.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.debug.*;
import com.oracle.truffle.api.instrument.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.repl.debug.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.shell.*;

@TruffleLanguage.Registration(name = "R", version = "3.1.3", mimeType = "application/x-r")
public class TruffleRLanguage extends TruffleLanguage {

    private DebugSupportProvider debugSupport;

    public TruffleRLanguage(Env env) {
        super(env);
    }

    @Override
    protected Object eval(Source code) throws IOException {
        boolean runShell = code.getName().equals("rshell.R");
        String[] args = new String[runShell ? 1 : 2];
        args[0] = "--debugger=rrepl";
        if (!runShell) {
            args[1] = "--file=" + code.getPath();
        }
        RCommand.main(args);
        return null;
    }

    @Override
    protected Object findExportedSymbol(String globalName, boolean onlyExplicit) {
        throw RInternalError.unimplemented();
    }

    @Override
    protected Object getLanguageGlobal() {
        throw RInternalError.unimplemented();
    }

    @Override
    protected boolean isObjectOfLanguage(Object object) {
        return false;
    }

    @Override
    protected ToolSupportProvider getToolSupport() {
        return getDebugSupport();
    }

    @Override
    protected DebugSupportProvider getDebugSupport() {
        if (debugSupport == null) {
            debugSupport = new RDebugSupportProvider();
        }
        return debugSupport;
    }

}
