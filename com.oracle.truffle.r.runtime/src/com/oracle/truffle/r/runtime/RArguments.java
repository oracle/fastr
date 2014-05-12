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

// @formatter:off
/**
 * Provide access to arguments contained in frames. This is a purely static class. It defines, by
 * means of slot offsets, where in a frame certain information is stored, such as the function
 * executed in the frame.
 *
 * The frame layout, depicted, is as follows:
 * <pre>
 *                            +-------------------+
 * INDEX_ENVIRONMENT       -> | REnvironment      |
 *                            +-------------------+
 * INDEX_FUNCTION          -> | RFunction         |
 *                            +-------------------+
 * INDEX_ENCLOSING_FRAME   -> | MaterializedFrame |
 *                            +-------------------+
 * INDEX_N_ARGS            -> | nArgs             |
 *                            +-------------------+
 * INDEX_N_NAMES           -> | nNames            |
 *                            +-------------------+
 * INDEX_ARGUMENTS         -> | arg_0             |
 *                            | arg_1             |
 *                            | ...               |
 *                            | arg_(nArgs-1)     |
 *                            +-------------------+
 * INDEX_ARGUMENTS + nArgs -> | name_0            |
 *                            | name_1            |
 *                            | ...               |
 *                            | name_(nNames-1)   |
 *                            +-------------------+
 * </pre>
 *
 * All frame elements should <b>always</b> be accessed through the getter and setter functions
 * defined in this class, as they provide a means of accessing the frame contents that is
 * transparent to layout changes.
 */
// @formatter:on
public final class RArguments {

    public static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];
    public static final String[] EMPTY_STRING_ARRAY = new String[0];

    private static final int INDEX_ENVIRONMENT = 0;
    private static final int INDEX_FUNCTION = 1;
    private static final int INDEX_ENCLOSING_FRAME = 2;
    private static final int INDEX_N_ARGS = 3;
    private static final int INDEX_N_NAMES = 4;
    private static final int INDEX_ARGUMENTS = 5;

    /**
     * At the least, the array contains the function, enclosing frame, and numbers of arguments and
     * names.
     */
    public static final int MINIMAL_ARRAY_LENGTH = 5;

    private RArguments() {
    }

    private static int getNArgs(Frame frame) {
        return (int) frame.getArguments()[INDEX_N_ARGS];
    }

    private static int getNNames(Frame frame) {
        return (int) frame.getArguments()[INDEX_N_NAMES];
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

    public static Object[] create(RFunction functionObj, Object[] evaluatedArgs, String[] names) {
        return create(null, functionObj, functionObj.getEnclosingFrame(), evaluatedArgs, names);
    }

    public static Object[] create(RFunction functionObj, MaterializedFrame enclosingFrame, Object[] evaluatedArgs) {
        return create(null, functionObj, enclosingFrame, evaluatedArgs, EMPTY_STRING_ARRAY);
    }

    public static Object[] create(REnvironment env, RFunction functionObj, MaterializedFrame enclosingFrame, Object[] evaluatedArgs, String[] names) {
        Object[] a = new Object[MINIMAL_ARRAY_LENGTH + evaluatedArgs.length + names.length];
        a[INDEX_ENVIRONMENT] = env;
        a[INDEX_FUNCTION] = functionObj;
        a[INDEX_ENCLOSING_FRAME] = enclosingFrame;
        a[INDEX_N_ARGS] = evaluatedArgs.length;
        a[INDEX_N_NAMES] = names.length;
        System.arraycopy(evaluatedArgs, 0, a, INDEX_ARGUMENTS, evaluatedArgs.length);
        System.arraycopy(names, 0, a, INDEX_ARGUMENTS + evaluatedArgs.length, names.length);
        return a;
    }

    public static REnvironment getEnvironment(Frame frame) {
        return (REnvironment) frame.getArguments()[INDEX_ENVIRONMENT];
    }

    public static RFunction getFunction(Frame frame) {
        return (RFunction) frame.getArguments()[INDEX_FUNCTION];
    }

    public static void copyArgumentsInto(Frame frame, Object[] target) {
        System.arraycopy(frame.getArguments(), INDEX_ARGUMENTS, target, 0, getNArgs(frame));
    }

    public static Object getArgument(Frame frame, int argIndex) {
        return frame.getArguments()[INDEX_ARGUMENTS + argIndex];
    }

    public static int getArgumentsLength(Frame frame) {
        return getNArgs(frame);
    }

    public static MaterializedFrame getEnclosingFrame(Frame frame) {
        return (MaterializedFrame) frame.getArguments()[INDEX_ENCLOSING_FRAME];
    }

    public static Object getName(Frame frame, int nameIndex) {
        return frame.getArguments()[INDEX_ARGUMENTS + getNArgs(frame) + nameIndex];
    }

    public static int getNamesLength(Frame frame) {
        return getNNames(frame);
    }

    public static void setEnvironment(Frame frame, REnvironment env) {
        frame.getArguments()[INDEX_ENVIRONMENT] = env;
    }

    public static void setEnclosingFrame(Frame frame, MaterializedFrame encl) {
        Object[] arguments = frame.getArguments();
        arguments[INDEX_ENCLOSING_FRAME] = encl;
        if (arguments[INDEX_FUNCTION] != null) {
            ((RFunction) arguments[INDEX_FUNCTION]).setEnclosingFrame(encl);
        }
    }

    /**
     * Support for the R {@code attach} function. Set {@code newEncl} as the new enclosing frame for
     * {@code frame} and set the enclosing frame for {@code newEncl} to the previous enclosing frame
     * for {@code frame}. assert {@code} does not denote a function.
     */
    public static void attachFrame(Frame frame, MaterializedFrame newEncl) {
        Object[] arguments = frame.getArguments();
        MaterializedFrame encl = (MaterializedFrame) arguments[INDEX_ENCLOSING_FRAME];
        Object[] newArguments = newEncl.getArguments();
        newArguments[INDEX_ENCLOSING_FRAME] = encl;
        arguments[INDEX_ENCLOSING_FRAME] = newEncl;
    }

    /**
     * Support for the R {@code detach} function. Set the enclosing frame for {@code frame} to the
     * enclosing frame of its current enclosing frame. assert {@code} does not denote a function.
     */
    public static void detachFrame(Frame frame) {
        Object[] arguments = frame.getArguments();
        MaterializedFrame encl = (MaterializedFrame) arguments[INDEX_ENCLOSING_FRAME];
        Object[] enclArguments = encl.getArguments();
        arguments[INDEX_ENCLOSING_FRAME] = enclArguments[INDEX_ENCLOSING_FRAME];
    }
}
