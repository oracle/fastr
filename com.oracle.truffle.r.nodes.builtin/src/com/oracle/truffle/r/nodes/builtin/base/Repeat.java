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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

/**
 * The {@code rep} builtin works as follows.
 * <ol>
 * <li>If {@code each} is greater than one, all elements of {@code x} are first replicated
 * {@code each} times.
 * <li>If {@code length.out} is given, the result of the first step is truncated or extended as
 * required. In this case, {@code times} is ignored.
 * <li>If {@code length.out} is not given, {@code times} is regarded:
 * <ul>
 * <li>If {@code times} is a one-element vector, the result of the first step is replicated
 * {@code times} times.
 * <li>If {@code times} is a vector longer than one, and {@code each} is greater than one, an error
 * is issued.
 * <li>If {@code times} is a vector longer than one, and {@code each} is one, and {@code times} is
 * as long as {@code x}, each element of {@code x} is given the number of times indicated by the
 * value at the same index of {@code times}. If {@code times} has a different length, an error is
 * issued.
 * </ul>
 * </ol>
 */
@RBuiltin(name = "rep", kind = PRIMITIVE, parameterNames = {"x", "times", "length.out", "each"})
@GenerateNodeFactory
public abstract class Repeat extends RBuiltinNode {

    protected abstract Object execute(VirtualFrame frame, RAbstractVector x, RAbstractIntVector times, int lengthOut, int each);

    @Child private Repeat repeatRecursive;

    private final ConditionProfile lengthOutOrTimes = ConditionProfile.createBinaryProfile();
    private final BranchProfile errorBranch = BranchProfile.create();
    private final ConditionProfile oneTimeGiven = ConditionProfile.createBinaryProfile();
    private final ConditionProfile replicateOnce = ConditionProfile.createBinaryProfile();

