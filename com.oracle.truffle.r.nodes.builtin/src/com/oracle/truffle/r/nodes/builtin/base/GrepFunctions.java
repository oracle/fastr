/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;
import java.util.regex.*;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

/**
 * {@code grep} in all its variants. Nothing in here merits being Truffle optimized.
 */
public class GrepFunctions {
    public abstract static class CommonCodeAdapter extends RBuiltinNode {

        private final BranchProfile errorProfile = BranchProfile.create();

        /**
         * Temporary method that handles the check for the arguments that are common to the majority
         * of the functions, that we don't yet implement. If any of the arguments are {@code true},
         * then an NYI error will be thrown (in the first one). If any of the arguments do not
         * apply, pass {@link RRuntime#LOGICAL_FALSE}.
         */
        protected void checkExtraArgs(byte ignoreCase, byte perl, byte fixed, byte useBytes, byte invert) {
            checkNotImplemented(RRuntime.fromLogical(ignoreCase), "ignoreCase", true);
            checkNotImplemented(RRuntime.fromLogical(perl), "perl", true);
            checkNotImplemented(RRuntime.fromLogical(fixed), "fixed", true);
            checkNotImplemented(RRuntime.fromLogical(useBytes), "useBytes", true);
            checkNotImplemented(RRuntime.fromLogical(invert), "invert", true);
        }

        /**
         * Temporary check for the {@code value} argument, which is only applicable to {@code grep}
         * and {@code agrep} (so not included in {@code checkExtraArgs}.
         *
         * @param value
         */
        protected void valueCheck(byte value) {
            if (RRuntime.fromLogical(value)) {
                errorProfile.enter();
                throw RError.nyi(getEncapsulatingSourceSection(), "value == true is not implemented");
            }
        }

        protected void checkNotImplemented(boolean condition, String arg, boolean b) {
            if (condition) {
                errorProfile.enter();
                throw RError.nyi(getEncapsulatingSourceSection(), arg + " == " + b + " not implemented");
            }
        }

        protected int[] trimIntResult(int[] tmp, int numMatches, int vecLength) {
            if (numMatches == 0) {
                return null;
            } else if (numMatches == vecLength) {
                return tmp;
            } else {
                // trim array to the appropriate size
                int[] result = new int[numMatches];
                for (int i = 0; i < result.length; i++) {
                    result[i] = tmp[i];
                }
                return result;
            }
        }

        protected String[] trimStringResult(String[] tmp, int numMatches, int vecLength) {
            if (numMatches == 0) {
                return null;
            } else if (numMatches == vecLength) {
                return tmp;
            } else {
                // trim array to the appropriate size
                String[] result = new String[numMatches];
                for (int i = 0; i < result.length; i++) {
                    result[i] = tmp[i];
                }
                return result;
            }
        }

        @SuppressWarnings("unused")
        protected boolean isFixed(String patternArg, RAbstractStringVector vector, byte ignoreCase, byte value, byte perl, byte fixed, byte useBytes, byte invert) {
            return RRuntime.fromLogical(fixed);
        }

        @SuppressWarnings("unused")
        protected boolean isValue(String patternArg, RAbstractStringVector vector, byte ignoreCase, byte value, byte perl, byte fixed, byte useBytes, byte invert) {
            return RRuntime.fromLogical(value);
        }
    }

    @RBuiltin(name = "grep", kind = INTERNAL, parameterNames = {"pattern", "x", "ignore.case", "perl", "value", "fixed", "useBytes", "invert"})
    public abstract static class Grep extends CommonCodeAdapter {

        @Specialization(guards = "!isValue")
        @TruffleBoundary
        protected RIntVector grepValueFalse(String patternArg, RAbstractStringVector vector, byte ignoreCase, byte value, byte perl, byte fixed, byte useBytes, byte invert) {
            controlVisibility();
            checkExtraArgs(ignoreCase, perl, RRuntime.LOGICAL_FALSE, useBytes, invert);
            String pattern = fixed == RRuntime.LOGICAL_TRUE ? patternArg : RegExp.checkPreDefinedClasses(patternArg);
            int[] result = findAllIndexes(pattern, vector, fixed);
            if (result == null) {
                return RDataFactory.createEmptyIntVector();
            } else {
                return RDataFactory.createIntVector(result, RDataFactory.COMPLETE_VECTOR);
            }
        }

