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

import java.util.*;
import java.util.regex.*;

import com.oracle.truffle.api.CompilerDirectives.*;
import com.oracle.truffle.api.dsl.*;
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
        protected void checkExtraArgs(byte ignoreCase, byte perl, byte fixed, byte useBytes, byte invert) {
            if (RRuntime.fromLogical(ignoreCase)) {
                notImplemented("ignoreCase");
            }
            if (RRuntime.fromLogical(perl)) {
                notImplemented("perl");
            }
            if (RRuntime.fromLogical(fixed)) {
                notImplemented("fixed");
            }
            if (RRuntime.fromLogical(useBytes)) {
                notImplemented("useBytes");
            }
            if (RRuntime.fromLogical(invert)) {
                notImplemented("invert");
            }
        }

        private void notImplemented(String arg) {
            throw RError.getGenericError(getEncapsulatingSourceSection(), arg + " arg not implemented");
        }
    }

    @RBuiltin(value = ".Internal.grep")
    public abstract static class Grep extends ExtraArgsChecker {

        @Specialization
        public RIntVector grep(String pattern, RAbstractStringVector vector, byte ignoreCase, byte value, byte perl, byte fixed, byte useBytes, byte invert) {
            controlVisibility();
            checkExtraArgs(ignoreCase, perl, fixed, useBytes, invert);
            if (RRuntime.fromLogical(value)) {
                throw RError.getGenericError(getEncapsulatingSourceSection(), "value == TRUE is not implemented");
            }
            int[] result = findAllIndexes(pattern, vector);
            if (result == null) {
                return RDataFactory.createEmptyIntVector();
            } else {
                return RDataFactory.createIntVector(result, RDataFactory.COMPLETE_VECTOR);
            }
        }

        protected static int[] findAllIndexes(String pattern, RAbstractStringVector vector) {
            int[] tmp = new int[vector.getLength()];
            int numMatches = 0;
            int ind = 0;
            for (int i = 0; i < vector.getLength(); i++) {
                if (findIndex(pattern, vector.getDataAt(i))) {
                    numMatches++;
                    tmp[ind++] = i + 1;
                }
            }
            if (numMatches == 0) {
                return null;
            } else if (numMatches == vector.getLength()) {
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

    @RBuiltin(value = ".Internal.grepl")
    public abstract static class GrepL extends ExtraArgsChecker {

        @Specialization
        public Object grep(String pattern, RAbstractStringVector vector, byte ignoreCase, byte perl, byte fixed, byte useBytes) {
            controlVisibility();
            checkExtraArgs(ignoreCase, perl, fixed, useBytes, RRuntime.LOGICAL_FALSE);
            byte[] data = new byte[vector.getLength()];
            for (int i = 0; i < vector.getLength(); i++) {
                data[i] = RRuntime.asLogical(Grep.findIndex(pattern, vector.getDataAt(i)));
            }
            return RDataFactory.createLogicalVector(data, RDataFactory.COMPLETE_VECTOR);
        }
    }

    @RBuiltin(".Internal.sub")
    public abstract static class Sub extends ExtraArgsChecker {

        @Specialization(order = 1)
        public String sub(String pattern, String replacement, String x, byte ignoreCase, byte perl, byte fixed, byte useBytes) {
            controlVisibility();
            checkExtraArgs(ignoreCase, perl, fixed, useBytes, RRuntime.LOGICAL_FALSE);
            return replaceMatch(pattern, replacement, x);
        }

        @Specialization(order = 10)
        public RStringVector sub(String pattern, String replacement, RStringVector vector, byte ignoreCase, byte perl, byte fixed, byte useBytes) {
            controlVisibility();
            checkExtraArgs(ignoreCase, perl, fixed, useBytes, RRuntime.LOGICAL_FALSE);
            return doSub(pattern, replacement, vector);
        }

        @Specialization(order = 12)
        public RStringVector sub(RStringVector pattern, String replacement, RStringVector vector, byte ignoreCase, byte perl, byte fixed, byte useBytes) {
            controlVisibility();
            checkExtraArgs(ignoreCase, perl, fixed, useBytes, RRuntime.LOGICAL_FALSE);
            // FIXME print a warning that only pattern[1] is used
            return doSub(pattern.getDataAt(0), replacement, vector);
        }

        @Specialization(order = 13)
        public RStringVector sub(String pattern, RStringVector replacement, RStringVector vector, byte ignoreCase, byte perl, byte fixed, byte useBytes) {
            controlVisibility();
            checkExtraArgs(ignoreCase, perl, fixed, useBytes, RRuntime.LOGICAL_FALSE);
            // FIXME print a warning that only replacement[1] is used
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

    @RBuiltin(".Internal.gsub")
    public abstract static class GSub extends Sub {

        @Specialization(order = 1)
        @Override
        public String sub(String pattern, String replacement, String x, byte ignoreCase, byte perl, byte fixed, byte useBytes) {
            controlVisibility();
            checkExtraArgs(ignoreCase, perl, fixed, useBytes, RRuntime.LOGICAL_FALSE);
            return replaceMatch(pattern, replacement, x);
        }

        @Specialization(order = 2)
        public String sub(RAbstractStringVector pattern, RAbstractStringVector replacement, RAbstractStringVector x, byte ignoreCase, byte perl, byte fixed, byte useBytes) {
            controlVisibility();
            checkExtraArgs(ignoreCase, perl, fixed, useBytes, RRuntime.LOGICAL_FALSE);
            // TODO print warnings that only the first element of each is used
            return replaceMatch(pattern.getDataAt(0), replacement.getDataAt(0), x.getDataAt(0));
        }

        @Override
        @SlowPath
        protected String replaceMatch(String pattern, String replacement, String input) {
            return input.replaceAll(pattern, replacement);
        }
    }

    @RBuiltin(value = ".Internal.regexpr")
    public abstract static class Regexp extends ExtraArgsChecker {

        @Specialization
        public Object regexp(String pattern, RAbstractStringVector vector, byte ignoreCase, byte perl, byte fixed, byte useBytes) {
            controlVisibility();
            checkExtraArgs(ignoreCase, perl, fixed, useBytes, RRuntime.LOGICAL_FALSE);
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

        @SlowPath
        public static Matcher getPatternMatcher(String pattern, String text) {
            return Pattern.compile(pattern).matcher(text);
        }
    }

    @RBuiltin(value = ".Internal.gregexpr")
    public abstract static class Gregexpr extends Regexp {

        @Specialization
        @Override
        public Object regexp(String pattern, RAbstractStringVector vector, byte ignoreCase, byte perl, byte fixed, byte useBytes) {
            controlVisibility();
            checkExtraArgs(ignoreCase, perl, fixed, useBytes, RRuntime.LOGICAL_FALSE);
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

}
