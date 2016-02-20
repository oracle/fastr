/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.instrument.wrappers;

import com.oracle.truffle.api.instrument.Probe;
import com.oracle.truffle.api.instrument.EventHandlerNode;
import com.oracle.truffle.api.instrument.WrapperNode;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.runtime.nodes.*;

@NodeInfo(cost = NodeCost.NONE)
public final class WriteSuperVariableNodeWrapper extends com.oracle.truffle.r.nodes.access.WriteSuperVariableNode implements WrapperNode {
    @Child com.oracle.truffle.r.nodes.access.WriteSuperVariableNode child;
    @Child private EventHandlerNode eventHandlerNode;

    public WriteSuperVariableNodeWrapper(com.oracle.truffle.r.nodes.access.WriteSuperVariableNode child) {
        super(child.getSourceSection());
        assert child != null;
        assert !(child instanceof WriteSuperVariableNodeWrapper);
        this.child = child;
    }

    public String instrumentationInfo() {
        return "Wrapper node for com.oracle.truffle.r.nodes.access.WriteSuperVariableNode";
    }

    public Node getChild() {
        return child;
    }

    public Probe getProbe() {
        try {
            return eventHandlerNode.getProbe();
        } catch (IllegalStateException e) {
            throw new IllegalStateException("A lite-Probed wrapper has no explicit Probe");
        }
    }

    public void insertEventHandlerNode(EventHandlerNode newEventHandlerNode) {
        this.eventHandlerNode = newEventHandlerNode;
    }

    @Override
    public com.oracle.truffle.r.runtime.nodes.RNode getRhs() {
        return child.getRhs();
    }

    @Override
    public void execute(com.oracle.truffle.api.frame.VirtualFrame frame, java.lang.Object value) {
        eventHandlerNode.enter(child, frame);

        try {
            child.execute(frame, value);
            eventHandlerNode.returnVoid(child, frame);
        } catch (Exception e) {
            eventHandlerNode.returnExceptional(child, frame, e);
            throw (e);
        }
    }

    @Override
    public java.lang.Object execute(com.oracle.truffle.api.frame.VirtualFrame frame) {
        eventHandlerNode.enter(child, frame);

        java.lang.Object result;
        try {
            result = child.execute(frame);
            eventHandlerNode.returnValue(child, frame, result);
            return result;
        } catch (Exception e) {
            eventHandlerNode.returnExceptional(child, frame, e);
            throw (e);
        }
    }

    @Override
    public java.lang.Object getName() {
        return child.getName();
    }

    @Override
    public RSyntaxNode getRSyntaxNode() {
        return child.asRSyntaxNode();
    }

    @Override
    public boolean isRInstrumentable() {
        return false;
    }

    @Override
    public boolean isSyntax() {
        return false;
    }

}