        protected int[] findAllIndexes(String pattern, RAbstractStringVector vector, byte fixed) {
            int[] tmp = new int[vector.getLength()];
            int numMatches = 0;
            int ind = 0;
            for (int i = 0; i < vector.getLength(); i++) {
                String text = vector.getDataAt(i);
                if (fixed == RRuntime.LOGICAL_TRUE ? text.contains(pattern) : findIndex(pattern, text)) {
                    numMatches++;
                    tmp[ind++] = i + 1;
                }
            }
            return trimIntResult(tmp, numMatches, vector.getLength());
        }

        protected static boolean findIndex(String pattern, String text) {
            Matcher m = Regexp.getPatternMatcher(pattern, text);
            if (m.find()) {
                return true;
            } else {
                return false;
            }
        }

        @Specialization(guards = "isValue")
        @TruffleBoundary
        protected RStringVector grepValueTrue(String patternArg, RAbstractStringVector vector, byte ignoreCase, byte value, byte perl, byte fixed, byte useBytes, byte invert) {
            controlVisibility();
            checkExtraArgs(ignoreCase, perl, RRuntime.LOGICAL_FALSE, useBytes, invert);
            String pattern = fixed == RRuntime.LOGICAL_TRUE ? patternArg : RegExp.checkPreDefinedClasses(patternArg);
            String[] result = findAllMatches(pattern, vector, fixed);
            if (result == null) {
                return RDataFactory.createEmptyStringVector();
            } else {
                return RDataFactory.createStringVector(result, RDataFactory.COMPLETE_VECTOR);
            }
        }

        protected String[] findAllMatches(String pattern, RAbstractStringVector vector, byte fixed) {
            String[] tmp = new String[vector.getLength()];
            int numMatches = 0;
            int ind = 0;
            for (int i = 0; i < vector.getLength(); i++) {
                String text = vector.getDataAt(i);
                String match;
                if (fixed == RRuntime.LOGICAL_TRUE) {
                    match = text.contains(pattern) ? text : null;
                } else {
                    match = findMatch(pattern, text);
                }
                if (match != null) {
                    numMatches++;
                    tmp[ind++] = match;
                }
            }
            return trimStringResult(tmp, numMatches, vector.getLength());
        }

        protected static String findMatch(String pattern, String text) {
            Matcher m = Regexp.getPatternMatcher(pattern, text);
            if (m.find()) {
                return m.group();
            } else {
                return null;
            }
        }

    }

    @RBuiltin(name = "grepl", kind = INTERNAL, parameterNames = {"pattern", "x", "ignore.case", "value", "perl", "fixed", "useBytes", "invert"})
    // invert is passed but is always FALSE
    public abstract static class GrepL extends CommonCodeAdapter {

        @Specialization(guards = "!isFixed")
        @TruffleBoundary
        @SuppressWarnings("unused")
        protected Object grepl(String patternArg, RAbstractStringVector vector, byte ignoreCase, byte value, byte perl, byte fixed, byte useBytes, byte invert) {
            controlVisibility();
            checkExtraArgs(ignoreCase, perl, RRuntime.LOGICAL_FALSE, useBytes, RRuntime.LOGICAL_FALSE);
            String pattern = RegExp.checkPreDefinedClasses(patternArg);
            byte[] data = new byte[vector.getLength()];
            for (int i = 0; i < vector.getLength(); i++) {
                data[i] = RRuntime.asLogical(Grep.findIndex(pattern, vector.getDataAt(i)));
            }
            return RDataFactory.createLogicalVector(data, RDataFactory.COMPLETE_VECTOR);
        }

        @Specialization(guards = "isFixed")
        @TruffleBoundary
        @SuppressWarnings("unused")
        protected Object greplFixed(String pattern, RAbstractStringVector vector, byte ignoreCase, byte value, byte perl, byte fixed, byte useBytes, byte invert) {
            controlVisibility();
            checkExtraArgs(ignoreCase, perl, RRuntime.LOGICAL_FALSE, useBytes, RRuntime.LOGICAL_FALSE);
            byte[] data = new byte[vector.getLength()];
            for (int i = 0; i < vector.getLength(); i++) {
                data[i] = RRuntime.asLogical(vector.getDataAt(i).contains(pattern));
            }
            return RDataFactory.createLogicalVector(data, RDataFactory.COMPLETE_VECTOR);
        }

    }

    @RBuiltin(name = "sub", kind = INTERNAL, parameterNames = {"pattern", "replacement", "x", "ignore.case", "perl", "fixed", "useBytes"})
    public abstract static class Sub extends CommonCodeAdapter {

        @Specialization
        @TruffleBoundary
        protected String sub(String patternArg, String replacement, String x, byte ignoreCase, byte perl, byte fixed, byte useBytes) {
            controlVisibility();
            checkExtraArgs(ignoreCase, perl, fixed, useBytes, RRuntime.LOGICAL_FALSE);
            String pattern = RegExp.checkPreDefinedClasses(patternArg);
            return replaceMatch(pattern, replacement, x);
        }

