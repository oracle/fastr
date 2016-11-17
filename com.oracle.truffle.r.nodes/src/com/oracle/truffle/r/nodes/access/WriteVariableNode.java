/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.access;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.runtime.FastROptions;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;

/**
 * The base of the {@code WriteVariableNode} type hierarchy. There are several variants for
 * different situations and this class provides static methods to create these.
 *
 * The types in this hierarchy do not implement {@link RSyntaxElement} - use
 * {@link WriteVariableSyntaxNode} instead.
 */
public abstract class WriteVariableNode extends RNode {

    public enum Mode {
        REGULAR,
        COPY,
        INVISIBLE
    }

    public abstract Object getName();

    public abstract RNode getRhs();

    public abstract void execute(VirtualFrame frame, Object value);

    /**
     * Variant for saving function arguments, i.e. from {@link RArguments} into the frame.
     */
    public static WriteVariableNode createArgSave(String name, RNode rhs) {
        if (FastROptions.InvisibleArgs.getBooleanValue()) {
            return WriteLocalFrameVariableNode.create(name, rhs, Mode.INVISIBLE);
        } else {
            return WriteLocalFrameVariableNode.create(name, rhs, Mode.REGULAR);
        }
    }

    /**
     * Variant for anonymous variables in the current frame.
     */
    public static WriteVariableNode createAnonymous(String name, RNode rhs, Mode mode) {
        return WriteLocalFrameVariableNode.create(name, rhs, mode);
    }

    /**
     * Variant for anonymous variables in either the current or a super frame..
     */
    public static WriteVariableNode createAnonymous(String name, RNode rhs, Mode mode, boolean isSuper) {
        if (isSuper) {
            return WriteSuperFrameVariableNode.create(name, rhs, mode);
        } else {
            return WriteLocalFrameVariableNode.create(name, rhs, mode);
        }
    }
}