    private Object repeatRecursive(VirtualFrame frame, RAbstractVector x, RAbstractIntVector times, int lengthOut, int each) {
        if (repeatRecursive == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            repeatRecursive = insert(RepeatFactory.create(new RNode[4], getBuiltin(), getSuppliedArgsNames()));
        }
        return repeatRecursive.execute(frame, x, times, lengthOut, each);
    }

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(1), ConstantNode.create(RRuntime.INT_NA), ConstantNode.create(1)};
    }

    @CreateCast("arguments")
    protected RNode[] castArguments(RNode[] arguments) {
        // times is at index 1; length.out, at 2; each, at 3
        arguments[1] = CastIntegerNodeGen.create(arguments[1], true, false, false);
        arguments[2] = FirstIntNodeGen.create(CastIntegerNodeGen.create(arguments[2], true, false, false));
        arguments[3] = CastIntegerNodeGen.create(arguments[3], true, false, false);
        return arguments;
    }

    @SuppressWarnings("unused")
    protected static boolean eachGreaterOne(RAbstractVector x, RAbstractIntVector times, int lengthOut, int each) {
        return each > 1;
    }

    @SuppressWarnings("unused")
    protected static boolean hasNames(RAbstractVector x, RAbstractIntVector times, int lengthOut, int each) {
        return x.getNames() != RNull.instance;
    }

    private RError invalidTimes() {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_ARGUMENT, "times");
    }

    @Specialization(guards = {"eachGreaterOne", "!hasNames"})
    public RAbstractVector repEachNoNames(RAbstractVector x, RAbstractIntVector times, int lengthOut, int each) {
        if (times.getLength() > 1) {
            errorBranch.enter();
            throw invalidTimes();
        }
        RAbstractVector input = handleEach(x, each);
        if (lengthOutOrTimes.profile(!RRuntime.isNA(lengthOut))) {
            return handleLengthOut(input, lengthOut, false);
        } else {
            return handleTimes(input, times, false);
        }
    }

    @Specialization(guards = {"!eachGreaterOne", "!hasNames"})
    public RAbstractVector repNoEachNoNames(RAbstractVector x, RAbstractIntVector times, int lengthOut, @SuppressWarnings("unused") int each) {
        if (lengthOutOrTimes.profile(!RRuntime.isNA(lengthOut))) {
            return handleLengthOut(x, lengthOut, true);
        } else {
            return handleTimes(x, times, true);
        }
    }

    @Specialization(guards = {"eachGreaterOne", "hasNames"})
    public RAbstractVector repEachNames(RAbstractVector x, RAbstractIntVector times, int lengthOut, int each) {
        if (times.getLength() > 1) {
            errorBranch.enter();
            throw invalidTimes();
        }
        RAbstractVector input = handleEach(x, each);
        RAbstractVector names = handleEach((RAbstractVector) x.getNames(), each);
        if (lengthOutOrTimes.profile(!RRuntime.isNA(lengthOut))) {
            names = handleLengthOut(names, lengthOut, false);
            RVector r = handleLengthOut(input, lengthOut, false);
            r.setNames(names);
            return r;
        } else {
            names = handleTimes(names, times, false);
            RVector r = handleTimes(input, times, false);
            r.setNames(names);
            return r;
        }
    }

    @Specialization(guards = {"!eachGreaterOne", "hasNames"})
    public RAbstractVector repNoEachNames(RAbstractVector x, RAbstractIntVector times, int lengthOut, @SuppressWarnings("unused") int each) {
        if (lengthOutOrTimes.profile(!RRuntime.isNA(lengthOut))) {
            RAbstractVector names = handleLengthOut((RAbstractVector) x.getNames(), lengthOut, true);
            RVector r = handleLengthOut(x, lengthOut, true);
            r.setNames(names);
            return r;
        } else {
            RAbstractVector names = handleTimes((RAbstractVector) x.getNames(), times, true);
            RVector r = handleTimes(x, times, true);
            r.setNames(names);
            return r;
        }
    }

    @Specialization
    public RAbstractContainer rep(VirtualFrame frame, RFactor x, RAbstractIntVector times, int lengthOut, int each) {
        RVector vec = (RVector) repeatRecursive(frame, x.getVector(), times, lengthOut, each);
        vec.setLevels(x.getLevels());
        return RVector.setVectorClassAttr(vec, x.getClassAttr(), null, null);
    }

    /**
     * Prepare the input vector by replicating its elements.
     */
    private static RVector handleEach(RAbstractVector x, int each) {
        RVector r = x.createEmptySameType(x.getLength() * each, x.isComplete());
        for (int i = 0; i < x.getLength(); ++i) {
            for (int j = i * each; j < (i + 1) * each; ++j) {
                r.transferElementSameType(j, x, i);
            }
        }
        return r;
    }

    /**
     * Extend or truncate the vector to a specified length.
     */
    private static RVector handleLengthOut(RAbstractVector x, int lengthOut, boolean copyIfSameSize) {
        if (x.getLength() == lengthOut) {
            return (RVector) (copyIfSameSize ? x.copy() : x);
        }
        return x.copyResized(lengthOut, false);
    }

    /**
     * Replicate the vector a given number of times.
     */
    private RVector handleTimes(RAbstractVector x, RAbstractIntVector times, boolean copyIfSameSize) {
        if (oneTimeGiven.profile(times.getLength() == 1)) {
            // only one times value is given
            final int howManyTimes = times.getDataAt(0);
            if (replicateOnce.profile(howManyTimes == 1)) {
                return (RVector) (copyIfSameSize ? x.copy() : x);
            } else {
                return x.copyResized(x.getLength() * howManyTimes, false);
            }
        } else {
            // times is a vector with several elements
            if (x.getLength() != times.getLength()) {
                errorBranch.enter();
                invalidTimes();
            }
            // iterate once over the times vector to determine result vector size
            int resultLength = 0;
            for (int i = 0; i < times.getLength(); ++i) {
                resultLength += times.getDataAt(i);
            }
            // create and populate result vector
            RVector r = x.createEmptySameType(resultLength, x.isComplete());
            int wp = 0; // write pointer
            for (int i = 0; i < x.getLength(); ++i) {
                for (int j = 0; j < times.getDataAt(i); ++j, ++wp) {
                    r.transferElementSameType(wp, x, i);
                }
            }
            return r;
        }
    }

}
