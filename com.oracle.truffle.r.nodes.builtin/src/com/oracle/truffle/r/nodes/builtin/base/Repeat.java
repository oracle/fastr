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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.PRIMITIVE;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RAttributeProfiles;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RFactor;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RNode;

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
public abstract class Repeat extends RBuiltinNode {

    protected abstract Object execute(RAbstractVector x, RAbstractIntVector times, int lengthOut, int each);

    @Child private Repeat repeatRecursive;

    private final ConditionProfile lengthOutOrTimes = ConditionProfile.createBinaryProfile();
    private final BranchProfile errorBranch = BranchProfile.create();
    private final ConditionProfile oneTimeGiven = ConditionProfile.createBinaryProfile();
    private final ConditionProfile replicateOnce = ConditionProfile.createBinaryProfile();
    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

    private Object repeatRecursive(RAbstractVector x, RAbstractIntVector times, int lengthOut, int each) {
        if (repeatRecursive == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            repeatRecursive = insert(RepeatNodeGen.create(new RNode[4], null, null));
        }
        return repeatRecursive.execute(x, times, lengthOut, each);
    }

    @Override
    public Object[] getDefaultParameterValues() {
        return new Object[]{RMissing.instance, 1, RRuntime.INT_NA, 1};
    }

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.toInteger(1);
        casts.firstIntegerWithWarning(2, RRuntime.INT_NA, "length.out");
        casts.toInteger(3);
    }

    protected boolean hasNames(RAbstractVector x) {
        return x.getNames(attrProfiles) != null;
    }

    private RError invalidTimes() {
        throw RError.error(this, RError.Message.INVALID_ARGUMENT, "times");
    }

    @Specialization(guards = {"x.getLength() == 1", "times.getLength() == 1", "each <= 1", "!hasNames(x)"})
    protected RAbstractVector repNoEachNoNamesSimple(RAbstractDoubleVector x, RAbstractIntVector times, int lengthOut, @SuppressWarnings("unused") int each) {
        int length = lengthOutOrTimes.profile(!RRuntime.isNA(lengthOut)) ? lengthOut : times.getDataAt(0);
        double[] data = new double[length];
        Arrays.fill(data, x.getDataAt(0));
        return RDataFactory.createDoubleVector(data, !RRuntime.isNA(x.getDataAt(0)));
    }

    @Specialization(guards = {"each > 1", "!hasNames(x)"})
    protected RAbstractVector repEachNoNames(RAbstractVector x, RAbstractIntVector times, int lengthOut, int each) {
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

    @Specialization(guards = {"each <= 1", "!hasNames(x)"})
    protected RAbstractVector repNoEachNoNames(RAbstractVector x, RAbstractIntVector times, int lengthOut, @SuppressWarnings("unused") int each) {
        if (lengthOutOrTimes.profile(!RRuntime.isNA(lengthOut))) {
            return handleLengthOut(x, lengthOut, true);
        } else {
            return handleTimes(x, times, true);
        }
    }

    @Specialization(guards = {"each > 1", "hasNames(x)"})
    protected RAbstractVector repEachNames(RAbstractVector x, RAbstractIntVector times, int lengthOut, int each) {
        if (times.getLength() > 1) {
            errorBranch.enter();
            throw invalidTimes();
        }
        RAbstractVector input = handleEach(x, each);
        RStringVector names = (RStringVector) handleEach(x.getNames(attrProfiles), each);
        if (lengthOutOrTimes.profile(!RRuntime.isNA(lengthOut))) {
            names = (RStringVector) handleLengthOut(names, lengthOut, false);
            RVector r = handleLengthOut(input, lengthOut, false);
            r.setNames(names);
            return r;
        } else {
            names = (RStringVector) handleTimes(names, times, false);
            RVector r = handleTimes(input, times, false);
            r.setNames(names);
            return r;
        }
    }

    @Specialization(guards = {"each <= 1", "hasNames(x)"})
    protected RAbstractVector repNoEachNames(RAbstractVector x, RAbstractIntVector times, int lengthOut, @SuppressWarnings("unused") int each) {
        if (lengthOutOrTimes.profile(!RRuntime.isNA(lengthOut))) {
            RStringVector names = (RStringVector) handleLengthOut(x.getNames(attrProfiles), lengthOut, true);
            RVector r = handleLengthOut(x, lengthOut, true);
            r.setNames(names);
            return r;
        } else {
            RStringVector names = (RStringVector) handleTimes(x.getNames(attrProfiles), times, true);
            RVector r = handleTimes(x, times, true);
            r.setNames(names);
            return r;
        }
    }

    @Specialization
    protected RAbstractContainer rep(RFactor x, RAbstractIntVector times, int lengthOut, int each) {
        RVector vec = (RVector) repeatRecursive(x.getVector(), times, lengthOut, each);
        vec.setAttr(RRuntime.LEVELS_ATTR_KEY, x.getLevels(attrProfiles));
        return RVector.setVectorClassAttr(vec, x.getClassAttr(attrProfiles), null, null);
    }

    /**
     * Prepare the input vector by replicating its elements.
     */
    private static RVector handleEach(RAbstractVector x, int each) {
        RVector r = x.createEmptySameType(x.getLength() * each, x.isComplete());
        for (int i = 0; i < x.getLength(); i++) {
            for (int j = i * each; j < (i + 1) * each; j++) {
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
            for (int i = 0; i < times.getLength(); i++) {
                resultLength += times.getDataAt(i);
            }
            // create and populate result vector
            RVector r = x.createEmptySameType(resultLength, x.isComplete());
            int wp = 0; // write pointer
            for (int i = 0; i < x.getLength(); i++) {
                for (int j = 0; j < times.getDataAt(i); ++j, ++wp) {
                    r.transferElementSameType(wp, x, i);
                }
            }
            return r;
        }
    }
}
