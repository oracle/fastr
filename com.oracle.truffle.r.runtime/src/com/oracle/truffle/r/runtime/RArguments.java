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
package com.oracle.truffle.r.runtime;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.runtime.data.*;

public final class RArguments extends Arguments {

    public static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];
    private final RFunction function;
    private MaterializedFrame enclosingFrame;
    private final Object[] arguments;
    private final Object[] names;

    private RArguments(RFunction function, MaterializedFrame enclosingFrame, Object[] arguments, Object[] names) {
        this.function = function;
        this.enclosingFrame = enclosingFrame;
        this.arguments = arguments;
        this.names = names;
    }

    public RFunction getFunction() {
        return function;
    }

    public Object[] getArgumentsArray() {
        return arguments;
    }

    public static RArguments get(Frame frame) {
        return frame.getArguments(RArguments.class);
    }

    public static RArguments create() {
        return create(null, null, EMPTY_OBJECT_ARRAY);
    }

    public static RArguments create(RFunction functionObj) {
        return create(functionObj, EMPTY_OBJECT_ARRAY);
    }

    public static RArguments create(RFunction functionObj, Object[] evaluatedArgs) {
        return create(functionObj, functionObj.getEnclosingFrame(), evaluatedArgs);
    }

    public static RArguments create(RFunction functionObj, MaterializedFrame enclosingFrame, Object[] evaluatedArgs) {
        return create(functionObj, enclosingFrame, evaluatedArgs, EMPTY_OBJECT_ARRAY);
    }

    public static RArguments create(RFunction functionObj, MaterializedFrame enclosingFrame, Object[] evaluatedArgs, Object[] names) {
        return new RArguments(functionObj, enclosingFrame, evaluatedArgs, names);
    }

    public Object getArgument(int argIndex) {
        return arguments[argIndex];
    }

    public int getLength() {
        return arguments.length;
    }

    public MaterializedFrame getEnclosingFrame() {
        return enclosingFrame;
    }

    public Object[] getNames() {
        return names;
    }

    public void setEnclosingFrame(MaterializedFrame frame) {
        this.enclosingFrame = frame;
        this.function.setEnclosingFrame(frame);
    }
}
