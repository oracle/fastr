/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.function;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.InstrumentableNode.WrapperNode;
import com.oracle.truffle.api.instrumentation.ProbeNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.runtime.Arguments;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

public class RCallBaseNodeWrapperFactory {

    @NodeInfo(cost = NodeCost.NONE)
    public static final class RCallBaseNodeWrapper extends RCallBaseNode implements WrapperNode {
        @Child private RCallBaseNode delegate;
        @Child private ProbeNode probeNode;

        public RCallBaseNodeWrapper(RCallBaseNode delegate, ProbeNode probeNode) {
            assert delegate != null;
            this.delegate = delegate;
            this.probeNode = probeNode;
        }

        @Override
        public Arguments<RSyntaxNode> getArguments() {
            return delegate.getArguments();
        }

        @Override
        public RNode getFunction() {
            return delegate.getFunction();
        }

        @Override
        public Node getDelegateNode() {
            return delegate;
        }

        @Override
        public ProbeNode getProbeNode() {
            return probeNode;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object returnValue;
            for (;;) {
                boolean wasOnReturnExecuted = false;
                try {
                    probeNode.onEnter(frame);
                    returnValue = delegate.execute(frame);
                    wasOnReturnExecuted = true;
                    probeNode.onReturnValue(frame, returnValue);
                    break;
                } catch (Throwable t) {
                    Object result = probeNode.onReturnExceptionalOrUnwind(frame, t, wasOnReturnExecuted);
                    if (result == ProbeNode.UNWIND_ACTION_REENTER) {
                        continue;
                    } else if (result != null) {
                        returnValue = result;
                        break;
                    }
                    throw t;
                }
            }
            return returnValue;
        }

        @Override
        public Object visibleExecute(VirtualFrame frame) {
            Object returnValue;
            for (;;) {
                boolean wasOnReturnExecuted = false;
                try {
                    probeNode.onEnter(frame);
                    returnValue = delegate.visibleExecute(frame);
                    wasOnReturnExecuted = true;
                    probeNode.onReturnValue(frame, returnValue);
                    break;
                } catch (Throwable t) {
                    Object result = probeNode.onReturnExceptionalOrUnwind(frame, t, wasOnReturnExecuted);
                    if (result == ProbeNode.UNWIND_ACTION_REENTER) {
                        continue;
                    } else if (result != null) {
                        returnValue = result;
                        break;
                    }
                    throw t;
                }
            }
            return returnValue;
        }

        @Override
        public Object execute(VirtualFrame frame, Object function) {
            Object returnValue;
            for (;;) {
                boolean wasOnReturnExecuted = false;
                try {
                    probeNode.onEnter(frame);
                    returnValue = delegate.execute(frame, function);
                    wasOnReturnExecuted = true;
                    probeNode.onReturnValue(frame, returnValue);
                    break;
                } catch (Throwable t) {
                    Object result = probeNode.onReturnExceptionalOrUnwind(frame, t, wasOnReturnExecuted);
                    if (result == ProbeNode.UNWIND_ACTION_REENTER) {
                        continue;
                    } else if (result != null) {
                        returnValue = result;
                        break;
                    }
                    throw t;
                }
            }
            return returnValue;
        }

        @Override
        public RSyntaxNode getRSyntaxNode() {
            return delegate.asRSyntaxNode();
        }

        @Override
        public SourceSection getSourceSection() {
            return delegate.getSourceSection();
        }
    }
}
