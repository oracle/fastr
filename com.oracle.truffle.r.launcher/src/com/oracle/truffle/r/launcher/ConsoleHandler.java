/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

/**
 * The interface to a source of input/output for the context, which may have different
 * implementations for different contexts.
 */
public abstract class ConsoleHandler {

    /**
     * Read a line of input, newline is <b>NOT</b> included in result.
     */
    public abstract String readLine();

    /**
     * Return the current 1-based line number.
     */
    public abstract int getCurrentLineIndex();

    /**
     * Set the R prompt.
     */
    public abstract void setPrompt(String prompt);

    /**
     * Get the R prompt.
     * 
     * @return the prompt
     */
    public abstract String getPrompt();

    public void setContext(@SuppressWarnings("unused") Context context) {
        // ignore by default
    }

    public Object getPolyglotWrapper() {
        return new PolyglotWrapper(this);
    }

    public InputStream createInputStream() {
        return new InputStream() {
            byte[] buffer = null;
            int pos = -1;

            @Override
            public int read() throws IOException {
                if (buffer == null) {
                    pos = 0;
                    String line = readLine();
                    if (line == null) {
                        return -1;
                    }
                    buffer = line.getBytes(StandardCharsets.UTF_8);
                }
                if (pos == buffer.length) {
                    buffer = null;
                    return '\n';
                } else {
                    return buffer[pos++];
                }
            }
        };
    }

    private static class PolyglotWrapper implements ProxyObject {
        private static final String GET_PROMPT_KEY = "getPrompt";
        private static final String SET_PROMPT_KEY = "setPrompt";
        private static final String[] keys = new String[] { GET_PROMPT_KEY, SET_PROMPT_KEY };
        private final GetPromptWrapper getPrompt;
        private final SetPromptWrapper setPrompt;

        private PolyglotWrapper(ConsoleHandler target) {
            getPrompt = new GetPromptWrapper(target);
            setPrompt = new SetPromptWrapper(target);
        }

        @Override
        public Object getMember(String key) {
            if (GET_PROMPT_KEY.equals(key)) {
                return getPrompt;
            } else if (SET_PROMPT_KEY.equals(key)) {
                return setPrompt;
            } else {
                return null;
            }
        }

        @Override
        public Object getMemberKeys() {
            return keys;
        }

        @Override
        public boolean hasMember(String key) {
            return key.contains(key);
        }

        @Override
        public void putMember(String key, Value value) {
            throw new UnsupportedOperationException("putMember");
        }
    }

    private static class GetPromptWrapper implements ProxyExecutable {
        private final ConsoleHandler target;

        private GetPromptWrapper(ConsoleHandler target) {
            this.target = target;
        }

        @Override
        public Object execute(Value... arguments) {
            return target.getPrompt();
        }
    }

    private static class SetPromptWrapper implements ProxyExecutable {
        private final ConsoleHandler target;

        private SetPromptWrapper(ConsoleHandler target) {
            this.target = target;
        }

        @Override
        public Object execute(Value... arguments) {
            target.setPrompt(arguments[0].asString());
            return null;
        }
    }
}
