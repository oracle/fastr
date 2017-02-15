/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2015, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.notEmpty;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.attributes.SetFixedAttributeNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RegExp;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
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
        @Child protected PCRERFFI.MaketablesNode maketablesNode = RFFIFactory.getRFFI().getPCRERFFI().createMaketablesNode();
        @Child protected PCRERFFI.CompileNode compileNode = RFFIFactory.getRFFI().getPCRERFFI().createCompileNode();

        protected static void castPattern(Casts casts) {
            // with default error message, NO_CALLER does not work
            casts.arg("pattern").mustBe(stringValue(), RError.NO_CALLER, RError.Message.INVALID_ARGUMENT, "pattern").asVector().mustBe(notEmpty(), RError.NO_CALLER, RError.Message.INVALID_ARGUMENT,
                            "pattern");
        }

        protected static void castText(Casts casts, String textId) {
            casts.arg(textId).mustBe(stringValue(), RError.NO_CALLER, RError.Message.INVALID_ARGUMENT, textId);
        }

        protected static void castIgnoreCase(Casts casts) {
            casts.arg("ignore.case").asLogicalVector().findFirst(RRuntime.LOGICAL_FALSE);
        }

        protected static void castPerl(Casts casts) {
            casts.arg("perl").asLogicalVector().findFirst(RRuntime.LOGICAL_FALSE);
        }

        protected static void castFixed(Casts casts, byte defaultValue) {
            casts.arg("fixed").asLogicalVector().findFirst(defaultValue);
        }

        protected static void castValue(Casts casts) {
            casts.arg("value").asLogicalVector().findFirst(RRuntime.LOGICAL_FALSE);
        }

        protected static void castUseBytes(Casts casts) {
            casts.arg("useBytes").asLogicalVector().findFirst(RRuntime.LOGICAL_FALSE);
        }

        protected static void castInvert(Casts casts) {
            casts.arg("invert").asLogicalVector().findFirst(RRuntime.LOGICAL_FALSE);
        }

        protected static void castCosts(Casts casts) {
            casts.arg("costs").asIntegerVector();
        }

        protected static void castBounds(Casts casts) {
            casts.arg("bounds").asDoubleVector();
        }

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

        protected PCRERFFI.Result compilePerlPattern(String pattern, boolean ignoreCase) {
            int cflags = ignoreCase ? PCRERFFI.CASELESS : 0;
            long tables = maketablesNode.execute();
            PCRERFFI.Result pcre = compileNode.execute(pattern, cflags, tables);
            if (pcre.result == 0) {
                // TODO output warning if pcre.errorMessage not NULL
                throw RError.error(this, RError.Message.INVALID_REGEXP, pattern);
            }
            return pcre;
        }
    }

    private abstract static class GrepAdapter extends CommonCodeAdapter {
        @Child PCRERFFI.ExecNode execNode = RFFIFactory.getRFFI().getPCRERFFI().createExecNode();

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
                PCRERFFI.Result pcre = compilePerlPattern(pattern, ignoreCase);
                // TODO pcre_study for vectors > 10 ? (cf GnuR)
                int[] ovector = new int[30];
                for (int i = 0; i < len; i++) {
                    String text = vector.getDataAt(i);
                    if (!RRuntime.isNA(text)) {
                        if (execNode.execute(pcre.result, 0, text, 0, 0, ovector) >= 0) {
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
                    RStringVector oldNames = vector.getNames();
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

    @RBuiltin(name = "grep", kind = INTERNAL, parameterNames = {"pattern", "text", "ignore.case", "value", "perl", "fixed", "useBytes", "invert"}, behavior = PURE)
    public abstract static class Grep extends GrepAdapter {

        static {
            Casts casts = new Casts(Grep.class);
            castPattern(casts);
            castText(casts, "text");
            castIgnoreCase(casts);
            castValue(casts);
            castPerl(casts);
            castFixed(casts, RRuntime.LOGICAL_FALSE);
            castUseBytes(casts);
            castInvert(casts);
        }

        @Specialization
        @TruffleBoundary
        protected Object grepValueFalse(RAbstractStringVector patternArgVec, RAbstractStringVector vector, byte ignoreCaseLogical, byte valueLogical, byte perlLogical, byte fixedLogical,
                        byte useBytes, byte invertLogical) {
            return doGrep(patternArgVec, vector, ignoreCaseLogical, valueLogical, perlLogical, fixedLogical, useBytes, invertLogical, false);
        }
    }

    @RBuiltin(name = "grepl", kind = INTERNAL, parameterNames = {"pattern", "text", "ignore.case", "value", "perl", "fixed", "useBytes", "invert"}, behavior = PURE)
    public abstract static class GrepL extends GrepAdapter {

        static {
            Casts casts = new Casts(GrepL.class);
            castPattern(casts);
            castText(casts, "text");
            castIgnoreCase(casts);
            castValue(casts);
            castPerl(casts);
            castFixed(casts, RRuntime.LOGICAL_FALSE);
            castUseBytes(casts);
            castInvert(casts);
        }

        @Specialization
        @TruffleBoundary
        protected Object grepl(RAbstractStringVector patternArgVec, RAbstractStringVector vector, byte ignoreCaseLogical, byte valueLogical, byte perlLogical, byte fixedLogical, byte useBytes,
                        byte invertLogical) {
            // invert is passed but is always FALSE
            return doGrep(patternArgVec, vector, ignoreCaseLogical, valueLogical, perlLogical, fixedLogical, useBytes, invertLogical, true);
        }
    }

    protected abstract static class SubAdapter extends CommonCodeAdapter {
        @Child PCRERFFI.ExecNode execNode = RFFIFactory.getRFFI().getPCRERFFI().createExecNode();

        protected static void castReplacement(Casts casts) {
            // with default error message, NO_CALLER does not work
            casts.arg("replacement").mustBe(stringValue(), RError.NO_CALLER, RError.Message.INVALID_ARGUMENT, "replacement").asVector().mustBe(notEmpty(), RError.NO_CALLER,
                            RError.Message.INVALID_ARGUMENT, "replacement");
        }

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
                    pcre = compilePerlPattern(pattern, ignoreCase);
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
                        int lastEndOffset = 0;
                        int lastEndIndex = 0;
                        int[] ovector = new int[30];
                        int nmatch = 0;
                        int eflag = 0;
                        int lastEnd = -1;
                        int[] fromByteMapping = getFromByteMapping(input); // non-null if it's
                                                                           // necessary

                        StringBuffer sb = new StringBuffer();
                        while (execNode.execute(pcre.result, 0, input, lastEndOffset, eflag, ovector) >= 0) {
                            nmatch++;

                            // offset == byte position
                            // index == character position
                            int startOffset = ovector[0];
                            int endOffset = ovector[1];
                            int startIndex = (fromByteMapping != null) ? fromByteMapping[startOffset] : startOffset;
                            int endIndex = (fromByteMapping != null) ? fromByteMapping[endOffset] : endOffset;

                            for (int j = lastEndIndex; j < startIndex; j++) {
                                sb.append(input.charAt(j));
                            }
                            if (endOffset > lastEnd) {
                                pcreStringAdj(sb, input, replacement, ovector, fromByteMapping);
                                lastEnd = endOffset;
                            }
                            lastEndIndex = endIndex;
                            lastEndOffset = endOffset;
                            if (lastEndIndex >= input.length() || !gsub) {
                                break;
                            }
                            if (startOffset == endOffset) {
                                sb.append(input.charAt(lastEndIndex));
                                if (fromByteMapping != null) {
                                    for (int j = lastEndOffset + 1; j < fromByteMapping.length; j++) {
                                        if (fromByteMapping[j] > 0) {
                                            lastEndOffset = j;
                                            lastEndIndex = fromByteMapping[lastEndOffset];
                                            break;
                                        }
                                    }
                                } else {
                                    lastEndOffset++;
                                    lastEndIndex++;
                                }
                            }
                            eflag |= PCRERFFI.NOTBOL;
                        }
                        if (nmatch == 0) {
                            value = input;
                        } else {
                            /* copy the tail */
                            for (int j = lastEndIndex; j < input.length(); j++) {
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
                ret.copyAttributesFrom(vector);
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

        private static void pcreStringAdj(StringBuffer sb, String input, String repl, int[] ovector, int[] fromByteMapping) {
            boolean upper = false;
            boolean lower = false;
            int px = 0;
            while (px < repl.length()) {
                char p = repl.charAt(px++);
                if (p == '\\') {
                    char p1 = repl.charAt(px++);
                    if (p1 >= '1' && p1 <= '9') {
                        int k = p1 - '0';
                        int startIndex = (fromByteMapping != null) ? fromByteMapping[ovector[2 * k]] : ovector[2 * k];
                        int endIndex = (fromByteMapping != null) ? fromByteMapping[ovector[2 * k + 1]] : ovector[2 * k + 1];
                        for (int i = startIndex; i < endIndex; i++) {
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

    @RBuiltin(name = "sub", kind = INTERNAL, parameterNames = {"pattern", "replacement", "text", "ignore.case", "perl", "fixed", "useBytes"}, behavior = PURE)
    public abstract static class Sub extends SubAdapter {

        static {
            Casts casts = new Casts(Sub.class);
            castPattern(casts);
            castReplacement(casts);
            castText(casts, "text");
            castIgnoreCase(casts);
            castPerl(casts);
            castFixed(casts, RRuntime.LOGICAL_FALSE);
            castUseBytes(casts);
        }

        @Specialization
        @TruffleBoundary
        protected RStringVector subRegexp(RAbstractStringVector patternArgVec, RAbstractStringVector replacementVec, RAbstractStringVector x, byte ignoreCaseLogical, byte perlLogical,
                        byte fixedLogical, byte useBytes) {
            return doSub(patternArgVec, replacementVec, x, ignoreCaseLogical, perlLogical, fixedLogical, useBytes, false);
        }
    }

    @RBuiltin(name = "gsub", kind = INTERNAL, parameterNames = {"pattern", "replacement", "text", "ignore.case", "perl", "fixed", "useBytes"}, behavior = PURE)
    public abstract static class GSub extends SubAdapter {

        static {
            Casts casts = new Casts(GSub.class);
            castPattern(casts);
            castReplacement(casts);
            castText(casts, "text");
            castIgnoreCase(casts);
            castPerl(casts);
            castFixed(casts, RRuntime.LOGICAL_FALSE);
            castUseBytes(casts);
        }

        @Specialization
        @TruffleBoundary
        protected RStringVector gsub(RAbstractStringVector patternArgVec, RAbstractStringVector replacementVec, RAbstractStringVector x, byte ignoreCaseLogical, byte perlLogical, byte fixedLogical,
                        byte useBytes) {
            return doSub(patternArgVec, replacementVec, x, ignoreCaseLogical, perlLogical, fixedLogical, useBytes, true);
        }
    }

    @RBuiltin(name = "regexpr", kind = INTERNAL, parameterNames = {"pattern", "text", "ignore.case", "perl", "fixed", "useBytes"}, behavior = PURE)
    public abstract static class Regexp extends CommonCodeAdapter {

        @Child SetFixedAttributeNode setMatchLengthAttrNode = SetFixedAttributeNode.create("match.length");
        @Child SetFixedAttributeNode setUseBytesAttrNode = SetFixedAttributeNode.create("useBytes");
        @Child SetFixedAttributeNode setCaptureStartAttrNode = SetFixedAttributeNode.create("capture.start");
        @Child SetFixedAttributeNode setCaptureLengthAttrNode = SetFixedAttributeNode.create("capture.length");
        @Child SetFixedAttributeNode setCaptureNamesAttrNode = SetFixedAttributeNode.create("capture.names");
        @Child SetFixedAttributeNode setDimNamesAttrNode = SetFixedAttributeNode.createDimNames();
        @Child PCRERFFI.ExecNode execNode = RFFIFactory.getRFFI().getPCRERFFI().createExecNode();
        @Child PCRERFFI.GetCaptureNamesNode getCaptureNamesNode = RFFIFactory.getRFFI().getPCRERFFI().createGetCaptureNamesNode();
        @Child PCRERFFI.GetCaptureCountNode getCaptureCountNode = RFFIFactory.getRFFI().getPCRERFFI().createGetCaptureCountNode();

        static {
            Casts casts = new Casts(Regexp.class);
            castPattern(casts);
            castText(casts, "text");
            castIgnoreCase(casts);
            castPerl(casts);
            castFixed(casts, RRuntime.LOGICAL_FALSE);
            castUseBytes(casts);
        }

        protected static final class Info {
            protected final int index;
            protected final int size;
            protected final int[] captureStart;
            protected final int[] captureLength;
            protected final String[] captureNames;
            protected final boolean hasCapture;

            public Info(int index, int size, int[] captureStart, int[] captureLength, String[] captureNames) {
                this.index = index;
                this.size = size;
                this.captureStart = captureStart;
                this.captureLength = captureLength;
                this.captureNames = captureNames;
                this.hasCapture = captureStart != null && captureLength != null;
            }
        }

        private static void setNoCaptureValues(int[] captureStart, int[] captureLength, int namesLen, int vecLen, int index) {
            for (int j = 0; j < namesLen; j++) {
                captureStart[j * vecLen + index] = -1;
                captureLength[j * vecLen + index] = -1;
            }
        }

        @Specialization
        @TruffleBoundary
        protected Object regexp(RAbstractStringVector patternArg, RAbstractStringVector vector, byte ignoreCaseL, byte perlL, byte fixedL, byte useBytesL) {
            checkExtraArgs(RRuntime.LOGICAL_FALSE, RRuntime.LOGICAL_FALSE, RRuntime.LOGICAL_FALSE, useBytesL, RRuntime.LOGICAL_FALSE);
            boolean ignoreCase = RRuntime.fromLogical(ignoreCaseL);
            boolean fixed = RRuntime.fromLogical(fixedL);
            boolean perl = RRuntime.fromLogical(perlL);
            if (patternArg.getLength() > 1) {
                throw RInternalError.unimplemented("multi-element patterns in regexpr not implemented yet");
            }
            String pattern = patternArg.getDataAt(0);
            if (!perl) {
                pattern = RegExp.checkPreDefinedClasses(pattern);
            }
            // TODO: useBytes normally depends on the value of the parameter and (if false) on
            // whether the string is ASCII
            boolean useBytes = true;
            boolean hasAnyCapture = false;
            int[] result = new int[vector.getLength()];
            int[] matchLength = new int[vector.getLength()];
            String[] captureNames = null;
            int[] captureStart = null;
            int[] captureLength = null;
            if (pattern.length() == 0) {
                // emtpy pattern
                Arrays.fill(result, 1);
            } else {
                for (int i = 0; i < vector.getLength(); i++) {
                    Info res = getInfo(pattern, vector.getDataAt(i), ignoreCase, perl, fixed).get(0);
                    result[i] = res.index;
                    matchLength[i] = res.size;
                    if (res.hasCapture) {
                        hasAnyCapture = true;
                        if (captureNames == null) {
                            // first time we see captures
                            captureNames = res.captureNames;
                            captureStart = new int[captureNames.length * vector.getLength()];
                            captureLength = new int[captureNames.length * vector.getLength()];
                            // previous matches had no capture - fill in result with -1-s
                            for (int k = 0; k < i; k++) {
                                setNoCaptureValues(captureStart, captureLength, captureNames.length, vector.getLength(), k);
                            }
                        }
                        assert captureNames.length == res.captureStart.length;
                        assert captureNames.length == res.captureLength.length;
                        for (int j = 0; j < captureNames.length; j++) {
                            captureStart[j * vector.getLength() + i] = res.captureStart[j];
                            captureLength[j * vector.getLength() + i] = res.captureLength[j];
                        }
                    } else if (hasAnyCapture) {
                        // no capture for this part of the vector, but there are previous captures
                        setNoCaptureValues(captureStart, captureLength, captureNames.length, vector.getLength(), i);
                    }
                }
            }
            RIntVector ret = RDataFactory.createIntVector(result, RDataFactory.COMPLETE_VECTOR);
            setMatchLengthAttrNode.execute(ret, RDataFactory.createIntVector(matchLength, RDataFactory.COMPLETE_VECTOR));
            if (useBytes) {
                setUseBytesAttrNode.execute(ret, RRuntime.LOGICAL_TRUE);
            }
            if (hasAnyCapture) {
                RStringVector captureNamesVec = RDataFactory.createStringVector(captureNames, RDataFactory.COMPLETE_VECTOR);
                RIntVector captureStartVec = RDataFactory.createIntVector(captureStart, RDataFactory.COMPLETE_VECTOR, new int[]{vector.getLength(), captureNames.length});
                setDimNamesAttrNode.execute(captureStartVec, RDataFactory.createList(new Object[]{RNull.instance, captureNamesVec.copy()}));
                setCaptureStartAttrNode.execute(ret, captureStartVec);
                RIntVector captureLengthVec = RDataFactory.createIntVector(captureLength, RDataFactory.COMPLETE_VECTOR, new int[]{vector.getLength(), captureNames.length});
                setDimNamesAttrNode.execute(captureLengthVec, RDataFactory.createList(new Object[]{RNull.instance, captureNamesVec.copy()}));
                setCaptureLengthAttrNode.execute(ret, captureLengthVec);
                setCaptureNamesAttrNode.execute(ret, captureNamesVec);
            }
            return ret;
        }

        protected List<Info> getInfo(String pattern, String text, boolean ignoreCase, boolean perl, boolean fixed) {
            List<Info> list = new ArrayList<>();
            if (fixed) {
                int index = 0;
                while (true) {
                    if (ignoreCase) {
                        index = text.toLowerCase().indexOf(pattern.toLowerCase(), index);
                    } else {
                        index = text.indexOf(pattern, index);
                    }
                    if (index == -1) {
                        break;
                    }
                    list.add(new Info(index + 1, pattern.length(), null, null, null));
                    index += pattern.length();
                }
            } else if (perl) {
                PCRERFFI.Result pcre = compilePerlPattern(pattern, ignoreCase);
                int maxCaptureCount = getCaptureCountNode.execute(pcre.result, 0);
                int[] ovector = new int[(maxCaptureCount + 1) * 3];
                int offset = 0;
                while (true) {
                    int captureCount = execNode.execute(pcre.result, 0, text, offset, 0, ovector);
                    if (captureCount >= 0) {
                        String[] captureNames = getCaptureNamesNode.execute(pcre.result, 0, maxCaptureCount);
                        for (int i = 0; i < captureNames.length; i++) {
                            if (captureNames[i] == null) {
                                captureNames[i] = "";
                            }
                        }
                        assert captureCount - 1 == captureNames.length;
                        int[] captureStart = null;
                        int[] captureLength = null;
                        if (captureCount > 1) {
                            captureStart = new int[captureCount - 1];
                            captureLength = new int[captureCount - 1];
                            int ind = 0;
                            for (int i = 2; i < captureCount * 2; i += 2) {
                                captureStart[ind] = ovector[i] + 1;
                                captureLength[ind] = ovector[i + 1] - ovector[i];
                                ind++;
                            }
                        }
                        // R starts counting at index 1
                        list.add(new Info(ovector[0] + 1, ovector[1] - ovector[0], captureStart, captureLength, captureNames));
                        offset = ovector[1];
                    } else {
                        break;
                    }
                }
            } else {
                Matcher m = getPatternMatcher(pattern, text, ignoreCase);
                while (m.find()) {
                    // R starts counting at index 1
                    list.add(new Info(m.start() + 1, m.end() - m.start(), null, null, null));
                }
            }
            if (list.size() > 0) {
                return list;
            }
            list.add(new Info(-1, -1, null, null, null));
            return list;
        }

        @TruffleBoundary
        private static Matcher getPatternMatcher(String pattern, String text, boolean ignoreCase) {
            return Pattern.compile(pattern, ignoreCase ? Pattern.CASE_INSENSITIVE : 0).matcher(text);
        }
    }

    @RBuiltin(name = "gregexpr", kind = INTERNAL, parameterNames = {"pattern", "text", "ignore.case", "perl", "fixed", "useBytes"}, behavior = PURE)
    public abstract static class Gregexpr extends Regexp {

        @Child SetFixedAttributeNode setMatchLengthAttrNode = SetFixedAttributeNode.create("match.length");
        @Child SetFixedAttributeNode setUseBytesAttrNode = SetFixedAttributeNode.create("useBytes");
        @Child SetFixedAttributeNode setCaptureStartAttrNode = SetFixedAttributeNode.create("capture.start");
        @Child SetFixedAttributeNode setCaptureLengthAttrNode = SetFixedAttributeNode.create("capture.length");
        @Child SetFixedAttributeNode setCaptureNamesAttrNode = SetFixedAttributeNode.create("capture.names");
        @Child SetFixedAttributeNode setDimNamesAttrNode = SetFixedAttributeNode.createDimNames();

        static {
            Casts casts = new Casts(Gregexpr.class);
            castPattern(casts);
            castText(casts, "text");
            castIgnoreCase(casts);
            castPerl(casts);
            castFixed(casts, RRuntime.LOGICAL_FALSE);
            castUseBytes(casts);
        }

        private void setNoCaptureAttributes(RIntVector vec, RStringVector captureNames) {
            int len = captureNames.getLength();
            int[] captureStartData = new int[len];
            int[] captureLengthData = new int[len];
            Arrays.fill(captureStartData, -1);
            Arrays.fill(captureLengthData, -1);
            RIntVector captureStart = RDataFactory.createIntVector(captureStartData, RDataFactory.COMPLETE_VECTOR, new int[]{1, captureNames.getLength()});
            setDimNamesAttrNode.execute(captureStart, RDataFactory.createList(new Object[]{RNull.instance, captureNames.copy()}));
            RIntVector captureLength = RDataFactory.createIntVector(captureLengthData, RDataFactory.COMPLETE_VECTOR, new int[]{1, captureNames.getLength()});
            setDimNamesAttrNode.execute(captureLength, RDataFactory.createList(new Object[]{RNull.instance, captureNames.copy()}));
            setCaptureStartAttrNode.execute(vec, captureStart);
            setCaptureLengthAttrNode.execute(vec, captureLength);
            setCaptureNamesAttrNode.execute(vec, captureNames);
        }

        @Specialization
        @TruffleBoundary
        @Override
        protected Object regexp(RAbstractStringVector patternArg, RAbstractStringVector vector, byte ignoreCaseL, byte perlL, byte fixedL, byte useBytesL) {
            checkExtraArgs(RRuntime.LOGICAL_FALSE, RRuntime.LOGICAL_FALSE, RRuntime.LOGICAL_FALSE, useBytesL, RRuntime.LOGICAL_FALSE);
            boolean ignoreCase = RRuntime.fromLogical(ignoreCaseL);
            boolean fixed = RRuntime.fromLogical(fixedL);
            boolean perl = RRuntime.fromLogical(perlL);
            if (patternArg.getLength() > 1) {
                throw RInternalError.unimplemented("multi-element patterns in gregexpr not implemented yet");
            }
            String pattern = patternArg.getDataAt(0);
            if (!perl) {
                pattern = RegExp.checkPreDefinedClasses(pattern);
            }
            // TODO: useBytes normally depends on the value of the parameter and (if false) on
            // whether the string is ASCII
            boolean useBytes = true;
            Object[] result = new Object[vector.getLength()];
            boolean hasAnyCapture = false;
            RStringVector captureNames = null;
            for (int i = 0; i < vector.getLength(); i++) {
                RIntVector res;
                if (pattern.length() == 0) {
                    String txt = vector.getDataAt(i);
                    res = RDataFactory.createIntVector(txt.length());
                    for (int j = 0; j < txt.length(); j++) {
                        res.setDataAt(res.getDataWithoutCopying(), j, j + 1);
                    }
                    setMatchLengthAttrNode.execute(res, RDataFactory.createIntVector(txt.length()));
                    if (useBytes) {
                        setUseBytesAttrNode.execute(res, RRuntime.LOGICAL_TRUE);
                    }
                } else {
                    List<Info> l = getInfo(pattern, vector.getDataAt(i), ignoreCase, perl, fixed);
                    res = toIndexOrSizeVector(l, true);
                    setMatchLengthAttrNode.execute(res, toIndexOrSizeVector(l, false));
                    if (useBytes) {
                        setUseBytesAttrNode.execute(res, RRuntime.LOGICAL_TRUE);
                    }
                    RIntVector captureStart = toCaptureStartOrLength(l, true);
                    if (captureStart != null) {
                        RIntVector captureLength = toCaptureStartOrLength(l, false);
                        assert captureLength != null;
                        captureNames = getCaptureNamesVector(l);
                        assert captureNames.getLength() > 0;
                        if (!hasAnyCapture) {
                            // set previous result list elements to "no capture"
                            for (int j = 0; j < i; j++) {
                                setNoCaptureAttributes((RIntVector) result[j], captureNames);
                            }
                        }
                        hasAnyCapture = true;
                        setCaptureStartAttrNode.execute(res, captureStart);
                        setCaptureLengthAttrNode.execute(res, captureLength);
                        setCaptureNamesAttrNode.execute(res, captureNames);
                    } else if (hasAnyCapture) {
                        assert captureNames != null;
                        // it's capture names from previous iteration, so copy
                        setNoCaptureAttributes(res, (RStringVector) captureNames.copy());
                    }
                }

                result[i] = res;
            }
            return RDataFactory.createList(result);
        }

        private static RIntVector toIndexOrSizeVector(List<Info> list, boolean index) {
            int[] arr = new int[list.size()];
            for (int i = 0; i < list.size(); i++) {
                Info res = list.get(i);
                arr[i] = index ? res.index : res.size;
            }
            return RDataFactory.createIntVector(arr, RDataFactory.COMPLETE_VECTOR);
        }

        private RIntVector toCaptureStartOrLength(List<Info> list, boolean start) {
            assert list.size() > 0;
            Info firstInfo = list.get(0);
            if (!firstInfo.hasCapture) {
                return null;
            }
            assert firstInfo.captureNames.length > 0;
            int[] arr = new int[list.size() * firstInfo.captureNames.length];
            int ind = 0;
            for (int i = 0; i < firstInfo.captureNames.length; i++) {
                for (int j = 0; j < list.size(); j++) {
                    Info info = list.get(j);
                    assert info.captureNames.length == firstInfo.captureNames.length;
                    assert info.captureStart.length == firstInfo.captureStart.length;
                    assert info.captureLength.length == firstInfo.captureLength.length;
                    arr[ind++] = start ? info.captureStart[i] : info.captureLength[i];
                }
            }
            RIntVector ret = RDataFactory.createIntVector(arr, RDataFactory.COMPLETE_VECTOR, new int[]{list.size(), firstInfo.captureNames.length});
            setDimNamesAttrNode.execute(ret, RDataFactory.createList(new Object[]{RNull.instance, RDataFactory.createStringVector(firstInfo.captureNames, RDataFactory.COMPLETE_VECTOR)}));
            return ret;
        }

        private static RStringVector getCaptureNamesVector(List<Info> list) {
            assert list.size() > 0;
            Info firstInfo = list.get(0);
            if (!firstInfo.hasCapture) {
                return null;
            }
            assert firstInfo.captureNames.length > 0;
            return RDataFactory.createStringVector(firstInfo.captureNames, RDataFactory.COMPLETE_VECTOR);
        }
    }

    @RBuiltin(name = "agrep", kind = INTERNAL, parameterNames = {"pattern", "x", "ignore.case", "value", "costs", "bounds", "useBytes", "fixed"}, behavior = PURE)
    public abstract static class AGrep extends CommonCodeAdapter {

        static {
            Casts casts = new Casts(AGrep.class);
            castPattern(casts);
            castText(casts, "x");
            castIgnoreCase(casts);
            castValue(casts);
            castCosts(casts);
            castBounds(casts);
            castUseBytes(casts);
            castFixed(casts, RRuntime.LOGICAL_TRUE);
        }

        @SuppressWarnings("unused")
        @Specialization
        @TruffleBoundary
        protected Object aGrep(RAbstractStringVector patternArg, RAbstractStringVector vector, byte ignoreCase, byte value, RAbstractIntVector costs, RAbstractDoubleVector bounds, byte useBytes,
                        byte fixed) {
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

    @RBuiltin(name = "agrepl", kind = INTERNAL, parameterNames = {"pattern", "x", "ignore.case", "value", "costs", "bounds", "useBytes", "fixed"}, behavior = PURE)
    public abstract static class AGrepL extends CommonCodeAdapter {

        static {
            Casts casts = new Casts(AGrepL.class);
            castPattern(casts);
            castText(casts, "x");
            castIgnoreCase(casts);
            castValue(casts);
            castCosts(casts);
            castBounds(casts);
            castUseBytes(casts);
            castFixed(casts, RRuntime.LOGICAL_TRUE);
        }

        @SuppressWarnings("unused")
        @Specialization
        @TruffleBoundary
        protected Object aGrep(RAbstractStringVector patternArg, RAbstractStringVector vector, byte ignoreCase, byte value, RAbstractIntVector costs, RAbstractDoubleVector bounds, byte useBytes,
                        byte fixed) {
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

    @RBuiltin(name = "strsplit", kind = INTERNAL, parameterNames = {"x", "split", "fixed", "perl", "useBytes"}, behavior = PURE)
    public abstract static class Strsplit extends CommonCodeAdapter {
        @Child PCRERFFI.ExecNode execNode = RFFIFactory.getRFFI().getPCRERFFI().createExecNode();

        static {
            Casts casts = new Casts(Strsplit.class);
            casts.arg("x").mustBe(stringValue(), RError.SHOW_CALLER, RError.Message.NON_CHARACTER);
            casts.arg("split").mustBe(stringValue(), RError.SHOW_CALLER, RError.Message.NON_CHARACTER);
            castFixed(casts, RRuntime.LOGICAL_FALSE);
            castPerl(casts);
            castUseBytes(casts);
        }

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
            long pcreTables = perl ? maketablesNode.execute() : 0;
            PCRERFFI.Result[] pcreSplits = perl ? new PCRERFFI.Result[splits.length] : null;

            na.enable(x);
            for (int i = 0; i < splits.length; i++) {
                String currentSplit = split.getDataAt(i);
                splits[i] = fixed || perl ? split.getDataAt(i) : RegExp.checkPreDefinedClasses(split.getDataAt(i));
                if (perl) {
                    if (!currentSplit.isEmpty()) {
                        pcreSplits[i] = compileNode.execute(currentSplit, 0, pcreTables);
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
                            resultItem = splitIntl(data, currentSplit, fixed);
                        }
                        if (resultItem.getLength() == 0) {
                            if (fixed) {
                                resultItem = RDataFactory.createStringVector(data);
                            } else {
                                resultItem = RDataFactory.createStringVector(data.length());
                            }
                        }
                    }
                    result[i] = resultItem;
                }
            }
            RList ret = RDataFactory.createList(result);
            if (x.getNames() != null) {
                ret.copyNamesFrom(x);
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

        private static RStringVector splitIntl(String input, String separator, boolean fixed) {
            assert !RRuntime.isNA(input);

            if (fixed) {
                ArrayList<String> matches = new ArrayList<>();
                int idx = input.indexOf(separator);
                if (idx < 0) {
                    return RDataFactory.createStringVector(input);
                }
                int lastIdx = 0;
                while (idx > -1) {
                    matches.add(input.substring(lastIdx, idx));
                    lastIdx = idx + separator.length();
                    if (lastIdx > input.length()) {
                        break;
                    }
                    idx = input.indexOf(separator, lastIdx);
                }
                String m = input.substring(lastIdx);
                if (!m.isEmpty()) {
                    matches.add(m);
                }
                return RDataFactory.createStringVector(matches.toArray(new String[matches.size()]), false);
            } else {
                if (input.equals(separator)) {
                    return RDataFactory.createStringVector("");
                } else {
                    return RDataFactory.createStringVector(input.split(separator), true);
                }
            }
        }

        private static RStringVector emptySplitIntl(String input) {
            assert !RRuntime.isNA(input);
            String[] result = new String[input.length()];
            for (int i = 0; i < input.length(); i++) {
                result[i] = new String(new char[]{input.charAt(i)});
            }
            return RDataFactory.createStringVector(result, true);
        }

        private RStringVector splitPerl(String data, PCRERFFI.Result pcre) {
            ArrayList<String> matches = new ArrayList<>();
            int lastEndOffset = 0;
            int lastEndIndex = 0;
            int[] ovector = new int[30];
            int[] fromByteMapping = getFromByteMapping(data); // non-null if it's necessary

            while (execNode.execute(pcre.result, 0, data, lastEndOffset, 0, ovector) >= 0) {
                // offset == byte position
                // index == character position
                int startOffset = ovector[0];
                int endOffset = ovector[1];
                int startIndex = (fromByteMapping != null) ? fromByteMapping[startOffset] : startOffset;
                int endIndex = (fromByteMapping != null) ? fromByteMapping[endOffset] : endOffset;
                String match = data.substring(lastEndIndex, startIndex);
                lastEndOffset = endOffset;
                lastEndIndex = endIndex;
                matches.add(match);
            }
            if (lastEndIndex < data.length()) {
                matches.add(data.substring(lastEndIndex));
            }
            String[] result = new String[matches.size()];
            matches.toArray(result);
            return RDataFactory.createStringVector(result, RDataFactory.COMPLETE_VECTOR);
        }
    }

    private static int getByteLength(String data) {
        int byteLength = 0;
        int pos = 0;
        while (pos < data.length()) {
            char c = data.charAt(pos);
            if (c < 128) {
                byteLength++;
            } else if (c < 2048) {
                byteLength += 2;
            } else {
                if (Character.isHighSurrogate(c)) {
                    byteLength += 4;
                    pos++;
                } else {
                    byteLength += 3;
                }
            }
            pos++;
        }
        return byteLength;
    }

    private static int[] getFromByteMapping(String data) {
        int byteLength = getByteLength(data);
        if (byteLength == data.length()) {
            return null;
        }
        int[] result = new int[byteLength + 1];
        byteLength = 0;
        int pos = 0;
        while (pos < data.length()) {
            result[byteLength] = pos;
            char c = data.charAt(pos);
            if (c < 128) {
                byteLength++;
            } else if (c < 2048) {
                byteLength += 2;
            } else {
                if (Character.isHighSurrogate(c)) {
                    byteLength += 4;
                    pos++;
                } else {
                    byteLength += 3;
                }
            }
            pos++;
        }
        result[byteLength] = pos;
        return result;
    }
}
