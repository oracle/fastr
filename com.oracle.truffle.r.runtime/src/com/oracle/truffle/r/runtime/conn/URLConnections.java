/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.conn.ConnectionSupport.*;

import java.io.*;
import java.net.*;
import java.nio.*;

import com.oracle.truffle.r.runtime.*;

public class URLConnections {
    public static class URLRConnection extends BaseRConnection {
        protected final String urlString;

        public URLRConnection(String url, String modeString) throws IOException {
            super(ConnectionClass.URL, modeString, AbstractOpenMode.Read);
            this.urlString = url;
        }

        @Override
        public String getSummaryDescription() {
            return urlString;
        }

        @Override
        protected void createDelegateConnection() throws IOException {
            DelegateRConnection delegate = null;
            switch (getOpenMode().abstractOpenMode) {
                case Read:
                    delegate = new URLReadRConnection(this);
                    break;
                default:
                    throw RError.nyi(RError.SHOW_CALLER2, "open mode: " + getOpenMode());
            }
            setDelegate(delegate);
        }
    }

    private static class URLReadRConnection extends DelegateReadRConnection implements ReadWriteHelper {

        private BufferedInputStream inputStream;

        protected URLReadRConnection(URLRConnection base) throws MalformedURLException, IOException {
            super(base);
            URL url = new URL(base.urlString);
            inputStream = new BufferedInputStream(url.openStream());
        }

        @Override
        public int readBin(ByteBuffer buffer) throws IOException {
            throw RError.nyi(null, "readBin on URL");
        }

        @Override
        public byte[] readBinChars() throws IOException {
            throw RError.nyi(null, "readBinChars on URL");
        }

        @Override
        public String[] readLinesInternal(int n, boolean warn, boolean skipNul) throws IOException {
            return readLinesHelper(inputStream, n, warn, skipNul);
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return inputStream;
        }

        @Override
        public void closeAndDestroy() throws IOException {
            base.closed = true;
            close();
        }

        @Override
        public void close() throws IOException {
            inputStream.close();
        }

        @Override
        public String readChar(int nchars, boolean useBytes) throws IOException {
            return readCharHelper(nchars, inputStream, useBytes);
        }

    }

}
