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
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;

import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.AbstractOpenMode;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.BaseRConnection;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.ConnectionClass;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.env.REnvironment.PutException;

public class TextConnections {
    public static class TextRConnection extends BaseRConnection {
        protected String description;
        private final RAbstractStringVector object;
        protected REnvironment env;

        public TextRConnection(String description, RAbstractStringVector object, REnvironment env, String modeString) throws IOException {
            super(ConnectionClass.Text, modeString, AbstractOpenMode.Read);
            this.description = description;
            this.object = object;
            this.env = env;
            openNonLazyConnection();
        }

        @Override
        public String getSummaryDescription() {
            return description;
        }

        @Override
        protected void createDelegateConnection() throws IOException {
            DelegateRConnection delegate = null;
            switch (getOpenMode().abstractOpenMode) {
                case Read:
                    if (object != null) {
                        delegate = new TextReadRConnection(this, object);
                    } else {
                        throw RError.error(RError.SHOW_CALLER2, RError.Message.INVALID_ARGUMENT, "text");
                    }
                    break;
                case Write:
                    delegate = new TextWriteRConnection(this, object);
                    break;
                default:
                    throw RError.nyi(RError.SHOW_CALLER2, "open mode: " + getOpenMode().modeString);
            }
            setDelegate(delegate);
        }

        public RAbstractStringVector getValue() {
            return ((GetConnectionValue) theConnection).getValue();
        }
    }

    private interface GetConnectionValue {
        RAbstractStringVector getValue();
    }

    private static class TextReadRConnection extends DelegateReadRConnection implements GetConnectionValue {
        private final String[] lines;
        private int index;

        TextReadRConnection(TextRConnection base, RAbstractStringVector object) {
            super(base);
            assert object != null;
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < object.getLength(); i++) {
                sb.append(object.getDataAt(i));
                // vector elements are implicitly terminated with a newline
                sb.append('\n');
            }
            lines = sb.toString().split("\\n");
        }

