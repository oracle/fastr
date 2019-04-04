/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.conn;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.RCompression;
import static com.oracle.truffle.r.runtime.RCompression.Type.NONE;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.ByteChannel;

import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.AbstractOpenMode;
import static com.oracle.truffle.r.runtime.conn.ConnectionSupport.AbstractOpenMode.Lazy;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.BaseRConnection;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.ConnectionClass;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.OpenMode;

public class URLConnections {
    public static class URLRConnection extends BaseRConnection {
        protected final String urlString;
        private RCompression.Type cType;

        public URLRConnection(String url, String modeString, String encoding) throws IOException {
            super(ConnectionClass.URL, modeString, AbstractOpenMode.Read, encoding);
            this.urlString = url;
            this.cType = NONE;
        }

        @Override
        public String getSummaryDescription() {
            return urlString;
        }

        @Override
        protected void createDelegateConnection() throws IOException {
            setDelegate(createDelegateConnectionImpl());
        }

        private DelegateRConnection createDelegateConnectionImpl() throws RError, IOException {
            DelegateRConnection delegate = null;
            OpenMode openMode = getOpenMode();
            AbstractOpenMode mode = openMode.abstractOpenMode;
            if (mode == Lazy) {
                mode = AbstractOpenMode.getOpenMode(openMode.modeString);
            }
            switch (mode) {
                case Read:
                case ReadBinary:
                    delegate = new URLReadRConnection(this, cType);
                    break;
                default:
                    throw RError.nyi(RError.SHOW_CALLER2, "open mode: " + getOpenMode());
            }
            return delegate;
        }

        @TruffleBoundary
        @Override
        public void setCompressiontype(RCompression.Type cType) throws IOException {
            this.cType = cType;
            // changind the compression type delegate (as via the gzcon builtin)
            // should not change the opened state
            setDelegate(createDelegateConnectionImpl(), opened);
        }

    }

    private static class URLReadRConnection extends DelegateReadRConnection {

        private final ByteChannel rchannel;

        protected URLReadRConnection(URLRConnection base, RCompression.Type cType) throws MalformedURLException, IOException {
            super(base);
            URL url = new URL(base.urlString);
            switch (cType) {
                case GZIP:
                    rchannel = createGZIPDelegateInputConnection(base, new BufferedInputStream(url.openStream()));
                    break;
                case NONE:
                    rchannel = ConnectionSupport.newChannel(new BufferedInputStream(url.openStream()));
                    break;
                default:
                    throw RInternalError.shouldNotReachHere("unsupported compression type. Can be GZIP or NONE");
            }
        }

        @Override
        public ByteChannel getChannel() {
            return rchannel;
        }

        @Override
        public boolean isSeekable() {
            return false;
        }
    }
}
