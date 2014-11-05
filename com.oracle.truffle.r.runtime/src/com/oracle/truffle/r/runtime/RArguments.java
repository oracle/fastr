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
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.env.*;

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
 * INDEX_CALL_SRC          -> | SourceSection     |
 *                            +-------------------+
 * INDEX_ENCLOSING_FRAME   -> | MaterializedFrame |
 *                            +-------------------+
 * INDEX_N_ARGS            -> | nArgs             |
 *                            +-------------------+
 * INDEX_DEPTH             -> | depth             |
 *                            +-------------------+
 * INDEX_IS_IRREGULAR      -> | isIrregular       |
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
 *
 * The INDEX_ENVIRONMENT slot is typically not set for frames associated with function evaluations,
 * because such environment instances are only created on demand. It is however, set for frames
 * associated with packages and the global environment.
 */
// @formatter:on
public final class RArguments {

    public static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];
    public static final String[] EMPTY_STRING_ARRAY = new String[0];

    private static final int INDEX_ENVIRONMENT = 0;
    private static final int INDEX_FUNCTION = 1;
    private static final int INDEX_CALL_SRC = 2;
    private static final int INDEX_ENCLOSING_FRAME = 3;
    private static final int INDEX_N_ARGS = 4;
    private static final int INDEX_DEPTH = 5;
    private static final int INDEX_IS_IRREGULAR = 6;
    private static final int INDEX_N_NAMES = 7;
    private static final int INDEX_ARGUMENTS = 8;

    private static final int S3_VAR_COUNT = 9;
    /*
     * These indices are relative to INDEX_ARGUMENTS + nArgs+ nNames
     */
    private static final int S3_INDEX_GENERIC = 0;
    private static final int S3_INDEX_CLASS = 1;
    private static final int S3_INDEX_METHOD = 2;
    private static final int S3_INDEX_CALL_ENV = 3;
    private static final int S3_INDEX_DEF_ENV = 4;
    private static final int S3_INDEX_GROUP = 5;

    /**
     * At the least, the array contains the function, enclosing frame, and numbers of arguments and
     * names.
     */
    public static final int MINIMAL_ARRAY_LENGTH = INDEX_N_NAMES + 1;

    private static final ValueProfile materializedFrameProfile = ValueProfile.createClassProfile();

    private RArguments() {
    }

    /**
     * Get the {@code arguments} checking for an "eval" frame. An eval can create a pseudo-call
     * where {@code arguments.length == 1} and the real {@code arguments} are at
     * {@code arguments[0]}. See {@code REngine}.
     */
    private static Object[] getArgumentsWithEvalCheck(Frame frame) {
        Object[] arguments = frame.getArguments();
        if (arguments.length == 1) {
            return materializedFrameProfile.profile((Frame) arguments[0]).getArguments();
        } else {
            return arguments;
        }

    }

    private static int getNArgs(Frame frame) {
        return (int) getArgumentsWithEvalCheck(frame)[INDEX_N_ARGS];
    }

    private static int getNNames(Frame frame) {
        return (int) getArgumentsWithEvalCheck(frame)[INDEX_N_NAMES];
    }

    private static int getS3StartIndex(Object[] args) {
        return INDEX_ARGUMENTS + (int) args[INDEX_N_ARGS] + (int) args[INDEX_N_NAMES];
    }

    private static void createHelper(Object[] a, REnvironment env, RFunction functionObj, SourceSection callSrc, int depth, MaterializedFrame enclosingFrame, Object[] evaluatedArgs, String[] names) {
        a[INDEX_ENVIRONMENT] = env;
        a[INDEX_FUNCTION] = functionObj;
        a[INDEX_CALL_SRC] = callSrc;
        a[INDEX_ENCLOSING_FRAME] = enclosingFrame;
        a[INDEX_DEPTH] = depth;
        a[INDEX_IS_IRREGULAR] = false;
        a[INDEX_N_ARGS] = evaluatedArgs.length;
        a[INDEX_N_NAMES] = names.length;
        copyArguments(evaluatedArgs, a, INDEX_ARGUMENTS);
        copyArguments(names, a, INDEX_ARGUMENTS + evaluatedArgs.length);
        assert envFunctionInvariant(a);
    }

    /**
     * This method is used instead of System.arraycopy because the arraycopy would be optimized too
     * late (after Truffle partial evaluation). At this late stage, there is no more information
     * about the finalness of array contents, and according to the Java spec array contents can
     * change at any point in time. Therefore, even though source is known at compile time, Graal
     * would have to be conservative and keep the array copy.
     */
    @ExplodeLoop
    private static void copyArguments(Object[] source, Object[] destination, int position) {
        for (int i = 0; i < source.length; i++) {
            destination[position + i] = source[i];
        }
    }

    private static boolean envFunctionInvariant(Object[] a) {
        return !(a[INDEX_ENVIRONMENT] == null && a[INDEX_FUNCTION] == null);
    }

    /**
     * A method for creating an uninitialized array, used only in very special situations as it
     * temporarily violates {@link #envFunctionInvariant}.
     */
    public static Object[] createUnitialized() {
        Object[] a = new Object[MINIMAL_ARRAY_LENGTH];
        a[INDEX_DEPTH] = 0;
        a[INDEX_IS_IRREGULAR] = false;
        return a;
    }

    public static Object[] create(RFunction functionObj, SourceSection callSrc, int depth) {
        return create(functionObj, callSrc, depth, EMPTY_OBJECT_ARRAY);
    }

    public static Object[] create(RFunction functionObj, SourceSection callSrc, int depth, Object[] evaluatedArgs) {
        return create(functionObj, callSrc, depth, functionObj.getEnclosingFrame(), evaluatedArgs);
    }

    public static Object[] createS3Args(RFunction functionObj, SourceSection callSrc, int depth, Object[] evaluatedArgs, String[] names) {
        return createS3Args(null, functionObj, callSrc, depth, functionObj.getEnclosingFrame(), evaluatedArgs, names);
    }

    public static Object[] create(RFunction functionObj, SourceSection callSrc, int depth, Object[] evaluatedArgs, String[] names) {
        return create(null, functionObj, callSrc, depth, functionObj.getEnclosingFrame(), evaluatedArgs, names);
    }

    public static Object[] create(RFunction functionObj, SourceSection callSrc, int depth, MaterializedFrame enclosingFrame, Object[] evaluatedArgs) {
        return create(null, functionObj, callSrc, depth, enclosingFrame, evaluatedArgs, EMPTY_STRING_ARRAY);
    }

    public static Object[] create(REnvironment env, RFunction functionObj, SourceSection callSrc, int depth, MaterializedFrame enclosingFrame, Object[] evaluatedArgs, String[] names) {
        Object[] a = new Object[MINIMAL_ARRAY_LENGTH + evaluatedArgs.length + names.length];
        createHelper(a, env, functionObj, callSrc, depth, enclosingFrame, evaluatedArgs, names);
        return a;
    }

    public static Object[] createS3Args(REnvironment env, RFunction functionObj, SourceSection callSrc, int depth, MaterializedFrame enclosingFrame, Object[] evaluatedArgs, String[] names) {
        Object[] a = new Object[MINIMAL_ARRAY_LENGTH + evaluatedArgs.length + names.length + S3_VAR_COUNT];
        createHelper(a, env, functionObj, callSrc, depth, enclosingFrame, evaluatedArgs, names);
        return a;
    }

    public static String getS3Generic(Frame frame) {
        Object[] args = getArgumentsWithEvalCheck(frame);
        int s3StartIndex = getS3StartIndex(args);
        assert (args.length > s3StartIndex);
        return (String) args[s3StartIndex + S3_INDEX_GENERIC];
    }

    public static void setS3Generic(Frame frame, final String generic) {
        Object[] args = getArgumentsWithEvalCheck(frame);
        int s3StartIndex = getS3StartIndex(args);
        assert (args.length > s3StartIndex);
        args[s3StartIndex + S3_INDEX_GENERIC] = generic;
    }

    public static RStringVector getS3Class(Frame frame) {
        Object[] args = getArgumentsWithEvalCheck(frame);
        int s3StartIndex = getS3StartIndex(args);
        assert (args.length > s3StartIndex);
        return (RStringVector) args[s3StartIndex + S3_INDEX_CLASS];
    }

    public static void setS3Class(Frame frame, final RStringVector klass) {
        Object[] args = getArgumentsWithEvalCheck(frame);
        int s3StartIndex = getS3StartIndex(args);
        assert (args.length > s3StartIndex);
        args[s3StartIndex + S3_INDEX_CLASS] = klass;
    }

    public static String getS3Method(Frame frame) {
        Object[] args = getArgumentsWithEvalCheck(frame);
        int s3StartIndex = getS3StartIndex(args);
        assert (args.length > s3StartIndex);
        return (String) args[s3StartIndex + S3_INDEX_METHOD];
    }

    public static void setS3Method(Frame frame, final String method) {
        Object[] args = getArgumentsWithEvalCheck(frame);
        int s3StartIndex = getS3StartIndex(args);
        assert (args.length > s3StartIndex);
        args[s3StartIndex + S3_INDEX_METHOD] = method;
    }

    public static Frame getS3DefEnv(Frame frame) {
        Object[] args = getArgumentsWithEvalCheck(frame);
        int s3StartIndex = getS3StartIndex(args);
        assert (args.length > s3StartIndex);
        return (Frame) args[s3StartIndex + S3_INDEX_DEF_ENV];
    }

    public static void setS3DefEnv(Frame frame, Frame defEnv) {
        Object[] args = getArgumentsWithEvalCheck(frame);
        int s3StartIndex = getS3StartIndex(args);
        assert (args.length > s3StartIndex);
        args[s3StartIndex + S3_INDEX_DEF_ENV] = defEnv;
    }

    public static Frame getS3CallEnv(Frame frame) {
        Object[] args = getArgumentsWithEvalCheck(frame);
        int s3StartIndex = getS3StartIndex(args);
        assert (args.length > s3StartIndex);
        return (Frame) args[s3StartIndex + S3_INDEX_CALL_ENV];
    }

    public static void setS3CallEnv(Frame frame, Frame callEnv) {
        Object[] args = getArgumentsWithEvalCheck(frame);
        int s3StartIndex = getS3StartIndex(args);
        assert (args.length > s3StartIndex);
        args[s3StartIndex + S3_INDEX_CALL_ENV] = callEnv;
    }

    public static String getS3Group(Frame frame) {
        Object[] args = getArgumentsWithEvalCheck(frame);
        int s3StartIndex = getS3StartIndex(args);
        assert (args.length > s3StartIndex);
        return (String) args[s3StartIndex + S3_INDEX_GROUP];
    }

    public static void setS3Group(Frame frame, final String group) {
        Object[] args = getArgumentsWithEvalCheck(frame);
        int s3StartIndex = getS3StartIndex(args);
        assert (args.length > s3StartIndex);
        args[s3StartIndex + S3_INDEX_GROUP] = group;
    }

    public static REnvironment getEnvironment(Frame frame) {
        return (REnvironment) getArgumentsWithEvalCheck(frame)[INDEX_ENVIRONMENT];
    }

    public static RFunction getFunction(Frame frame) {
        return (RFunction) getArgumentsWithEvalCheck(frame)[INDEX_FUNCTION];
    }

    public static SourceSection getCallSourceSection(Frame frame) {
        return (SourceSection) getArgumentsWithEvalCheck(frame)[INDEX_CALL_SRC];
    }

    public static int getDepth(Frame frame) {
        return (Integer) getArgumentsWithEvalCheck(frame)[INDEX_DEPTH];
    }

    public static boolean getIsIrregular(Frame frame) {
        return (boolean) getArgumentsWithEvalCheck(frame)[INDEX_IS_IRREGULAR];
    }

    public static void copyArgumentsInto(Frame frame, Object[] target) {
        System.arraycopy(getArgumentsWithEvalCheck(frame), INDEX_ARGUMENTS, target, 0, getNArgs(frame));
    }

    public static Object getArgument(Frame frame, int argIndex) {
        assert (argIndex >= 0 && argIndex < getNArgs(frame));
        return getArgumentsWithEvalCheck(frame)[INDEX_ARGUMENTS + argIndex];
    }

    /**
     * <b>Only to be called from AccessArgumentNode!</b>
     *
     * @param frame
     * @param argIndex
     * @param newValue
     */
    public static void setArgument(Frame frame, int argIndex, Object newValue) {
        assert (argIndex >= 0 && argIndex < getNArgs(frame));
        getArgumentsWithEvalCheck(frame)[INDEX_ARGUMENTS + argIndex] = newValue;
    }

    public static int getArgumentsLength(Frame frame) {
        return getNArgs(frame);
    }

    public static MaterializedFrame getEnclosingFrame(Frame frame) {
        return (MaterializedFrame) getArgumentsWithEvalCheck(frame)[INDEX_ENCLOSING_FRAME];
    }

    public static String getName(Frame frame, int nameIndex) {
        return (String) getArgumentsWithEvalCheck(frame)[INDEX_ARGUMENTS + getNArgs(frame) + nameIndex];
    }

    public static int getNamesLength(Frame frame) {
        return getNNames(frame);
    }

    public static void setEnvironment(Frame frame, REnvironment env) {
        getArgumentsWithEvalCheck(frame)[INDEX_ENVIRONMENT] = env;
    }

    /**
     * Explicitly set the function. Used by {@code REngine.eval}.
     */
    public static void setFunction(Frame frame, RFunction function) {
        getArgumentsWithEvalCheck(frame)[INDEX_FUNCTION] = function;
    }

    /**
     * Explicitly set the callSrc. Used by {@code REngine.eval}.
     */
    public static void setCallSourceSection(Frame frame, SourceSection callSrc) {
        getArgumentsWithEvalCheck(frame)[INDEX_CALL_SRC] = callSrc;
    }

    public static void setDepth(Frame frame, int depth) {
        getArgumentsWithEvalCheck(frame)[INDEX_DEPTH] = depth;
    }

    public static void setIsIrregular(Frame frame, boolean isIrregularFrame) {
        getArgumentsWithEvalCheck(frame)[INDEX_IS_IRREGULAR] = isIrregularFrame;
    }

    public static void setIsIrregular(Object[] arguments, boolean isIrregularFrame) {
        assert arguments.length > INDEX_ARGUMENTS;
        arguments[INDEX_IS_IRREGULAR] = isIrregularFrame;
    }

    public static void setEnclosingFrame(Frame frame, MaterializedFrame encl) {
        Object[] arguments = getArgumentsWithEvalCheck(frame);
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
        Object[] arguments = getArgumentsWithEvalCheck(frame);
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
        Object[] arguments = getArgumentsWithEvalCheck(frame);
        MaterializedFrame encl = (MaterializedFrame) arguments[INDEX_ENCLOSING_FRAME];
        Object[] enclArguments = encl.getArguments();
        arguments[INDEX_ENCLOSING_FRAME] = enclArguments[INDEX_ENCLOSING_FRAME];
    }
}
