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
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.AbstractOpenMode;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.BaseRConnection;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.ConnectionClass;
import com.oracle.truffle.r.runtime.conn.RConnection.SeekMode;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RExternalPtr;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.ffi.CallRFFI;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.DLL.DLLInfo;
import com.oracle.truffle.r.runtime.ffi.DLL.SymbolHandle;
import com.oracle.truffle.r.runtime.ffi.NativeCallInfo;

/**
 * Represents a custom connection created in native code and having its own native read and write
 * functions.
 */
public class NativeConnections {

    private static final String OPEN_NATIVE_CONNECTION = "__OpenNativeConnection";
    private static final String CLOSE_NATIVE_CONNECTION = "__CloseNativeConnection";
    private static final String READ_NATIVE_CONNECTION = "__ReadNativeConnection";
    private static final String WRITE_NATIVE_CONNECTION = "__WriteNativeConnection";
    private static final String GET_FLAG_NATIVE_CONNECTION = "__GetFlagNativeConnection";
    private static final String SEEK_NATIVE_CONNECTION = "__SeekNativeConnection";

    private static final Map<String, NativeCallInfo> callInfoTable = new HashMap<>(4);

    private static NativeCallInfo getNativeFunctionInfo(String name) {
        NativeCallInfo nativeCallInfo = callInfoTable.get(name);
        if (nativeCallInfo == null) {
            DLLInfo findLibraryContainingSymbol = DLL.findLibraryContainingSymbol(name);
            SymbolHandle findSymbol = DLL.findSymbol(name, findLibraryContainingSymbol);
            nativeCallInfo = new NativeCallInfo(name, findSymbol, findLibraryContainingSymbol);
            callInfoTable.put(name, nativeCallInfo);
        }
        return nativeCallInfo;
    }

    public static class NativeRConnection extends BaseRConnection {
        private final String customConClass;
        private final String description;
        private final RExternalPtr addr;

        public NativeRConnection(String description, String modeString, String customConClass, RExternalPtr addr) throws IOException {
            super(ConnectionClass.NATIVE, modeString, AbstractOpenMode.Read);
            this.customConClass = Objects.requireNonNull(customConClass);
            this.description = Objects.requireNonNull(description);
            this.addr = addr;
        }

        @Override
        protected void createDelegateConnection() throws IOException {
            DelegateRConnection delegate = null;
            switch (getOpenMode().abstractOpenMode) {
                case Read:
                case ReadBinary:
                    delegate = new ReadNativeConnection(this);
                    break;
                case Write:
                case WriteBinary:
                    delegate = new WriteNativeConnection(this);
                    break;
                case ReadWrite:
                case ReadWriteBinary:
                case ReadWriteTrunc:
                case ReadWriteTruncBinary:
                    delegate = new ReadWriteNativeConnection(this);
                    break;

            }
            setDelegate(delegate);
        }

        @Override
        public String getSummaryDescription() {
            return description;
        }

        @Override
        public String getConnectionClass() {
            return customConClass;
        }

        public RExternalPtr getNativeAddress() {
            return addr;
        }

        @Override
        public boolean canRead() {
            return getFlag("canread");
        }

        @Override
        public boolean canWrite() {
            return getFlag("canwrite");
        }

        public boolean getFlag(String name) {
            NativeCallInfo ni = NativeConnections.getNativeFunctionInfo(GET_FLAG_NATIVE_CONNECTION);
            RootCallTarget nativeCallTarget = CallRFFI.InvokeCallRootNode.create().getCallTarget();

            Object result = nativeCallTarget.call(ni, new Object[]{addr, name});
            if (result instanceof RLogicalVector) {
                return ((RLogicalVector) result).getDataAt(0) == RRuntime.LOGICAL_TRUE;
            }
            throw new RInternalError("could not get value of flag " + name);
        }
    }

    /**
     * @param addr Native address of the Rconnection data structure.
     * @param offset seek offset
     * @param seekMode seek anchor
     * @param rw seek mode (read=1, write=2, last)
     * @return the old cursor position
     */
    private static long seekSingleMode(RExternalPtr addr, long offset, SeekMode seekMode, int rw) {
        RDoubleVector where = RDataFactory.createDoubleVectorFromScalar(offset);
        RIntVector seekCode;
        switch (seekMode) {
            case CURRENT:
                seekCode = RDataFactory.createIntVectorFromScalar(1);
                break;
            case END:
                seekCode = RDataFactory.createIntVectorFromScalar(2);
                break;
            case START:
                seekCode = RDataFactory.createIntVectorFromScalar(0);
                break;
            default:
                seekCode = RDataFactory.createIntVectorFromScalar(-1);
                break;
        }
        RIntVector rwCode = RDataFactory.createIntVectorFromScalar(rw);

        NativeCallInfo ni = NativeConnections.getNativeFunctionInfo(SEEK_NATIVE_CONNECTION);
        RootCallTarget nativeCallTarget = CallRFFI.InvokeCallRootNode.create().getCallTarget();
        Object result = nativeCallTarget.call(ni, new Object[]{addr, where, seekCode, rwCode});
        if (result instanceof RDoubleVector) {
            return (long) ((RDoubleVector) result).getDataAt(0);
        }
        throw RInternalError.shouldNotReachHere("unexpected result type");
    }

    static class ReadNativeConnection extends DelegateReadRConnection {

        private final ByteChannel ch;
        private final boolean seekable;

        protected ReadNativeConnection(NativeRConnection base) throws IOException {
            super(base, 4096);
            ch = new NativeChannel(base);
            NativeConnections.openNative(base.addr);

            // In theory, the flag could change any time, but this makes no sense.
            seekable = base.getFlag("canseek");
        }

