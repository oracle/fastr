/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.ffi.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.SuppressFBWarnings;
import com.oracle.truffle.r.runtime.ffi.util.ResourcesCleaner.Releasable;

import sun.misc.Unsafe;

/**
 * Thin layer abstraction over {@code sun.misc.Unsafe}. Raw native pointers can be wrapped using
 * {@link #wrapNativeMemory(long, Object)} to ensure the clean-up of the memory once the owning
 * object is collected.
 */
public abstract class NativeMemory {

    private NativeMemory() {
        // only static members
    }

    public static final int OBJECT_SIZE = Unsafe.ARRAY_OBJECT_INDEX_SCALE;
    public static final int OBJECT_HEADER_SIZE = Unsafe.ARRAY_BOOLEAN_BASE_OFFSET + OBJECT_SIZE * 2;

    public enum ElementType {
        BYTE(1, Unsafe.ARRAY_BYTE_BASE_OFFSET),
        INT(Integer.BYTES, Unsafe.ARRAY_INT_BASE_OFFSET),
        LONG(Long.BYTES, Unsafe.ARRAY_LONG_BASE_OFFSET),
        DOUBLE(Double.BYTES, Unsafe.ARRAY_DOUBLE_BASE_OFFSET);

        private final int bytes;
        private final int offset;

        ElementType(int bytes, int offset) {
            this.bytes = bytes;
            this.offset = offset;
        }
    }

    private static final Unsafe UNSAFE = initUnsafe();

    private static Unsafe initUnsafe() {
        assert Integer.BYTES == Unsafe.ARRAY_INT_INDEX_SCALE;
        assert Double.BYTES == Unsafe.ARRAY_DOUBLE_INDEX_SCALE;
        assert Long.BYTES == Unsafe.ARRAY_LONG_INDEX_SCALE;
        try {
            return Unsafe.getUnsafe();
        } catch (SecurityException se) {
            try {
                Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
                theUnsafe.setAccessible(true);
                return (Unsafe) theUnsafe.get(Unsafe.class);
            } catch (Exception e) {
                throw RInternalError.shouldNotReachHere("Cannot initialize sun.misc.Unsafe");
            }
        }
    }

    public static long allocate(long size, Object debugInfo) {
        traceAllocateStart(size, debugInfo);
        long result = UNSAFE.allocateMemory(size);
        traceAllocate(result, size, debugInfo);
        return result;
    }

    public static long allocate(ElementType type, long size, Object debugInfo) {
        traceAllocateStart(size, debugInfo);
        long result = UNSAFE.allocateMemory(size * type.bytes);
        traceAllocate(result, size, debugInfo);
        return result;
    }

    public static void free(long address, Object debugInfo) {
        traceFree(address, debugInfo);
        UNSAFE.freeMemory(address);
        traceFreeDone(address, debugInfo);
    }

    @TruffleBoundary
    public static NativeMemoryWrapper wrapNativeMemory(long address, Object owner) {
        return new FreeingNativeMemoryWrapper(address, owner);
    }

    /**
     * Allows to wrap native memory address in the same object as
     * {@link #wrapNativeMemory(long, Object)}, but without cleaning-up the memory when the owning
     * object is collected. This can be useful in places where {@link NativeMemoryWrapper} is
     * expected but you do not have control over the life cycle of the given memory (i.e., it is
     * externally owned).
     */
    public static NativeMemoryWrapper wrapExternalNativeMemory(long address, Object owner) {
        return new ExternalNativeMemoryWrapper(address, owner);
    }

    public static void copyMemory(Object source, NativeMemoryWrapper destination, ElementType type, long elementsCount) {
        copyMemory(source, type.offset, type.bytes, destination.getAddress(), elementsCount);
    }

    public static void copyMemory(Object source, long destination, ElementType type, long elementsCount) {
        copyMemory(source, type.offset, type.bytes, destination, elementsCount);
    }

    private static void copyMemory(Object source, int elementBase, long elementSize, long destination, long elementsCount) {
        // this takes relevant args as longs to make sure any calculations do not overflow
        UNSAFE.copyMemory(source, elementBase, null, destination, elementSize * elementsCount);
    }

    public static void copyMemory(NativeMemoryWrapper source, Object destination, ElementType type, int elementsCount) {
        copyMemory(source.getAddress(), type.offset, type.bytes, destination, elementsCount);
    }

    public static void copyMemory(long source, Object destination, ElementType type, int elementsCount) {
        copyMemory(source, type.offset, type.bytes, destination, elementsCount);
    }

