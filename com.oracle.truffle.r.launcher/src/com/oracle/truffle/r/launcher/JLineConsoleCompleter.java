/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.launcher;

import java.util.Arrays;
import java.util.List;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.shadowed.org.jline.reader.Candidate;

import org.graalvm.shadowed.org.jline.reader.Completer;
import org.graalvm.shadowed.org.jline.reader.LineReader;
import org.graalvm.shadowed.org.jline.reader.ParsedLine;

public class JLineConsoleCompleter implements Completer {

    private static boolean isTesting = false;
    private final Context context;

    public static void testingMode() {
        isTesting = true;
    }

    public JLineConsoleCompleter(Context context) {
        this.context = context;
    }

    @Override
    public void complete(LineReader reader, ParsedLine pl, List<Candidate> candidates) {
        try {
            // need the cursor pos in whole line for R completion func
            int cursor = 0;
            String line = pl.line();
            String word = pl.word();
            int wordCursor = pl.wordCursor();
            if (!word.isEmpty()) {
                // there might be 'gaps' of more the 1 delimiter,
                // so do not simply count words length + delimiter size
                for (int i = 0; i < pl.wordIndex(); i++) {
                    String w = pl.words().get(i);
                    cursor = line.indexOf(w, cursor);
                    cursor += w.length();
                }
                cursor = line.indexOf(word, cursor);
                cursor += wordCursor;
            } else {
                cursor = line.length();
            }

            complete(line, cursor, word, wordCursor, candidates);
        } catch (Throwable e) {
            if (isTesting) {
                throw e;
            }
            throw RMain.fatal(e, "error while determining completion");
        }
    }

    private static final Source GET_COMPLETION_FUNCTION = Source.newBuilder("R", "utils:::.completeToken", "<completion>").internal(true).buildLiteral();
    private static final Source GET_COMPLETION_ENV = Source.newBuilder("R", "utils:::.CompletionEnv", "<completion>").internal(true).buildLiteral();
    private static final Source SET_FUNCTION = Source.newBuilder("R", "`$<-`", "<completion>").internal(true).buildLiteral();

    // public for testing purposes
    public void complete(String buffer, int cursor, String word, int wordCursor, List<Candidate> candidates) {
        if (buffer.isEmpty()) {
            return;
        }
        Value completionFunction = context.eval(GET_COMPLETION_FUNCTION);
        Value completionEnv = context.eval(GET_COMPLETION_ENV);
        Value setFunction = context.eval(SET_FUNCTION);

        int start = getStart(buffer, completionEnv, cursor);
        setFunction.execute(completionEnv, "start", start);
        setFunction.execute(completionEnv, "end", cursor);
        setFunction.execute(completionEnv, "linebuffer", buffer);
        setFunction.execute(completionEnv, "token", start > -1 && start < buffer.length() && cursor > -1 && cursor <= buffer.length() ? buffer.substring(start, cursor).trim() : "");

        completionFunction.execute();

        if (completionEnv.hasMember("comps")) {
            Value completions = completionEnv.getMember("comps");
            int bracketIdx = buffer.lastIndexOf('(', cursor);
            int quoteIdx = isInQuotes(buffer, cursor);
            if (completions.isString()) {
                String res = completions.asString();
                String value = getValue(res, bracketIdx, quoteIdx, word, wordCursor);
                candidates.add(new Candidate(value, res, null, null, null, null, false));
            } else if (completions.hasArrayElements()) {
                long length = completions.getArraySize();
                String[] result = new String[(int) length];
                for (int i = 0; i < length; i++) {
                    result[i] = completions.getArrayElement(i).asString();
                }
                Arrays.sort(result, String.CASE_INSENSITIVE_ORDER);
                for (int i = 0; i < length; i++) {
                    String res = result[i];
                    String value = getValue(res, bracketIdx, quoteIdx, word, wordCursor);
                    candidates.add(new Candidate(value, res, null, null, null, null, false));
                }
            }
        }
    }

    private static String getValue(String res, int leftBracketIdx, int quoteIdx, String word, int wordCursor) {
        if (quoteIdx > leftBracketIdx) {
            return merge(word, res, wordCursor, true);
        }
        if (leftBracketIdx > -1) {
            return merge(word, res, wordCursor, false);
        }
        return res;
    }

    private static String merge(String word, String result, int wordCursor, boolean isPaths) {
        if (result.startsWith(word)) {
            return result;
        }

        if (word.endsWith(result)) {
            return word;
        }

        int idx = 1;
        boolean wasMatch = false;
        String match = null;
        if (wordCursor == word.length()) {
            while (idx <= word.length() && idx <= result.length()) {
                String wordEnd = word.substring(word.length() - idx);
                String resBeg = result.substring(0, idx);
                if (wordEnd.equals(resBeg)) {
                    match = word.substring(0, word.length() - idx) + result;
                } else if (wasMatch) {
                    break;
                }
                idx++;
            }
            if (match != null) {
                return match;
            }
            return word + result;
        } else {
            if (isPaths) {
                return merge(word.substring(0, wordCursor), result, wordCursor, isPaths) + word.substring(wordCursor);
            }
            return merge(word.substring(0, wordCursor), result, wordCursor, isPaths);
        }
    }

    private static int getStart(String buffer, Value env, int cursor) {
        int start = 0;

        // are we in quotes?
        int lastQuoteIdx = isInQuotes(buffer, cursor);
        if (lastQuoteIdx != -1) {
            return lastQuoteIdx + 1;
        }

        Value opt = env.getMember("options");
        if (opt.hasMembers()) {
            start = lastIdxOf(buffer, opt, "funarg.suffix", start, cursor);
            start = lastIdxOf(buffer, opt, "function.suffix", start, cursor);
        }

        // is there any preceeding ',' or ' ' - lets start from there
        String precBuffer = buffer.length() > cursor ? buffer.substring(0, cursor) : buffer;
        int idx = cursor >= precBuffer.length() ? precBuffer.length() - 1 : cursor - 1;
        while (idx >= start && precBuffer.charAt(idx) != ',' && precBuffer.charAt(idx) != ' ') {
            --idx;
        }
        if (idx > -1) {
            return ++idx;
        }

        return start;
    }

    private static int lastIdxOf(String buffer, Value opt, String key, int start, int cursor) {
        if (opt.hasMember(key)) {
            Value member = opt.getMember(key);
            if (member.isString()) {
                return lastIdxOf(buffer, member.asString(), start, cursor);
            } else if (member.hasArrayElements() && member.getArraySize() > 0 && member.getArrayElement(0).isString()) {
                return lastIdxOf(buffer, member.getArrayElement(0).asString(), start, cursor);
            }
        }
        return start;
    }

    private static int isInQuotes(String buffer, int cursor) {
        int idx = -1;
        int qidx = -1;
        int c = 0;
        while (++idx < cursor && idx < buffer.length()) {
            if (buffer.charAt(idx) == '\'' || buffer.charAt(idx) == '\"') {
                qidx = idx;
                c++;
            }
        }
        return c % 2 == 0 ? -1 : qidx;
    }

    private static int lastIdxOf(String buffer, String subs, int start, int cursor) {
        if (!subs.isEmpty()) {
            int idx = buffer.lastIndexOf(subs, cursor);
            if (idx == cursor) {
                idx = buffer.lastIndexOf(subs, cursor - 1);
            }
            if (idx > -1) {
                idx += subs.length();
                return Math.max(idx, start);
            }
        }
        return start;
    }
}
