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

import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

public class GrepFunctions {
    /**
     * Temporary adapter class that handles the check for all the arguments that we don't yet
     * implement.
     */
    public abstract static class ExtraArgsChecker extends RBuiltinNode {

        private final BranchProfile errorProfile = new BranchProfile();

        protected void checkExtraArgs(byte ignoreCase, byte perl, byte fixed, byte useBytes, byte invert) {
            checkNotImplemented(RRuntime.fromLogical(ignoreCase), "ignoreCase", true);
            checkNotImplemented(RRuntime.fromLogical(perl), "perl", true);
            checkNotImplemented(RRuntime.fromLogical(fixed), "fixed", true);
            checkNotImplemented(RRuntime.fromLogical(useBytes), "useBytes", true);
            checkNotImplemented(RRuntime.fromLogical(invert), "invert", true);
        }

        protected void valueCheck(byte value) {
            if (RRuntime.fromLogical(value)) {
                errorProfile.enter();
                throw RError.nyi(getEncapsulatingSourceSection(), "value == true is not implemented");
            }
        }

        @SlowPath
        protected void checkNotImplemented(boolean condition, String arg, boolean b) {
            if (condition) {
                errorProfile.enter();
                throw RError.nyi(getEncapsulatingSourceSection(), arg + " == " + b + " not implemented");
            }
        }

        protected int[] trimResult(int[] tmp, int numMatches, int vecLength) {
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
    }

    @RBuiltin(name = "grep", kind = INTERNAL, parameterNames = {"pattern", "x", "ignore.case", "perl", "value", "fixed", "useBytes", "invert"})
    public abstract static class Grep extends ExtraArgsChecker {

        @Override
        public RNode[] getParameterValues() {
            return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance), ConstantNode.create(RRuntime.LOGICAL_FALSE),
                            ConstantNode.create(RRuntime.LOGICAL_FALSE), ConstantNode.create(RRuntime.LOGICAL_FALSE), ConstantNode.create(RRuntime.LOGICAL_FALSE),
                            ConstantNode.create(RRuntime.LOGICAL_FALSE), ConstantNode.create(RRuntime.LOGICAL_FALSE)};
        }

        @Specialization
        protected RIntVector grep(String patternArg, RAbstractStringVector vector, byte ignoreCase, byte value, byte perl, byte fixed, byte useBytes, byte invert) {
            controlVisibility();
            checkExtraArgs(ignoreCase, perl, fixed, useBytes, invert);
            valueCheck(value);
            String pattern = RegExp.checkPreDefinedClasses(patternArg);
            int[] result = findAllIndexes(pattern, vector);
            if (result == null) {
                return RDataFactory.createEmptyIntVector();
            } else {
                return RDataFactory.createIntVector(result, RDataFactory.COMPLETE_VECTOR);
            }
        }

        protected int[] findAllIndexes(String pattern, RAbstractStringVector vector) {
            int[] tmp = new int[vector.getLength()];
            int numMatches = 0;
            int ind = 0;
            for (int i = 0; i < vector.getLength(); i++) {
                if (findIndex(pattern, vector.getDataAt(i))) {
                    numMatches++;
                    tmp[ind++] = i + 1;
                }
            }
            return trimResult(tmp, numMatches, vector.getLength());
        }

