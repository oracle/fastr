/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.oracle.truffle.r.runtime.ffi;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.TruffleLogger;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RLogger;
import com.oracle.truffle.r.runtime.ffi.interop.NativeCharArray;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;

/**
 * An interface to the <a href="https://www.pcre.org/current/doc/html/index.html">PCRE2</a> library
 * for Perl regular expressions.
 */
public final class PCRE2RFFI {
    private static final TruffleLogger logger = RLogger.getLogger(RLogger.LOGGER_PCRE);

    /**
     * Transcribed from pcre2.h. The enumeration is not complete.
     */
    public enum Error {
        // Errors from matching:
        NOMATCH(-1),
        PARTIAL(-2),
        BADDATA(-29);

        final int value;

        Error(int value) {
            this.value = value;
        }
    }

    /**
     * Options transcribed from pcre2.h. The enumeration is not complete.
     */
    public enum Option {
        ALLOW_EMPTY_CLASS(Integer.decode("0x00000001")),
        UTF(Integer.decode("0x00080000")),
        MULTILINE(Integer.decode("0x00000400")),
        CASELESS(Integer.decode("0x00000008")),
        FIRSTLINE(Integer.decode("0x00000100"));

        public final int value;

        Option(int value) {
            this.value = value;
        }
    }

    private final DownCallNodeFactory downCallNodeFactory;

    public PCRE2RFFI(DownCallNodeFactory downCallNodeFactory) {
        this.downCallNodeFactory = downCallNodeFactory;
    }

    /**
     * PCRE uses call by reference for error-related information, which we encapsulate and sanitize
     * in this class. The {@code compiledPattern} value (which is typically an opaque pointer to an
     * internal C struct), is the actual result of the function as per the PCRE2 spec.
     */
    public static final class CompileResult {
        // Pointer to the pcre2_code data, NULL if a pattern-compile error occured.
        public final Object compiledPattern;
        public final int errorCode;
        public final String errorMessage;
        public final int errOffset;

        CompileResult(Object compiledPattern, int errorCode, String errorMessage, int errOffset) {
            this.compiledPattern = compiledPattern;
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
            this.errOffset = errOffset;
        }
    }

    public static final class MatchData {
        private int matchCount;
        private final int captureCount;
        private final List<IndexRange> matches = new ArrayList<>();
        /**
         * Keys are capture indexes, as reported by PCRE. They are in range [0, max_capture_num],
         * where max_capture_num is reported by {@link GetCaptureCountNode}. We need to have a map
         * here, because there may be more than one match for a specific capture.
         *
         * If there are no matches for a specific capture, the value (the list) of this map is
         * padded with empty {@link IndexRange} (which is {@code (0,0)}).
         */
        private Map<Integer, List<IndexRange>> captures = new HashMap<>();
        private int maxCaptureMatchCount;

        /**
         * @param captureCount Capture count as reported by PCRE, i.e., by
         *            {@link GetCaptureCountNode}.
         */
        public MatchData(int captureCount) {
            this.captureCount = captureCount;
            for (int i = 0; i < captureCount; i++) {
                captures.put(i, new ArrayList<>());
            }
        }

        void addMatch(int startIdx, int endIdx) {
            IndexRange match = new IndexRange(startIdx, endIdx);
            addMatch(match);
        }

        public void addMatch(IndexRange match) {
            matches.add(match);
            matchCount++;
        }

        void addCapture(int captureIdx, int startIdx, int endIdx) {
            addCapture(captureIdx, new IndexRange(startIdx, endIdx));
        }

        public void addCapture(int captureIdx, IndexRange captureRange) {
            assert 0 <= captureIdx && captureIdx < captureCount;
            if (!captures.containsKey(captureIdx)) {
                captures.put(captureIdx, new ArrayList<>());
            }
            List<IndexRange> captureMatches = captures.get(captureIdx);
            captureMatches.add(captureRange);
            maxCaptureMatchCount = Math.max(captureMatches.size(), maxCaptureMatchCount);
        }

