/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.nio.*;

import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;

public class TextConnections {
    public static class TextRConnection extends BaseRConnection {
        protected String nm;
        protected RAbstractStringVector object;
        protected REnvironment env;

        public TextRConnection(String nm, RAbstractStringVector object, REnvironment env, String modeString) throws IOException {
            super(ConnectionClass.Text, modeString);
            this.nm = nm;
            this.object = object;
            this.env = env;
            openNonLazyConnection();
        }

        @Override
        public String getSummaryDescription() {
            return nm;
        }

        @Override
        protected void createDelegateConnection() throws IOException {
            DelegateRConnection delegate = null;
            switch (getOpenMode().abstractOpenMode) {
                case Read:
                    delegate = new TextReadRConnection(this);
                    break;
                default:
                    throw RError.nyi((SourceSection) null, "unimplemented open mode: " + getOpenMode());
            }
            setDelegate(delegate);
        }
    }

    private static class TextReadRConnection extends DelegateReadRConnection {
        private String[] lines;
        private int index;

        TextReadRConnection(TextRConnection base) {
            super(base);
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < base.object.getLength(); i++) {
                sb.append(base.object.getDataAt(i));
                // vector elements are implicitly terminated with a newline
                sb.append('\n');
            }
            lines = sb.toString().split("\\n");
        }

        @Override
        public String[] readLinesInternal(int n) throws IOException {
            int nleft = lines.length - index;
            int nlines = nleft;
            if (n > 0) {
                nlines = n > nleft ? nleft : n;
            }

            String[] result = new String[nlines];
            System.arraycopy(lines, index, result, 0, nlines);
            index += nlines;
            return result;
        }

        @SuppressWarnings("hiding")
        @Override
        public void writeLines(RAbstractStringVector lines, String sep) throws IOException {
            throw new IOException(RError.Message.CANNOT_WRITE_CONNECTION.message);
        }

        @Override
        public InputStream getInputStream() throws IOException {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        public void close() throws IOException {
            base.closed = true;
        }

        @Override
        public void internalClose() {
        }

        @Override
        public int readBin(ByteBuffer buffer) throws IOException {
            throw RError.nyi(null, " readBin on text connection");
        }

        @Override
        public byte[] readBinChars() throws IOException {
            throw RError.nyi(null, " readBinChars on text connection");
        }

        @Override
        public String readChar(int nchars, boolean useBytes) throws IOException {
            throw RError.nyi(null, " readChar on text connection");
        }

    }

}
