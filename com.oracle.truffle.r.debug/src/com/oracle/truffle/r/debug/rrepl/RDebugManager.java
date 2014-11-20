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
package com.oracle.truffle.r.debug.rrepl;

import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.instrument.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.debug.*;
import com.oracle.truffle.debug.impl.*;
import com.oracle.truffle.r.nodes.instrument.*;
import com.oracle.truffle.r.runtime.RContext;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.shell.*;

/**
 * Manager for FastR AST execution under RREPL debugging control.
 */
public final class RDebugManager extends AbstractDebugManager {

    public RDebugManager(DebugClient client) {
        super(client);
        Probe.registerASTProber(new RASTDebugProber());
    }

    public void run(Source source) {

        startExecution(source);
        try {
            boolean runShell = source.getName().equals("<shell>");
            String[] args = new String[runShell ? 1 : 2];
            args[0] = "--debugger=rrepl";
            if (!runShell) {
                args[1] = "--file=" + source.getPath();
            }
            RCommand.main(args);
        } catch (Exception e) {
            throw new DebugException("Can't run source " + source.getName() + ": " + e.getMessage());
        } finally {
            endExecution(source);
        }
    }

    public Object eval(Source source, Node node, MaterializedFrame frame) {
        // TODO Auto-generated method stub
        return RContext.getEngine().parseAndEval(source.getName(), source.getCode(), frame, REnvironment.frameToEnvironment(frame), true, false);
     }

}
