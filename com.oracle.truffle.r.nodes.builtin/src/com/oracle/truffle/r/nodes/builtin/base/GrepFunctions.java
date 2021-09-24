/*
 * Copyright (c) 1995-2015, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, a copy is available at
 * https://www.R-project.org/Licenses/
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.missingValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.notEmpty;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.nullValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.singleElement;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.Utils.toLowerCase;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.builtin.NodeWithArgumentCasts.Casts;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.Collections.ArrayListObj;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RegExp;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary;
import com.oracle.truffle.r.runtime.data.nodes.attributes.SetFixedAttributeNode;
import com.oracle.truffle.r.runtime.ffi.PCRE2RFFI;
import com.oracle.truffle.r.runtime.ffi.PCRE2RFFI.IndexRange;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;
import com.oracle.truffle.r.runtime.nodes.RBaseNodeWithWarnings;
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
    protected static void castPattern(Casts casts) {
        // with default error message, NO_CALLER does not work
        casts.arg("pattern").mustBe(stringValue(), RError.Message.INVALID_ARGUMENT, "pattern").asVector().mustBe(notEmpty(), RError.Message.INVALID_ARGUMENT,
                        "pattern");
    }

    protected static void castPatternSingle(Casts casts) {
        // with default error message, NO_CALLER does not work
        casts.arg("pattern").mustBe(stringValue(), RError.Message.INVALID_ARGUMENT, "pattern").asVector().mustBe(notEmpty(), RError.Message.INVALID_ARGUMENT,
                        "pattern").shouldBe(singleElement(), RError.Message.ARGUMENT_ONLY_FIRST, "pattern").findFirst();
    }

    protected static void castText(Casts casts, String textId) {
        casts.arg(textId).mustBe(stringValue(), RError.Message.INVALID_ARGUMENT, textId);
    }

    protected static void castIgnoreCase(Casts casts) {
        casts.arg("ignore.case").asLogicalVector().findFirst(RRuntime.LOGICAL_FALSE).map(toBoolean());
    }

    protected static void castPerl(Casts casts) {
        casts.arg("perl").asLogicalVector().findFirst(RRuntime.LOGICAL_FALSE).map(toBoolean());
    }

    protected static void castFixed(Casts casts, byte defaultValue) {
        casts.arg("fixed").asLogicalVector().findFirst(defaultValue).map(toBoolean());
    }

    protected static void castValue(Casts casts) {
        casts.arg("value").asLogicalVector().findFirst(RRuntime.LOGICAL_FALSE).map(toBoolean());
    }

    protected static void castCosts(Casts casts) {
        casts.arg("costs").defaultError(RError.Message.INVALID_ARG, "costs").mustBe((missingValue().or(nullValue()).not())).asIntegerVector();
    }

    protected static void castUseBytes(Casts casts) {
        casts.arg("useBytes").asLogicalVector().findFirst(RRuntime.LOGICAL_FALSE).map(toBoolean());
    }

    protected static void castInvert(Casts casts) {
        casts.arg("invert").asLogicalVector().findFirst(RRuntime.LOGICAL_FALSE).map(toBoolean());
    }

    protected static void castBounds(Casts casts) {
        casts.arg("bounds").defaultError(RError.Message.INVALID_ARG, "bounds").mustBe((missingValue().or(nullValue()).not())).asDoubleVector();
    }

    @NodeInfo(cost = NodeCost.NONE)
    public static class CommonCodeNode extends RBaseNodeWithWarnings {
        @Child protected PCRE2RFFI.CompileNode pcre2CompileNode = RFFIFactory.getPCRE2RFFI().createCompileNode();
        @Child protected PCRE2RFFI.MatchNode pcre2MatchNode = RFFIFactory.getPCRE2RFFI().createMatchNode();
        @Child protected PCRE2RFFI.GetCaptureCountNode pcre2CaptureCountNode = RFFIFactory.getPCRE2RFFI().createGetCaptureCountNode();
        @Child protected PCRE2RFFI.MemoryReleaseNode pcre2MemoryReleaseNode = RFFIFactory.getPCRE2RFFI().createMemoryReleaseNode();
        @Child private InteropLibrary interop = InteropLibrary.getFactory().createDispatched(DSLConfig.getInteropLibraryCacheSize());

        /**
         * Temporary method that handles the check for the arguments that are common to the majority
         * of the functions, that we don't yet implement. If any of the arguments are {@code true},
         * then an NYI error will be thrown (in the first one). If any of the arguments do not
         * apply, pass {@link RRuntime#LOGICAL_FALSE}.
         */
        protected void checkExtraArgs(boolean ignoreCase, boolean perl, boolean fixed, @SuppressWarnings("unused") boolean useBytes, boolean invert) {
            checkNotImplemented(ignoreCase, "ignoreCase", true);
            checkNotImplemented(perl, "perl", true);
            checkNotImplemented(fixed, "fixed", true);
            // We just ignore useBytes
            // checkNotImplemented(RRuntime.fromLogical(useBytes), "useBytes", true);
            checkNotImplemented(invert, "invert", true);
        }

        protected void checkCaseFixed(boolean ignoreCase, boolean fixed) {
            if (ignoreCase && fixed) {
                warning(RError.Message.ARGUMENT_IGNORED, "ignore.case = TRUE");
            }
        }

        protected boolean checkPerlFixed(boolean perl, boolean fixed) {
            if (fixed && perl) {
                warning(RError.Message.ARGUMENT_IGNORED, "perl = TRUE");
                return false;
            } else {
                return perl;
            }
        }

        /**
         * Temporary check for the {@code value} argument, which is only applicable to {@code grep}
         * and {@code agrep} (so not included in {@code checkExtraArgs}.
         *
         * @param value
         */
        protected void valueCheck(boolean value) {
            if (value) {
                throw RError.nyi(this, "value == true");
            }
        }

        protected void checkNotImplemented(boolean condition, String arg, boolean b) {
            if (condition) {
                CompilerDirectives.transferToInterpreter();
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
                int resultIdx = 0;
                for (int i = 0; i < tmp.length; i++) {
                    if (tmp[i] > 0) {
                        result[resultIdx] = tmp[i];
                        resultIdx++;
                    }
                }
                return result;
            }
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

        protected PCRE2RFFI.CompileResult compilePerlPattern(String pattern, boolean ignoreCase) {
            int options = ignoreCase ? PCRE2RFFI.Option.CASELESS.value : 0;
            PCRE2RFFI.CompileResult pcre = pcre2CompileNode.execute(pattern, options);
            if (interop.isNull(pcre.compiledPattern)) {
                assert pcre.errorMessage != null;
                throw error(Message.INVALID_REGEXP_REASON, pattern, pcre.errorMessage);
            }
            return pcre;
        }
    }

    protected static final class GrepCommonCodeNode extends CommonCodeNode {
        @Child private InteropLibrary interop = InteropLibrary.getFactory().createDispatched(DSLConfig.getInteropLibraryCacheSize());

        protected Object doGrep(String patternArg, RStringVector vector, boolean ignoreCase, boolean value, boolean perlPar, boolean fixed,
                        @SuppressWarnings("unused") boolean useBytes, boolean invert, boolean grepl) {
            try {
                boolean perl = perlPar;
                perl = checkPerlFixed(perlPar, fixed);
                checkCaseFixed(ignoreCase, fixed);

                String pattern = patternArg;
                int len = vector.getLength();
                if (RRuntime.isNA(pattern)) {
                    return value ? allStringNAResult(len) : allIntNAResult(len);
                }
                boolean[] matches = new boolean[len];
                if (!perl) {
                    // TODO case
                    if (!fixed) {
                        pattern = RegExp.transformPatternToGnurCompatible(pattern);
                    }
                    findAllMatches(matches, pattern, vector, fixed, ignoreCase);
                } else {
                    PCRE2RFFI.CompileResult compileResult = pcre2CompileNode.execute(pattern, 0);
                    if (interop.isNull(compileResult.compiledPattern)) {
                        assert compileResult.errorMessage != null;
                        throw error(Message.INVALID_REGEXP_REASON, pattern, compileResult.errorMessage);
                    }
                    int captureCount = pcre2CaptureCountNode.execute(compileResult.compiledPattern);
                    assert !interop.isNull(compileResult.compiledPattern);
                    for (int i = 0; i < vector.getLength(); i++) {
                        String text = vector.getDataAt(i);
                        PCRE2RFFI.MatchData matchData = pcre2MatchNode.execute(compileResult.compiledPattern, text, 0, true, captureCount);
                        matches[i] = matchData.getMatchCount() > 0;
                    }
                    pcre2MemoryReleaseNode.execute(compileResult.compiledPattern);
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
            } catch (PatternSyntaxException e) {
                throw error(Message.INVALID_REGEXP_REASON, patternArg, e.getMessage());
            }
        }

        protected static void findAllMatches(boolean[] result, String pattern, RStringVector vector, boolean fixed, boolean ignoreCase) {
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
            Matcher m = Regexpr.getPatternMatcher(pattern, text, ignoreCase);
            return m.find();
        }
    }

    public static CommonCodeNode createCommon() {
        return new CommonCodeNode();
    }

    public static GrepCommonCodeNode createGrepCommon() {
        return new GrepCommonCodeNode();
    }

    public static SubCommonCodeNode createSubCommon() {
        return new SubCommonCodeNode();
    }

    @ImportStatic(GrepFunctions.class)
    @RBuiltin(name = "grep", kind = INTERNAL, parameterNames = {"pattern", "text", "ignore.case", "value", "perl", "fixed", "useBytes", "invert"}, behavior = PURE)
    public abstract static class Grep extends RBuiltinNode.Arg8 {

        static {
            Casts casts = new Casts(Grep.class);
            castPatternSingle(casts);
            castText(casts, "text");
            castIgnoreCase(casts);
            castValue(casts);
            castPerl(casts);
            castFixed(casts, RRuntime.LOGICAL_FALSE);
            castUseBytes(casts);
            castInvert(casts);
        }

        protected boolean checkContains(boolean value, boolean perl, boolean fixed, boolean useBytes) {
            return fixed && !useBytes && !value && !perl;
        }

        @Specialization(guards = {"checkContains(value, perl, fixed, useBytes)"})
        @TruffleBoundary
        protected Object grepValueFixed(String patternPar, RStringVector vector, boolean ignoreCase, @SuppressWarnings("unused") boolean value,
                        @SuppressWarnings("unused") boolean perl,
                        @SuppressWarnings("unused") boolean fixed, @SuppressWarnings("unused") boolean useBytes, boolean invert) {

            String pattern = ignoreCase ? toLowerCase(patternPar) : patternPar;

            int[] matchIndices = new int[vector.getLength()];
            int matches = 0;
            for (int i = 0; i < vector.getLength(); i++) {
                String s = vector.getDataAt(i);
                if (ignoreCase) {
                    s = toLowerCase(s);
                }

                if (s.contains(pattern) == !invert) {
                    // don't forget: R indices are 1-based
                    matchIndices[matches++] = i + 1;
                }
            }

            return RDataFactory.createIntVector(Arrays.copyOf(matchIndices, matches), true);
        }

        @Specialization
        @TruffleBoundary
        protected Object grepValueFalse(String patternArgVec, RStringVector vector, boolean ignoreCaseLogical, boolean valueLogical, boolean perlLogical, boolean fixedLogical,
                        boolean useBytes, boolean invertLogical,
                        @Cached("createGrepCommon()") GrepCommonCodeNode common) {
            return common.doGrep(patternArgVec, vector, ignoreCaseLogical, valueLogical, perlLogical, fixedLogical, useBytes, invertLogical, false);
        }
    }

    @ImportStatic(GrepFunctions.class)
    @RBuiltin(name = "grepl", kind = INTERNAL, parameterNames = {"pattern", "text", "ignore.case", "value", "perl", "fixed", "useBytes", "invert"}, behavior = PURE)
    public abstract static class GrepL extends RBuiltinNode.Arg8 {

        static {
            Casts casts = new Casts(GrepL.class);
            castPatternSingle(casts);
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
        protected Object grepl(String pattern, RStringVector vector, boolean ignoreCaseLogical, boolean valueLogical, boolean perlLogical, boolean fixedLogical, boolean useBytes,
                        boolean invertLogical,
                        @Cached("createGrepCommon()") GrepCommonCodeNode common) {
            // invert is passed but is always FALSE
            return common.doGrep(pattern, vector, ignoreCaseLogical, valueLogical, perlLogical, fixedLogical, useBytes, invertLogical, true);
        }
    }

    protected static void castReplacement(Casts casts) {
        // with default error message, NO_CALLER does not work
        casts.arg("replacement").mustBe(stringValue(), RError.Message.INVALID_ARGUMENT, "replacement").asVector().mustBe(notEmpty(), RError.Message.INVALID_ARGUMENT, "replacement").shouldBe(
                        singleElement(), RError.Message.ARGUMENT_ONLY_FIRST, "replacement").findFirst();
    }

    protected static final class SubCommonCodeNode extends CommonCodeNode {
        private static final String APPEND_MISSING_NL_PATTERN = "([^\n])$";
        private static final String APPEND_MISSING_NL_REPLACEMENT = "\\1\n";

        protected RStringVector doSub(String patternArg, String replacementArg, RStringVector vector, VectorDataLibrary vectorDataLib, boolean ignoreCase, boolean perlPar,
                        boolean fixedPar, @SuppressWarnings("unused") boolean useBytes, boolean gsub) {
            try {

                // This is a workaround for the incorrect evaluation of the pattern that
                // is supposed to append a missing new line character. The pattern being fixed is:
                // gsub("([^\n])$", "\\1\n", source)
                // The original (wrong) behavior appended a new line even if the source was
                // terminated by a new line.
                if (APPEND_MISSING_NL_PATTERN.equals(patternArg) && APPEND_MISSING_NL_REPLACEMENT.equals(replacementArg)) {
                    return appendMissingNewLine(vector, vectorDataLib);
                }

                boolean perl = perlPar;
                boolean fixed = fixedPar;
                checkNotImplemented(!(perl || fixed) && ignoreCase, "ignoreCase", true);
                checkCaseFixed(ignoreCase, fixed);
                perl = checkPerlFixed(perl, fixed);
                String pattern = patternArg;
                String replacement = replacementArg;

                int len = vectorDataLib.getLength(vector.getData());
                if (RRuntime.isNA(pattern)) {
                    return allStringNAResult(len);
                }

                assert !(perl && fixed);

                if (!fixed && isSimpleReplacement(pattern, replacement)) {
                    perl = false;
                    fixed = true;
                }
                if (perl && isSimpleRegex(pattern)) {
                    perl = false;
                }

                PCRE2RFFI.CompileResult pcre = null;
                int captureCount = 0;
                if (fixed) {
                    // TODO case
                } else if (perl) {
                    pcre = compilePerlPattern(pattern, ignoreCase);
                    captureCount = pcre2CaptureCountNode.execute(pcre.compiledPattern);
                } else {
                    pattern = RegExp.transformPatternToGnurCompatible(pattern);
                }
                String preparedReplacement = null;
                String[] result = new String[len];
                for (int i = 0; i < len; i++) {
                    String input = vectorDataLib.getStringAt(vector.getData(), i);
                    if (RRuntime.isNA(input)) {
                        result[i] = input;
                        continue;
                    }

                    String value;
                    if (fixed) {
                        if (gsub) {
                            if (preparedReplacement == null) {
                                preparedReplacement = replacement.replace("$", "\\$");
                                preparedReplacement = convertGroups(preparedReplacement, 0);
                            }
                            value = Pattern.compile(pattern, Pattern.LITERAL).matcher(input).replaceAll(preparedReplacement);
                        } else {
                            int ix = input.indexOf(pattern);
                            if (preparedReplacement == null) {
                                preparedReplacement = replacement.replace("\\\\", "\\");
                            }
                            value = ix < 0 ? input : input.substring(0, ix) + preparedReplacement + input.substring(ix + pattern.length());
                        }
                    } else if (perl) {
                        assert pcre != null;
                        boolean stopAfterFirstMatch = !gsub;
                        PCRE2RFFI.MatchData matchData = pcre2MatchNode.execute(pcre.compiledPattern, input, 0, stopAfterFirstMatch, captureCount);
                        boolean replacementContainsBackReferences = containsBackReferences(replacement);
                        if (!replacementContainsBackReferences) {
                            preparedReplacement = preparePcreReplacement(input, replacement, 0, matchData);
                        }
                        List<IndexRange> matches = matchData.getMatches();
                        int lastMatchEndIdx = 0;
                        StringBuilder sb = new StringBuilder();
                        for (int matchIdx = 0; matchIdx < matches.size(); matchIdx++) {
                            IndexRange match = matches.get(matchIdx);
                            if (replacementContainsBackReferences) {
                                preparedReplacement = preparePcreReplacement(input, replacement, matchIdx, matchData);
                            }
                            if (lastMatchEndIdx != match.startIdx) {
                                // Only prepend part of the input if the matches are not adjacent.
                                sb.append(input, lastMatchEndIdx, match.startIdx);
                            }
                            if (lastMatchEndIdx != match.startIdx || !match.isEmpty()) {
                                // If next `match` is empty, we do not want to append anything in
                                // `sb`.
                                sb.append(preparedReplacement);
                            }
                            if (lastMatchEndIdx == 0 && match.startIdx == 0 && match.isEmpty()) {
                                // Empty match at the beginning is a special case - we want to
                                // prepend the prepared replacement at the beginning.
                                sb.append(preparedReplacement);
                            }
                            lastMatchEndIdx = match.endIdx;
                        }
                        // Copy tail
                        sb.append(input, lastMatchEndIdx, input.length());
                        value = sb.toString();
                    } else {
                        Matcher matcher = Pattern.compile(pattern, Pattern.DOTALL).matcher(input);
                        if (preparedReplacement == null) {
                            preparedReplacement = replacement.replace("$", "\\$");
                            // matcher.groupCount() only depends on the pattern (not on the input)
                            preparedReplacement = convertGroups(preparedReplacement, matcher.groupCount());
                        }
                        if (gsub) {
                            value = matcher.replaceAll(preparedReplacement);
                        } else {
                            value = matcher.replaceFirst(preparedReplacement);
                        }
                    }
                    result[i] = value;
                }
                if (perl) {
                    assert pcre != null;
                    pcre2MemoryReleaseNode.execute(pcre.compiledPattern);
                }
                boolean isVectorComplete = vectorDataLib.isComplete(vector.getData());
                RStringVector ret = RDataFactory.createStringVector(result, isVectorComplete);
                ret.copyAttributesFrom(vector);
                return ret;
            } catch (PatternSyntaxException e) {
                throw error(Message.INVALID_REGEXP_REASON, patternArg, e.getMessage());
            }
        }

        private static RStringVector appendMissingNewLine(RStringVector vector, VectorDataLibrary vectorDataLib) {
            String[] newElems = null;
            Object vectorData = vector.getData();
            int vecLength = vectorDataLib.getLength(vectorData);
            for (int i = 0; i < vecLength; i++) {
                String elem = vectorDataLib.getStringAt(vectorData, i);
                if (!RRuntime.isNA(elem) && elem.charAt(elem.length() - 1) != '\n') {
                    if (newElems == null) {
                        newElems = new String[vecLength];
                        for (int j = 0; j < i; j++) {
                            newElems[j] = vectorDataLib.getStringAt(vectorData, j);
                        }
                    }

                    newElems[i] = vectorDataLib.getStringAt(vectorData, i) + "\n";
                } else if (newElems != null) {
                    newElems[i] = elem;
                }
            }

            return newElems == null ? vector : RDataFactory.createStringVector(newElems, vectorDataLib.isComplete(vectorData));
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

        private static boolean isSimpleRegex(String pattern) {
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
                    case '\n':
                    case '\t':
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

        /**
         * @param matchIdx Index of current match
         * @return PCRE2-specific replacement.
         */
        private static String preparePcreReplacement(String input, String replacement, int matchIdx, PCRE2RFFI.MatchData matchData) {
            StringBuilder sb = new StringBuilder();
            boolean upper = false;
            boolean lower = false;
            int replacementIdx = 0;
            while (replacementIdx < replacement.length()) {
                char c = replacement.charAt(replacementIdx);
                if (c == '\\' && replacementIdx < replacement.length() - 1) {
                    char nextChar = replacement.charAt(replacementIdx + 1);
                    if ('1' <= nextChar && nextChar <= '9') {
                        int captureIdx = nextChar - '0';
                        // Decrease `captureIdx` - it was in 1-based indexes.
                        captureIdx--;
                        String captureText = getCaptureText(input, captureIdx, matchIdx, matchData);
                        if (upper) {
                            sb.append(captureText.toUpperCase());
                        } else if (lower) {
                            sb.append(captureText.toLowerCase());
                        } else {
                            sb.append(captureText);
                        }
                    } else if (nextChar == 'U') {
                        upper = true;
                        lower = false;
                    } else if (nextChar == 'L') {
                        upper = false;
                        lower = true;
                    } else if (nextChar == 'E') {
                        upper = false;
                        lower = false;
                    } else {
                        sb.append(nextChar);
                    }
                    replacementIdx += 2;
                } else {
                    sb.append(c);
                    replacementIdx++;
                }
            }
            return sb.toString();
        }

        private static boolean containsBackReferences(String replacement) {
            int replacementIdx = 0;
            while (replacementIdx < replacement.length()) {
                char c = replacement.charAt(replacementIdx);
                if (c == '\\' && replacementIdx < replacement.length() - 1) {
                    char nextChar = replacement.charAt(replacementIdx + 1);
                    if ('1' <= nextChar && nextChar <= '9') {
                        return true;
                    }
                    replacementIdx += 2;
                } else {
                    replacementIdx++;
                }
            }
            return false;
        }

        private static String getCaptureText(String input, int captureIdx, int matchIdx, PCRE2RFFI.MatchData matchData) {
            List<IndexRange> captureMatches = matchData.getCaptures().get(captureIdx);
            assert captureMatches != null;
            if (captureMatches.size() <= matchIdx) {
                return "";
            } else {
                IndexRange capture = captureMatches.get(matchIdx);
                return input.substring(capture.startIdx, capture.endIdx);
            }
        }

        @TruffleBoundary
        private static String convertGroups(String value, int groupCount) {
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
                        if (c >= '1' && c <= '9') {
                            int gi = c - '0';
                            if (gi <= groupCount) {
                                result.append('$').append(c);
                            }
                        } else {
                            result.append('\\').append(c);
                        }
                    }
                } else {
                    result.append(c);
                }
                i++;
            }
            return result.toString();
        }
    }

    @ImportStatic(GrepFunctions.class)
    @RBuiltin(name = "sub", kind = INTERNAL, parameterNames = {"pattern", "replacement", "text", "ignore.case", "perl", "fixed", "useBytes"}, behavior = PURE)
    public abstract static class Sub extends RBuiltinNode.Arg7 {

        static {
            Casts casts = new Casts(Sub.class);
            castPatternSingle(casts);
            castReplacement(casts);
            castText(casts, "text");
            castIgnoreCase(casts);
            castPerl(casts);
            castFixed(casts, RRuntime.LOGICAL_FALSE);
            castUseBytes(casts);
        }

        @Specialization(limit = "getGenericDataLibraryCacheSize()")
        @TruffleBoundary
        protected RStringVector subRegexp(String patternArgVec, String replacementVec, RStringVector x, boolean ignoreCaseLogical, boolean perlLogical, boolean fixedLogical,
                        boolean useBytes,
                        @CachedLibrary("x.getData()") VectorDataLibrary xDataLib,
                        @Cached("createSubCommon()") SubCommonCodeNode common) {
            return common.doSub(patternArgVec, replacementVec, x, xDataLib, ignoreCaseLogical, perlLogical, fixedLogical, useBytes, false);
        }
    }

    @ImportStatic(GrepFunctions.class)
    @RBuiltin(name = "gsub", kind = INTERNAL, parameterNames = {"pattern", "replacement", "text", "ignore.case", "perl", "fixed", "useBytes"}, behavior = PURE)
    public abstract static class GSub extends RBuiltinNode.Arg7 {

        static {
            Casts casts = new Casts(GSub.class);
            castPatternSingle(casts);
            castReplacement(casts);
            castText(casts, "text");
            castIgnoreCase(casts);
            castPerl(casts);
            castFixed(casts, RRuntime.LOGICAL_FALSE);
            castUseBytes(casts);
        }

        @Specialization(limit = "getGenericDataLibraryCacheSize()")
        @TruffleBoundary
        protected RStringVector gsub(String patternArgVec, String replacementVec, RStringVector x, boolean ignoreCaseLogical, boolean perlLogical, boolean fixedLogical,
                        boolean useBytes,
                        @CachedLibrary("x.getData()") VectorDataLibrary xDataLib,
                        @Cached("createSubCommon()") SubCommonCodeNode common) {
            return common.doSub(patternArgVec, replacementVec, x, xDataLib, ignoreCaseLogical, perlLogical, fixedLogical, useBytes, true);
        }
    }

    @ImportStatic(GrepFunctions.class)
    @RBuiltin(name = "regexpr", kind = INTERNAL, parameterNames = {"pattern", "text", "ignore.case", "perl", "fixed", "useBytes"}, behavior = PURE)
    public abstract static class Regexpr extends RBuiltinNode.Arg6 {

        @Child private SetFixedAttributeNode setMatchLengthAttrNode = SetFixedAttributeNode.create("match.length");
        @Child private SetFixedAttributeNode setIndexTypeAttrNode = SetFixedAttributeNode.create("index.type");
        @Child private SetFixedAttributeNode setUseBytesAttrNode = SetFixedAttributeNode.create("useBytes");
        @Child private SetFixedAttributeNode setCaptureStartAttrNode = SetFixedAttributeNode.create("capture.start");
        @Child private SetFixedAttributeNode setCaptureLengthAttrNode = SetFixedAttributeNode.create("capture.length");
        @Child private SetFixedAttributeNode setCaptureNamesAttrNode = SetFixedAttributeNode.create("capture.names");
        @Child private SetFixedAttributeNode setDimNamesAttrNode = SetFixedAttributeNode.createDimNames();
        @Child private PCRE2RFFI.GetCaptureNamesNode getCaptureNamesNode = RFFIFactory.getPCRE2RFFI().createGetCaptureNamesNode();
        @Child private PCRE2RFFI.GetCaptureCountNode getCaptureCountNode = RFFIFactory.getPCRE2RFFI().createGetCaptureCountNode();

        static {
            Casts casts = new Casts(Regexpr.class);
            castPattern(casts);
            castText(casts, "text");
            castIgnoreCase(casts);
            castPerl(casts);
            castFixed(casts, RRuntime.LOGICAL_FALSE);
            castUseBytes(casts);
        }

        /**
         * All the indexes are 1-based. All instances of the class has to have the capture fields
         * reference the same array, i.e., for any {@code info1} and {@code info2} it must hold that
         * {@code info1.captureNames == info2.captureNames && info1.captureLength == info2.captureLength...}
         * . For {@code regexpr} builtins, the result has "capture.start", "capture.length", and
         * "capture.names" attributes if there were any captures in the match. These attributes are
         * constructed from the fields of this class.
         *
         * @see "?regexpr"
         */
        protected static final class Info {
            // First index of the match. Note that in one match there may be more than one capture.
            protected final int index;
            protected final int size;
            /**
             * Array of indexes of all the capture starts. It is possible, that the length of this
             * array is lower than the length of {@code captureNames} - this can happen, e.g., when
             * there are multiple matches per one capture. If there are multiple matches for one
             * capture, their start indexes are saved into this array in order of capture indexes.
             *
             * For example: Suppose {@code captureNames=[null, null], captureStart=[0,0,1,2}, the
             * first capture has 0, and 0 as start indexes, whereas the second capture has 1 and 2
             * as start indexes.
             */
            protected final int[] captureStart;
            protected final int[] captureLength;
            protected final String[] captureNames;
            protected final boolean hasCapture;
            protected final boolean hasCaptureNames;

            public Info(int index, int size, int[] captureStart, int[] captureLength, String[] captureNames) {
                this.index = index;
                this.size = size;
                this.captureStart = captureStart;
                this.captureLength = captureLength;
                this.captureNames = captureNames;
                this.hasCapture = captureStart != null && captureLength != null;
                this.hasCaptureNames = captureNames != null && captureNames.length > 0;
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
        protected Object regexp(RStringVector patternArg, RStringVector vector, boolean ignoreCase, boolean perl, boolean fixed, boolean useBytesL,
                        @Cached("createCommon()") CommonCodeNode common) {
            try {
                common.checkExtraArgs(false, false, false, useBytesL, false);
                if (patternArg.getLength() > 1) {
                    throw RInternalError.unimplemented("multi-element patterns in regexpr not implemented yet");
                }
                String pattern = patternArg.getDataAt(0);
                if (!perl && !fixed) {
                    pattern = RegExp.transformPatternToGnurCompatible(pattern);
                }
                // TODO: useBytes normally depends on the value of the parameter and (if false) on
                // whether the string is ASCII
                boolean useBytes = true;
                String indexType = "chars"; // TODO: normally should be: useBytes ? "bytes" :
                                            // "chars";
                boolean hasCaptureNames = false;
                int vectorLen = vector.getLength();
                int[] result = new int[vectorLen];
                int[] matchLength = new int[vectorLen];
                String[] captureNames = null;
                int[] captureStart = null;
                int[] captureLength = null;
                if (pattern.length() == 0) {
                    // emtpy pattern
                    Arrays.fill(result, 1);
                } else {
                    if (vectorLen == 0) {
                        String[] namesOrNull = getPatternCaptureNames(common, pattern, ignoreCase, perl, fixed);
                        if (namesOrNull != null && namesOrNull.length > 0) {
                            hasCaptureNames = true;
                            captureNames = namesOrNull;
                            captureStart = new int[0];
                            captureLength = new int[0];
                        }
                    }
                    for (int i = 0; i < vectorLen; i++) {
                        Info res = getInfo(common, pattern, vector.getDataAt(i), ignoreCase, perl, fixed, true).get(0);
                        result[i] = res.index;
                        matchLength[i] = res.size;
                        if (res.hasCapture) {
                            hasCaptureNames = true;
                            if (captureNames == null) {
                                // first time we see captures
                                captureNames = res.captureNames;
                                // length of res.captureNames gives the max amount of captures
                                captureStart = new int[captureNames.length * vectorLen];
                                captureLength = new int[captureNames.length * vectorLen];
                            }
                            for (int j = 0; j < res.captureStart.length; j++) {
                                // well, res.captureStart might be shorter then
                                // res.captureNames (but never more then by 1?),
                                // just ignore the remaining (zero) elements in captureStart
                                captureStart[j * vectorLen + i] = res.captureStart[j];
                                captureLength[j * vectorLen + i] = res.captureLength[j];
                            }
                        } else if (res.hasCaptureNames) {
                            // no capture for this part of the vector, but even then
                            // we want to return a "no capture" result
                            hasCaptureNames = true;
                            captureNames = res.captureNames;
                            if (captureStart == null) {
                                captureStart = new int[captureNames.length * vectorLen];
                                captureLength = new int[captureNames.length * vectorLen];
                            }
                            setNoCaptureValues(captureStart, captureLength, captureNames.length, vectorLen, i);
                        }
                    }
                }
                RIntVector ret = RDataFactory.createIntVector(result, RDataFactory.COMPLETE_VECTOR);
                setMatchLengthAttrNode.setAttr(ret, RDataFactory.createIntVector(matchLength, RDataFactory.COMPLETE_VECTOR));
                if (useBytes) {
                    setIndexTypeAttrNode.setAttr(ret, indexType);
                    setUseBytesAttrNode.setAttr(ret, RRuntime.LOGICAL_TRUE);
                }
                if (hasCaptureNames) {
                    RStringVector captureNamesVec = RDataFactory.createStringVector(captureNames, RDataFactory.COMPLETE_VECTOR);
                    RIntVector captureStartVec = RDataFactory.createIntVector(captureStart, RDataFactory.COMPLETE_VECTOR, new int[]{vectorLen, captureNames.length});
                    setDimNamesAttrNode.setAttr(captureStartVec, RDataFactory.createList(new Object[]{RNull.instance, captureNamesVec.copy()}));
                    setCaptureStartAttrNode.setAttr(ret, captureStartVec);
                    RIntVector captureLengthVec = RDataFactory.createIntVector(captureLength, RDataFactory.COMPLETE_VECTOR, new int[]{vectorLen, captureNames.length});

                    setDimNamesAttrNode.setAttr(captureLengthVec, RDataFactory.createList(new Object[]{RNull.instance, captureNamesVec.copy()}));
                    setCaptureLengthAttrNode.setAttr(ret, captureLengthVec);
                    setCaptureNamesAttrNode.setAttr(ret, captureNamesVec);
                }
                return ret;
            } catch (PatternSyntaxException e) {
                throw error(Message.INVALID_REGEXP_REASON, patternArg, e.getMessage());
            }
        }

        protected String[] getPatternCaptureNames(CommonCodeNode common, String pattern, boolean ignoreCase, boolean perl, boolean fixed) {
            if (fixed || !perl) {
                return null;
            }
            PCRE2RFFI.CompileResult pcre = common.compilePerlPattern(pattern, ignoreCase);
            int maxCaptureCount = getCaptureCountNode.execute(pcre.compiledPattern);
            if (maxCaptureCount < 0) {
                // TODO: pcre2-specific error message
                throw error(Message.PCRE_FULLINFO_RETURNED, maxCaptureCount);
            }
            return getCaptureNamesNode.execute(pcre.compiledPattern, maxCaptureCount);
        }

        protected List<Info> getInfo(CommonCodeNode common, String pattern, String text, boolean ignoreCase, boolean perl, boolean fixed) {
            return getInfo(common, pattern, text, ignoreCase, perl, fixed, false);
        }

        protected List<Info> getInfo(CommonCodeNode common, String pattern, String text, boolean ignoreCase, boolean perl, boolean fixed, boolean onlyFirst) {
            List<Info> list = new ArrayList<>();
            if (fixed) {
                int index = 0;
                while (true) {
                    if (ignoreCase) {
                        index = toLowerCase(text).indexOf(toLowerCase(pattern), index);
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
                PCRE2RFFI.CompileResult pcre = common.compilePerlPattern(pattern, ignoreCase);
                int captureCount = getCaptureCountNode.execute(pcre.compiledPattern);
                if (captureCount < 0) {
                    // TODO: PCRE2-specific error
                    throw error(Message.PCRE_FULLINFO_RETURNED, captureCount);
                }

                String[] captureNames = getCaptureNamesNode.execute(pcre.compiledPattern, captureCount);
                assert captureCount == captureNames.length;
                for (int i = 0; i < captureNames.length; i++) {
                    if (captureNames[i] == null) {
                        captureNames[i] = "";
                    }
                }
                PCRE2RFFI.MatchData matchData = common.pcre2MatchNode.execute(pcre.compiledPattern, text, 0, onlyFirst, captureCount);
                int[] captureStart = null;
                int[] captureLength = null;
                if (captureCount > 0) {
                    int captureArrayIdx = 0;
                    captureStart = new int[matchData.getCaptureMatchesCount()];
                    captureLength = new int[matchData.getCaptureMatchesCount()];
                    for (List<IndexRange> captureMatches : matchData.getCaptures().values()) {
                        for (IndexRange captureMatch : captureMatches) {
                            if (captureMatch.startIdx == captureMatch.endIdx && captureMatch.startIdx == 0) {
                                captureStart[captureArrayIdx] = 0;
                                captureLength[captureArrayIdx] = 0;
                            } else {
                                captureStart[captureArrayIdx] = captureMatch.startIdx + 1;
                                captureLength[captureArrayIdx] = captureMatch.endIdx - captureMatch.startIdx;
                            }
                            captureArrayIdx++;
                        }
                    }
                }
                List<IndexRange> matches = matchData.getMatches();
                // An element in `list` corresponds to one match. We assume here that `captureStart`
                // and `captureLength` are initialized if necessary.
                for (IndexRange match : matches) {
                    int matchSize = match.endIdx - match.startIdx;
                    // Empty matches at the end are ignored.
                    if (matchSize != 0 || match.startIdx != text.length()) {
                        list.add(new Info(match.startIdx + 1, matchSize, captureStart, captureLength, captureNames));
                    }
                }
                if (list.isEmpty() && captureCount > 0) {
                    // at least a return array of emtpty string names, is necessary for output
                    list.add(new Info(-1, -1, null, null, captureNames));
                }
            } else {
                Matcher m = getPatternMatcher(pattern, text, ignoreCase);
                while (m.find()) {
                    // R starts counting at index 1
                    list.add(new Info(Regexec.start(m) + 1, Regexec.end(m) - Regexec.start(m), null, null, null));
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
            String actualPattern = pattern;

            // If a pattern starts with a '*', GnuR virtually prepends an empty string literal to
            // the star. This won't match anything, so just remove '*' from the pattern.
            if (pattern.length() > 0 && pattern.charAt(0) == '*') {
                actualPattern = pattern.substring(1);
            }
            return Pattern.compile(actualPattern, Pattern.DOTALL | (ignoreCase ? Pattern.CASE_INSENSITIVE : 0)).matcher(text);
        }
    }

    @ImportStatic(GrepFunctions.class)
    @RBuiltin(name = "regexec", kind = INTERNAL, parameterNames = {"pattern", "text", "ignore.case", "fixed", "useBytes"}, behavior = PURE)
    public abstract static class Regexec extends RBuiltinNode.Arg5 {

        @Child private SetFixedAttributeNode setMatchLengthAttrNode = SetFixedAttributeNode.create("match.length");
        @Child private SetFixedAttributeNode setIndexTypeAttrNode = SetFixedAttributeNode.create("index.type");
        @Child private SetFixedAttributeNode setUseBytesAttrNode = SetFixedAttributeNode.create("useBytes");

        static {
            Casts casts = new Casts(Regexec.class);
            castPattern(casts);
            castText(casts, "text");
            castIgnoreCase(casts);
            castFixed(casts, RRuntime.LOGICAL_FALSE);
            castUseBytes(casts);
        }

        /**
         * @see Regexpr.Info
         */
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

        @Specialization
        protected Object regexp(RStringVector patternArgIn, RStringVector vector, boolean ignoreCaseIn, boolean fixedIn, boolean useBytesL,
                        @Cached("createClassProfile()") ValueProfile patternProfile,
                        @Cached("createIdentityProfile()") ValueProfile fixedProfile,
                        @Cached("createIdentityProfile()") ValueProfile ignoreCaseProfile,
                        @Cached("createCountingProfile()") LoopConditionProfile loopConditionProfile,
                        @Cached("createCommon()") CommonCodeNode common) {
            try {
                RStringVector patternArg = patternProfile.profile(patternArgIn);
                boolean fixed = fixedProfile.profile(fixedIn);
                boolean ignoreCase = ignoreCaseProfile.profile(ignoreCaseIn);
                common.checkExtraArgs(false, false, false, useBytesL, false);
                if (patternArg.getLength() > 1) {
                    throw RInternalError.unimplemented("multi-element patterns in regexpr not implemented yet");
                }
                RList ret = RDataFactory.createList(vector.getLength());
                String pattern = patternArg.getDataAt(0);
                pattern = RegExp.transformPatternToGnurCompatible(pattern);
                // TODO: useBytes normally depends on the value of the parameter and (if false) on
                // whether the string is ASCII
                boolean useBytes = true;
                String indexType = "chars"; // TODO: normally should be: useBytes ? "bytes" :
                                            // "chars";
                loopConditionProfile.profileCounted(vector.getLength());
                for (int i = 0; loopConditionProfile.inject(i < vector.getLength()); i++) {
                    int[] matchPos;
                    int[] matchLength;
                    if (pattern.length() == 0) {
                        // emtpy pattern
                        matchPos = new int[]{1};
                        matchLength = new int[]{0};
                    } else {
                        Info[] res = getInfo(pattern, vector.getDataAt(i), ignoreCase, fixed);
                        matchPos = new int[res.length];
                        matchLength = new int[res.length];
                        for (int j = 0; j < res.length; j++) {
                            matchPos[j] = res[j].index;
                            matchLength[j] = res[j].size;
                        }
                    }
                    RIntVector matches = RDataFactory.createIntVector(matchPos, RDataFactory.COMPLETE_VECTOR);
                    setMatchLengthAttrNode.setAttr(matches, RDataFactory.createIntVector(matchLength, RDataFactory.COMPLETE_VECTOR));
                    ret.setElement(i, matches);
                }
                if (useBytes) {
                    setIndexTypeAttrNode.setAttr(ret, indexType);
                    setUseBytesAttrNode.setAttr(ret, RRuntime.LOGICAL_TRUE);
                }
                return ret;
            } catch (PatternSyntaxException e) {
                throw error(Message.INVALID_REGEXP_REASON, patternArgIn, e.getMessage());
            }
        }

        protected Info[] getInfo(String pattern, String text, boolean ignoreCase, boolean fixed) {
            Info[] result = null;
            if (fixed) {
                int index;
                if (ignoreCase) {
                    index = toLowerCase(text).indexOf(toLowerCase(pattern));
                } else {
                    index = text.indexOf(pattern);
                }
                if (index != -1) {
                    result = new Info[]{new Info(index + 1, pattern.length(), null, null, null)};
                }
            } else {
                Matcher m = getPatternMatcher(pattern, text, ignoreCase);
                if (find(m)) {
                    result = new Info[m.groupCount() + 1];
                    for (int i = 0; i <= m.groupCount(); i++) {
                        result[i] = new Info(start(m, i) + 1, end(m, i) - start(m, i), null, null, null);
                    }
                }
            }
            if (result != null) {
                return result;
            }
            return new Info[]{new Info(-1, -1, null, null, null)};
        }

        @TruffleBoundary
        private static boolean find(Matcher m) {
            return m.find();
        }

        @TruffleBoundary
        private static int start(Matcher m, int i) {
            return m.start(i);
        }

        @TruffleBoundary
        private static int end(Matcher m, int i) {
            return m.end(i);
        }

        @TruffleBoundary
        private static int start(Matcher m) {
            return m.start();
        }

        @TruffleBoundary
        private static int end(Matcher m) {
            return m.end();
        }

        @TruffleBoundary
        private static Matcher getPatternMatcher(String pattern, String text, boolean ignoreCase) {
            return Pattern.compile(pattern, Pattern.DOTALL | (ignoreCase ? Pattern.CASE_INSENSITIVE : 0)).matcher(text);
        }
    }

    @ImportStatic(GrepFunctions.class)
    @RBuiltin(name = "gregexpr", kind = INTERNAL, parameterNames = {"pattern", "text", "ignore.case", "perl", "fixed", "useBytes"}, behavior = PURE)
    public abstract static class Gregexpr extends Regexpr {

        @Child private SetFixedAttributeNode setMatchLengthAttrNode = SetFixedAttributeNode.create("match.length");
        @Child private SetFixedAttributeNode setIndexTypeAttrNode = SetFixedAttributeNode.create("index.type");
        @Child private SetFixedAttributeNode setUseBytesAttrNode = SetFixedAttributeNode.create("useBytes");
        @Child private SetFixedAttributeNode setCaptureStartAttrNode = SetFixedAttributeNode.create("capture.start");
        @Child private SetFixedAttributeNode setCaptureLengthAttrNode = SetFixedAttributeNode.create("capture.length");
        @Child private SetFixedAttributeNode setCaptureNamesAttrNode = SetFixedAttributeNode.create("capture.names");
        @Child private SetFixedAttributeNode setDimNamesAttrNode = SetFixedAttributeNode.createDimNames();

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
            setDimNamesAttrNode.setAttr(captureStart, RDataFactory.createList(new Object[]{RNull.instance, captureNames.copy()}));
            RIntVector captureLength = RDataFactory.createIntVector(captureLengthData, RDataFactory.COMPLETE_VECTOR, new int[]{1, captureNames.getLength()});
            setDimNamesAttrNode.setAttr(captureLength, RDataFactory.createList(new Object[]{RNull.instance, captureNames.copy()}));
            setCaptureStartAttrNode.setAttr(vec, captureStart);
            setCaptureLengthAttrNode.setAttr(vec, captureLength);
            setCaptureNamesAttrNode.setAttr(vec, captureNames);
        }

        @Specialization
        @TruffleBoundary
        @Override
        protected Object regexp(RStringVector patternArg, RStringVector vector, boolean ignoreCaseL, boolean perlL, boolean fixedL, boolean useBytesL,
                        @Cached("createCommon()") CommonCodeNode common) {
            try {
                common.checkExtraArgs(false, false, false, useBytesL, false);
                boolean ignoreCase = ignoreCaseL;
                boolean fixed = fixedL;
                boolean perl = perlL;
                if (patternArg.getLength() > 1) {
                    throw RInternalError.unimplemented("multi-element patterns in gregexpr not implemented yet");
                }
                String pattern = patternArg.getDataAt(0);
                if (!perl && !fixed) {
                    pattern = RegExp.transformPatternToGnurCompatible(pattern);
                }
                // TODO: useBytes normally depends on the value of the parameter and (if false) on
                // whether the string is ASCII
                boolean useBytes = true;
                String indexType = "chars"; // TODO: normally should be: useBytes ? "bytes" :
                                            // "chars";
                Object[] result = new Object[vector.getLength()];
                boolean hasAnyCapture = false;
                RStringVector captureNames = null;
                for (int i = 0; i < vector.getLength(); i++) {
                    RIntVector res;
                    if (pattern.length() == 0) {
                        String txt = vector.getDataAt(i);
                        int[] resData = new int[txt.length()];
                        for (int j = 0; j < txt.length(); j++) {
                            resData[j] = j + 1;
                        }
                        res = RDataFactory.createIntVector(resData, RDataFactory.COMPLETE_VECTOR);
                        setMatchLengthAttrNode.setAttr(res, RDataFactory.createIntVector(txt.length()));
                        if (useBytes) {
                            setIndexTypeAttrNode.setAttr(res, indexType);
                            setUseBytesAttrNode.setAttr(res, RRuntime.LOGICAL_TRUE);
                        }
                    } else {
                        List<Info> l = getInfo(common, pattern, vector.getDataAt(i), ignoreCase, perl, fixed);
                        res = toIndexOrSizeVector(l, true);
                        setMatchLengthAttrNode.setAttr(res, toIndexOrSizeVector(l, false));
                        if (useBytes) {
                            setIndexTypeAttrNode.setAttr(res, indexType);
                            setUseBytesAttrNode.setAttr(res, RRuntime.LOGICAL_TRUE);
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
                            setCaptureStartAttrNode.setAttr(res, captureStart);
                            setCaptureLengthAttrNode.setAttr(res, captureLength);
                            setCaptureNamesAttrNode.setAttr(res, captureNames);
                        } else if (hasAnyCapture) {
                            assert captureNames != null;
                            // it's capture names from previous iteration, so copy
                            setNoCaptureAttributes(res, (RStringVector) captureNames.copy());
                        }
                    }

                    result[i] = res;
                }
                return RDataFactory.createList(result);
            } catch (PatternSyntaxException e) {
                throw error(Message.INVALID_REGEXP_REASON, patternArg, e.getMessage());
            }
        }

        private static RIntVector toIndexOrSizeVector(List<Info> list, boolean index) {
            int[] arr = new int[list.size()];
            for (int i = 0; i < list.size(); i++) {
                Info res = list.get(i);
                arr[i] = index ? res.index : res.size;
            }
            return RDataFactory.createIntVector(arr, RDataFactory.COMPLETE_VECTOR);
        }

        // TODO: refactor to ...(String[] captureName, int[] captureStarts, int[] captureLengths,
        // boolean start)
        private RIntVector toCaptureStartOrLength(List<Info> list, boolean start) {
            assert list.size() > 0;
            Info firstInfo = list.get(0);
            if (!firstInfo.hasCapture) {
                return null;
            }
            int captureNamesCount = firstInfo.captureNames.length;
            assert captureNamesCount > 0;
            for (Info info : list) {
                assert info.captureNames.length == captureNamesCount;
                assert info.captureStart.length == firstInfo.captureStart.length;
                assert info.captureLength.length == firstInfo.captureLength.length;
            }
            int[] captureStarts = firstInfo.captureStart;
            int[] captureLengths = firstInfo.captureLength;
            String[] captureNames = firstInfo.captureNames;
            assert captureStarts.length >= captureNames.length;
            int nrows = captureStarts.length / captureNames.length;
            int ncols = captureNames.length;
            int[] arr = new int[nrows * ncols];
            assert arr.length == captureStarts.length;
            assert arr.length == captureLengths.length;
            for (int i = 0; i < arr.length; i++) {
                arr[i] = start ? captureStarts[i] : captureLengths[i];
            }
            RIntVector ret = RDataFactory.createIntVector(arr, RDataFactory.COMPLETE_VECTOR, new int[]{nrows, ncols});
            // Rows are unnamed, and names of columns correspond to the names of the captures.
            setDimNamesAttrNode.setAttr(ret, RDataFactory.createList(new Object[]{RNull.instance, RDataFactory.createStringVector(captureNames, RDataFactory.COMPLETE_VECTOR)}));
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

    @ImportStatic(GrepFunctions.class)
    @RBuiltin(name = "agrep", kind = INTERNAL, parameterNames = {"pattern", "x", "ignore.case", "value", "costs", "bounds", "useBytes", "fixed"}, behavior = PURE)
    public abstract static class AGrep extends RBuiltinNode.Arg8 {

        static {
            Casts casts = new Casts(AGrep.class);
            castPatternSingle(casts);
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
        protected Object aGrep(String pattern, RStringVector vector, boolean ignoreCase, boolean value, RIntVector costs, RDoubleVector bounds, boolean useBytes, boolean fixed,
                        @Cached("createCommon()") CommonCodeNode common) {
            // TODO implement completely; this is a very basic implementation for fixed=TRUE only.
            common.checkExtraArgs(!fixed && ignoreCase, false, false, useBytes, false);
            common.valueCheck(value);
            common.checkNotImplemented(!fixed, "fixed", false);
            int[] tmp = new int[vector.getLength()];
            int numMatches = 0;
            long maxDistance = Math.round(pattern.length() * bounds.getDataAt(0));
            for (int i = 0; i < vector.getLength(); i++) {
                int ld;
                if (ignoreCase) {
                    // reliable only with fixed=true
                    ld = ld(toLowerCase(pattern), toLowerCase(vector.getDataAt(i)));
                } else {
                    ld = ld(pattern, vector.getDataAt(i));
                }
                if (ld <= maxDistance) {
                    tmp[i] = i + 1;
                    numMatches++;
                }
            }
            tmp = common.trimIntResult(tmp, numMatches, tmp.length);
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

    @ImportStatic(GrepFunctions.class)
    @RBuiltin(name = "agrepl", kind = INTERNAL, parameterNames = {"pattern", "x", "ignore.case", "value", "costs", "bounds", "useBytes", "fixed"}, behavior = PURE)
    public abstract static class AGrepL extends RBuiltinNode.Arg8 {

        static {
            Casts casts = new Casts(AGrepL.class);
            castPatternSingle(casts);
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
        protected Object aGrep(String pattern, RStringVector vector, boolean ignoreCase, boolean value, RIntVector costs, RDoubleVector bounds, boolean useBytes, boolean fixed,
                        @Cached("createCommon()") CommonCodeNode common) {
            // TODO implement properly, this only supports strict equality!
            common.checkExtraArgs(ignoreCase, false, false, useBytes, false);
            byte[] data = new byte[vector.getLength()];
            for (int i = 0; i < vector.getLength(); i++) {
                data[i] = RRuntime.asLogical(pattern.equals(vector.getDataAt(i)));
            }
            return RDataFactory.createLogicalVector(data, RDataFactory.COMPLETE_VECTOR);
        }
    }

    @ImportStatic({GrepFunctions.class, DSLConfig.class})
    @RBuiltin(name = "strsplit", kind = INTERNAL, parameterNames = {"x", "split", "fixed", "perl", "useBytes"}, behavior = PURE)
    public abstract static class Strsplit extends RBuiltinNode.Arg5 {

        static {
            Casts casts = new Casts(Strsplit.class);
            casts.arg("x").mustBe(stringValue(), RError.Message.NON_CHARACTER);
            casts.arg("split").mustBe(stringValue(), RError.Message.NON_CHARACTER);
            castFixed(casts, RRuntime.LOGICAL_FALSE);
            castPerl(casts);
            castUseBytes(casts);
        }

        private final NACheck na = NACheck.create();

        @Specialization
        @TruffleBoundary
        protected RList split(RStringVector x, RStringVector splitArg, boolean fixed, boolean perlLogical, @SuppressWarnings("unused") boolean useBytes,
                        @Cached("createCommon()") CommonCodeNode commonNode,
                        @CachedLibrary("getInteropLibraryCacheSize()") InteropLibrary interop) {
            boolean perl = commonNode.checkPerlFixed(perlLogical, fixed);
            Object[] result = new Object[x.getLength()];
            // treat split = NULL as split = ""
            RStringVector split = splitArg.getLength() == 0 ? RDataFactory.createStringVectorFromScalar("") : splitArg;
            String[] splits = new String[split.getLength()];
            PCRE2RFFI.CompileResult[] pcrePatterns = perl ? new PCRE2RFFI.CompileResult[splits.length] : null;

            na.enable(x);
            for (int i = 0; i < splits.length; i++) {
                String currentSplit = split.getDataAt(i);
                splits[i] = fixed || perl ? split.getDataAt(i) : RegExp.transformPatternToGnurCompatible(split.getDataAt(i));
                if (perl) {
                    if (!currentSplit.isEmpty()) {
                        pcrePatterns[i] = commonNode.pcre2CompileNode.execute(currentSplit, 0);
                        if (interop.isNull(pcrePatterns[i].compiledPattern)) {
                            assert pcrePatterns[i].errorMessage != null;
                            throw error(RError.Message.INVALID_REGEXP_REASON, currentSplit, pcrePatterns[i].errorMessage);
                        }
                    }
                }
            }
            for (int i = 0; i < x.getLength(); i++) {
                String data = x.getDataAt(i);
                assert data != null;
                if (data.length() == 0) {
                    result[i] = RDataFactory.createEmptyStringVector();
                    continue;
                }
                String currentSplit = splits[i % splits.length];
                try {
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
                                resultItem = splitPerl(data, pcrePatterns[i % splits.length], commonNode);
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
                } catch (PatternSyntaxException e) {
                    throw error(Message.INVALID_REGEXP_REASON, currentSplit, e.getMessage());
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
            if (!(x instanceof RStringVector)) {
                throw error(RError.Message.NON_CHARACTER);
            } else {
                throw error(RError.Message.INVALID_OR_UNIMPLEMENTED_ARGUMENTS);
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
                result[i] = String.valueOf(input.charAt(i));
            }
            return RDataFactory.createStringVector(result, true);
        }

        private static RStringVector splitPerl(String data, PCRE2RFFI.CompileResult pcre, CommonCodeNode common) {
            int captureCount = common.pcre2CaptureCountNode.execute(pcre.compiledPattern);
            PCRE2RFFI.MatchData matchData = common.pcre2MatchNode.execute(pcre.compiledPattern, data, 0, false, captureCount);
            List<IndexRange> matches = matchData.getMatches();
            int matchCount = matchData.getMatchCount();
            assert matchCount == matches.size();
            // Matches at the end of the input are ignored in `strsplit`. See `?strsplit`.
            int validMatchCount = hasMatchAtEnd(matches, data) ? matchCount - 1 : matchCount;
            String[] result = new String[validMatchCount + 1];
            int lastMatchEndIdx = 0;
            for (int i = 0; i < validMatchCount; i++) {
                IndexRange match = matches.get(i);
                result[i] = data.substring(lastMatchEndIdx, match.startIdx);
                lastMatchEndIdx = match.endIdx;
            }
            if (hasMatchAtEnd(matches, data)) {
                assert matches.size() > 0;
                IndexRange lastMatch = matches.get(matches.size() - 1);
                result[result.length - 1] = data.substring(lastMatchEndIdx, lastMatch.startIdx);
            } else {
                result[result.length - 1] = data.substring(lastMatchEndIdx);
            }
            return RDataFactory.createStringVector(result, RDataFactory.COMPLETE_VECTOR);
        }

        /**
         * Is there a match at the end of {@code input}?
         * 
         * @param matches Matches from {@link com.oracle.truffle.r.runtime.ffi.PCRE2RFFI.MatchData}.
         */
        private static boolean hasMatchAtEnd(List<IndexRange> matches, String input) {
            if (matches.size() == 0) {
                return false;
            }
            IndexRange lastMatch = matches.get(matches.size() - 1);
            return lastMatch.endIdx == input.length();
        }
    }

    @RBuiltin(name = "grepRaw", kind = INTERNAL, parameterNames = {"pattern", "x", "offset", "ignore.case", "fixed", "value", "all", "invert"}, behavior = PURE)
    public abstract static class GrepRaw extends RBuiltinNode.Arg8 {
        static {
            Casts casts = new Casts(GrepRaw.class);
            casts.arg("pattern").asRawVector();
            casts.arg("x").asRawVector();
            casts.arg("offset").asIntegerVector().findFirst(1);
            casts.arg("ignore.case").asLogicalVector().findFirst().map(toBoolean());
            casts.arg("fixed").asLogicalVector().findFirst().map(toBoolean());
            casts.arg("value").asLogicalVector().findFirst().map(toBoolean());
            casts.arg("all").asLogicalVector().findFirst().map(toBoolean());
            casts.arg("invert").asLogicalVector().findFirst().map(toBoolean());
        }

        /**
         * After haystack is searched for pattern, it is divided into ranges which are either
         * matched or unmatched. This class contains all these ranges.
         *
         * Note that there cannot be two adjacent unmatched ranges.
         */
        public static class HaystackDescriptor implements Iterable<Range> {
            private ArrayListObj<Range> ranges = new ArrayListObj<>();
            private RRawVector haystack;
            private int matchedRangesCount;
            private int unmatchedRangesCount;

            public HaystackDescriptor(RRawVector haystack) {
                this.haystack = haystack;
            }

            public RRawVector getHaystack() {
                return haystack;
            }

            public void addRange(Range range) {
                if (ranges.size() > 1) {
                    Range lastRange = ranges.get(ranges.size() - 1);
                    assert !(lastRange.isUnmatched() && range.isUnmatched());
                }

                ranges.add(range);

                if (range.isMatched()) {
                    matchedRangesCount++;
                } else {
                    unmatchedRangesCount++;
                }
            }

            public int getRangesCount() {
                return ranges.size();
            }

            public int getMatchedRangesCount() {
                return matchedRangesCount;
            }

            public int getUnmatchedRangesCount() {
                return unmatchedRangesCount;
            }

            public Range getRange(int index) {
                return ranges.get(index);
            }

            @Override
            public Iterator<Range> iterator() {
                return new Iterator<Range>() {
                    private int currIdx = 0;

                    @Override
                    public boolean hasNext() {
                        return currIdx < ranges.size();
                    }

                    @Override
                    public Range next() {
                        return ranges.get(currIdx++);
                    }
                };
            }
        }

        public static class Range {
            private int fromIdx;
            private int toIdx;
            private boolean matched;

            public Range(int fromIdx, int toIdx, boolean matched) {
                assert fromIdx < toIdx;
                this.fromIdx = fromIdx;
                this.toIdx = toIdx;
                this.matched = matched;
            }

            public boolean isMatched() {
                return matched;
            }

            public boolean isUnmatched() {
                return !matched;
            }

            public int getFromIdx() {
                return fromIdx;
            }

            public int getToIdx() {
                return toIdx;
            }

            public int size() {
                return toIdx - fromIdx;
            }
        }

        public static final class FixedPatternFinder {
            private RRawVector pattern;
            private RRawVector haystack;
            private int haystackIdx;

            public FixedPatternFinder(RRawVector pattern, RRawVector haystack) {
                this.pattern = pattern;
                this.haystack = haystack;
                this.haystackIdx = 0;
            }

            public FixedPatternFinder(RRawVector pattern, RRawVector haystack, int offset) {
                assert 1 <= offset && offset <= haystack.getLength();
                this.pattern = pattern;
                this.haystack = haystack;
                this.haystackIdx = offset - 1;
            }

            public HaystackDescriptor findFirst() {
                HaystackDescriptor haystackDescriptor = new HaystackDescriptor(haystack);
                int firstMatchedIndex = findNextIndex();

                if (firstMatchedIndex == -1) {
                    haystackDescriptor.addRange(new Range(0, haystack.getLength(), false));
                    return haystackDescriptor;
                }

                if (firstMatchedIndex > 0) {
                    haystackDescriptor.addRange(new Range(0, firstMatchedIndex, false));
                }
                haystackDescriptor.addRange(
                                new Range(firstMatchedIndex, firstMatchedIndex + pattern.getLength(), true));
                if (firstMatchedIndex + pattern.getLength() < haystack.getLength()) {
                    haystackDescriptor.addRange(
                                    new Range(firstMatchedIndex + pattern.getLength(), haystack.getLength(), false));
                }
                return haystackDescriptor;
            }

            public HaystackDescriptor findAll() {
                HaystackDescriptor haystackDescriptor = new HaystackDescriptor(haystack);
                int endOfLastMatch = 0;
                int index;
                while ((index = findNextIndex()) != -1) {
                    if (endOfLastMatch < index) {
                        // Create unmatched range [endOfLastMatch, index].
                        haystackDescriptor.addRange(new Range(endOfLastMatch, index, false));
                    }
                    // Create matched range [index, index + len(pattern)].
                    haystackDescriptor.addRange(new Range(index, index + pattern.getLength(), true));
                    endOfLastMatch = index + pattern.getLength();
                }

                // Create last unmatched range if necessary.
                if (endOfLastMatch < haystack.getLength()) {
                    haystackDescriptor.addRange(new Range(endOfLastMatch, haystack.getLength(), false));
                }

                return haystackDescriptor;
            }

            private int findNextIndex() {
                for (int i = haystackIdx; i < haystack.getLength(); i++) {
                    if (patternMatchesAtIndex(i)) {
                        haystackIdx = i + pattern.getLength();
                        return i;
                    }
                }
                return -1;
            }

            private boolean patternMatchesAtIndex(int idx) {
                for (int patternIdx = 0; patternIdx < pattern.getLength(); patternIdx++) {
                    if (pattern.getRawDataAt(patternIdx) != haystack.getRawDataAt(idx + patternIdx)) {
                        return false;
                    }
                }
                return true;
            }
        }

        /**
         * Invert all values for all=TRUE. More specifically for value=T, invert=T, all=T.
         */
        private static RList invertAllMatches(HaystackDescriptor haystackDescriptor) {
            ArrayListObj<RRawVector> vectors = new ArrayListObj<>();

            if (haystackDescriptor.getRangesCount() == 1) {
                Range range = haystackDescriptor.getRange(0);
                if (range.isMatched()) {
                    // Return list([], []).
                    Object[] data = new Object[2];
                    data[0] = RDataFactory.createEmptyRawVector();
                    data[1] = RDataFactory.createEmptyRawVector();
                    return RDataFactory.createList(data);
                } else {
                    // Return list(haystack).
                    RList list = RDataFactory.createList(1);
                    list.setDataAt(0, haystackDescriptor.getHaystack());
                    return list;
                }
            }

            for (int i = 0; i < haystackDescriptor.getRangesCount(); i++) {
                Range range = haystackDescriptor.getRange(i);
                Range previousRange = null;
                if (i > 0) {
                    previousRange = haystackDescriptor.getRange(i - 1);
                }

                if (previousRange != null) {
                    if (range.isMatched() && previousRange.isMatched()) {
                        vectors.add(RDataFactory.createEmptyRawVector());
                    }
                }

                if (isRangeFirstInHaystack(range) && range.isMatched() ||
                                isRangeLastInHaystack(range, haystackDescriptor) && range.isMatched()) {
                    vectors.add(RDataFactory.createEmptyRawVector());
                }

                if (range.isUnmatched()) {
                    vectors.add(constructVectorFromRange(range, haystackDescriptor));
                }
            }

            Object[] data = vectors.toArray();
            return RDataFactory.createList(data);
        }

        /**
         * Invert all values for all=FALSE. More specifically for value=T, invert=T, all=F.
         */
        private static RRawVector invertFirstMatch(HaystackDescriptor haystackDescriptor) {
            final RRawVector haystack = haystackDescriptor.getHaystack();
            byte[] unmatchedData = new byte[haystack.getLength()];
            int unmatchedDataIdx = 0;
            for (Range range : haystackDescriptor) {
                if (range.isUnmatched()) {
                    for (int i = range.getFromIdx(); i < range.getToIdx(); i++) {
                        unmatchedData[unmatchedDataIdx++] = haystack.getRawDataAt(i);
                    }
                }
            }

            byte[] unmatchedDataFit = Arrays.copyOf(unmatchedData, unmatchedDataIdx);
            return RDataFactory.createRawVector(unmatchedDataFit);
        }

        private static boolean isRangeFirstInHaystack(Range range) {
            return range.getFromIdx() == 0;
        }

        private static boolean isRangeLastInHaystack(Range range, HaystackDescriptor haystackDescriptor) {
            return range.getToIdx() >= haystackDescriptor.getHaystack().getLength();
        }

        private static RRawVector constructVectorFromRange(Range range, HaystackDescriptor haystackDescriptor) {
            byte[] data = new byte[range.size()];
            for (int idx = range.fromIdx, dataIdx = 0; idx < range.toIdx; idx++, dataIdx++) {
                data[dataIdx] = haystackDescriptor.getHaystack().getRawDataAt(idx);
            }
            return RDataFactory.createRawVector(data);
        }

        private static HaystackDescriptor findFirstOccurrence(RRawVector pattern, RRawVector rawVector, int offset) {
            FixedPatternFinder patternFinder = new FixedPatternFinder(pattern, rawVector, offset);
            return patternFinder.findFirst();
        }

        private static HaystackDescriptor findAllOccurrences(RRawVector pattern, RRawVector rawVector, int offset) {
            FixedPatternFinder patternFinder = new FixedPatternFinder(pattern, rawVector, offset);
            return patternFinder.findAll();
        }

        private static Object createEmptyReturnValue(boolean valueArgument, boolean allArgument) {
            if (valueArgument && allArgument) {
                return RDataFactory.createList();
            } else if (valueArgument && !allArgument) {
                return RDataFactory.createEmptyRawVector();
            } else {
                return RDataFactory.createEmptyIntVector();
            }
        }

        private static RIntVector convertMatchedRangesToRIndices(HaystackDescriptor haystackDescriptor) {
            int[] rVectorIndices = new int[haystackDescriptor.getMatchedRangesCount()];
            int vectorIdx = 0;
            for (Range range : haystackDescriptor) {
                if (range.isMatched()) {
                    rVectorIndices[vectorIdx++] = range.getFromIdx() + 1;
                }
            }
            return RDataFactory.createIntVector(rVectorIndices, true);
        }

        private static RIntVector convertFirstMatchedRangeToRIndex(HaystackDescriptor haystackDescriptor) {
            Range firstMatchedRange = null;
            for (Range range : haystackDescriptor) {
                if (range.isMatched()) {
                    firstMatchedRange = range;
                    break;
                }
            }
            assert firstMatchedRange != null;
            return RDataFactory.createIntVector(new int[]{firstMatchedRange.getFromIdx() + 1}, true);
        }

        @Specialization(guards = {"fixed", "!invert"})
        protected Object grepFixedNoInvert(RRawVector pattern, RRawVector x, int offset, @SuppressWarnings("unused") Object ignoreCase, @SuppressWarnings("unused") boolean fixed, boolean value,
                        boolean all,
                        @SuppressWarnings("unused") boolean invert) {
            if (x.getLength() == 0) {
                return RDataFactory.createEmptyIntVector();
            }
            HaystackDescriptor haystackDescriptor = null;
            if (all) {
                haystackDescriptor = findAllOccurrences(pattern, x, offset);
            } else {
                haystackDescriptor = findFirstOccurrence(pattern, x, offset);
            }
            final int matchedRangesCount = haystackDescriptor.getMatchedRangesCount();

            if (matchedRangesCount == 0) {
                return createEmptyReturnValue(value, all);
            }

            if (all) {
                if (value) {
                    // Return list(pattern, pattern, ...)
                    Object[] data = new Object[matchedRangesCount];
                    Arrays.fill(data, pattern);
                    return RDataFactory.createList(data);
                } else {
                    return convertMatchedRangesToRIndices(haystackDescriptor);
                }
            } else {
                if (value) {
                    return pattern;
                } else {
                    return convertFirstMatchedRangeToRIndex(haystackDescriptor);
                }
            }
        }

        @Specialization(guards = {"fixed", "value", "invert"})
        protected Object grepFixedInvert(RRawVector pattern, RRawVector x, int offset, @SuppressWarnings("unused") Object ignoreCase, @SuppressWarnings("unused") boolean fixed,
                        @SuppressWarnings("unused") boolean value, boolean all,
                        @SuppressWarnings("unused") boolean invert) {
            if (x.getLength() == 0) {
                return RDataFactory.createEmptyIntVector();
            }
            HaystackDescriptor haystackDescriptor = null;
            if (all) {
                haystackDescriptor = findAllOccurrences(pattern, x, offset);
            } else {
                haystackDescriptor = findFirstOccurrence(pattern, x, offset);
            }

            if (all) {
                return invertAllMatches(haystackDescriptor);
            } else {
                return invertFirstMatch(haystackDescriptor);
            }
        }

        @Specialization(guards = {"fixed", "invert", "!value"})
        protected Object grepFixedIgnoreInvert(RRawVector pattern, RRawVector x, int offset, @SuppressWarnings("unused") Object ignoreCase, boolean fixed, boolean value, boolean all,
                        boolean invert) {
            warning(Message.ARGUMENT_IGNORED, "invert = TRUE");
            if (x.getLength() == 0) {
                return RDataFactory.createEmptyIntVector();
            }
            return grepFixedNoInvert(pattern, x, offset, ignoreCase, fixed, value, all, invert);
        }

        @Specialization(guards = "!fixed")
        protected Object grepUnfixed(@SuppressWarnings("unused") RRawVector pattern, @SuppressWarnings("unused") RRawVector x, @SuppressWarnings("unused") Object offset,
                        @SuppressWarnings("unused") Object ignoreCase, @SuppressWarnings("unused") boolean fixed,
                        @SuppressWarnings("unused") boolean value, @SuppressWarnings("unused") boolean all,
                        @SuppressWarnings("unused") boolean invert) {
            throw RInternalError.unimplemented("grepRaw with fixed = FALSE");
        }
    }
}
