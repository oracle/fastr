/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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
import java.nio.channels.ByteChannel;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.BaseRConnection;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

/**
 * Denotes an R {@code connection} instance used in the {@code base} I/O library.
 */
public interface RConnection extends AutoCloseable {

    static BaseRConnection fromIndex(int con) {
        return RContext.getInstance().stateRConnection.getConnection(con, true);
    }

    /**
     * Return the underlying input stream (for internal use).<br>
     * <p>
     * <b>NOTE:</b> The connection may do some caching internally! Therefore, the behavior is
     * undefined if you mix using the input stream directly and using the read methods of the
     * connection.
     * </p>
     *
     */
    InputStream getInputStream() throws IOException;

    /**
     * Return the underlying output stream (for internal use).<br>
     * <p>
     * <b>NOTE:</b> The connection may do some caching internally! Therefore, the behavior is
     * undefined if you mix using the output stream directly and using the write methods of the
     * connection.
     * </p>
     */
    OutputStream getOutputStream() throws IOException;

    /**
     * Return the underlying byte channel (for internal use).
     *
     * @throws IOException
     */
    ByteChannel getChannel() throws IOException;

    /**
     * Close the connection. The corresponds to the {@code R close} function.
     */
    void closeAndDestroy() throws IOException;

    /**
     * Returns {@ode true} iff we can read on this connection.
     */
    boolean canRead();

    /**
     * Returns {@ode true} iff we can write on this connection.
     */
    boolean canWrite();

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
    RConnection forceOpen(String modeString) throws IOException;

    /**
     * Closes the internal state of the stream, but does not set the connection state to "closed",
     * i.e., allowing it to be re-opened.
     */
    @Override
    void close() throws IOException;

    enum SeekMode {
        ENQUIRE,
        START,
        CURRENT,
        END
    }

    enum SeekRWMode {
        LAST,
        READ,
        WRITE
    }

    /**
     * Support for {@code isSeekable} Internal.
     */
    boolean isSeekable();

    /**
     * Allows to seek in the connection.
     */
    long seek(long offset, SeekMode seekMode, SeekRWMode seekRWMode) throws IOException;

    /**
     * Internal support for reading one byte at a time.
     */
    int read() throws IOException;

    /**
     * Write the {@code lines} to the connection, with {@code sep} appended after each "line". N.B.
     * The output will only appear as a sequence of lines if {@code sep == "\n"}.
     */
    void writeLines(RAbstractStringVector lines, String sep, boolean useBytes) throws IOException;

    void flush() throws IOException;

    int getDescriptor();

    /**
     * Writes {@code s} optionally followed by a newline to the connection. This does not correspond
     * to any R builtin function but is used internally for console output, errors, warnings etc.
     * Since these can be diverted by the {@code sink} builtin, every output connection class must
     * support this.
     */
    void writeString(String s, boolean nl) throws IOException;

    /**
     * Internal connection-specific support for the {@code writeChar} builtin.
     *
     * @param s string to output
     * @param pad number of (zero) pad bytes
     * @param eos string to append to s
     * @param useBytes
     */
    void writeChar(String s, int pad, String eos, boolean useBytes) throws IOException;

    /**
     * Internal connection-specific support for the {@code readChar} builtin.
     *
     * @param nchars number of characters to read
     */
    String readChar(int nchars, boolean useBytes) throws IOException;

    /**
     * Internal connection-specific support for the {@code writeBin} builtin. The implementation
     * should attempt to write all the data, denoted by {@code buffer.remaining()}.
     */
    void writeBin(ByteBuffer buffer) throws IOException;

    /**
     * Internal connection-specific support for the {@code readBin} builtin. The buffer is allocated
     * for the expected amount of data, denoted by {@code buffer.remaining()}. The implementation
     * should attempt to read that much data, returning the actual number read as the result. EOS is
     * denoted by a return value of zero.
     */
    int readBin(ByteBuffer buffer) throws IOException;

    /**
     * Internal connection-specific support for the {@code readBin} builtin on character data.
     * character data is null-terminated and, therefore of length unknown to the caller. The result
     * contains the bytes read, including the null terminator. A return value of {@code null}
     * implies that no data was read. The caller must locate the null terminator to determine the
     * length of the string.
     */
    byte[] readBinChars() throws IOException;

    /**
     * Read (n > 0 up to n else unlimited) lines on the connection.
     */
    @TruffleBoundary
    String[] readLines(int n, boolean warn, boolean skipNul) throws IOException;

    /**
     * Returns {@code true} iff this is a text mode connection.
     */
    boolean isTextMode();

    /**
     * Returns {@code true} iff this connection is open.
     */
    boolean isOpen();

    /**
     * Truncates the connection (if possible).
     */
    void truncate() throws IOException;

    void pushBack(RAbstractStringVector lines, boolean addNewLine);
}