        @Override
        public ByteChannel getChannel() throws IOException {
            return ch;
        }

        @Override
        protected long seekInternal(long offset, SeekMode seekMode, SeekRWMode seekRWMode) throws IOException {
            // seek mode read -> rw = 1
            return seekSingleMode(((NativeRConnection) base).addr, offset, seekMode, 1);
        }

        @Override
        public boolean isSeekable() {
            return seekable;
        }
    }

    static class WriteNativeConnection extends DelegateWriteRConnection {

        private final ByteChannel ch;
        private final boolean seekable;

        protected WriteNativeConnection(NativeRConnection base) throws IOException {
            super(base);
            ch = new NativeChannel(base);
            NativeConnections.openNative(base.addr);

            // In theory, the flag could change any time, but this makes no sense.
            seekable = base.getFlag("canseek");
        }

        @Override
        public ByteChannel getChannel() throws IOException {
            return ch;
        }

        @Override
        protected long seekInternal(long offset, SeekMode seekMode, SeekRWMode seekRWMode) throws IOException {
            // seek mode write -> rw = 2
            return seekSingleMode(((NativeRConnection) base).addr, offset, seekMode, 2);
        }

        @Override
        public boolean isSeekable() {
            return seekable;
        }
    }

    static class ReadWriteNativeConnection extends DelegateWriteRConnection {

        private final ByteChannel ch;
        private final boolean seekable;
        private int lastSeekMode = 1;

        protected ReadWriteNativeConnection(NativeRConnection base) throws IOException {
            super(base);
            ch = new NativeChannel(base);
            NativeConnections.openNative(base.addr);

            // In theory, the flag could change any time, but this makes no sense.
            seekable = base.getFlag("canseek");
        }

        @Override
        public ByteChannel getChannel() throws IOException {
            return ch;
        }

        @Override
        protected long seekInternal(long offset, SeekMode seekMode, SeekRWMode seekRWMode) throws IOException {
            int rwMode;
            switch (seekRWMode) {
                case READ:
                    rwMode = 1;
                    break;
                case WRITE:
                    rwMode = 2;
                    break;
                default:
                    rwMode = lastSeekMode;

            }
            lastSeekMode = rwMode;
            return seekSingleMode(((NativeRConnection) base).addr, offset, seekMode, rwMode);
        }

        @Override
        public boolean isSeekable() {
            return seekable;
        }
    }

    private static void openNative(RExternalPtr addr) throws IOException {
        NativeCallInfo ni = NativeConnections.getNativeFunctionInfo(OPEN_NATIVE_CONNECTION);
        RootCallTarget nativeCallTarget = CallRFFI.InvokeCallRootNode.create().getCallTarget();

        Object result = nativeCallTarget.call(ni, new Object[]{addr});
        if (!(result instanceof RLogicalVector && ((RLogicalVector) result).getDataAt(0) == RRuntime.LOGICAL_TRUE)) {
            throw new IOException("could not open connection");
        }
    }

    private static class NativeChannel implements ByteChannel {

        // Note: we wrap the ByteBuffer's array with a raw vector, which is on the native side
        // converted to a pointer using RAW macro. This turns the raw vector into a native memory
        // backed vector and any consecutive (write) operations in the native code are actually not
        // done on the original vector that backs the byte buffer, so we need to copy back the date
        // to the byte buffer. It would be more efficient to use direct byte buffer, but then we'd
        // need to make the native call interface (CallRFFI.InvokeCallRootNode) more flexible so
        // that it can accept other argument types than SEXPs.

        private final NativeRConnection base;

        NativeChannel(NativeRConnection base) {
            this.base = base;
        }

        @Override
        public int read(ByteBuffer dst) throws IOException {
            NativeCallInfo ni = NativeConnections.getNativeFunctionInfo(READ_NATIVE_CONNECTION);
            RootCallTarget nativeCallTarget = CallRFFI.InvokeCallRootNode.create().getCallTarget();
            RRawVector bufferVec = RDataFactory.createRawVector(dst.remaining());
            Object call = nativeCallTarget.call(ni, new Object[]{base.addr, bufferVec, dst.remaining()});
            if (!(call instanceof RIntVector)) {
                throw RInternalError.shouldNotReachHere("unexpected result type from native function, did the signature change?");
            }
            int nread = ((RIntVector) call).getDataAt(0);
            if (nread > 0) {
                // this should also update the buffer position
                for (int i = 0; i < bufferVec.getLength(); i++) {
                    dst.put(bufferVec.getRawDataAt(i));
                }
            }
            return nread;
        }

        @Override
        public boolean isOpen() {
            return true;
        }

        @Override
        public void close() throws IOException {
            NativeCallInfo ni = NativeConnections.getNativeFunctionInfo(CLOSE_NATIVE_CONNECTION);
            RootCallTarget nativeCallTarget = CallRFFI.InvokeVoidCallRootNode.create().getCallTarget();
            nativeCallTarget.call(ni, new Object[]{base.addr});
        }

        @Override
        public int write(ByteBuffer src) throws IOException {
            NativeCallInfo ni = NativeConnections.getNativeFunctionInfo(WRITE_NATIVE_CONNECTION);
            RootCallTarget nativeCallTarget = CallRFFI.InvokeCallRootNode.create().getCallTarget();
            ByteBuffer slice = src;
            if (src.position() > 0) {
                slice = src.slice();
            }
            RRawVector bufferVec = RDataFactory.createRawVector(slice.array());
            Object result = nativeCallTarget.call(ni, new Object[]{base.addr, bufferVec, src.remaining()});
            if (result instanceof RIntVector) {
                return ((RIntVector) result).getDataAt(0);
            }
            throw RInternalError.shouldNotReachHere("unexpected result type");
        }
    }
}
