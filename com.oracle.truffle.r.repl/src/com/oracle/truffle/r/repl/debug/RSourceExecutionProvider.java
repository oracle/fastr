/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.repl.debug;

import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.instrument.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.tools.debug.engine.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RContext.Engine.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.shell.*;

/**
 * Manager for FastR AST execution under RREPL debugging control.
 */
public final class RSourceExecutionProvider extends SourceExecutionProvider {

    @Override
    public void languageRun(Source source) throws DebugException {
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
        }
    }

    @Override
    public Object languageEval(Source source, Node node, MaterializedFrame frame) {
        return RContext.getEngine().parseAndEval(source, frame, false, false);
    }

    @Override
    public AdvancedInstrumentRootFactory languageAdvancedInstrumentRootFactory(String exprText, AdvancedInstrumentResultListener resultListener) throws DebugException {
        try {
            RNode astNode = parseExpr(exprText);
            return new RToolEvalNodeFactory(astNode);
        } catch (ParseException ex) {
            throw new DebugException("Unable to parse tool-supplied expression");
        }

    }

    private static RNode parseExpr(String exprText) throws ParseException {
        return (RNode) ((RLanguage) RContext.getEngine().parse(Source.fromText(exprText, "<tool eval>")).getDataAt(0)).getRep();
    }

    private static final class RToolEvalNodeFactory implements AdvancedInstrumentRootFactory {
        private final RNode exprNode;

        private RToolEvalNodeFactory(RNode exprNode) {
            this.exprNode = exprNode;
        }

        public AdvancedInstrumentRoot createInstrumentRoot(Probe probe, Node node) {
            return new RAdvancedInstrumentRoot(NodeUtil.cloneNode(exprNode));
        }

    }

    private static final class RAdvancedInstrumentRoot extends AdvancedInstrumentRoot {
        @Child RNode exprNode;

        private RAdvancedInstrumentRoot(RNode exprNodeArg) {
            this.exprNode = insert(exprNodeArg);
        }

        public String instrumentationInfo() {
            return "R ToolEval Node";
        }

        @Override
        public Object executeRoot(Node node, VirtualFrame frame) {
            Object result = exprNode.execute(frame);
            Object asVector = RRuntime.asAbstractVector(result);
            if (asVector instanceof RLogicalVector) {
                result = RRuntime.fromLogical(((RLogicalVector) asVector).getDataAt(0));
            }
            return result;
        }

    }

}