        /**
         * There may be some captures with least amount of matches than other captures. There may
         * even be some captures that were not reported by PCRE at all, and so they have zero
         * matches. We want to pad these captures with empty matches so that every capture has the
         * same amount of matches.
         *
         * Calling this method makes this instance effectively immutable, so you should not add any
         * more captures via {@link #addCapture(int, IndexRange)}.
         */
        public void padCapturesWithEmptyMatches() {
            logger.fine(() -> "MatchData: padCapturesWithEmptyMatches");
            for (List<IndexRange> captureMatches : captures.values()) {
                assert captureMatches.size() <= maxCaptureMatchCount;
                if (captureMatches.size() < maxCaptureMatchCount) {
                    for (int i = captureMatches.size(); i < maxCaptureMatchCount; i++) {
                        captureMatches.add(new IndexRange(0, 0));
                    }
                }
            }
        }

        public int getMatchCount() {
            assert matchCount == matches.size();
            return matchCount;
        }

        public Map<Integer, List<IndexRange>> getCaptures() {
            return captures;
        }

        public List<IndexRange> getMatches() {
            return matches;
        }

        /**
         * Returns the count of all the matches of all the captures. Usually, there is at most one
         * match per one capture.
         */
        public int getCaptureMatchesCount() {
            int totalCaptureMatches = 0;
            for (List<IndexRange> captureMatches : captures.values()) {
                totalCaptureMatches += captureMatches.size();
            }
            return totalCaptureMatches;
        }

        @Override
        @CompilerDirectives.TruffleBoundary
        public String toString() {
            return String.format("MatchData{matches=%s, captures=%s}",
                            Arrays.toString(matches.toArray()), capturesToString());
        }

        private String capturesToString() {
            if (captures.isEmpty()) {
                return "{}";
            }
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            for (Map.Entry<Integer, List<IndexRange>> entry : captures.entrySet()) {
                int captureIdx = entry.getKey();
                List<IndexRange> captureMatches = entry.getValue();
                assert captureMatches != null;
                sb.append(captureIdx).append(":").append(captureMatches);
                sb.append(",");
            }
            // Delete last comma
            sb.deleteCharAt(sb.length() - 1);
            sb.append("}");
            return sb.toString();
        }
    }

    public static final class IndexRange {
        public final int startIdx;
        public final int endIdx;

        public IndexRange(int startIdx, int endIdx) {
            this.startIdx = startIdx;
            this.endIdx = endIdx;
        }

        public boolean isEmpty() {
            return startIdx == endIdx;
        }

        @Override
        public String toString() {
            return "(" + startIdx + "," + endIdx + ")";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            IndexRange that = (IndexRange) o;
            return startIdx == that.startIdx && endIdx == that.endIdx;
        }

        @Override
        public int hashCode() {
            return Objects.hash(startIdx, endIdx);
        }
    }

    /**
     * This is called from the native when a match occurs.
     */
    @ExportLibrary(InteropLibrary.class)
    @ImportStatic(DSLConfig.class)
    public static class MatchCallback implements TruffleObject {
        private final MatchData matchData;

        private MatchCallback(MatchData matchData) {
            this.matchData = matchData;
        }

        @ExportMessage
        boolean isExecutable() {
            return true;
        }

        /**
         * Signature of the corresponding native callback:
         * {@code void match_callback(size_t start_idx, size_t end_idx)}
         */
        @ExportMessage
        Object execute(Object[] args, @CachedLibrary(limit = "getInteropLibraryCacheSize()") InteropLibrary interop) {
            // Arguments are `size_t start_idx` and `end_idx`.
            assert args.length == 2;
            assert interop.fitsInLong(args[0]);
            assert interop.fitsInLong(args[1]);
            int startIdx;
            int endIdx;
            try {
                startIdx = interop.asInt(args[0]);
                endIdx = interop.asInt(args[1]);
            } catch (UnsupportedMessageException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
            logger.fine(() -> String.format("MatchCallback: (%d, %d)", startIdx, endIdx));
            matchData.addMatch(startIdx, endIdx);
            return 0;
        }
    }

