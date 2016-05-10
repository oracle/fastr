/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2015, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.INTERNAL;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RegExp;
import com.oracle.truffle.r.runtime.data.RAttributeProfiles;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.ffi.PCRERFFI;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

/**
 * {@code grep} in all its variants. No usages in the general case merits being Truffle optimized,
 * so everything is behind {@link TruffleBoundary}. It is possible that some special cases might
 * show up on a hot path and be worthy of a custom specialization.
 * <p>
 * TODO implement all the options, in particular perl support for all functions.
 * <p>
 * A note on {@code useBytes}. We are currently ignoring this option completely. It's all related to
 * locales and multi-byte character representations of non-ASCII locales. Since Java represents
 * Unicode directly in strings and characters, it's not entirely clear what we should do but, since
 * we are generally ignoring this issue everywhere in the code base, we are effectively assuming
 * ASCII.
 * <p>
 * Parts of this code, notably the perl support, were translated from GnuR grep.c.
 */
public class GrepFunctions {
    public abstract static class CommonCodeAdapter extends RBuiltinNode {

        /**
         * This profile is needed to satisfy API requirements.
         */
        protected final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

        /**
         * Temporary method that handles the check for the arguments that are common to the majority
         * of the functions, that we don't yet implement. If any of the arguments are {@code true},
         * then an NYI error will be thrown (in the first one). If any of the arguments do not
         * apply, pass {@link RRuntime#LOGICAL_FALSE}.
         */
        protected void checkExtraArgs(byte ignoreCase, byte perl, byte fixed, @SuppressWarnings("unused") byte useBytes, byte invert) {
            checkNotImplemented(RRuntime.fromLogical(ignoreCase), "ignoreCase", true);
            checkNotImplemented(RRuntime.fromLogical(perl), "perl", true);
            checkNotImplemented(RRuntime.fromLogical(fixed), "fixed", true);
            // We just ignore useBytes
            // checkNotImplemented(RRuntime.fromLogical(useBytes), "useBytes", true);
            checkNotImplemented(RRuntime.fromLogical(invert), "invert", true);
        }

        protected void checkCaseFixed(boolean ignoreCase, boolean fixed) {
            if (ignoreCase && fixed) {
                RError.warning(this, RError.Message.ARGUMENT_IGNORED, "ignore.case = TRUE");
            }
        }

        protected boolean checkPerlFixed(boolean perl, boolean fixed) {
            if (fixed && perl) {
                RError.warning(this, RError.Message.ARGUMENT_IGNORED, "perl = TRUE");
                return false;
            } else {
                return perl;
            }
        }

        protected String checkLength(RAbstractStringVector arg, String name) {
            if (arg.getLength() < 1) {
                throw RError.error(this, RError.Message.INVALID_ARGUMENT, name);
            } else if (arg.getLength() > 1) {
                RError.warning(this, RError.Message.ARGUMENT_ONLY_FIRST, name);
            }
            return arg.getDataAt(0);
        }

        /**
         * Temporary check for the {@code value} argument, which is only applicable to {@code grep}
         * and {@code agrep} (so not included in {@code checkExtraArgs}.
         *
         * @param value
         */
        protected void valueCheck(byte value) {
            if (RRuntime.fromLogical(value)) {
                throw RError.nyi(this, "value == true");
            }
        }

