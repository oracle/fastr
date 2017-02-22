/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.conn;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.LinkedList;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.BaseRConnection;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RAttributesLayout;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RExternalPtr;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

/**
 * Denotes an R {@code connection} instance used in the {@code base} I/O library.
 *
 * TODO Refactor the pushBack code into ConnectionsSupport
 */
public abstract class RConnection implements AutoCloseable {

    public final RAbstractIntVector asVector() {
        String[] classes = new String[]{ConnectionSupport.getBaseConnection(this).getConnectionClass().getPrintName(), "connection"};

        RAbstractIntVector result = RDataFactory.createIntVector(new int[]{getDescriptor()}, true);

        RStringVector classVector = RDataFactory.createStringVector(classes, RDataFactory.COMPLETE_VECTOR);
        // it's important to put "this" into the externalptr, so that it doesn't get collected
        RExternalPtr connectionId = RDataFactory.createExternalPtr(null, this, RDataFactory.createSymbol("connection"), RNull.instance);
        DynamicObject attrs = RAttributesLayout.createClassWithConnId(classVector, connectionId);
        result.initAttributes(attrs);
        return result;
    }

    public static BaseRConnection fromIndex(int con) {
        return RContext.getInstance().stateRConnection.getConnection(con, true);
    }

    private LinkedList<String> pushBack;

    /**
     * Indicates that the last line read operation was incomplete.<br>
     * This is only relevant for connections in text and non-blocking mode.
     */
    private boolean incomplete = false;

    public abstract String[] readLinesInternal(int n, boolean warn, boolean skipNul) throws IOException;

    private String readOneLineWithPushBack(String[] res, int ind, @SuppressWarnings("unused") boolean warn, @SuppressWarnings("unused") boolean skipNul) {
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
    private String[] readLinesWithPushBack(int n, boolean warn, boolean skipNul) throws IOException {
        String[] res = new String[n];
        for (int i = 0; i < n; i++) {
            String s = readOneLineWithPushBack(res, i, warn, skipNul);
            if (s == null) {
                if (res[i] == null) {
                    // no more push back value
                    System.arraycopy(readLinesInternal(n - i, warn, skipNul), 0, res, i, n - i);
                    pushBack = null;
                    break;
                }
                // else res[i] has been filled - move to trying to fill the next one
            } else {
                // reached the last push back value without reaching and of line
                assert pushBack.size() == 0;
                System.arraycopy(readLinesInternal(n - i, warn, skipNul), 0, res, i, n - i);
                res[i] = s + res[i];
                pushBack = null;
                break;
            }
        }
        return res;
    }

    /**
     * Read (n > 0 up to n else unlimited) lines on the connection.
     *
     */
    @TruffleBoundary
    public String[] readLines(int n, boolean warn, boolean skipNul) throws IOException {
        if (pushBack == null) {
            return readLinesInternal(n, warn, skipNul);
        } else if (pushBack.size() == 0) {
            pushBack = null;
            return readLinesInternal(n, warn, skipNul);
        } else {
            return readLinesWithPushBack(n, warn, skipNul);
        }
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
    public abstract void closeAndDestroy() throws IOException;

    /**
     * Returns {@ode true} iff we can read on this connection.
     */
    public abstract boolean canRead();

    /**
     * Returns {@ode true} iff we can write on this connection.
     */
    public abstract boolean canWrite();

    /**
     * Forces the connection open. If the connection was already open does nothing. Otherwise, tries
     * to open the connection in the given mode. In either case returns an opened connection.
     *
     * builtins that need to ensure that a connection is open should use thr try-with-resources
     * pattern, e.g:
     *
     *
     * <pre>
     * boolean wasOpen = true;
     * try (RConnection openConn = conn.forceOpen(mode)) {
     *     // work with openConn
     * } catch (IOException ex) {
     *     throw RError ...
     * }
     * </pre>
     *
     * N.B. While the returned value likely will be the same as {@code this}, callers should not
     * rely on it but should use the result in the body of the {@code try} block. If the connection
     * cannot be opened {@link IOException} is thrown.
     */
    public abstract RConnection forceOpen(String modeString) throws IOException;

    /**
     * Closes the internal state of the stream, but does not set the connection state to "closed",
     * i.e., allowing it to be re-opened.
     */
    @Override
    public abstract void close() throws IOException;

    /**
     * Pushes lines back to the connection.
     */
    @TruffleBoundary
    public final void pushBack(RAbstractStringVector lines, boolean addNewLine) {
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
    public final int pushBackLength() {
        return pushBack == null ? 0 : pushBack.size();
    }

    /**
     * Clears the pushback.
     */
    @TruffleBoundary
    public final void pushBackClear() {
        pushBack = null;
    }

    public enum SeekMode {
        ENQUIRE,
        START,
        CURRENT,
        END
    }

    public enum SeekRWMode {
        LAST,
        READ,
        WRITE
    }

    /**
     * Support for {@code isSeekable} Internal.
     */
    public abstract boolean isSeekable();

    /**
     * Support for {@code seek} Internal. Also clears push back lines.
     */
    public long seek(long offset, SeekMode seekMode, SeekRWMode seekRWMode) throws IOException {
        if (isSeekable()) {
            // discard any push back strings
            pushBackClear();

            return seekInternal(offset, seekMode, seekRWMode);
        }
        throw RError.error(RError.SHOW_CALLER, RError.Message.SEEK_NOT_ENABLED);
    }

    /**
     * Allows to seek in the connection.
     */
    protected abstract long seekInternal(long offset, SeekMode seekMode, SeekRWMode seekRWMode) throws IOException;

    /**
     * Internal support for reading one character at a time.
     */
    public abstract int getc() throws IOException;

    /**
     * Write the {@code lines} to the connection, with {@code sep} appended after each "line". N.B.
     * The output will only appear as a sequence of lines if {@code sep == "\n"}.
     */
    public abstract void writeLines(RAbstractStringVector lines, String sep, boolean useBytes) throws IOException;

    public abstract void flush() throws IOException;

    public abstract int getDescriptor();

    /**
     * Writes {@code s} optionally followed by a newline to the connection. This does not correspond
     * to any R builtin function but is used internally for console output, errors, warnings etc.
     * Since these can be diverted by the {@code sink} builtin, every output connection class must
     * support this.
     */
    public abstract void writeString(String s, boolean nl) throws IOException;

    /**
     * Internal connection-specific support for the {@code writeChar} builtin.
     *
     * @param s string to output
     * @param pad number of (zero) pad bytes
     * @param eos string to append to s
     * @param useBytes TODO
     */
    public abstract void writeChar(String s, int pad, String eos, boolean useBytes) throws IOException;

    /**
     * Internal connection-specific support for the {@code readChar} builtin.
     *
     * @param nchars number of characters to read
     */
    public abstract String readChar(int nchars, boolean useBytes) throws IOException;

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

    /**
     * Returns {@code true} iff this connection is open.
     */
    public abstract boolean isOpen();

    /**
     * Returns {@code true} iff the last read operation was blocked or there is unflushed output.
     */
    public boolean isIncomplete() {
        return incomplete;
    }

}
