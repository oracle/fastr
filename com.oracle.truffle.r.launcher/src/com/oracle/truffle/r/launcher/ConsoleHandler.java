/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.function.Supplier;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import com.oracle.truffle.r.launcher.RCmdOptions.RCmdOption;

/**
 * The interface to a source of input/output for the context, which may have different
 * implementations for different contexts.
 */
public abstract class ConsoleHandler {

    public static ConsoleHandler createConsoleHandler(RCmdOptions options, DelegatingConsoleHandler useDelegatingWrapper, InputStream inStream, OutputStream outStream) {
        /*
         * Whether the input is from stdin, a file (-f), or an expression on the command line (-e)
         * it goes through the console. N.B. -f and -e can't be used together and this is already
         * checked.
         */
        RStartParams rsp = new RStartParams(options, false);
        String fileArgument = rsp.getFileArgument();
        if (fileArgument != null) {
            List<String> lines;
            try {
                /*
                 * If initial==false, ~ expansion will not have been done and the open will fail.
                 * It's harmless to always do it.
                 */
                File file = fileArgument.startsWith("~") ? new File(System.getProperty("user.home") + fileArgument.substring(1)) : new File(fileArgument);
                lines = Files.readAllLines(file.toPath());
            } catch (MalformedInputException e) {
                throw RMain.fatal("cannot open file '%s': Invalid byte sequence for given charset", fileArgument);
            } catch (IOException e) {
                throw RMain.fatal("cannot open file '%s': No such file or directory", fileArgument);
            }
            return new StringConsoleHandler(lines, outStream);
        } else if (options.getStringList(RCmdOption.EXPR) != null) {
            List<String> exprs = options.getStringList(RCmdOption.EXPR);
            for (int i = 0; i < exprs.size(); i++) {
                exprs.set(i, REPL.unescapeSpace(exprs.get(i)));
            }
            return new StringConsoleHandler(exprs, outStream);
        } else {
            boolean isInteractive = options.getBoolean(RCmdOption.INTERACTIVE);
            if (!isInteractive && rsp.askForSave()) {
                throw RMain.fatal("you must specify '--save', '--no-save' or '--vanilla'");
            }
            boolean useReadLine = isInteractive && !rsp.noReadline();
            if (useDelegatingWrapper != null) {
                /*
                 * If we are in embedded mode, the creation of ConsoleReader and the ConsoleHandler
                 * should be lazy, as these may not be necessary and can cause hangs if stdin has
                 * been redirected.
                 */
                Supplier<ConsoleHandler> delegateFactory = useReadLine ? () -> new JLineConsoleHandler(inStream, outStream, rsp.isSlave())
                                : () -> new DefaultConsoleHandler(inStream, outStream, isInteractive);
                useDelegatingWrapper.setDelegate(delegateFactory);
                return useDelegatingWrapper;
            } else {
                if (useReadLine) {
                    return new JLineConsoleHandler(inStream, outStream, rsp.isSlave());
                } else {
                    return new DefaultConsoleHandler(inStream, outStream, isInteractive);
                }
            }
        }
    }

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

    private static final class PolyglotWrapper implements ProxyObject {
        private static final String GET_PROMPT_KEY = "getPrompt";
        private static final String SET_PROMPT_KEY = "setPrompt";
        private static final String[] keys = new String[]{GET_PROMPT_KEY, SET_PROMPT_KEY};
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
            for (String k : keys) {
                if (k.equals(key)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void putMember(String key, Value value) {
            throw new UnsupportedOperationException("putMember");
        }
    }

    private static final class GetPromptWrapper implements ProxyExecutable {
        private final ConsoleHandler target;

        private GetPromptWrapper(ConsoleHandler target) {
            this.target = target;
        }

        @Override
        public Object execute(Value... arguments) {
            return target.getPrompt();
        }
    }

    private static final class SetPromptWrapper implements ProxyExecutable {
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