    /**
     * This is called from native when a match of a capture occurs.
     */
    @ExportLibrary(InteropLibrary.class)
    @ImportStatic(DSLConfig.class)
    public static class CaptureCallback implements TruffleObject {
        private final MatchData matchData;

        CaptureCallback(MatchData matchData) {
            this.matchData = matchData;
        }

        @ExportMessage
        boolean isExecutable() {
            return true;
        }

        /**
         * Signature of the corresponding native callback:
         * {@code void capture_callback(size_t capture_idx, size_t start_idx, size_t end_idx)}
         */
        @ExportMessage
        Object execute(Object[] args, @CachedLibrary(limit = "getInteropLibraryCacheSize()") InteropLibrary interop) {
            assert args.length == 3;
            assert interop.fitsInLong(args[0]);
            assert interop.fitsInLong(args[1]);
            assert interop.fitsInLong(args[2]);
            int captureIdx;
            int startIdx;
            int endIdx;
            try {
                captureIdx = interop.asInt(args[0]);
                startIdx = interop.asInt(args[1]);
                endIdx = interop.asInt(args[2]);
            } catch (UnsupportedMessageException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
            logger.fine(() -> String.format("Capture: capture_idx=%d, range=(%d, %d)", captureIdx, startIdx, endIdx));
            matchData.addCapture(captureIdx, startIdx, endIdx);
            return 0;
        }
    }

    @ExportLibrary(InteropLibrary.class)
    @ImportStatic(DSLConfig.class)
    public static class SetCaptureNameCallback implements TruffleObject {
        private final String[] captureNames;

        public SetCaptureNameCallback(int maxCaptureCount) {
            captureNames = new String[maxCaptureCount];
        }

        public String[] getCaptureNames() {
            return captureNames;
        }

        @ExportMessage
        boolean isExecutable() {
            return true;
        }

        @ExportMessage
        Object execute(Object[] args, @CachedLibrary(limit = "getInteropLibraryCacheSize()") InteropLibrary interop) {
            assert args.length == 2;
            assert interop.isString(args[0]);
            assert interop.fitsInInt(args[1]);
            String name;
            int captureIdx;
            try {
                name = interop.asString(args[0]);
                captureIdx = interop.asInt(args[1]);
            } catch (UnsupportedMessageException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
            logger.fine(() -> String.format("SetCaptureNameCallback: capture_idx=%d, name=%s", captureIdx, name));
            set(captureIdx, name);
            return 0;
        }

        private void set(int idx, String name) {
            captureNames[idx] = name;
        }
    }

    public static final class CompileNode extends NativeCallNode {
        public CompileNode(DownCallNodeFactory downCallNodeFactory) {
            super(downCallNodeFactory.createDownCallNode());
        }

        public static CompileNode create() {
            return RFFIFactory.getPCRE2RFFI().createCompileNode();
        }

        @CompilerDirectives.TruffleBoundary
        public CompileResult execute(String pattern, int options) {
            int[] errorCode = new int[]{0};
            int[] errorOffSet = new int[]{0};
            Object pcreCode = call(NativeFunction.compile, pattern, options, errorCode, errorOffSet);
            // TODO: Fill in error message if necessary.
            return new CompileResult(pcreCode, errorCode[0], null, errorOffSet[0]);
        }
    }

    public static final class GetCaptureCountNode extends NativeCallNode {
        @Child private InteropLibrary interop = InteropLibrary.getFactory().createDispatched(DSLConfig.getInteropLibraryCacheSize());

        public GetCaptureCountNode(DownCallNodeFactory downCallNodeFactory) {
            super(downCallNodeFactory.createDownCallNode());
        }

        public int execute(Object pcrePattern) {
            Object captureCount = call(NativeFunction.capture_count, pcrePattern);
            assert interop.isNumber(captureCount);
            int captureCountInt = -1;
            try {
                captureCountInt = interop.asInt(captureCount);
            } catch (UnsupportedMessageException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
            logger.fine("GetCaptureCountNode.execute = " + captureCountInt);
            return captureCountInt;
        }
    }

