/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

import sun.misc.Unsafe;

abstract class InteropRootNode extends RootNode {
    InteropRootNode() {
        super(RContext.getInstance().getLanguage());
    }

    @Override
    public final SourceSection getSourceSection() {
        return RSyntaxNode.INTERNAL;
    }
}

class UnsafeAdapter {
    public static final Unsafe UNSAFE = initUnsafe();

    private static Unsafe initUnsafe() {
        try {
            return Unsafe.getUnsafe();
        } catch (SecurityException se) {
            try {
                Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
                theUnsafe.setAccessible(true);
                return (Unsafe) theUnsafe.get(Unsafe.class);
            } catch (Exception e) {
                throw new RuntimeException("exception while trying to get Unsafe", e);
            }
        }
    }
}

public final class NativeDataAccess {
    private NativeDataAccess() {
        // no instances
    }

    private static final class NativeMirror {
        private final long id;
        private long dataAddress;
        private long length;

        NativeMirror() {
            this.id = counter.incrementAndGet();
        }

        void allocateNative(Object source, long sourceLength, int len, int elementBase, int elementSize) {
            assert dataAddress == 0;
            dataAddress = UnsafeAdapter.UNSAFE.allocateMemory(sourceLength * elementSize);
            UnsafeAdapter.UNSAFE.copyMemory(source, elementBase, null, dataAddress, sourceLength * elementSize);
            this.length = len;
        }

        @Override
        protected void finalize() throws Throwable {
            super.finalize();
            nativeMirrors.remove(id);
            // System.out.println(String.format("gc'ing %16x", id));
            if (dataAddress != 0) {
                UnsafeAdapter.UNSAFE.freeMemory(dataAddress);
                assert (dataAddress = 0xbadbad) != 0;
            }
        }
    }

    private static final AtomicLong counter = new AtomicLong(0xdef000000000000L);
    private static final ConcurrentHashMap<Long, WeakReference<RObject>> nativeMirrors = new ConcurrentHashMap<>();

    public static CallTarget createIsPointer() {
        return Truffle.getRuntime().createCallTarget(new InteropRootNode() {
            @Override
            public Object execute(VirtualFrame frame) {
                return isPointer(ForeignAccess.getReceiver(frame));
            }
        });
    }

    public static boolean isPointer(Object obj) {
        return obj instanceof RObject;
    }

    public static CallTarget createAsPointer() {
        return Truffle.getRuntime().createCallTarget(new InteropRootNode() {
            @Override
            public Object execute(VirtualFrame frame) {
                return asPointer(ForeignAccess.getReceiver(frame));
            }
        });
    }

    public static long asPointer(Object arg) {
        if (arg instanceof RObject) {
            RObject obj = (RObject) arg;
            NativeMirror mirror = (NativeMirror) obj.getNativeMirror();
            if (mirror == null) {
                obj.setNativeMirror(mirror = new NativeMirror());
                // System.out.println(String.format("adding %16x = %s", mirror.id, obj));
                nativeMirrors.put(mirror.id, new WeakReference<>(obj));
            }
            return mirror.id;
        }
        throw UnsupportedMessageException.raise(Message.AS_POINTER);
    }

    public static Object lookup(long address) {
        WeakReference<RObject> reference = nativeMirrors.get(address);
        if (reference == null) {
            CompilerDirectives.transferToInterpreter();
            throw RInternalError.shouldNotReachHere("unknown/stale native reference");
        }
        RObject result = reference.get();
        if (result == null) {
            CompilerDirectives.transferToInterpreter();
            throw RInternalError.shouldNotReachHere("unknown/stale native reference");
        }
        return result;
    }

    static long getDataLength(RVector<?> vector) {
        return ((NativeMirror) vector.getNativeMirror()).length;
    }

    static int getIntData(RVector<?> vector, int index) {
        long address = ((NativeMirror) vector.getNativeMirror()).dataAddress;
        assert address != 0;
        return UnsafeAdapter.UNSAFE.getInt(address + index * Unsafe.ARRAY_INT_INDEX_SCALE);
    }

    static void setIntData(RVector<?> vector, int index, int value) {
        long address = ((NativeMirror) vector.getNativeMirror()).dataAddress;
        assert address != 0;
        UnsafeAdapter.UNSAFE.putInt(address + index * Unsafe.ARRAY_INT_INDEX_SCALE, value);
    }

    static double getDoubleData(RVector<?> vector, int index) {
        long address = ((NativeMirror) vector.getNativeMirror()).dataAddress;
        assert address != 0;
        return UnsafeAdapter.UNSAFE.getDouble(address + index * Unsafe.ARRAY_INT_INDEX_SCALE);
    }

    static void setDoubleData(RVector<?> vector, int index, double value) {
        long address = ((NativeMirror) vector.getNativeMirror()).dataAddress;
        assert address != 0;
        UnsafeAdapter.UNSAFE.putDouble(address + index * Unsafe.ARRAY_INT_INDEX_SCALE, value);
    }

    static long allocateNativeContents(RLogicalVector vector, byte[] data, int length) {
        NativeMirror mirror = (NativeMirror) vector.getNativeMirror();
        assert mirror != null;
        assert mirror.dataAddress == 0 ^ data == null;
        if (mirror.dataAddress == 0) {
            // System.out.println(String.format("allocating native for logical vector %16x",
            // mirror.id));
            int[] intArray = new int[data.length];
            for (int i = 0; i < data.length; i++) {
                intArray[i] = RRuntime.logical2int(data[i]);
            }
            ((NativeMirror) vector.getNativeMirror()).allocateNative(intArray, data.length, length, Unsafe.ARRAY_INT_BASE_OFFSET, Unsafe.ARRAY_INT_INDEX_SCALE);
        }
        return mirror.dataAddress;
    }

    static long allocateNativeContents(RIntVector vector, int[] data, int length) {
        NativeMirror mirror = (NativeMirror) vector.getNativeMirror();
        assert mirror != null;
        assert mirror.dataAddress == 0 ^ data == null;
        if (mirror.dataAddress == 0) {
            // System.out.println(String.format("allocating native for int vector %16x",
            // mirror.id));
            ((NativeMirror) vector.getNativeMirror()).allocateNative(data, data.length, length, Unsafe.ARRAY_INT_BASE_OFFSET, Unsafe.ARRAY_INT_INDEX_SCALE);
        }
        return mirror.dataAddress;
    }

    static long allocateNativeContents(RDoubleVector vector, double[] data, int length) {
        NativeMirror mirror = (NativeMirror) vector.getNativeMirror();
        assert mirror != null;
        assert mirror.dataAddress == 0 ^ data == null;
        if (mirror.dataAddress == 0) {
            // System.out.println(String.format("allocating native for double vector %16x",
            // mirror.id));
            ((NativeMirror) vector.getNativeMirror()).allocateNative(data, data.length, length, Unsafe.ARRAY_DOUBLE_BASE_OFFSET, Unsafe.ARRAY_DOUBLE_INDEX_SCALE);
        }
        return mirror.dataAddress;
    }
}