        @SlowPath
        protected static boolean findIndex(String pattern, String text) {
            Matcher m = Regexp.getPatternMatcher(pattern, text);
            if (m.find()) {
                return true;
            } else {
                return false;
            }
        }
    }

    @RBuiltin(name = "grepl", kind = INTERNAL, parameterNames = {"pattern", "x", "ignore.case", "value", "perl", "fixed", "useBytes", "invert"})
    public abstract static class GrepL extends ExtraArgsChecker {

        @Override
        public RNode[] getParameterValues() {
            return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance), ConstantNode.create(RRuntime.LOGICAL_FALSE),
                            ConstantNode.create(RRuntime.LOGICAL_FALSE), ConstantNode.create(RRuntime.LOGICAL_FALSE), ConstantNode.create(RRuntime.LOGICAL_FALSE),
                            ConstantNode.create(RRuntime.LOGICAL_FALSE), ConstantNode.create(RRuntime.LOGICAL_FALSE),};
        }

        @Specialization
        @SuppressWarnings("unused")
        protected Object grep(String patternArg, RAbstractStringVector vector, byte ignoreCase, byte value, byte perl, byte fixed, byte useBytes, byte invert) {
            controlVisibility();
            checkExtraArgs(ignoreCase, perl, fixed, useBytes, RRuntime.LOGICAL_FALSE);
            String pattern = RegExp.checkPreDefinedClasses(patternArg);
            byte[] data = new byte[vector.getLength()];
            for (int i = 0; i < vector.getLength(); i++) {
                data[i] = RRuntime.asLogical(Grep.findIndex(pattern, vector.getDataAt(i)));
            }
            return RDataFactory.createLogicalVector(data, RDataFactory.COMPLETE_VECTOR);
        }
    }

    @RBuiltin(name = "sub", kind = INTERNAL, parameterNames = {"pattern", "replacement", "x", "ignore.case", "perl", "fixed", "useBytes"})
    public abstract static class Sub extends ExtraArgsChecker {

        @Override
        public RNode[] getParameterValues() {
            return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance), ConstantNode.create(RRuntime.LOGICAL_FALSE),
                            ConstantNode.create(RRuntime.LOGICAL_FALSE), ConstantNode.create(RRuntime.LOGICAL_FALSE), ConstantNode.create(RRuntime.LOGICAL_FALSE)};
        }

        @Specialization
        protected String sub(String patternArg, String replacement, String x, byte ignoreCase, byte perl, byte fixed, byte useBytes) {
            controlVisibility();
            checkExtraArgs(ignoreCase, perl, fixed, useBytes, RRuntime.LOGICAL_FALSE);
            String pattern = RegExp.checkPreDefinedClasses(patternArg);
            return replaceMatch(pattern, replacement, x);
        }

        @Specialization
        protected RStringVector sub(String patternArg, String replacement, RStringVector vector, byte ignoreCase, byte perl, byte fixed, byte useBytes) {
            controlVisibility();
            checkExtraArgs(ignoreCase, perl, fixed, useBytes, RRuntime.LOGICAL_FALSE);
            String pattern = RegExp.checkPreDefinedClasses(patternArg);
            return doSub(pattern, replacement, vector);
        }

        @Specialization
        protected RStringVector sub(RStringVector patternArg, String replacement, RStringVector vector, byte ignoreCase, byte perl, byte fixed, byte useBytes) {
            controlVisibility();
            checkExtraArgs(ignoreCase, perl, fixed, useBytes, RRuntime.LOGICAL_FALSE);
            // FIXME print a warning that only pattern[1] is used
            String pattern = RegExp.checkPreDefinedClasses(patternArg.getDataAt(0));
            return doSub(pattern, replacement, vector);
        }

        @Specialization
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

        @Override
        public RNode[] getParameterValues() {
            return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance), ConstantNode.create(RRuntime.LOGICAL_FALSE),
                            ConstantNode.create(RRuntime.LOGICAL_FALSE), ConstantNode.create(RRuntime.LOGICAL_FALSE), ConstantNode.create(RRuntime.LOGICAL_FALSE)};
        }

        @Specialization
        @Override
        protected String sub(String patternArg, String replacement, String x, byte ignoreCase, byte perl, byte fixed, byte useBytes) {
            controlVisibility();
            checkExtraArgs(ignoreCase, perl, fixed, useBytes, RRuntime.LOGICAL_FALSE);
            String pattern = RegExp.checkPreDefinedClasses(patternArg);
            return replaceMatch(pattern, replacement, x);
        }

        @Specialization
        protected String sub(RAbstractStringVector patternArg, RAbstractStringVector replacement, RAbstractStringVector x, byte ignoreCase, byte perl, byte fixed, byte useBytes) {
            controlVisibility();
            checkExtraArgs(ignoreCase, perl, fixed, useBytes, RRuntime.LOGICAL_FALSE);
            String pattern = RegExp.checkPreDefinedClasses(patternArg.getDataAt(0));
            // TODO print warnings that only the first element of each is used
            return replaceMatch(pattern, replacement.getDataAt(0), x.getDataAt(0));
        }

        @Override
        @SlowPath
        protected String replaceMatch(String pattern, String replacement, String input) {
            return input.replaceAll(pattern, replacement);
        }
    }

    @RBuiltin(name = "regexpr", kind = INTERNAL, parameterNames = {"pattern", "text", "ignore.case", "perl", "fixed", "useBytes"})
    public abstract static class Regexp extends ExtraArgsChecker {

        @Override
        public RNode[] getParameterValues() {
            return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance), ConstantNode.create(RRuntime.LOGICAL_FALSE),
                            ConstantNode.create(RRuntime.LOGICAL_FALSE), ConstantNode.create(RRuntime.LOGICAL_FALSE), ConstantNode.create(RRuntime.LOGICAL_FALSE)};
        }

        @Specialization
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

        @SlowPath
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

        @SlowPath
        public static Matcher getPatternMatcher(String pattern, String text) {
            return Pattern.compile(pattern).matcher(text);
        }
    }

    @RBuiltin(name = "gregexpr", kind = INTERNAL, parameterNames = {"pattern", "text", "ignore.case", "perl", "fixed", "useBytes"})
    public abstract static class Gregexpr extends Regexp {

        @Override
        public RNode[] getParameterValues() {
            return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance), ConstantNode.create(RRuntime.LOGICAL_FALSE),
                            ConstantNode.create(RRuntime.LOGICAL_FALSE), ConstantNode.create(RRuntime.LOGICAL_FALSE), ConstantNode.create(RRuntime.LOGICAL_FALSE)};
        }

        @Specialization
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
    public abstract static class AGrep extends ExtraArgsChecker {

        @Override
        public RNode[] getParameterValues() {
            return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance), ConstantNode.create(0.1d), ConstantNode.create(RNull.instance),
                            ConstantNode.create(RRuntime.LOGICAL_FALSE), ConstantNode.create(RRuntime.LOGICAL_FALSE), ConstantNode.create(RRuntime.LOGICAL_TRUE),
                            ConstantNode.create(RRuntime.LOGICAL_FALSE)};
        }

        @SuppressWarnings("unused")
        @Specialization
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
            tmp = trimResult(tmp, numMatches, tmp.length);
            if (tmp == null) {
                return RDataFactory.createEmptyIntVector();
            } else {
                return RDataFactory.createIntVector(tmp, RDataFactory.COMPLETE_VECTOR);
            }
        }
    }

    @RBuiltin(name = "agrepl", kind = INTERNAL, parameterNames = {"pattern", "x", "max.distance", "costs", "ignore.case", "fixed", "useBytes"})
    public abstract static class AGrepL extends ExtraArgsChecker {

        @Override
        public RNode[] getParameterValues() {
            return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance), ConstantNode.create(0.1d), ConstantNode.create(RNull.instance),
                            ConstantNode.create(RRuntime.LOGICAL_FALSE), ConstantNode.create(RRuntime.LOGICAL_TRUE), ConstantNode.create(RRuntime.LOGICAL_FALSE)};
        }

        @SuppressWarnings("unused")
        @Specialization
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
