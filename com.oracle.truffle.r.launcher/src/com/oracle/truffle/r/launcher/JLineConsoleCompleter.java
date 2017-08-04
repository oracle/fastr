/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.launcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import jline.console.completer.Completer;

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
    public int complete(String buffer, int cursor, List<CharSequence> candidates) {
        try {
            return completeImpl(buffer, cursor, candidates);
        } catch (Throwable e) {
            if (isTesting) {
                throw e;
            }
            throw RCommand.fatal(e, "error while determining completion");
        }
    }

    private static final Source GET_COMPLETION_FUNCTION = Source.newBuilder("R", "utils:::.completeToken", "<completion>").internal(true).buildLiteral();
    private static final Source GET_COMPLETION_ENV = Source.newBuilder("R", "utils:::.CompletionEnv", "<completion>").internal(true).buildLiteral();
    private static final Source SET_FUNCTION = Source.newBuilder("R", "`$<-`", "<completion>").internal(true).buildLiteral();

    private int completeImpl(String buffer, int cursor, List<CharSequence> candidates) {
        if (buffer.isEmpty()) {
            return cursor;
        }
        Value completionFunction = context.eval(GET_COMPLETION_FUNCTION);
        Value completionEnv = context.eval(GET_COMPLETION_ENV);
        Value setFunction = context.eval(SET_FUNCTION);

        int start = getStart(buffer, completionEnv, cursor);
        setFunction.execute(completionEnv, "start", start);
        setFunction.execute(completionEnv, "end", cursor);
        setFunction.execute(completionEnv, "linebuffer", buffer);
        setFunction.execute(completionEnv, "token", buffer.substring(start, cursor));

        completionFunction.execute();

        if (completionEnv.hasMember("comps")) {
            Value completions = completionEnv.getMember("comps");
            if (completions.isString()) {
                candidates.add(completions.asString());
                return start;
            } else if (completions.hasArrayElements()) {
                long length = completions.getArraySize();
                List<String> result = new ArrayList<>((int) length);
                for (int i = 0; i < length; i++) {
                    result.add(completions.getArrayElement(i).asString());
                }
                Collections.sort(result, String.CASE_INSENSITIVE_ORDER);
                candidates.addAll(result);
                return start;
            }
        }
        return cursor;
    }

    private static int getStart(String buffer, Value env, int cursor) {
        int start = 0;
        Value opt = env.getMember("options");
        if (opt.hasMembers()) {
            start = lastIdxOf(buffer, opt, "funarg.suffix", start, cursor);
            start = lastIdxOf(buffer, opt, "function.suffix", start, cursor);
        }
        start = lastIdxOf(buffer, "\"", start, cursor);
        start = lastIdxOf(buffer, "'", start, cursor);
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

    private static int lastIdxOf(String buffer, String subs, int start, int cursor) {
        if (!subs.isEmpty()) {
            int idx = buffer.lastIndexOf(subs, cursor);
            if (idx == cursor) {
                idx = buffer.lastIndexOf(subs, cursor - 1);
            }
            if (idx > -1) {
                idx += subs.length();
                return idx > start ? idx : start;
            }
        }
        return start;
    }
}