        @Specialization
        @TruffleBoundary
        protected RStringVector sub(String patternArg, String replacement, RStringVector vector, byte ignoreCase, byte perl, byte fixed, byte useBytes) {
            controlVisibility();
            checkExtraArgs(ignoreCase, perl, fixed, useBytes, RRuntime.LOGICAL_FALSE);
            String pattern = RegExp.checkPreDefinedClasses(patternArg);
            return doSub(pattern, replacement, vector);
        }

        @Specialization
        @TruffleBoundary
        protected RStringVector sub(RStringVector patternArg, String replacement, RStringVector vector, byte ignoreCase, byte perl, byte fixed, byte useBytes) {
            controlVisibility();
            checkExtraArgs(ignoreCase, perl, fixed, useBytes, RRuntime.LOGICAL_FALSE);
            // FIXME print a warning that only pattern[1] is used
            String pattern = RegExp.checkPreDefinedClasses(patternArg.getDataAt(0));
            return doSub(pattern, replacement, vector);
        }

        @Specialization
        @TruffleBoundary
        protected RStringVector sub(String patternArg, RStringVector replacement, RStringVector vector, byte ignoreCase, byte perl, byte fixed, byte useBytes) {
            controlVisibility();
            checkExtraArgs(ignoreCase, perl, fixed, useBytes, RRuntime.LOGICAL_FALSE);
            // FIXME print a warning that only replacement[1] is used
            String pattern = RegExp.checkPreDefinedClasses(patternArg);
            return doSub(pattern, replacement.getDataAt(0), vector);
        }

        protected RStringVector doSub(String pattern, String replacement, RStringVector vector) {
            int len = vector.getLength();
            String[] result = new String[len];
            for (int i = 0; i < len; i++) {
                String input = vector.getDataAt(i);
                result[i] = replaceMatch(pattern, replacement, input);
            }
            return RDataFactory.createStringVector(result, vector.isComplete());
        }

        protected String replaceMatch(String pattern, String replacement, String input) {
            return input.replaceFirst(pattern, replacement);
        }
    }

    @RBuiltin(name = "gsub", kind = INTERNAL, parameterNames = {"pattern", "replacement", "x", "ignore.case", "perl", "fixed", "useBytes"})
    public abstract static class GSub extends Sub {

        @Specialization
        @TruffleBoundary
        @Override
        protected String sub(String patternArg, String replacement, String x, byte ignoreCase, byte perl, byte fixed, byte useBytes) {
            controlVisibility();
            checkExtraArgs(ignoreCase, perl, fixed, useBytes, RRuntime.LOGICAL_FALSE);
            String pattern = RegExp.checkPreDefinedClasses(patternArg);
            return replaceMatch(pattern, replacement, x);
        }

        @Specialization
        @TruffleBoundary
        protected String sub(RAbstractStringVector patternArg, RAbstractStringVector replacement, RAbstractStringVector x, byte ignoreCase, byte perl, byte fixed, byte useBytes) {
            controlVisibility();
            checkExtraArgs(ignoreCase, perl, fixed, useBytes, RRuntime.LOGICAL_FALSE);
            String pattern = RegExp.checkPreDefinedClasses(patternArg.getDataAt(0));
            // TODO print warnings that only the first element of each is used
            return replaceMatch(pattern, replacement.getDataAt(0), x.getDataAt(0));
        }

        @Override
        @TruffleBoundary
        protected String replaceMatch(String pattern, String replacement, String input) {
            return input.replaceAll(pattern, replacement);
        }
    }

    @RBuiltin(name = "regexpr", kind = INTERNAL, parameterNames = {"pattern", "text", "ignore.case", "perl", "fixed", "useBytes"})
    public abstract static class Regexp extends CommonCodeAdapter {

        @Specialization
        @TruffleBoundary
        protected Object regexp(RAbstractStringVector patternArg, RAbstractStringVector vector, byte ignoreCase, byte perl, byte fixed, byte useBytes) {
            controlVisibility();
            checkExtraArgs(ignoreCase, perl, fixed, useBytes, RRuntime.LOGICAL_FALSE);
            String pattern = RegExp.checkPreDefinedClasses(patternArg.getDataAt(0));
            int[] result = new int[vector.getLength()];
            for (int i = 0; i < vector.getLength(); i++) {
                result[i] = findIndex(pattern, vector.getDataAt(i)).get(0);
            }
            return RDataFactory.createIntVector(result, RDataFactory.COMPLETE_VECTOR);
        }

