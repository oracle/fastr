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
package com.oracle.truffle.r.nodes.instrument.trace;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrument.Instrument;
import com.oracle.truffle.api.instrument.Probe;
import com.oracle.truffle.api.instrument.StandardSyntaxTag;
import com.oracle.truffle.api.instrument.TruffleEventReceiver;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.utilities.CyclicAssumption;
import com.oracle.truffle.r.nodes.function.FunctionDefinitionNode;
import com.oracle.truffle.r.nodes.function.FunctionStatementsNode;
import com.oracle.truffle.r.nodes.function.FunctionUID;
import com.oracle.truffle.r.nodes.instrument.RInstrument;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RContext;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RFunction;

import java.util.WeakHashMap;

public class TraceHandling {

    /**
     * Records all functions that have debug receivers installed.
     */
    private static final WeakHashMap<FunctionUID, TraceFunctionEventReceiver> receiverMap = new WeakHashMap<>();

    public static boolean enableTrace(RFunction func) {
        FunctionDefinitionNode fdn = (FunctionDefinitionNode) func.getRootNode();
        TraceFunctionEventReceiver fbr = receiverMap.get(fdn.getUID());
        if (fbr == null) {
            Probe probe = attachTraceHandler(fdn);
            return probe != null;
        } else {
            fbr.enable();
            return true;
        }

    }

    private static Probe attachTraceHandler(FunctionDefinitionNode fdn) {
        Probe probe = RInstrument.findSingleProbe(fdn.getUID(), StandardSyntaxTag.START_METHOD);
        if (probe == null) {
            return null;
        }
        TraceFunctionEventReceiver fser = new TraceFunctionEventReceiver(fdn);
        probe.attach(fser.getInstrument());
        return probe;
    }

    private abstract static class TraceEventReceiver implements TruffleEventReceiver {

        protected final FunctionDefinitionNode functionDefinitionNode;
        @CompilationFinal private boolean disabled;
        CyclicAssumption disabledUnchangedAssumption = new CyclicAssumption("trace event disabled state unchanged");

        protected TraceEventReceiver(FunctionDefinitionNode functionDefinitionNode) {
            this.functionDefinitionNode = functionDefinitionNode;
        }

        @SuppressWarnings("unused")
        FunctionDefinitionNode getFunctionDefinitionNode() {
            return functionDefinitionNode;
        }

        @Override
        public void returnVoid(Node node, VirtualFrame frame) {
            if (!disabled()) {
                throw RInternalError.shouldNotReachHere();
            }
        }

        @Override
        public void returnExceptional(Node node, VirtualFrame frame, Exception exception) {
        }

        @Override
        public void returnValue(Node node, VirtualFrame frame, Object result) {
        }

        boolean disabled() {
            return disabled;
        }

        @SuppressWarnings("unused")
        void disable() {
            setDisabledState(true);
        }

        void enable() {
            setDisabledState(false);
        }

        private void setDisabledState(boolean newState) {
            if (newState != disabled) {
                disabledUnchangedAssumption.invalidate();
                disabled = newState;
            }
        }

    }

    private static class TraceFunctionEventReceiver extends TraceEventReceiver {

        @SuppressWarnings("unused") FunctionDefinitionNode fdn;
        Instrument instrument;

        TraceFunctionEventReceiver(FunctionDefinitionNode fdn) {
            super(fdn);
            instrument = Instrument.create(this);
        }

        Instrument getInstrument() {
            return instrument;
        }

        @Override
        public void enter(Node node, VirtualFrame frame) {
            if (!disabled()) {
                @SuppressWarnings("unused")
                FunctionStatementsNode fsn = (FunctionStatementsNode) node;
                RContext.getInstance().getConsoleHandler().printf("trace: %s%n", RArguments.safeGetCallSourceString(frame));
            }
        }
    }

}