    private static void copyMemory(long source, int elementBase, int elementSize, Object destination, int elementsCount) {
        // this takes relevant args as longs to make sure any calculations do not overflow
        UNSAFE.copyMemory(null, source, destination, elementBase, (long) elementSize * (long) elementsCount);
    }

    public static String copyCString(long address, Charset encoding) {
        return copyCString(address, Integer.MAX_VALUE, encoding);
    }

    public static String copyCString(NativeMemoryWrapper address, long maxLength, Charset encoding) {
        return copyCString(address.getAddress(), maxLength, encoding);
    }

    @TruffleBoundary
    public static String copyCString(long address, long maxLength, Charset encoding) {
        int length = 0;
        while (length < maxLength && NativeMemory.getByte(address, length) != 0) {
            length++;
        }
        byte[] bytes = new byte[length];
        NativeMemory.copyMemory(address, bytes, ElementType.BYTE, length);
        return new String(bytes, encoding);
    }

    public static void putByte(NativeMemoryWrapper address, long offset, byte value) {
        putByte(address.getAddress(), offset, value);
    }

    public static void putByte(long address, long offset, byte value) {
        UNSAFE.putByte(address + offset * Unsafe.ARRAY_BYTE_INDEX_SCALE, value);
    }

    public static void putInt(NativeMemoryWrapper address, long offset, int value) {
        putInt(address.getAddress(), offset, value);
    }

    public static void putInt(long address, long offset, int value) {
        UNSAFE.putInt(address + offset * Unsafe.ARRAY_INT_INDEX_SCALE, value);
    }

    public static void putLong(NativeMemoryWrapper address, long offset, long value) {
        putLong(address.getAddress(), offset, value);
    }

    public static void putLong(long address, long offset, long value) {
        UNSAFE.putLong(address + offset * Unsafe.ARRAY_LONG_INDEX_SCALE, value);
    }

    public static void putLong(long address, long value) {
        putLong(address, 0, value);
    }

    public static void putDouble(NativeMemoryWrapper address, long offset, double value) {
        putDouble(address.getAddress(), offset, value);
    }

    public static void putDouble(long address, long offset, double value) {
        UNSAFE.putDouble(address + offset * Unsafe.ARRAY_DOUBLE_INDEX_SCALE, value);
    }

    public static byte getByte(NativeMemoryWrapper dataAddress, long index) {
        return getByte(dataAddress.getAddress(), index);
    }

    public static byte getByte(long dataAddress, long index) {
        return UNSAFE.getByte(dataAddress + index * Unsafe.ARRAY_BYTE_INDEX_SCALE);
    }

    public static int getInt(NativeMemoryWrapper dataAddress, long index) {
        return getInt(dataAddress.getAddress(), index);
    }

    public static int getInt(long dataAddress, long index) {
        return UNSAFE.getInt(dataAddress + index * Unsafe.ARRAY_INT_INDEX_SCALE);
    }

    public static long getLong(NativeMemoryWrapper dataAddress, long index) {
        return getLong(dataAddress.getAddress(), index);
    }

    public static long getLong(long dataAddress, long index) {
        return UNSAFE.getLong(dataAddress + index * Unsafe.ARRAY_LONG_INDEX_SCALE);
    }

    public static long getLong(long dataAddress) {
        return getLong(dataAddress, 0);
    }

    public static double getDouble(NativeMemoryWrapper dataAddress, long index) {
        return getDouble(dataAddress.getAddress(), +index);
    }

    public static double getDouble(long dataAddress, long index) {
        return UNSAFE.getDouble(dataAddress + index * Unsafe.ARRAY_DOUBLE_INDEX_SCALE);
    }

    /**
     * Puts an integer, double, byte or long at given address with given offset. The offset is
     * interpreted {@code index * size of the value type in bytes}.
     */
    public static void putValue(long address, long index, Object value) {
        if (value instanceof Integer) {
            NativeMemory.putInt(address, index, (Integer) value);
        } else if (value instanceof Double) {
            NativeMemory.putDouble(address, index, (Double) value);
        } else if (value instanceof Byte) {
            NativeMemory.putByte(address, index, (Byte) value);
        } else if (value instanceof Long) {
            NativeMemory.putLong(address, index, (Long) value);
        } else {
            CompilerDirectives.transferToInterpreter();
            throw RInternalError.shouldNotReachHere(Objects.toString(value));
        }
    }