        protected static List<Integer> findIndex(String pattern, String text) {
            Matcher m = getPatternMatcher(pattern, text);
            List<Integer> list = new ArrayList<>();
            while (m.find()) {
                // R starts counting at index 1
                list.add(m.start() + 1);
            }
            if (list.size() > 0) {
                return list;
            }
            list.add(-1);
            return list;
        }

        @TruffleBoundary
        public static Matcher getPatternMatcher(String pattern, String text) {
            return Pattern.compile(pattern).matcher(text);
        }
    }

    @RBuiltin(name = "gregexpr", kind = INTERNAL, parameterNames = {"pattern", "text", "ignore.case", "perl", "fixed", "useBytes"})
    public abstract static class Gregexpr extends Regexp {

        @Specialization
        @TruffleBoundary
        @Override
        protected Object regexp(RAbstractStringVector patternArg, RAbstractStringVector vector, byte ignoreCase, byte perl, byte fixed, byte useBytes) {
            controlVisibility();
            checkExtraArgs(ignoreCase, perl, fixed, useBytes, RRuntime.LOGICAL_FALSE);
            String pattern = RegExp.checkPreDefinedClasses(patternArg.getDataAt(0));
            Object[] result = new Object[vector.getLength()];
            for (int i = 0; i < vector.getLength(); i++) {
                int[] data = toIntArray(findIndex(pattern, vector.getDataAt(i)));
                result[i] = RDataFactory.createIntVector(data, RDataFactory.COMPLETE_VECTOR);
            }
            return RDataFactory.createList(result);
        }

        private static int[] toIntArray(List<Integer> list) {
            int[] arr = new int[list.size()];
            for (int i = 0; i < list.size(); i++) {
                arr[i] = list.get(i);
            }
            return arr;
        }
    }

    @RBuiltin(name = "agrep", kind = INTERNAL, parameterNames = {"pattern", "x", "max.distance", "costs", "ignore.case", "value", "fixed", "useBytes"})
    public abstract static class AGrep extends CommonCodeAdapter {

        @SuppressWarnings("unused")
        @Specialization
        @TruffleBoundary
        protected Object aGrep(RAbstractStringVector patternArg, RAbstractStringVector vector, byte ignoreCase, byte value, RIntVector costs, RDoubleVector bounds, byte useBytes, byte fixed) {
            // TODO implement properly, this only supports strict equality!
            controlVisibility();
            checkExtraArgs(ignoreCase, RRuntime.LOGICAL_FALSE, RRuntime.LOGICAL_FALSE, useBytes, RRuntime.LOGICAL_FALSE);
            valueCheck(value);
            checkNotImplemented(!RRuntime.fromLogical(fixed), "fixed", false);
            int[] tmp = new int[vector.getLength()];
            int numMatches = 0;
            String pattern = patternArg.getDataAt(0);
            for (int i = 0; i < vector.getLength(); i++) {
                if (pattern.equals(vector.getDataAt(i))) {
                    tmp[i] = i + 1;
                    numMatches++;
                }
            }
            tmp = trimIntResult(tmp, numMatches, tmp.length);
            if (tmp == null) {
                return RDataFactory.createEmptyIntVector();
            } else {
                return RDataFactory.createIntVector(tmp, RDataFactory.COMPLETE_VECTOR);
            }
        }
    }

    @RBuiltin(name = "agrepl", kind = INTERNAL, parameterNames = {"pattern", "x", "max.distance", "costs", "ignore.case", "fixed", "useBytes"})
    public abstract static class AGrepL extends CommonCodeAdapter {

        @SuppressWarnings("unused")
        @Specialization
        @TruffleBoundary
        protected Object aGrep(RAbstractStringVector patternArg, RAbstractStringVector vector, byte ignoreCase, RIntVector costs, RDoubleVector bounds, byte useBytes, byte fixed) {
            // TODO implement properly, this only supports strict equality!
            controlVisibility();
            checkExtraArgs(ignoreCase, RRuntime.LOGICAL_FALSE, RRuntime.LOGICAL_FALSE, useBytes, RRuntime.LOGICAL_FALSE);
            byte[] data = new byte[vector.getLength()];
            String pattern = patternArg.getDataAt(0);
            for (int i = 0; i < vector.getLength(); i++) {
                data[i] = RRuntime.asLogical(pattern.equals(vector.getDataAt(i)));
            }
            return RDataFactory.createLogicalVector(data, RDataFactory.COMPLETE_VECTOR);
        }
    }

}