        @Override
        public String[] readLines(int n, boolean warn, boolean skipNul) throws IOException {
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

        @Override
        public void close() throws IOException {
            // nothing to do
        }

        @Override
        public boolean isSeekable() {
            return false;
        }

        @Override
        public ReadableByteChannel getChannel() {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        public RAbstractStringVector getValue() {
            throw RError.error(RError.SHOW_CALLER2, RError.Message.NOT_AN_OUTPUT_TEXT_CONNECTION);
        }
    }

    private static class TextWriteRConnection extends DelegateWriteRConnection implements GetConnectionValue {
        private String incompleteLine;
        private RStringVector textVec;
        private String idName;
        private RAbstractStringVector object;

        /** Indicates if the connection is anonymous, i.e., not input object has been provided. */
        private final boolean anonymous;

        private void initTextVec(RStringVector v, TextRConnection textBase) {

            if (anonymous) {
                object = v;
            } else {
                idName = object.getDataAt(0);
                try {
                    textVec = v;
                    textBase.env.put(idName, textVec);
                } catch (PutException ex) {
                    throw RError.error(RError.SHOW_CALLER2, ex);
                }
                // lock the binding
                textBase.env.lockBinding(idName);
            }
        }

        protected TextWriteRConnection(BaseRConnection base, RAbstractStringVector object) {
            super(base);
            this.object = object;
            this.anonymous = object == null;
            TextRConnection textBase = (TextRConnection) base;
            initTextVec(RDataFactory.createStringVector(0), textBase);
        }

        @Override
        public WritableByteChannel getChannel() {
            return Channels.newChannel(new ConnectionOutputStream());
        }

        @Override
        public void closeAndDestroy() throws IOException {
            /* Check if we ended up with an incomplete line */
            if (incompleteLine != null) {
                appendData(new String[]{incompleteLine});
                incompleteLine = null;
                base.setIncomplete(false);
            }
            base.closed = true;
            TextRConnection textBase = (TextRConnection) base;
            unlockBinding(textBase);
        }

        private void unlockBinding(TextRConnection textBase) {
            if (idName != null) {
                textBase.env.unlockBinding(idName);
            }
        }

        @Override
        public void close() throws IOException {
        }

        private void writeStringInternal(String result) {
            int nlIndex;
            int px = 0;
            boolean endOfLine = false;
            ArrayList<String> appendedLines = new ArrayList<>();
            while ((nlIndex = result.indexOf('\n', px)) >= 0) {
                if (incompleteLine != null) {
                    appendedLines.add(new StringBuilder(incompleteLine).append(result.substring(px, nlIndex)).toString());
                    incompleteLine = null;
                    base.setIncomplete(false);
                } else {
                    appendedLines.add(result.substring(px, nlIndex));
                }
                nlIndex++;
                px = nlIndex;
                endOfLine = true;
            }
            if (incompleteLine != null && !endOfLine) {
                // end of line not found - accumulate incomplete line
                incompleteLine = new StringBuffer(incompleteLine).append(result).toString();
                base.setIncomplete(true);
            } else if (px < result.length()) {
                // only reset incompleteLine if
                incompleteLine = result.substring(px);
                base.setIncomplete(true);
            }
            if (appendedLines.size() > 0) {
                // update the vector data
                String[] appendedData = new String[appendedLines.size()];
                appendedLines.toArray(appendedData);
                appendData(appendedData);
            }
        }

        void appendData(String[] appendedData) {
            String[] existingData = textVec != null ? textVec.getDataWithoutCopying() : new String[0];
            String[] updateData = appendedData;
            if (existingData.length > 0) {
                updateData = new String[existingData.length + appendedData.length];
                System.arraycopy(existingData, 0, updateData, 0, existingData.length);
                System.arraycopy(appendedData, 0, updateData, existingData.length, appendedData.length);
            }
            TextRConnection textBase = (TextRConnection) base;
            unlockBinding(textBase);
            // TODO: is vector really complete?
            initTextVec(RDataFactory.createStringVector(updateData, RDataFactory.COMPLETE_VECTOR), textBase);
        }

        @Override
        public void writeLines(RAbstractStringVector lines, String sep, boolean useBytes) throws IOException {
            StringBuffer sb = new StringBuffer();
            if (incompleteLine != null) {
                sb.append(incompleteLine);
                incompleteLine = null;
                base.setIncomplete(false);
            }
            for (int i = 0; i < lines.getLength(); i++) {
                sb.append(lines.getDataAt(i));
                sb.append(sep);
            }
            writeStringInternal(sb.toString());
        }

        @Override
        public void flush() throws IOException {
        }

        @Override
        public void writeString(String s, boolean nl) throws IOException {
            writeStringInternal(nl ? new StringBuffer(s).append('\n').toString() : s);
        }

        @Override
        public void writeChar(String s, int pad, String eos, boolean useBytes) throws IOException {
            throw RError.error(RError.SHOW_CALLER2, RError.Message.NOT_ENABLED_FOR_THIS_CONN, "write");
        }

        @Override
        public void writeBin(ByteBuffer buffer) throws IOException {
            throw RError.error(RError.SHOW_CALLER2, RError.Message.ONLY_WRITE_BINARY_CONNECTION);
        }

        @Override
        public RAbstractStringVector getValue() {
            return object;
        }

        private class ConnectionOutputStream extends OutputStream {

            @Override
            public void close() {
            }

            @Override
            public void flush() {
            }

            @Override
            public void write(byte[] b) throws IOException {
                writeStringInternal(new String(b));
            }

            @Override
            public void write(byte[] b, int off, int len) {
                throw RInternalError.unimplemented();
            }

            @Override
            public void write(int b) {
                throw RInternalError.unimplemented();
            }
        }

        @Override
        public long seek(long offset, SeekMode seekMode, SeekRWMode seekRWMode) throws IOException {
            throw RError.error(RError.SHOW_CALLER, RError.Message.SEEK_NOT_RELEVANT_FOR_TEXT_CON);
        }

        @Override
        public boolean isSeekable() {
            return false;
        }
    }

    /**
     * Strictly implementation-internal connection that is used to support the external debugger.
     */
    public static class InternalStringWriteConnection extends BaseRConnection {
        private StringBuffer sb = new StringBuffer();

        public InternalStringWriteConnection() throws IOException {
            super(ConnectionClass.Terminal, "w", AbstractOpenMode.Write);
            this.opened = true;
        }

        @Override
        protected void createDelegateConnection() throws IOException {
        }

        @Override
        public String getSummaryDescription() {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        public void writeLines(RAbstractStringVector lines, String sep, boolean useBytes) throws IOException {
            for (int i = 0; i < lines.getLength(); i++) {
                String line = lines.getDataAt(i);
                sb.append(line);
                sb.append(sep);
            }
        }

        @Override
        public void writeString(String s, boolean nl) throws IOException {
            sb.append(s);
            if (nl) {
                sb.append('\n');
            }
        }

        public String getString() {
            String result = sb.toString();
            sb = new StringBuffer();
            return result;
        }
    }
}