    /**
     * Wraps given native memory address associated with given Java object.
     */
    public static class NativeMemoryWrapper extends WeakReference<Object> {
        private final long address;

        protected NativeMemoryWrapper(long address, Object referent, ReferenceQueue<? super Object> q) {
            super(referent, q);
            assert address != 0 : "MEMORY ERROR";
            this.address = address;
        }

        public final long getAddress() {
            return address;
        }

        public void release() {
            // nop
        }
    }

    /**
     * Subclass that holds onto an externally owned data, i.e., it should not free the memory.
     */
    private static final class ExternalNativeMemoryWrapper extends NativeMemoryWrapper {
        private ExternalNativeMemoryWrapper(long address, Object referent) {
            super(address, referent, null);
        }

        @Override
        public String toString() {
            return String.format("%x (external)", getAddress());
        }
    }

    /**
     * Subclass that frees the native memory.
     */
    private static final class FreeingNativeMemoryWrapper extends NativeMemoryWrapper implements Releasable {
        @SuppressFBWarnings(value = "UWF_NULL_FIELD", justification = "used for debugging") private final String ownerInfo;

        /**
         * The instances need to be kept alive until they are pooled from the reference queue and
         * the release method can be called. I.e., NativeMemoryWrapper must not be collected sooner
         * than the referent/owner.
         */
        private static final ConcurrentHashMap<NativeMemoryWrapper, NativeMemoryWrapper> active = new ConcurrentHashMap<>();

        private FreeingNativeMemoryWrapper(long address, Object owner) {
            super(address, owner, ResourcesCleaner.nativeReferenceQueue());
            // Assertion check: creating NativeMemoryWrapper for address that was not allocated via
            // NativeMemory.allocate
            assert ALLOCATED == null || ALLOCATED.get(getAddress()) != null : "MEMORY ERROR: " + Long.toHexString(getAddress()) + " " + owner.getClass().getSimpleName();
            this.ownerInfo = NEEDS_DEBUG_INFO ? owner.getClass().getSimpleName() : null;
            addToActive();
        }

        @Override
        public void release() {
            NativeMemory.free(getAddress(), ownerInfo);
            removeFromActive();
        }

        @TruffleBoundary
        private void addToActive() {
            NativeMemoryWrapper existing = active.put(this, this);
            assert existing == null : "MEMORY ERROR (ALLOC)";
        }

        @TruffleBoundary
        private void removeFromActive() {
            NativeMemoryWrapper existing = active.remove(this);
            assert existing == this : "MEMORY ERROR (FREE)";
        }

        @Override
        public String toString() {
            return String.format("%x (%s)", getAddress(), ownerInfo);
        }
    }

    // ------------------------------------------------------
    // Tracing, sanity debug checks, ...
    // grep the output log for "MEMORY ERROR"

    private static final boolean MEMORY_CHECK = false; // check for leaks, double free, etc.
    private static final boolean PRINT_ALLOC_STACK_TRACE_IN_FREE_LOG = false;
    private static final boolean PRINT_STACK_TRACES_IN_MEM_LEAKS_REPORT = false;

    private static final boolean LOG_ALLOC_FREE = false; // only one log per allocate and free
    private static final boolean LOG_ALLOC_FREE_FULL = false; // pre and post logs for allocate/free

    @SuppressWarnings("unused") private static final boolean RECORD_STACK_TRACES = PRINT_ALLOC_STACK_TRACE_IN_FREE_LOG || PRINT_STACK_TRACES_IN_MEM_LEAKS_REPORT;
    @SuppressWarnings("unused") private static final boolean NEEDS_DEBUG_INFO = MEMORY_CHECK || LOG_ALLOC_FREE || LOG_ALLOC_FREE_FULL;

    private static final class DebugInfo {
        private final String info;
        @SuppressFBWarnings(value = "UWF_NULL_FIELD", justification = "used for debugging") private final RuntimeException stackTrace;

        private DebugInfo(String info) {
            this.info = info;
            this.stackTrace = RECORD_STACK_TRACES ? new RuntimeException() : null;
        }

        @Override
        public String toString() {
            return info;
        }
    }

    private static final ConcurrentHashMap<Long, DebugInfo> ALLOCATED = MEMORY_CHECK ? new ConcurrentHashMap<>() : null;

    static {
        if (MEMORY_CHECK) {
            Runtime.getRuntime().addShutdownHook(new Thread(NativeMemory::memoryCheckShutdownHook));
        }
    }

