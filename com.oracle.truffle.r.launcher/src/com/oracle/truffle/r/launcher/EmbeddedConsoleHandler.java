/*
 * Copyright (c) 2018, 2023, Oracle and/or its affiliates. All rights reserved.
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
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * In embedded mode the console functions as defined in {@code Rinterface.h} can be overridden. This
 * class supports that, delegating to a standard console handler if not redirected.
 *
 * N.B. At the time the constructor is created, we do not know if the console is overridden so we
 * have be lazy about that.
 *
 * Since we do not have access to FastR internals in the launcher project. We're using internal
 * FastR builtins to implement the functionality.
 *
 */
public final class EmbeddedConsoleHandler extends DelegatingConsoleHandler {
    private Context context;
    private Supplier<ConsoleHandler> delegateFactory;
    private ConsoleHandler delegate;
    private int currentLine;

    private Value readLine;
    private Value write;
    private Value writeErr;

    @Override
    public void setContext(Context context) {
        this.context = context;
    }

    @Override
    public void setDelegate(Supplier<ConsoleHandler> delegateFactory) {
        this.delegateFactory = delegateFactory;
    }

    @SuppressWarnings("try")
    @Override
    public String readLine() {
        try (ContextClose ignored = inContext()) {
            String l = isOverridden("R_ReadConsole") ? getReadLine().execute("TODO prompt>").asString() : getDelegate().readLine();
            currentLine++;
            return l;
        }
    }

    @SuppressWarnings("try")
    @Override
    public void setPrompt(String prompt) {
        try (ContextClose ignored = inContext()) {
            if (!isOverridden("R_ReadConsole")) {
                getDelegate().setPrompt(prompt);
            } else {
                // TODO: set prompt
            }
        }
    }

    @SuppressWarnings("try")
    @Override
    public String getPrompt() {
        try (ContextClose ignored = inContext()) {
            if (!isOverridden("R_ReadConsole")) {
                return getDelegate().getPrompt();
            } else {
                // TODO: get prompt
                return "";
            }
        }
    }

    @Override
    public InputStream createInputStream() {
        return new InputStream() {
            private String currentLine;
            private int currentLineIdx;
            private InputStream delegateInputStream;

            @SuppressWarnings("try")
            @Override
            public int read() throws IOException {
                try (ContextClose ignored = inContext()) {
                    return readImpl();
                }
            }

            private int readImpl() throws IOException {
                if (!isOverridden("R_ReadConsole")) {
                    if (delegateInputStream == null) {
                        delegateInputStream = getDelegate().createInputStream();
                    }
                    return delegateInputStream.read();
                }
                if (currentLine == null || currentLineIdx >= currentLine.length()) {
                    currentLine = readLine();
                    currentLineIdx = 0;
                    if (currentLine == null) {
                        return 0;
                    }
                }
                return currentLine.charAt(currentLineIdx++);
            }
        };
    }

    public OutputStream createStdOutputStream(OutputStream defaultValue) {
        return createOutputSteam(this::getWrite, defaultValue);
    }

    public OutputStream createErrOutputStream(OutputStream defaultValue) {
        return createOutputSteam(this::getWriteErr, defaultValue);
    }

    private OutputStream createOutputSteam(Supplier<Value> writeCallTargetSupplier, OutputStream defaultStream) {
        return new BufferedOutputStream(new EmbeddedConsoleOutputStream(writeCallTargetSupplier, defaultStream), 128);
    }

    private static boolean isOverridden(String name) {
        // TODO: add a internal function to check isOverridden
        return false;
//        RInterfaceCallbacks clbk = RInterfaceCallbacks.valueOf(name);
//        return clbk.isOverridden();
    }

    private final class EmbeddedConsoleOutputStream extends OutputStream {
        private final OutputStream delegate;
        private Supplier<Value> writeCallTarget;

        EmbeddedConsoleOutputStream(Supplier<Value> writeCallTarget, OutputStream delegate) {
            this.writeCallTarget = writeCallTarget;
            this.delegate = delegate;
        }

        @SuppressWarnings("try")
        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            // Note: R_WriteConsole callback is (seemingly...) used for both stdout and stderr
            try (ContextClose ignored = inContext()) {
                if (!isOverridden("R_WriteConsole")) {
                    delegate.write(b, off, len);
                } else {
                    String str = StandardCharsets.US_ASCII.decode(ByteBuffer.wrap(b, off, len)).toString();
                    writeCallTarget.get().execute(str);
                }
            }
        }

        @Override
        public void write(int b) throws IOException {
            write(new byte[]{(byte) b}, 0, 1);
        }
    }

    private ConsoleHandler getDelegate() {
        if (delegate == null) {
            delegate = delegateFactory.get();
        }
        return delegate;
    }

    private ContextClose inContext() {
        context.enter();
        return new ContextClose();
    }

    private final class ContextClose implements AutoCloseable {
        @Override
        public void close() {
            context.leave();
        }
    }

    private Value getReadLine() {
        if (readLine == null) {
            // TODO: function to return readNode call target as a Value
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            Function<String, String> f = (s) -> {
                try {
                    System.out.print(s);
                    System.out.flush();
                    return in.readLine();
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe);
                }
            };
            readLine = context.asValue(f);
        }
        return readLine;
    }

    private Value getWrite() {
        if (write == null) {
            // TODO: function to return write console call target as a Value
            Consumer<String> c = System.out::println;
            write = context.asValue(c);
        }
        return write;
    }

    private Value getWriteErr() {
        if (writeErr == null) {
            // TODO: function to return write console call target as a Value
            Consumer<String> c = System.err::println;
            writeErr = context.asValue(c);
        }
        return writeErr;
    }

    @Override
    public int getCurrentLineIndex() {
        return currentLine;
    }
}
