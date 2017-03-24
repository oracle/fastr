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
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
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
        private final long addr;

        public NativeRConnection(String description, String modeString, String customConClass, long addr) throws IOException {
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

        public long getNativeAddress() {
            return addr;
        }

        public boolean getFlag(String name) {
            NativeCallInfo ni = NativeConnections.getNativeFunctionInfo(GET_FLAG_NATIVE_CONNECTION);
            RootCallTarget nativeCallTarget = CallRFFI.InvokeCallRootNode.create().getCallTarget();

            RIntVector addrVec = convertAddrToIntVec(addr);
            Object result = nativeCallTarget.call(ni, new Object[]{addrVec, name});
            if (result instanceof RLogicalVector) {
                return ((RLogicalVector) result).getDataAt(0) == RRuntime.LOGICAL_TRUE;
            }
            throw new RInternalError("could not get value of flag " + name);
        }
    }

    static class ReadNativeConnection extends DelegateReadRConnection {

        private final ByteChannel ch;

        protected ReadNativeConnection(NativeRConnection base) throws IOException {
            super(base, 4096);
            ch = new NativeChannel(base);
            NativeConnections.openNative(base.addr);
        }

        @Override
        public ByteChannel getChannel() throws IOException {
            return ch;
        }

        @Override
        protected long seekInternal(long offset, SeekMode seekMode, SeekRWMode seekRWMode) throws IOException {
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
            RIntVector rwCode = RDataFactory.createIntVectorFromScalar(1);

            NativeCallInfo ni = NativeConnections.getNativeFunctionInfo(SEEK_NATIVE_CONNECTION);
            RootCallTarget nativeCallTarget = CallRFFI.InvokeCallRootNode.create().getCallTarget();
            RIntVector addrVec = convertAddrToIntVec(((NativeRConnection) base).addr);
            Object result = nativeCallTarget.call(ni, new Object[]{addrVec, where, seekCode, rwCode});
            if (result instanceof RDoubleVector) {
                return (long) ((RDoubleVector) result).getDataAt(0);
            }
            throw RInternalError.shouldNotReachHere("unexpected result type");
        }

        @Override
        public boolean isSeekable() {
            return ((NativeRConnection) base).getFlag("canseek");
        }
    }

    static class WriteNativeConnection extends DelegateWriteRConnection {

        private final ByteChannel ch;

        protected WriteNativeConnection(NativeRConnection base) throws IOException {
            super(base);
            ch = new NativeChannel(base);
            NativeConnections.openNative(base.addr);
        }

        @Override
        public ByteChannel getChannel() throws IOException {
            return ch;
        }

        @Override
        public boolean isSeekable() {
            return ((NativeRConnection) base).getFlag("canseek");
        }
    }

    private static void openNative(long addr) throws IOException {
        NativeCallInfo ni = NativeConnections.getNativeFunctionInfo(OPEN_NATIVE_CONNECTION);
        RootCallTarget nativeCallTarget = CallRFFI.InvokeCallRootNode.create().getCallTarget();

        RIntVector addrVec = convertAddrToIntVec(addr);
        Object result = nativeCallTarget.call(ni, new Object[]{addrVec});
        if (!(result instanceof RLogicalVector && ((RLogicalVector) result).getDataAt(0) == RRuntime.LOGICAL_TRUE)) {
            throw new IOException("could not open connection");
        }
    }

    private static class NativeChannel implements ByteChannel {

        private final NativeRConnection base;

        public NativeChannel(NativeRConnection base) {
            this.base = base;
        }

        @Override
        public int read(ByteBuffer dst) throws IOException {
            NativeCallInfo ni = NativeConnections.getNativeFunctionInfo(READ_NATIVE_CONNECTION);
            RootCallTarget nativeCallTarget = CallRFFI.InvokeCallRootNode.create().getCallTarget();
            RIntVector vec = NativeConnections.convertAddrToIntVec(base.addr);
            Object call = nativeCallTarget.call(ni, new Object[]{vec, dst.array(), dst.remaining()});

            if (call instanceof RIntVector) {
                int nread = ((RIntVector) call).getDataAt(0);
                // update buffer's position !
                dst.position(nread);
                return nread;
            }

            throw RInternalError.shouldNotReachHere("unexpected result type");
        }

        @Override
        public boolean isOpen() {
            return true;
        }

        @Override
        public void close() throws IOException {
            NativeCallInfo ni = NativeConnections.getNativeFunctionInfo(CLOSE_NATIVE_CONNECTION);
            RootCallTarget nativeCallTarget = CallRFFI.InvokeCallRootNode.create().getCallTarget();
            RIntVector vec = NativeConnections.convertAddrToIntVec(base.addr);
            nativeCallTarget.call(ni, new Object[]{vec});
        }

        @Override
        public int write(ByteBuffer src) throws IOException {
            NativeCallInfo ni = NativeConnections.getNativeFunctionInfo(WRITE_NATIVE_CONNECTION);
            RootCallTarget nativeCallTarget = CallRFFI.InvokeCallRootNode.create().getCallTarget();

            RIntVector vec = NativeConnections.convertAddrToIntVec(base.addr);
            Object result = nativeCallTarget.call(ni, new Object[]{vec, src.array(), src.remaining()});

            if (result instanceof RIntVector) {
                return ((RIntVector) result).getDataAt(0);
            }

            throw RInternalError.shouldNotReachHere("unexpected result type");
        }
    }

    static RIntVector convertAddrToIntVec(long addr) {
        int high = (int) (addr >> 32);
        int low = (int) addr;
        RIntVector vec = RDataFactory.createIntVector(new int[]{low, high}, true);
        return vec;
    }
}
