/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.r.runtime.ops.na.*;

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

        protected boolean isTrue(byte fixed) {
            return RRuntime.fromLogical(fixed);
        }

    }

    @RBuiltin(name = "grep", kind = INTERNAL, parameterNames = {"pattern", "x", "ignore.case", "perl", "value", "fixed", "useBytes", "invert"})
    public abstract static class Grep extends CommonCodeAdapter {

        public static boolean isNAAndPerl(RAbstractStringVector vector, byte perl) {
            return vector.getLength() == 1 && RRuntime.isNA(vector.getDataAt(0)) && perl == RRuntime.LOGICAL_TRUE;
        }

        @Specialization(guards = "!isTrue(value)")
        @TruffleBoundary
        protected RIntVector grepValueFalse(RAbstractStringVector patternArgVec, RAbstractStringVector vector, byte ignoreCase, @SuppressWarnings("unused") byte value, byte perl, byte fixed,
                        byte useBytes, byte invert) {
            controlVisibility();
            // HACK to finesse lack of perl==TRUE in utils::localeToCharset (on Linux)
            if (isNAAndPerl(vector, perl)) {
                return RDataFactory.createEmptyIntVector();
            }
            String patternArg = patternArgVec.getDataAt(0);
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

        @Specialization(guards = "isTrue(value)")
        @TruffleBoundary
        protected RStringVector grepValueTrue(RAbstractStringVector patternArgVec, RAbstractStringVector vector, byte ignoreCase, @SuppressWarnings("unused") byte value, byte perl, byte fixed,
                        byte useBytes, byte invert) {
            controlVisibility();
            checkExtraArgs(ignoreCase, perl, RRuntime.LOGICAL_FALSE, useBytes, invert);
            String patternArg = patternArgVec.getDataAt(0);
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
                return text;
            } else {
                return null;
            }
        }

    }

    @RBuiltin(name = "grepl", kind = INTERNAL, parameterNames = {"pattern", "x", "ignore.case", "value", "perl", "fixed", "useBytes", "invert"})
    // invert is passed but is always FALSE
    public abstract static class GrepL extends CommonCodeAdapter {

        @Specialization(guards = "!isTrue(fixed)")
        @TruffleBoundary
        @SuppressWarnings("unused")
        protected Object grepl(RAbstractStringVector patternArgVec, RAbstractStringVector vector, byte ignoreCase, byte value, byte perl, byte fixed, byte useBytes, byte invert) {
            controlVisibility();
            checkExtraArgs(ignoreCase, perl, RRuntime.LOGICAL_FALSE, useBytes, RRuntime.LOGICAL_FALSE);
            String pattern = RegExp.checkPreDefinedClasses(patternArgVec.getDataAt(0));
            byte[] data = new byte[vector.getLength()];
            for (int i = 0; i < vector.getLength(); i++) {
                data[i] = RRuntime.asLogical(Grep.findIndex(pattern, vector.getDataAt(i)));
            }
            return RDataFactory.createLogicalVector(data, RDataFactory.COMPLETE_VECTOR);
        }

        @Specialization(guards = "isTrue(fixed)")
        @TruffleBoundary
        @SuppressWarnings("unused")
        protected Object greplFixed(RAbstractStringVector patternArgVec, RAbstractStringVector vector, byte ignoreCase, byte value, byte perl, byte fixed, byte useBytes, byte invert) {
            controlVisibility();
            checkExtraArgs(ignoreCase, perl, RRuntime.LOGICAL_FALSE, useBytes, RRuntime.LOGICAL_FALSE);
            byte[] data = new byte[vector.getLength()];
            for (int i = 0; i < vector.getLength(); i++) {
                data[i] = RRuntime.asLogical(vector.getDataAt(i).contains(patternArgVec.getDataAt(0)));
            }
            return RDataFactory.createLogicalVector(data, RDataFactory.COMPLETE_VECTOR);
        }

    }

    protected abstract static class SubAdapter extends CommonCodeAdapter {
        private final ConditionProfile fixedProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile gsubProfile = ConditionProfile.createBinaryProfile();
        private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

        protected RStringVector doSub(RAbstractStringVector patternArgVec, RAbstractStringVector replacementVec, RAbstractStringVector vector, boolean fixed, boolean gsub) {
            // FIXME print a warning that only pattern[1] is used
            String pattern;
            if (fixedProfile.profile((fixed))) {
                pattern = patternArgVec.getDataAt(0);
            } else {
                pattern = RegExp.checkPreDefinedClasses(patternArgVec.getDataAt(0));
            }
            String replacement = replacementVec.getDataAt(0);
            int len = vector.getLength();
            String[] result = new String[len];
            for (int i = 0; i < len; i++) {
                String input = vector.getDataAt(i);
                String value;
                if (fixedProfile.profile((fixed))) {
                    if (gsubProfile.profile(gsub)) {
                        value = input.replace(pattern, replacement);
                    } else {
                        int ix = input.indexOf(pattern);
                        value = ix < 0 ? pattern : input.substring(0, ix) + replacement + input.substring(ix + 1);
                    }
                } else {
                    replacement = convertGroups(replacement);
                    if (gsubProfile.profile(gsub)) {
                        value = input.replaceAll(pattern, replacement);
                    } else {
                        value = input.replaceFirst(pattern, replacement);
                    }
                }
                result[i] = value;
            }
            RStringVector ret = RDataFactory.createStringVector(result, vector.isComplete());
            ret.copyAttributesFrom(attrProfiles, vector);
            return ret;
        }

        @TruffleBoundary
        private static String convertGroups(String value) {
            int x = 0;
            int groupStart = groupIndex(value, x);
            if (groupStart < 0) {
                return value;
            }
            StringBuffer result = new StringBuffer();
            while (groupStart >= 0) {
                result.append(value.substring(x, groupStart));
                result.append('$');
                result.append(value.charAt(groupStart + 1));
                x = groupStart + 2;
                groupStart = groupIndex(value, x);
            }
            result.append(value.substring(x));
            return result.toString();
        }

        private static int groupIndex(String value, int x) {
            int ix = value.indexOf('\\', x);
            if (ix < 0 || ix >= value.length() - 2) {
                return ix;
            } else {
                char ch = value.charAt(ix + 1);
                if (Character.isDigit(ch)) {
                    return ix;
                } else {
                    return -1;
                }
            }
        }

    }

    @RBuiltin(name = "sub", kind = INTERNAL, parameterNames = {"pattern", "replacement", "x", "ignore.case", "perl", "fixed", "useBytes"})
    public abstract static class Sub extends SubAdapter {

        @Specialization
        @TruffleBoundary
        protected RStringVector subRegexp(RAbstractStringVector patternArgVec, RAbstractStringVector replacementVec, RAbstractStringVector x, byte ignoreCase, byte perl, byte fixed, byte useBytes) {
            controlVisibility();
            checkExtraArgs(ignoreCase, perl, fixed, useBytes, RRuntime.LOGICAL_FALSE);
            return doSub(patternArgVec, replacementVec, x, RRuntime.fromLogical(fixed), false);
        }
    }

    @RBuiltin(name = "gsub", kind = INTERNAL, parameterNames = {"pattern", "replacement", "x", "ignore.case", "perl", "fixed", "useBytes"})
    public abstract static class GSub extends SubAdapter {

        @Specialization
        @TruffleBoundary
        protected RStringVector gsub(RAbstractStringVector patternArgVec, RAbstractStringVector replacementVec, RAbstractStringVector x, byte ignoreCase, byte perl, byte fixed, byte useBytes) {
            controlVisibility();
            checkExtraArgs(ignoreCase, perl, fixed, useBytes, RRuntime.LOGICAL_FALSE);
            return doSub(patternArgVec, replacementVec, x, RRuntime.fromLogical(fixed), true);
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
            // TODO implement completely; this is a very basic implementation for fixed=TRUE only.
            controlVisibility();
            checkExtraArgs(ignoreCase, RRuntime.LOGICAL_FALSE, RRuntime.LOGICAL_FALSE, useBytes, RRuntime.LOGICAL_FALSE);
            valueCheck(value);
            checkNotImplemented(!RRuntime.fromLogical(fixed), "fixed", false);
            int[] tmp = new int[vector.getLength()];
            int numMatches = 0;
            String pattern = patternArg.getDataAt(0);
            long maxDistance = Math.round(pattern.length() * bounds.getDataAt(0));
            for (int i = 0; i < vector.getLength(); i++) {
                int ld = ld(pattern, vector.getDataAt(i));
                if (ld <= maxDistance) {
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

        // Taken from:
        // http://people.cs.pitt.edu/~kirk/cs1501/Pruhs/Spring2006/assignments/editdistance/Levenshtein%20Distance.htm

        private static int ld(String s, String t) {
            int[][] d; // matrix
            int n; // length of s
            int m; // length of t
            int i; // iterates through s
            int j; // iterates through t
            char si; // ith character of s
            char tj; // jth character of t
            int cost; // cost

            // Step 1

            n = s.length();
            m = t.length();
            if (n == 0) {
                return m;
            }
            if (m == 0) {
                return n;
            }
            d = new int[n + 1][m + 1];

            // Step 2

            for (i = 0; i <= n; i++) {
                d[i][0] = i;
            }

            for (j = 0; j <= m; j++) {
                d[0][j] = j;
            }

            // Step 3

            for (i = 1; i <= n; i++) {
                si = s.charAt(i - 1);
                // Step 4
                for (j = 1; j <= m; j++) {
                    tj = t.charAt(j - 1);
                    // Step 5
                    if (si == tj) {
                        cost = 0;
                    } else {
                        cost = 1;
                    }
                    // Step 6
                    d[i][j] = min3(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + cost);

                }

            }

            // Step 7

            return d[n][m];

        }

        private static int min3(int a, int b, int c) {
            int mi;
            mi = a;
            if (b < mi) {
                mi = b;
            }
            if (c < mi) {
                mi = c;
            }
            return mi;
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

    @RBuiltin(name = "strsplit", kind = INTERNAL, parameterNames = {"x", "split", "fixed", "perl", "useBytes"})
    public abstract static class Strsplit extends CommonCodeAdapter {

        private final NACheck na = NACheck.create();
        private final ConditionProfile emptySplitProfile = ConditionProfile.createBinaryProfile();
        private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

        @Specialization
        protected RList split(RAbstractStringVector x, RAbstractStringVector split, byte fixed, byte perl, byte useBytes) {
            controlVisibility();
            checkExtraArgs(RRuntime.LOGICAL_FALSE, perl, RRuntime.LOGICAL_FALSE, useBytes, RRuntime.LOGICAL_FALSE);
            RStringVector[] result = new RStringVector[x.getLength()];
            na.enable(x);
            String[] splits = new String[split.getLength()];
            for (int i = 0; i < splits.length; i++) {
                splits[i] = fixed == RRuntime.LOGICAL_TRUE ? split.getDataAt(i) : RegExp.checkPreDefinedClasses(split.getDataAt(i));
            }
            for (int i = 0; i < x.getLength(); ++i) {
                String data = x.getDataAt(i);
                String currentSplit = splits[i % splits.length];
                if (emptySplitProfile.profile(currentSplit.isEmpty())) {
                    result[i] = na.check(data) ? RDataFactory.createNAStringVector() : emptySplitIntl(data);
                } else {
                    result[i] = na.check(data) ? RDataFactory.createNAStringVector() : splitIntl(data, currentSplit);
                }
            }
            RList ret = RDataFactory.createList(result);
            if (x.getNames(attrProfiles) != null) {
                ret.copyNamesFrom(attrProfiles, x);
            }
            return ret;
        }

        @TruffleBoundary
        private static RStringVector splitIntl(String input, String separator) {
            assert !RRuntime.isNA(input);
            return RDataFactory.createStringVector(input.split(separator), true);
        }

        private static RStringVector emptySplitIntl(String input) {
            assert !RRuntime.isNA(input);
            String[] result = new String[input.length()];
            for (int i = 0; i < input.length(); i++) {
                result[i] = new String(new char[]{input.charAt(i)});
            }
            return RDataFactory.createStringVector(result, true);
        }
    }

}