        protected void checkNotImplemented(boolean condition, String arg, boolean b) {
            if (condition) {
                throw RError.nyi(this, arg + " == " + b);
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

        protected boolean isTrue(byte fixed) {
            return RRuntime.fromLogical(fixed);
        }

        protected RStringVector allStringNAResult(int len) {
            String[] naData = new String[len];
            for (int i = 0; i < len; i++) {
                naData[i] = RRuntime.STRING_NA;
            }
            return RDataFactory.createStringVector(naData, RDataFactory.INCOMPLETE_VECTOR);
        }

        protected RIntVector allIntNAResult(int len) {
            int[] naData = new int[len];
            for (int i = 0; i < len; i++) {
                naData[i] = RRuntime.INT_NA;
            }
            return RDataFactory.createIntVector(naData, RDataFactory.INCOMPLETE_VECTOR);
        }
    }

    private abstract static class GrepAdapter extends CommonCodeAdapter {
        protected Object doGrep(RAbstractStringVector patternArgVec, RAbstractStringVector vector, byte ignoreCaseLogical, byte valueLogical, byte perlLogical, byte fixedLogical,
                        @SuppressWarnings("unused") byte useBytes, byte invertLogical, boolean grepl) {
            boolean value = RRuntime.fromLogical(valueLogical);
            boolean invert = RRuntime.fromLogical(invertLogical);
            boolean perl = RRuntime.fromLogical(perlLogical);
            boolean ignoreCase = RRuntime.fromLogical(ignoreCaseLogical);
            boolean fixed = RRuntime.fromLogical(fixedLogical);
            perl = checkPerlFixed(RRuntime.fromLogical(perlLogical), fixed);
            checkCaseFixed(ignoreCase, fixed);

            String pattern = checkLength(patternArgVec, "pattern");
            int len = vector.getLength();
            if (RRuntime.isNA(pattern)) {
                return value ? allStringNAResult(len) : allIntNAResult(len);
            }
            boolean[] matches = new boolean[len];
            if (fixed && !perl) {
                // TODO case
                if (!fixed) {
                    pattern = RegExp.checkPreDefinedClasses(pattern);
                }
                findAllMatches(matches, pattern, vector, fixed, ignoreCase);
            } else {
                int cflags = ignoreCase ? PCRERFFI.CASELESS : 0;
                long tables = RFFIFactory.getRFFI().getPCRERFFI().maketables();
                PCRERFFI.Result pcre = RFFIFactory.getRFFI().getPCRERFFI().compile(pattern, cflags, tables);
                if (pcre.result == 0) {
                    // TODO output warning if pcre.errorMessage not NULL
                    throw RError.error(this, RError.Message.INVALID_REGEXP, pattern);
                }
                // TODO pcre_study for vectors > 10 ? (cf GnuR)
                int[] ovector = new int[30];
                for (int i = 0; i < len; i++) {
                    String text = vector.getDataAt(i);
                    if (!RRuntime.isNA(text)) {
                        if (RFFIFactory.getRFFI().getPCRERFFI().exec(pcre.result, 0, text, 0, 0, ovector) >= 0) {
                            matches[i] = true;
                        }
                    }
                }
            }

            if (grepl) {
                byte[] data = new byte[len];
                for (int i = 0; i < len; i++) {
                    data[i] = RRuntime.asLogical(matches[i]);
                }
                return RDataFactory.createLogicalVector(data, RDataFactory.COMPLETE_VECTOR);
            }

            int nmatches = 0;
            for (int i = 0; i < len; i++) {
                if (invert ^ matches[i]) {
                    nmatches++;
                }
            }

            if (nmatches == 0) {
                return value ? RDataFactory.createEmptyStringVector() : RDataFactory.createEmptyIntVector();
            } else {
                if (value) {
                    RStringVector oldNames = vector.getNames(attrProfiles);
                    String[] newNames = null;
                    if (oldNames != null) {
                        newNames = new String[nmatches];
                    }
                    String[] data = new String[nmatches];
                    int j = 0;
                    for (int i = 0; i < len; i++) {
                        if (invert ^ matches[i]) {
                            if (newNames != null) {
                                newNames[j] = oldNames.getDataAt(i);
                            }
                            data[j++] = vector.getDataAt(i);
                        }
                    }
                    return RDataFactory.createStringVector(data, RDataFactory.COMPLETE_VECTOR, newNames == null ? null : RDataFactory.createStringVector(newNames, RDataFactory.COMPLETE_VECTOR));
                } else {
                    int[] data = new int[nmatches];
                    int j = 0;
                    for (int i = 0; i < len; i++) {
                        if (invert ^ matches[i]) {
                            data[j++] = i + 1;
                        }
                    }
                    return RDataFactory.createIntVector(data, RDataFactory.COMPLETE_VECTOR);
                }
            }
        }

        protected void findAllMatches(boolean[] result, String pattern, RAbstractStringVector vector, boolean fixed, boolean ignoreCase) {
            for (int i = 0; i < result.length; i++) {
                String text = vector.getDataAt(i);
                if (!RRuntime.isNA(text)) {
                    if (fixed) {
                        result[i] = text.contains(pattern);
                    } else {
                        result[i] = findMatch(pattern, text, ignoreCase);
                    }
                }
            }
        }

        protected static boolean findMatch(String pattern, String text, boolean ignoreCase) {
            Matcher m = Regexp.getPatternMatcher(pattern, text, ignoreCase);
            return m.find();
        }
    }

    @RBuiltin(name = "grep", kind = INTERNAL, parameterNames = {"pattern", "x", "ignore.case", "perl", "value", "fixed", "useBytes", "invert"})
    public abstract static class Grep extends GrepAdapter {

        @Specialization
        @TruffleBoundary
        protected Object grepValueFalse(RAbstractStringVector patternArgVec, RAbstractStringVector vector, byte ignoreCaseLogical, byte valueLogical, byte perlLogical, byte fixedLogical,
                        byte useBytes, byte invertLogical) {
            return doGrep(patternArgVec, vector, ignoreCaseLogical, valueLogical, perlLogical, fixedLogical, useBytes, invertLogical, false);
        }
    }

    @RBuiltin(name = "grepl", kind = INTERNAL, parameterNames = {"pattern", "x", "ignore.case", "value", "perl", "fixed", "useBytes", "invert"})
    public abstract static class GrepL extends GrepAdapter {

        @Specialization
        @TruffleBoundary
        protected Object grepl(RAbstractStringVector patternArgVec, RAbstractStringVector vector, byte ignoreCaseLogical, byte valueLogical, byte perlLogical, byte fixedLogical, byte useBytes,
                        byte invertLogical) {
            // invert is passed but is always FALSE
            return doGrep(patternArgVec, vector, ignoreCaseLogical, valueLogical, perlLogical, fixedLogical, useBytes, invertLogical, true);
        }
    }

    protected abstract static class SubAdapter extends CommonCodeAdapter {

        protected RStringVector doSub(RAbstractStringVector patternArgVec, RAbstractStringVector replacementVec, RAbstractStringVector vector, byte ignoreCaseLogical, byte perlLogical,
                        byte fixedLogical, @SuppressWarnings("unused") byte useBytes, boolean gsub) {
            try {
                boolean perl = RRuntime.fromLogical(perlLogical);
                boolean fixed = RRuntime.fromLogical(fixedLogical);
                boolean ignoreCase = RRuntime.fromLogical(ignoreCaseLogical);
                checkNotImplemented(!(perl || fixed) && ignoreCase, "ignoreCase", true);
                checkCaseFixed(ignoreCase, fixed);
                perl = checkPerlFixed(perl, fixed);
                String pattern = checkLength(patternArgVec, "pattern");
                String replacement = checkLength(replacementVec, "replacement");

                int len = vector.getLength();
                if (RRuntime.isNA(pattern)) {
                    return allStringNAResult(len);
                }

                assert !(perl && fixed);

                if (!fixed && isSimpleReplacement(pattern, replacement)) {
                    perl = false;
                    fixed = true;
                }
                if (perl && isSimpleRegex(pattern, replacement)) {
                    perl = false;
                }

                PCRERFFI.Result pcre = null;
                if (fixed) {
                    // TODO case
                } else if (perl) {
                    int cflags = ignoreCase ? PCRERFFI.CASELESS : 0;
                    long tables = RFFIFactory.getRFFI().getPCRERFFI().maketables();
                    pcre = RFFIFactory.getRFFI().getPCRERFFI().compile(pattern, cflags, tables);
                    if (pcre.result == 0) {
                        // TODO output warning if pcre.errorMessage not NULL
                        throw RError.error(this, RError.Message.INVALID_REGEXP, pattern);
                    }
                    // TODO pcre_study for vectors > 10 ? (cf GnuR)
                } else {
                    pattern = RegExp.checkPreDefinedClasses(pattern);
                }
                String[] result = new String[len];
                for (int i = 0; i < len; i++) {
                    String input = vector.getDataAt(i);
                    if (RRuntime.isNA(input)) {
                        result[i] = input;
                        continue;
                    }

                    String value;
                    if (fixed) {
                        if (gsub) {
                            value = input.replace(pattern, replacement);
                        } else {
                            int ix = input.indexOf(pattern);
                            value = ix < 0 ? input : input.substring(0, ix) + replacement + input.substring(ix + pattern.length());
                        }
                    } else if (perl) {
                        int offset = 0;
                        int[] ovector = new int[30];
                        int nmatch = 0;
                        int eflag = 0;
                        int lastEnd = -1;
                        StringBuffer sb = new StringBuffer();
                        while (RFFIFactory.getRFFI().getPCRERFFI().exec(pcre.result, 0, input, offset, eflag, ovector) >= 0) {
                            nmatch++;
                            for (int j = offset; j < ovector[0]; j++) {
                                sb.append(input.charAt(j));
                            }
                            if (ovector[1] > lastEnd) {
                                pcreStringAdj(sb, input, replacement, ovector);
                                lastEnd = ovector[1];
                            }
                            offset = ovector[1];
                            if (offset >= input.length() || !gsub) {
                                break;
                            }
                            if (ovector[0] == ovector[1]) {
                                sb.append(input.charAt(offset++));
                            }
                            eflag |= PCRERFFI.NOTBOL;
                        }
                        if (nmatch == 0) {
                            value = input;
                        } else {
                            /* copy the tail */
                            for (int j = offset; j < input.length(); j++) {
                                sb.append(input.charAt(j));
                            }
                            value = sb.toString();
                        }
                    } else {
                        replacement = convertGroups(replacement);

                        if (gsub) {
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
            } catch (PatternSyntaxException e) {
                CompilerDirectives.transferToInterpreter();
                throw new RInternalError(e, "internal error: %s", e.getMessage());
            }
        }

        private static final int SIMPLE_PATTERN_MAX_LENGTH = 5;

        private static boolean isSimpleReplacement(String pattern, @SuppressWarnings("unused") String replacement) {
            if (pattern.length() > SIMPLE_PATTERN_MAX_LENGTH) {
                return false;
            }
            for (int i = 0; i < pattern.length(); i++) {
                switch (pattern.charAt(i)) {
                    case '.':
                    case '^':
                    case '$':
                    case '*':
                    case '+':
                    case '?':
                    case '(':
                    case ')':
                    case '[':
                    case '{':
                    case '\\':
                    case '|':
                        return false;
                    default:
                        break;
                }
            }
            // TODO: since no groups were captured, do we need to check special chars in
            // replacement?
            return true;
        }

        private static boolean isSimpleRegex(String pattern, @SuppressWarnings("unused") String replacement) {
            int i = 0;
            // perl behaves differently for nullable regexes
            boolean nonEmpty = false;
            boolean lastNonEmpty = false;
            loop: while (i < pattern.length()) {
                switch (pattern.charAt(i)) {
                    case '\\':
                        i++;
                        if (i < pattern.length()) {
                            switch (pattern.charAt(i)) {
                                case 'n':
                                case 't':
                                case '\\':
                                case '.':
                                case '*':
                                case '+':
                                    i++;
                                    continue loop;
                            }
                        }
                        return false;
                    case '^':
                    case '$':
                    case '(':
                    case ')':
                    case '[':
                    case '{':
                        return false;
                    case '*':
                    case '?':
                        lastNonEmpty = false;
                        break;
                    default:
                        nonEmpty |= lastNonEmpty;
                        lastNonEmpty = true;
                        break;
                }
                i++;
            }
            // TODO: since no groups were captured, do we need to check special chars in
            // replacement?
            return nonEmpty;
        }

        private static void pcreStringAdj(StringBuffer sb, String input, String repl, int[] ovector) {
            boolean upper = false;
            boolean lower = false;
            int px = 0;
            while (px < repl.length()) {
                char p = repl.charAt(px++);
                if (p == '\\') {
                    char p1 = repl.charAt(px++);
                    if (p1 >= '1' && p1 <= '9') {
                        int k = p1 - '0';
                        for (int i = ovector[2 * k]; i < ovector[2 * k + 1]; i++) {
                            char c = input.charAt(i);
                            sb.append(upper ? Character.toUpperCase(c) : (lower ? Character.toLowerCase(c) : c));
                        }

                    } else if (p1 == 'U') {
                        upper = true;
                        lower = false;
                    } else if (p1 == 'L') {
                        upper = false;
                        lower = true;
                    } else if (p1 == 'E') {
                        upper = false;
                        lower = false;
                    } else {
                        sb.append(p);
                    }
                } else {
                    sb.append(p);
                }
            }
        }

        @TruffleBoundary
        private static String convertGroups(String value) {
            StringBuilder result = new StringBuilder();
            int i = 0;
            while (i < value.length()) {
                char c = value.charAt(i);
                if (c == '\\') {
                    i++;
                    if (i >= value.length()) {
                        result.append('\\');
                    } else {
                        c = value.charAt(i);
                        if (c >= '0' && c <= '9') {
                            result.append('$');
                        } else {
                            result.append('\\');
                        }
                        result.append(c);
                    }
                } else {
                    result.append(c);
                }
                i++;
            }
            return result.toString();
        }
    }

    @RBuiltin(name = "sub", kind = INTERNAL, parameterNames = {"pattern", "replacement", "x", "ignore.case", "perl", "fixed", "useBytes"})
    public abstract static class Sub extends SubAdapter {

        @Specialization
        @TruffleBoundary
        protected RStringVector subRegexp(RAbstractStringVector patternArgVec, RAbstractStringVector replacementVec, RAbstractStringVector x, byte ignoreCaseLogical, byte perlLogical,
                        byte fixedLogical, byte useBytes) {
            return doSub(patternArgVec, replacementVec, x, ignoreCaseLogical, perlLogical, fixedLogical, useBytes, false);
        }
    }

    @RBuiltin(name = "gsub", kind = INTERNAL, parameterNames = {"pattern", "replacement", "x", "ignore.case", "perl", "fixed", "useBytes"})
    public abstract static class GSub extends SubAdapter {

        @Specialization
        @TruffleBoundary
        protected RStringVector gsub(RAbstractStringVector patternArgVec, RAbstractStringVector replacementVec, RAbstractStringVector x, byte ignoreCaseLogical, byte perlLogical, byte fixedLogical,
                        byte useBytes) {
            return doSub(patternArgVec, replacementVec, x, ignoreCaseLogical, perlLogical, fixedLogical, useBytes, true);
        }
    }

    @RBuiltin(name = "regexpr", kind = INTERNAL, parameterNames = {"pattern", "text", "ignore.case", "perl", "fixed", "useBytes"})
    public abstract static class Regexp extends CommonCodeAdapter {

        @Specialization
        @TruffleBoundary
        protected Object regexp(RAbstractStringVector patternArg, RAbstractStringVector vector, byte ignoreCaseL, byte perlL, byte fixedL, byte useBytesL) {
            checkExtraArgs(RRuntime.LOGICAL_FALSE, perlL, RRuntime.LOGICAL_FALSE, useBytesL, RRuntime.LOGICAL_FALSE);
            boolean ignoreCase = RRuntime.fromLogical(ignoreCaseL);
            String pattern = RegExp.checkPreDefinedClasses(patternArg.getDataAt(0));
            int[] result = new int[vector.getLength()];
            for (int i = 0; i < vector.getLength(); i++) {
                result[i] = findIndex(pattern, vector.getDataAt(i), ignoreCase, fixedL == RRuntime.LOGICAL_TRUE).get(0);
            }
            return RDataFactory.createIntVector(result, RDataFactory.COMPLETE_VECTOR);
        }

        protected static List<Integer> findIndex(String pattern, String text, boolean ignoreCase, boolean fixed) {
            List<Integer> list = new ArrayList<>();
            if (fixed) {
                int index;
                if (ignoreCase) {
                    index = text.toLowerCase().indexOf(pattern.toLowerCase());
                } else {
                    index = text.indexOf(pattern);
                }
                list.add(index == -1 ? index : index + 1);
            } else {
                Matcher m = getPatternMatcher(pattern, text, ignoreCase);
                while (m.find()) {
                    // R starts counting at index 1
                    list.add(m.start() + 1);
                }
                if (list.size() > 0) {
                    return list;
                }
                list.add(-1);
            }
            return list;
        }

        @TruffleBoundary
        private static Matcher getPatternMatcher(String pattern, String text, boolean ignoreCase) {
            return Pattern.compile(pattern, ignoreCase ? Pattern.CASE_INSENSITIVE : 0).matcher(text);
        }
    }

    @RBuiltin(name = "gregexpr", kind = INTERNAL, parameterNames = {"pattern", "text", "ignore.case", "perl", "fixed", "useBytes"})
    public abstract static class Gregexpr extends Regexp {

        @Specialization
        @TruffleBoundary
        @Override
        protected Object regexp(RAbstractStringVector patternArg, RAbstractStringVector vector, byte ignoreCaseL, byte perlL, byte fixedL, byte useBytesL) {
            checkExtraArgs(RRuntime.LOGICAL_FALSE, perlL, fixedL, useBytesL, RRuntime.LOGICAL_FALSE);
            boolean ignoreCase = RRuntime.fromLogical(ignoreCaseL);
            String pattern = RegExp.checkPreDefinedClasses(patternArg.getDataAt(0));
            Object[] result = new Object[vector.getLength()];
            for (int i = 0; i < vector.getLength(); i++) {
                int[] data = toIntArray(findIndex(pattern, vector.getDataAt(i), ignoreCase, true));
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

        @Specialization
        @TruffleBoundary
        protected RList split(RAbstractStringVector x, RAbstractStringVector splitArg, byte fixedLogical, byte perlLogical, @SuppressWarnings("unused") byte useBytes) {
            boolean fixed = RRuntime.fromLogical(fixedLogical);
            boolean perl = checkPerlFixed(RRuntime.fromLogical(perlLogical), fixed);
            RStringVector[] result = new RStringVector[x.getLength()];
            // treat split = NULL as split = ""
            RAbstractStringVector split = splitArg.getLength() == 0 ? RDataFactory.createStringVectorFromScalar("") : splitArg;
            String[] splits = new String[split.getLength()];
            long pcreTables = perl ? RFFIFactory.getRFFI().getPCRERFFI().maketables() : 0;
            PCRERFFI.Result[] pcreSplits = perl ? new PCRERFFI.Result[splits.length] : null;

            na.enable(x);
            for (int i = 0; i < splits.length; i++) {
                String currentSplit = split.getDataAt(i);
                splits[i] = fixed || perl ? split.getDataAt(i) : RegExp.checkPreDefinedClasses(split.getDataAt(i));
                if (perl) {
                    if (!currentSplit.isEmpty()) {
                        pcreSplits[i] = RFFIFactory.getRFFI().getPCRERFFI().compile(currentSplit, 0, pcreTables);
                        if (pcreSplits[i].result == 0) {
                            // TODO output warning if pcre.errorMessage not NULL
                            throw RError.error(this, RError.Message.INVALID_REGEXP, currentSplit);
                        }
                        // TODO pcre_study for vectors > 10 ? (cf GnuR)
                    }
                }
            }
            for (int i = 0; i < x.getLength(); i++) {
                String data = x.getDataAt(i);
                String currentSplit = splits[i % splits.length];
                if (currentSplit.isEmpty()) {
                    result[i] = na.check(data) ? RDataFactory.createNAStringVector() : emptySplitIntl(data);
                } else if (RRuntime.isNA(currentSplit)) {
                    // NA doesn't split
                    result[i] = RDataFactory.createStringVectorFromScalar(data);
                } else {
                    RStringVector resultItem;
                    if (na.check(data)) {
                        resultItem = RDataFactory.createNAStringVector();
                    } else {
                        if (perl) {
                            resultItem = splitPerl(data, pcreSplits[i % splits.length]);
                        } else {
                            resultItem = splitIntl(data, currentSplit);
                        }
                    }
                    result[i] = resultItem;
                }
            }
            RList ret = RDataFactory.createList(result);
            if (x.getNames(attrProfiles) != null) {
                ret.copyNamesFrom(attrProfiles, x);
            }
            return ret;
        }

        @SuppressWarnings("unused")
        @TruffleBoundary
        @Fallback
        protected RList split(Object x, Object splitArg, Object fixedLogical, Object perlLogical, Object useBytes) {
            if (!(x instanceof RAbstractStringVector)) {
                throw RError.error(this, RError.Message.NON_CHARACTER);
            } else {
                throw RError.error(this, RError.Message.INVALID_OR_UNIMPLEMENTED_ARGUMENTS);
            }
        }

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

        private static RStringVector splitPerl(String data, PCRERFFI.Result pcre) {
            ArrayList<String> matches = new ArrayList<>();
            int offset = 0;
            int[] ovector = new int[30];
            while (RFFIFactory.getRFFI().getPCRERFFI().exec(pcre.result, 0, data, offset, 0, ovector) >= 0) {
                String match;
                if (ovector[1] > 0) {
                    match = data.substring(offset, ovector[0]);
                    offset = ovector[1];
                } else {
                    match = data.substring(offset, offset + 1);
                    offset++;
                }
                matches.add(match);
            }
            if (offset < data.length()) {
                matches.add(data.substring(offset));
            }
            String[] result = new String[matches.size()];
            matches.toArray(result);
            return RDataFactory.createStringVector(result, RDataFactory.COMPLETE_VECTOR);
        }
    }
}
