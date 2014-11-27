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
package com.oracle.truffle.r.nodes.instrument;

import com.oracle.truffle.api.instrument.*;
import com.oracle.truffle.api.instrument.ProbeNode.Instrumentable;
import com.oracle.truffle.api.instrument.ProbeNode.WrapperNode;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.nodes.*;

/**
 * Denotes an {@link RNode} that can be instrumented using the {@link Instrumentable}. By default
 * all {@link RNode} instances are instrumentable.
 */
public interface RInstrumentableNode {

    default boolean isInstrumentable() {
        return true;
    }

    default Probe probe() {
        RNode thisNode = (RNode) this;
        Node parent = thisNode.getParent();

        if (parent == null) {
            throw new IllegalStateException("Cannot call probe() on a node without a parent.");
        }

        if (parent instanceof WrapperNode) {
            return ((WrapperNode) parent).getProbe();
        }

        // Create a new wrapper/probe with this node as its child.
        WrapperNode wrapper = createWrapperNode(thisNode);

        // Connect it to a Probe
        Probe probe = ProbeNode.insertProbe(wrapper);

        // Replace this node in the AST with the wrapper
        thisNode.replace((RNode) wrapper);
        return probe;

    }

    /**
     * Any AST nodes that are used as exactly typed fields of other nodes, i.e., not just
     * {@link RNode} must override this method to create an explicit subclass that implements
     * {@link WrapperNode} and forwards all pertinent behavior to {@code child}. The default
     * implementation is suitable for all children typed as plain {@link RNode}.
     */
    default WrapperNode createWrapperNode(RNode child) {
        return new RNodeWrapper(child);
    }

    /**
     * Unwrap a (potentially) wrapped node, returning the child. Since an AST may contain wrapper
     * nodes <b>anywhere</b>, this method <b>must</b> be called before casting or checking the type
     * of a node.
     */
    default RNode unwrap() {
        if (this instanceof WrapperNode) {
            return (RNode) ((WrapperNode) this).getChild();
        } else {
            return (RNode) this;
        }
    }

    default Node unwrapParent() {
        Node p = ((Node) this).getParent();
        if (p instanceof WrapperNode) {
            return p.getParent();
        } else {
            return p;
        }
    }

}
