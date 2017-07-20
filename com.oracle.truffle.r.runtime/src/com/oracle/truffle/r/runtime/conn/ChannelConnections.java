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
import java.nio.channels.ByteChannel;
import java.util.Objects;

import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.AbstractOpenMode;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.BaseRConnection;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.ConnectionClass;

/**
 * A connection type for communicating to arbitrary Java NIO channels.
 */
public class ChannelConnections {

    public static class ChannelRConnection extends BaseRConnection {

        private final String description;
        private final ByteChannel channel;

        public ChannelRConnection(String description, ByteChannel channel, String modeString, String encoding) throws IOException {
            super(ConnectionClass.CHANNEL, modeString, AbstractOpenMode.Read, encoding);
            this.description = description;
            this.channel = Objects.requireNonNull(channel);
            openNonLazyConnection();
        }

        @Override
        protected void createDelegateConnection() throws IOException {
            final AbstractOpenMode mode = getOpenMode().abstractOpenMode;
            switch (mode) {
                case Read:
                case ReadBinary:
                case Write:
                case WriteBinary:
                case ReadWrite:
                case ReadWriteBinary:
                    setDelegate(new ChannelReadWriteRConnection(this));
                    break;
                default:
                    throw RError.nyi(RError.SHOW_CALLER, mode.name());
            }
        }

        @Override
        public String getSummaryDescription() {
            return description;
        }
    }

    private static class ChannelReadWriteRConnection extends DelegateReadWriteRConnection {

        private final ChannelRConnection base;

        protected ChannelReadWriteRConnection(ChannelRConnection base) {
            super(base, 0);
            this.base = base;
        }

        @Override
        public ByteChannel getChannel() throws IOException {
            return base.channel;
        }

        @Override
        public boolean isSeekable() {
            return false;
        }
    }

}
