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
package com.oracle.truffle.r.nodes.instrument.wrappers;

import com.oracle.truffle.api.instrument.Probe;
import com.oracle.truffle.api.instrument.EventHandlerNode;
import com.oracle.truffle.api.instrument.WrapperNode;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.runtime.nodes.*;

@NodeInfo(cost = NodeCost.NONE)
public final class ReadArgumentNodeWrapper extends com.oracle.truffle.r.nodes.access.ReadArgumentNode implements WrapperNode {
    @Child com.oracle.truffle.r.nodes.access.ReadArgumentNode child;
    @Child private EventHandlerNode eventHandlerNode;

    public ReadArgumentNodeWrapper(com.oracle.truffle.r.nodes.access.ReadArgumentNode child) {
        assert child != null;
        assert !(child instanceof ReadArgumentNodeWrapper);
        this.child = child;
    }

    public String instrumentationInfo() {
        return "Wrapper node for com.oracle.truffle.r.nodes.access.ReadArgumentNode";
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
    public RSyntaxNode getRSyntaxNode() {
        return child.asRSyntaxNode();
    }

    @Override
    public boolean isRInstrumentable() {
        return false;
    }

}
