/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime;

import java.io.*;
import java.nio.*;
import java.util.*;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

/**
 * Denotes an R {@code connection} instance used in the {@code base} I/O library.
 */
public abstract class RConnection implements RClassHierarchy {

    public static final int GZIP_BUFFER_SIZE = (2 << 20);

    private LinkedList<String> pushBack;

    public abstract String[] readLinesInternal(int n) throws IOException;

    private String readOneLineWithPushBack(String[] res, int ind) {
        String s = pushBack.pollLast();
        if (s == null) {
            return null;
        } else {
            String[] lines = s.split("\n", 2);
            if (lines.length == 2) {
                // we hit end of the line
                if (lines[1].length() != 0) {
                    // suffix is not empty and needs to be processed later
                    pushBack.push(lines[1]);
                }
                res[ind] = lines[0];
                return null;
            } else {
                // no end of the line found yet
                StringBuilder sb = new StringBuilder();
                do {
                    assert lines.length == 1;
                    sb.append(lines[0]);
                    s = pushBack.pollLast();
                    if (s == null) {
                        break;
                    }

                    lines = s.split("\n", 2);
                    if (lines.length == 2) {
                        // we hit end of the line
                        if (lines[1].length() != 0) {
                            // suffix is not empty and needs to be processed later
                            pushBack.push(lines[1]);
                        }
                        res[ind] = sb.append(lines[0]).toString();
                        return null;
                    } // else continue
                } while (true);
                return sb.toString();
            }
        }
    }

    @TruffleBoundary
    private String[] readLinesWithPushBack(int n) throws IOException {
        String[] res = new String[n];
        for (int i = 0; i < n; i++) {
            String s = readOneLineWithPushBack(res, i);
            if (s == null) {
                if (res[i] == null) {
                    // no more push back value
                    System.arraycopy(readLinesInternal(n - i), 0, res, i, n - i);
                    pushBack = null;
                    break;
                }
                // else res[i] has been filled - move to trying to fill the next one
            } else {
                // reached the last push back value without reaching and of line
                assert pushBack.size() == 0;
                System.arraycopy(readLinesInternal(n - i), 0, res, i, n - i);
                res[i] = s + res[i];
                pushBack = null;
                break;
            }
        }
        return res;
    }

    /**
     * Read (n > 0 up to n else unlimited) lines on the connection.
     */
    @TruffleBoundary
    public String[] readLines(int n) throws IOException {
        if (pushBack == null) {
            return readLinesInternal(n);
        } else if (pushBack.size() == 0) {
            pushBack = null;
            return readLinesInternal(n);
        } else {
            return readLinesWithPushBack(n);
        }
    }

    /**
     * Returns {@code true} if this is the "stdin" connection.
     */
    public boolean isStdin() {
        return false;
    }

    /**
     * Returns {@code true} if this is the "stdour" connection.
     */
    public boolean isStdout() {
        return false;
    }

    /**
     * Returns {@code true} if this is the "stderr" connection.
     */
    public boolean isStderr() {
        return false;
    }

    /**
     * Return the underlying input stream (for internal use). TODO Replace with a more principled
     * solution.
     */
    public abstract InputStream getInputStream() throws IOException;

    /**
     * Return the underlying output stream (for internal use). TODO Replace with a more principled
     * solution.
     */
    public abstract OutputStream getOutputStream() throws IOException;

    /**
     * Close the connection. The corresponds to the {@code R close} function.
     */
    public abstract void close() throws IOException;

    /**
     * Returns {@ode true} iff we can read on this connection.
     */
    public abstract boolean canRead();

    /**
     * Returns {@ode true} iff we can write on this connection.
     */
    public abstract boolean canWrite();

    /**
     * Forces the connection open. If the connection was already open, returns {@code true} and does
     * nothing. Otherwise, tries to open the connection in the given mode, returning {@code false}
     * if successful and throwing an {@link IOException} if not.
     *
     * For builtins that implicitly open/close a connection, the following idiom should be used:
     *
     * <pre>
     * boolean wasOpen = true;
     * try {
     *     wasOpen = conn.forceOpen(mode);
     * } finally {
     *     if (!wasOpen) {
     *         conn.internalClose();
     *     }
     * }
     * </pre>
     */
    public abstract boolean forceOpen(String modeString) throws IOException;

    /**
     * Closes the internal state of the stream, but does not set the connection state to "closed",
     * i.e., allowing it to be re-opened.
     */
    public abstract void internalClose() throws IOException;

    /**
     * Implements {@link RClassHierarchy}.
     */
    public abstract RStringVector getClassHierarchy();

    /**
     * Pushes lines back to the connection.
     */
    @TruffleBoundary
    public void pushBack(RAbstractStringVector lines, boolean addNewLine) {
        if (pushBack == null) {
            pushBack = new LinkedList<>();
        }
        for (int i = 0; i < lines.getLength(); i++) {
            String newLine = lines.getDataAt(i);
            if (addNewLine) {
                newLine = newLine + '\n';
            }
            pushBack.addFirst(newLine);
        }
    }

    /**
     * Return the length of the push back.
     */
    @TruffleBoundary
    public int pushBackLength() {
        return pushBack == null ? 0 : pushBack.size();
    }

    /**
     * Clears the pushback.
     */
    @TruffleBoundary
    public void pushBackClear() {
        pushBack = null;
    }

    /**
     * Write the {@code lines} to the connection, with {@code sep} appended after each "line". N.B.
     * The output will only appear as a sequence of lines if {@code sep == "\n"}.
     */
    public abstract void writeLines(RAbstractStringVector lines, String sep) throws IOException;

    public abstract void flush() throws IOException;

    public RStringVector readChar(RAbstractIntVector nchars, @SuppressWarnings("unused") boolean useBytes) throws IOException {
        InputStream inputStream = getInputStream();
        String[] data = new String[nchars.getLength()];
        for (int i = 0; i < data.length; i++) {
            byte[] bytes = new byte[nchars.getDataAt(i)];
            inputStream.read(bytes);
            int j = 0;
            for (; j < bytes.length; j++) {
                // strings end at 0
                if (bytes[j] == 0) {
                    break;
                }
            }
            data[i] = new String(bytes, 0, j, "US-ASCII");
        }

        return RDataFactory.createStringVector(data, RDataFactory.COMPLETE_VECTOR);

    }

    public abstract int getDescriptor();

    /**
     * Internal connection-specific support for the {@code writeBin} builtin. The implementation
     * should attempt to write all the data, denoted by {@code buffer.remaining()}.
     */
    public abstract void writeBin(ByteBuffer buffer) throws IOException;

    /**
     * Internal connection-specific support for the {@code readBin} builtin. The buffer is allocated
     * for the expected amount of data, denoted by {@code buffer.remaining()}. The implementation
     * should attempt to read that much data, returning the actual number read as the result. EOS is
     * denoted by a return value of zero.
     */
    public abstract int readBin(ByteBuffer buffer) throws IOException;

    /**
     * Internal connection-specific support for the {@code readBin} builtin on character data.
     * character data is null-terminated and, therefore of length unknown to the caller. The result
     * contains the bytes read, including the null terminator. A return value of {@code null}
     * implies that no data was read. The caller must locate the null terminator to determine the
     * length of the string.
     */
    public abstract byte[] readBinChars() throws IOException;

    /**
     * Returns {@code true} iff this is a text mode connection.
     */
    public abstract boolean isTextMode();

}
