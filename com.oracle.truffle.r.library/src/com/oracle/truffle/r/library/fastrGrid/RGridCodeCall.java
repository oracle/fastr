/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.library.fastrGrid;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.nodes.builtin.RInternalCodeBuiltinNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RInternalCode;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;

/**
 * Allows to call arbitrary R function from {@code fastrGrid.R} source.
 */
class RGridCodeCall extends Node {
    private static final FrameDescriptor emptyFrameDescriptor = new FrameDescriptor("<fastrGrid.R tmp frame>");
    @Child private RInternalCodeBuiltinNode child;

    RGridCodeCall(String functionName) {
        child = new RInternalCodeBuiltinNode(RContext.getInstance(), "grid", RInternalCode.loadSourceRelativeTo(GPar.class, "fastrGrid.R"), functionName);
    }

    static {
        FrameSlotChangeMonitor.initializeFunctionFrameDescriptor("<fastrGrid.R tmp frame>", emptyFrameDescriptor);
    }

    public Object call(Object arg1) {
        return execute(new RArgsValuesAndNames(new Object[]{arg1}, ArgumentsSignature.empty(1)));
    }

    public Object execute(RArgsValuesAndNames args) {
        Object[] dummyFrameArgs = RArguments.createUnitialized();
        VirtualFrame dummyFrame = Truffle.getRuntime().createVirtualFrame(dummyFrameArgs, emptyFrameDescriptor);
        return child.call(dummyFrame, args);
    }
}
