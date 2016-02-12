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

import java.io.*;
import java.nio.*;
import java.util.*;

import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.env.REnvironment.PutException;

public class TextConnections {
    public static class TextRConnection extends BaseRConnection {
        protected String description;
        protected RAbstractStringVector object;
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
                    delegate = new TextReadRConnection(this);
                    break;
                case Write:
                    delegate = new TextWriteRConnection(this);
                    break;
                default:
                    throw RError.nyi(RError.SHOW_CALLER2, "open mode: " + getOpenMode().modeString);
            }
            setDelegate(delegate);
        }

        public String[] getValue() {
            return ((GetConnectionValue) theConnection).getValue();
        }
    }

    private interface GetConnectionValue {
        String[] getValue();
    }

    private static class TextReadRConnection extends DelegateReadRConnection implements GetConnectionValue {
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
        public String[] readLinesInternal(int n, boolean warn, boolean skipNul) throws IOException {
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
        public void writeLines(RAbstractStringVector lines, String sep, boolean useBytes) throws IOException {
            throw new IOException(RError.Message.CANNOT_WRITE_CONNECTION.message);
        }

        @Override
        public InputStream getInputStream() throws IOException {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        public void closeAndDestroy() throws IOException {
            base.closed = true;
        }

        @Override
        public void close() {
        }

        @Override
        public int readBin(ByteBuffer buffer) throws IOException {
            throw RError.nyi(null, "readBin on text connection");
        }

        @Override
        public byte[] readBinChars() throws IOException {
            throw RError.nyi(null, "readBinChars on text connection");
        }

        @Override
        public String readChar(int nchars, boolean useBytes) throws IOException {
            throw RError.nyi(null, "readChar on text connection");
        }

        public String[] getValue() {
            throw RError.nyi(null, "textConnectionValue");
        }

    }

    private static class TextWriteRConnection extends DelegateWriteRConnection implements GetConnectionValue {
        private String incompleteLine;
        private RStringVector textVec;
        private String idName;

        private void initTextVec(RStringVector v, TextRConnection textBase) {
            if (textBase.description.equals("NULL")) {
                throw RError.nyi(null, "anonymous text output connection");
            }
            idName = textBase.object.getDataAt(0);
            try {
                textVec = v;
                textBase.env.put(idName, textVec);
            } catch (PutException ex) {
                throw RError.error(RError.SHOW_CALLER2, ex);
            }
            // lock the binding
            textBase.env.lockBinding(idName);
        }

        protected TextWriteRConnection(BaseRConnection base) {
            super(base);
            TextRConnection textBase = (TextRConnection) base;
            initTextVec(RDataFactory.createStringVector(0), textBase);
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        public void closeAndDestroy() throws IOException {
            /* Check if we ended up with an incomplete line */
            if (incompleteLine != null) {
                appendData(new String[]{incompleteLine});
                incompleteLine = null;
            }
            base.closed = true;
            TextRConnection textBase = (TextRConnection) base;
            textBase.env.unlockBinding(idName);
        }

        @Override
        public void close() throws IOException {
        }

        private void writeStringInternal(String result) {
            int nlIndex;
            int px = 0;
            ArrayList<String> appendedLines = new ArrayList<>();
            while ((nlIndex = result.indexOf('\n', px)) >= 0) {
                appendedLines.add(result.substring(px, nlIndex));
                nlIndex++;
                px = nlIndex;
            }
            if (px < result.length()) {
                incompleteLine = result.substring(px);
            }
            if (appendedLines.size() > 0) {
                // update the vector data
                String[] appendedData = new String[appendedLines.size()];
                appendedLines.toArray(appendedData);
                appendData(appendedData);
            }

        }

        void appendData(String[] appendedData) {
            String[] existingData = textVec.getDataWithoutCopying();
            String[] updateData = appendedData;
            if (existingData.length > 0) {
                updateData = new String[existingData.length + appendedData.length];
                System.arraycopy(existingData, 0, updateData, 0, existingData.length);
                System.arraycopy(appendedData, 0, updateData, existingData.length, appendedData.length);
            }
            TextRConnection textBase = (TextRConnection) base;
            /*
             * N.B. This assumes one thread per RContext else another thread could be calling
             * lockBinding
             */
            textBase.env.unlockBinding(idName);
            // TODO: is vector really complete?
            initTextVec(RDataFactory.createStringVector(updateData, RDataFactory.COMPLETE_VECTOR), textBase);
        }

        @Override
        public void writeLines(RAbstractStringVector lines, String sep, boolean useBytes) throws IOException {
            StringBuffer sb = new StringBuffer();
            if (incompleteLine != null) {
                sb.append(incompleteLine);
                incompleteLine = null;
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
            throw RError.nyi(null, "writeChar on text connection");
        }

        @Override
        public void writeBin(ByteBuffer buffer) throws IOException {
            throw RError.nyi(null, "writeBin on text connection");
        }

        public String[] getValue() {
            throw RError.nyi(null, "textConnectionValue");
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