    public static final class GetCaptureNamesNode extends NativeCallNode {
        @Child private InteropLibrary interop = InteropLibrary.getFactory().createDispatched(DSLConfig.getInteropLibraryCacheSize());

        public GetCaptureNamesNode(DownCallNodeFactory downCallNodeFactory) {
            super(downCallNodeFactory.createDownCallNode());
        }

        public String[] execute(Object pcrePattern, int captureCount) {
            SetCaptureNameCallback setCaptureNameCallback = new SetCaptureNameCallback(captureCount);
            Object namesCountNative = call(NativeFunction.get_capture_names, setCaptureNameCallback, pcrePattern);
            assert interop.isNumber(namesCountNative);
            int namesCount = 0;
            try {
                namesCount = interop.asInt(namesCountNative);
            } catch (UnsupportedMessageException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
            String[] captureNames = setCaptureNameCallback.getCaptureNames();
            // Length is the same, some elements may be null though.
            assert captureNames.length == captureCount;
            assert namesCount <= captureCount;
            logger.fine(() -> "GetCaptureNamesNode = " + Arrays.toString(captureNames));
            return captureNames;
        }
    }

    public static final class MatchNode extends NativeCallNode {
        public MatchNode(DownCallNodeFactory downCallNodeFactory) {
            super(downCallNodeFactory.createDownCallNode());
        }

        public static MatchNode create() {
            return RFFIFactory.getPCRE2RFFI().createMatchNode();
        }

        /**
         * Performs a match in the text with the given compiled pattern.
         *
         * @param pcreCompiledPattern A PCRE-specific pattern obtained with {@link CompileNode}.
         * @param subject Text to be searched.
         * @param options See {@link Option}.
         */
        @CompilerDirectives.TruffleBoundary
        public MatchData execute(Object pcreCompiledPattern, String subject, int options, boolean stopAfterFirstMatch, int captureCount) {
            MatchData matchData = new MatchData(captureCount);
            MatchCallback matchCallback = new MatchCallback(matchData);
            CaptureCallback captureCallback = new CaptureCallback(matchData);
            byte[] subjectBytes = subject.getBytes(StandardCharsets.UTF_8);
            NativeCharArray nativeCharArray = new NativeCharArray(subjectBytes);
            Object matchCount = call(NativeFunction.match, matchCallback, captureCallback,
                            pcreCompiledPattern, nativeCharArray, options, stopAfterFirstMatch ? 1 : 0);
            assert InteropLibrary.getUncached().isNumber(matchCount);
            int matchCountInt;
            try {
                matchCountInt = InteropLibrary.getUncached().asInt(matchCount);
            } catch (UnsupportedMessageException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
            assert matchCountInt == matchData.getMatchCount();
            matchData = convertIndexes(matchData, subject, subjectBytes, captureCount);
            matchData.padCapturesWithEmptyMatches();
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Returning from MatchNode.execute: " + matchData);
            }
            return matchData;
        }

        /**
         * When a match or capture callback is made from PCRE2, the indexes in these callbacks are
         * indexes into the byte array, not indexes into Java string. We have to convert these
         * indexes to indexes into Java string.
         *
         * @return A copy of match data with converted indexes.
         */
        private static MatchData convertIndexes(MatchData matchData, String subject, byte[] subjectBytes, int captureCount) {
            if (subject.length() == subjectBytes.length) {
                return matchData;
            }
            MatchData newMatchData = new MatchData(captureCount);
            assert subjectBytes.length > subject.length();
            int[] bytesToStrIndexes = bytesToStrIndexMapping(subject, subjectBytes);
            assert bytesToStrIndexes.length == subjectBytes.length;
            // Convert indexes in all the matches.
            List<IndexRange> convertedMatches = convertListOfIndexes(matchData.matches, bytesToStrIndexes, subject, subjectBytes);
            for (IndexRange convertedMatch : convertedMatches) {
                newMatchData.addMatch(convertedMatch);
            }
            // Convert indexes in all the captures.
            Map<Integer, List<IndexRange>> convertedCaptures = new HashMap<>();
            for (Map.Entry<Integer, List<IndexRange>> entry : matchData.captures.entrySet()) {
                int captureIdx = entry.getKey();
                List<IndexRange> captureMatches = entry.getValue();
                assert captureMatches != null;
                List<IndexRange> convertedCaptureMatches = convertListOfIndexes(captureMatches, bytesToStrIndexes, subject, subjectBytes);
                convertedCaptures.put(captureIdx, convertedCaptureMatches);
            }
            newMatchData.captures = convertedCaptures;
            return newMatchData;
        }