    @SuppressWarnings("unused")
    private static void memoryCheckShutdownHook() {
        try {
            System.out.print("Running GC before reporting potential memory leaks...");
            for (int i = 0; i < 3; i++) {
                System.gc();
                Thread.sleep(300);
            }
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // nop
        }

        if (ALLOCATED.isEmpty()) {
            System.out.println("No leaks detected!");
        } else {
            System.out.println("MEMORY ERROR: LEAKS DETECTED");
            for (Entry<Long, DebugInfo> item : ALLOCATED.entrySet()) {
                if (PRINT_STACK_TRACES_IN_MEM_LEAKS_REPORT) {
                    System.out.println("\n\n-----");
                }
                System.out.printf("%x (%s)\n", item.getKey(), item.getValue().info);
                if (item.getValue().stackTrace != null && PRINT_STACK_TRACES_IN_MEM_LEAKS_REPORT) {
                    System.out.println(shortStackTrace(item.getValue().stackTrace));
                }
            }
        }
    }

    private static void traceAllocateStart(long size, Object debugInfo) {
        if (LOG_ALLOC_FREE_FULL) {
            logAllocStart(size, debugInfo);
        }
    }

    private static void traceAllocate(long result, long size, Object debugInfo) {
        if (LOG_ALLOC_FREE) {
            logAlloc(result, size, debugInfo);
        }
        if (MEMORY_CHECK) {
            recordAllocation(result, debugInfo);
        }
    }

    private static void traceFreeDone(long address, Object debugInfo) {
        if (LOG_ALLOC_FREE_FULL) {
            logFree(address, debugInfo);
        }
    }

    private static void traceFree(long address, Object debugInfo) {
        if (LOG_ALLOC_FREE) {
            logFreeStart(address, debugInfo);
        }
        if (MEMORY_CHECK) {
            recordFree(address, debugInfo);
        }
    }

    @TruffleBoundary
    private static void logAllocStart(long size, Object debugInfo) {
        System.out.printf("NativeMemory: allocating memory of size %21d (%s)\n", size, formatDebugInfo(debugInfo));
    }

    @TruffleBoundary
    private static void logAlloc(long result, long size, Object debugInfo) {
        System.out.printf("NativeMemory: done allocating %x of size %10d (%s)\n", result, size, formatDebugInfo(debugInfo));
    }

    @TruffleBoundary
    private static void logFreeStart(long address, Object debugInfo) {
        StringBuffer sb = new StringBuffer(String.format("NativeMemory: going to free %14x (%s)\n", address, formatDebugInfo(debugInfo)));
        if (PRINT_ALLOC_STACK_TRACE_IN_FREE_LOG) {
            DebugInfo origInfo = ALLOCATED.get(address);
            if (origInfo != null && origInfo.stackTrace != null) {
                sb.append("Alloc stack trace:\n").append(shortStackTrace(origInfo.stackTrace)).append("\n-----\n");
            }
        }
        // printing everything at once, because this is run from the cleaner thread so otherwise the
        // output may interleave with the main thread
        System.out.println(sb.toString());
    }

    @TruffleBoundary
    private static void logFree(long address, Object debugInfo) {
        System.out.printf("NativeMemory: done freeing %x (%s)\n", address, formatDebugInfo(debugInfo));
    }

    @TruffleBoundary
    private static void recordAllocation(long result, Object debugInfo) {
        DebugInfo previous = ALLOCATED.put(result, new DebugInfo(formatDebugInfo(debugInfo)));
        // Allocate returned address that has been allocated already and was not freed
        assert previous == null : "MEMORY ERROR:" + previous;
    }

    @TruffleBoundary
    private static void recordFree(long address, Object debugInfo) {
        DebugInfo previous = ALLOCATED.remove(address);
        // Freeing address that was not allocated by us
        assert previous != null : "MEMORY ERROR:" + formatDebugInfo(debugInfo);
    }

    private static String formatDebugInfo(Object info) {
        if (info == null) {
            return "";
        } else if (info instanceof String) {
            return (String) info;
        } else {
            return info.getClass().getSimpleName();
        }
    }

    private static String shortStackTrace(Throwable ex) {
        StringWriter writer = new StringWriter();
        PrintWriter printer = new PrintWriter(writer);
        ex.printStackTrace(printer);
        String[] lines = writer.getBuffer().toString().split("\n");
        if (lines.length > 12) {
            lines = Arrays.copyOfRange(lines, 5, 12);
            return String.join("\n", lines);
        } else {
            return writer.getBuffer().toString();
        }
    }
}
