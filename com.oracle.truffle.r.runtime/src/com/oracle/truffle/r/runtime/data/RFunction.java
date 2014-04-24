/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.frame.FrameInstance.*;
import com.oracle.truffle.api.impl.*;
import com.oracle.truffle.r.runtime.*;

/**
 * An instance of {@link RFunction} represents a function defined in R. The properties of a function
 * are as follows:
 * <ul>
 * <li>The {@link #name} is optional. It is given only for builtins.
 * <li>The {@link #target} represents the actually callable entry point to the function.
 * <li>Functions may represent builtins; this is indicated by the {@link #builtin} flag set to
 * {@code true}.
 * <li>The lexically enclosing environment of this function's definition is referenced by
 * {@link #enclosingFrame}.
 * </ul>
 */
public final class RFunction extends RScalar {

    private final String name;
    private final CallTarget target;
    private final boolean builtin;
    private MaterializedFrame enclosingFrame;

    public RFunction(String name, CallTarget target, boolean builtin, MaterializedFrame enclosingFrame) {
        this.name = name;
        this.target = target;
        this.builtin = builtin;
        this.enclosingFrame = enclosingFrame;
    }

    public RFunction(String name, CallTarget target, boolean builtin) {
        this(name, target, builtin, null);
    }

    public boolean isBuiltin() {
        return builtin;
    }

    public String getName() {
        return name;
    }

    public CallTarget getTarget() {
        return target;
    }

    public MaterializedFrame getEnclosingFrame() {
        return enclosingFrame;
    }

    public Object call(VirtualFrame frame, Object[] argsObject) {
        return DefaultCallNode.callProxy(MATERIALIZED_FRAME_NOTIFY, target, frame, argsObject);
    }

    public void setEnclosingFrame(MaterializedFrame frame) {
        this.enclosingFrame = frame;
    }

    private static final MaterializedFrameNotify MATERIALIZED_FRAME_NOTIFY = new MaterializedFrameNotify() {

        public FrameAccess getOutsideFrameAccess() {
            return FrameAccess.MATERIALIZE;
        }

        public void setOutsideFrameAccess(FrameAccess outsideFrameAccess) {
            Utils.fatalError("should not be called");
        }

    };

}
