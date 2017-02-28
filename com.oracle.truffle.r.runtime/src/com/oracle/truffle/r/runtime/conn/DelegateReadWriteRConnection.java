/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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
import java.nio.channels.Channels;

import com.oracle.truffle.r.runtime.conn.ConnectionSupport.BaseRConnection;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

abstract class DelegateReadWriteRConnection extends DelegateRConnection {

    private final ByteBuffer tmp = ByteBuffer.allocate(1);

    protected DelegateReadWriteRConnection(BaseRConnection base) {
        super(base);
    }

    @Override
    public boolean canRead() {
        return true;
    }

    @Override
    public boolean canWrite() {
        return true;
    }

    @Override
    public int getc() throws IOException {
        tmp.clear();
        int nread = getChannel().read(tmp);
        tmp.rewind();
        return nread > 0 ? tmp.get() : -1;
    }

    @Override
    public String readChar(int nchars, boolean useBytes) throws IOException {
        return DelegateRConnection.readCharHelper(nchars, getChannel(), useBytes);
    }

    @Override
    public int readBin(ByteBuffer buffer) throws IOException {
        return getChannel().read(buffer);
    }

    @Override
    public byte[] readBinChars() throws IOException {
        return DelegateRConnection.readBinCharsHelper(getChannel());
    }

    @Override
    public void flush() {
        // nothing to do for channels
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return Channels.newOutputStream(getChannel());
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return Channels.newInputStream(getChannel());
    }

    @Override
    public void close() throws IOException {
        getChannel().close();
    }

    @Override
    public void closeAndDestroy() throws IOException {
        base.closed = true;
        close();
    }

    @Override
    public void writeBin(ByteBuffer buffer) throws IOException {
        getChannel().write(buffer);
    }

    @Override
    public void writeChar(String s, int pad, String eos, boolean useBytes) throws IOException {
        DelegateRConnection.writeCharHelper(getChannel(), s, pad, eos);
    }

    @Override
    public void writeLines(RAbstractStringVector lines, String sep, boolean useBytes) throws IOException {
        boolean incomplete = DelegateRConnection.writeLinesHelper(getChannel(), lines, sep, base.getEncoding());
        base.setIncomplete(incomplete);
    }

    @Override
    public void writeString(String s, boolean nl) throws IOException {
        DelegateRConnection.writeStringHelper(getChannel(), s, nl, base.getEncoding());
    }

}
