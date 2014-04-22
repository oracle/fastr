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

import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.runtime.data.*;

/**
 * Provide access to arguments contained in frames. This is a purely static class. It defines, by
 * means of slot offsets, where in a frame certain information is stored, such as the function
 * executed in the frame.
 */
public final class RArguments {

    public static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    private static final int INDEX_FUNCTION = 0;
    private static final int INDEX_ENCLOSING_FRAME = 1;
    private static final int INDEX_ARGUMENTS = 2;
    private static final int INDEX_NAMES = 3;

    private RArguments() {
    }

    public static RFunction getFunction(Frame frame) {
        return (RFunction) frame.getArguments()[INDEX_FUNCTION];
    }

    public static Object[] create() {
        return create(null, null, EMPTY_OBJECT_ARRAY);
    }

    public static Object[] create(RFunction functionObj) {
        return create(functionObj, EMPTY_OBJECT_ARRAY);
    }

    public static Object[] create(RFunction functionObj, Object[] evaluatedArgs) {
        return create(functionObj, functionObj.getEnclosingFrame(), evaluatedArgs);
    }

    public static Object[] create(RFunction functionObj, MaterializedFrame enclosingFrame, Object[] evaluatedArgs) {
        return create(functionObj, enclosingFrame, evaluatedArgs, EMPTY_OBJECT_ARRAY);
    }

    public static Object[] create(RFunction functionObj, MaterializedFrame enclosingFrame, Object[] evaluatedArgs, Object[] names) {
        return new Object[]{functionObj, enclosingFrame, evaluatedArgs, names};
    }

    public static Object[] getArgumentsArray(Frame frame) {
        return (Object[]) frame.getArguments()[INDEX_ARGUMENTS];
    }

    public static Object getArgument(Frame frame, int argIndex) {
        return getArgumentsArray(frame)[argIndex];
    }

    public static int getArgumentsLength(Frame frame) {
        return getArgumentsArray(frame).length;
    }

    public static MaterializedFrame getEnclosingFrame(Frame frame) {
        return (MaterializedFrame) frame.getArguments()[INDEX_ENCLOSING_FRAME];
    }

    public static Object[] getNames(Frame frame) {
        return (Object[]) frame.getArguments()[INDEX_NAMES];
    }

    public static void setEnclosingFrame(Frame frame, MaterializedFrame encl) {
        Object[] arguments = frame.getArguments();
        arguments[INDEX_ENCLOSING_FRAME] = encl;
        if (arguments[INDEX_FUNCTION] != null) {
            ((RFunction) arguments[INDEX_FUNCTION]).setEnclosingFrame(encl);
        }
    }
}