        private static List<IndexRange> convertListOfIndexes(List<IndexRange> rangeList, int[] bytesToStrIndexes, String subject, byte[] subjectBytes) {
            List<IndexRange> convertedRanges = new ArrayList<>(rangeList.size());
            for (IndexRange range : rangeList) {
                int newStartIdx = (range.startIdx == subjectBytes.length) ? subject.length() : bytesToStrIndexes[range.startIdx];
                int newEndIdx = (range.endIdx == subjectBytes.length) ? subject.length() : bytesToStrIndexes[range.endIdx];
                if (convertedRanges.size() > 0) {
                    IndexRange prevNewRange = convertedRanges.get(convertedRanges.size() - 1);
                    // Check whether we just created a duplicate range.
                    // Generally, we are OK with duplicate ranges - there may be in captures.
                    // But if the index conversion has just created a duplicate range, we do not
                    // want to store this duplicate.
                    if ((newStartIdx != range.startIdx || newEndIdx != range.endIdx) && prevNewRange.startIdx == newStartIdx && prevNewRange.endIdx == newEndIdx) {
                        // We have just created a duplicate - do not store it.
                        continue;
                    }
                }
                convertedRanges.add(new IndexRange(newStartIdx, newEndIdx));
            }
            return convertedRanges;
        }

        /**
         * Returns an array of indexes with the same length as given {@code strBytes}, where in
         * {@code array[i]}, there is an index into {@code str}. In other words, returns an array of
         * indexes that maps indexes of bytes into the String.
         */
        private static int[] bytesToStrIndexMapping(String str, byte[] strBytes) {
            assert str.length() < strBytes.length;
            int indexMappingIdx = 0;
            int[] indexMapping = new int[strBytes.length];
            for (int strIdx = 0; strIdx < str.length(); strIdx++) {
                String subStr = str.substring(strIdx, strIdx + 1);
                byte[] subStrBytes = subStr.getBytes(StandardCharsets.UTF_8);
                for (int i = 0; i < subStrBytes.length; i++) {
                    indexMapping[indexMappingIdx] = strIdx;
                    indexMappingIdx++;
                }
            }
            return indexMapping;
        }
    }

    public static final class MemoryReleaseNode extends NativeCallNode {
        public MemoryReleaseNode(DownCallNodeFactory downCallNodeFactory) {
            super(downCallNodeFactory.createDownCallNode());
        }

        public void execute(Object pcreCompiledPattern) {
            assert !InteropLibrary.getUncached().isNull(pcreCompiledPattern);
            call(NativeFunction.pattern_free, pcreCompiledPattern);
        }

        public static MemoryReleaseNode create() {
            return RFFIFactory.getPCRE2RFFI().createMemoryReleaseNode();
        }
    }

    public MemoryReleaseNode createMemoryReleaseNode() {
        return new MemoryReleaseNode(downCallNodeFactory);
    }

    public CompileNode createCompileNode() {
        return new CompileNode(downCallNodeFactory);
    }

    public MatchNode createMatchNode() {
        return new MatchNode(downCallNodeFactory);
    }

    public GetCaptureNamesNode createGetCaptureNamesNode() {
        return new GetCaptureNamesNode(downCallNodeFactory);
    }

    public GetCaptureCountNode createGetCaptureCountNode() {
        return new GetCaptureCountNode(downCallNodeFactory);
    }
}
