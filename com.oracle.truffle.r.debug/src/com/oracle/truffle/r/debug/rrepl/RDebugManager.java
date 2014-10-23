/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.r.debug.rrepl;

import static com.oracle.truffle.api.instrument.StandardSyntaxTag.*;

import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.instrument.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.debug.*;
import com.oracle.truffle.debug.impl.*;
import com.oracle.truffle.debug.instrument.*;
import com.oracle.truffle.r.engine.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.instrument.*;
//import com.oracle.truffle.r.nodes.instrument.*;
import com.oracle.truffle.r.runtime.*;
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
        return null;
    }

}
