package com.oracle.truffle.r.runtime.ffi.util;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

@ExportLibrary(InteropLibrary.class)
public abstract class NativeArrayWrapper implements TruffleObject {
    private final int size;
    private final long arrayPtr;

    private NativeArrayWrapper(long arrayPtr, int size) {
        this.size = size;
        this.arrayPtr = arrayPtr;
    }

    public static NativeArrayWrapper createIntWrapper(long arrayPtr, int size) {
        return new NativeIntArrayWrapper(arrayPtr, size);
    }

    public abstract void writeElement(long arrayAddr, long index, Object value);
    public abstract Object readElement(long arrayAddr, long index);

    @ExportMessage
    public boolean hasArrayElements() {
        return true;
    }

    @ExportMessage
    public boolean isPointer() {
        return true;
    }

    @ExportMessage
    public long asPointer() {
        return arrayPtr;
    }

    @ExportMessage
    public void writeArrayElement(long index, Object value) {
        writeElement(arrayPtr, index, value);
    }

    @ExportMessage
    public Object readArrayElement(long index) {
        return readElement(arrayPtr, index);
    }

    @ExportMessage
    public void removeArrayElement(@SuppressWarnings("unused") long index) throws UnsupportedMessageException {
        throw UnsupportedMessageException.create();
    }

    @ExportMessage
    public long getArraySize() {
        return size;
    }


    @ExportMessage(name = "isArrayElementModifiable")
    @ExportMessage(name = "isArrayElementReadable")
    public boolean isArrayElementModifiable(long index) {
        return 0 <= index && index < size;
    }

    @ExportMessage
    public boolean isArrayElementInsertable(@SuppressWarnings("unused") long index) {
        return false;
    }

    @ExportMessage
    public boolean isArrayElementRemovable(@SuppressWarnings("unused") long index) {
        return false;
    }

    private static final class NativeIntArrayWrapper extends NativeArrayWrapper {
        public NativeIntArrayWrapper(long arrayPtr, int size) {
            super(arrayPtr, size);
        }

        @Override
        public void writeElement(long arrayPtr, long index, Object value) {
            NativeMemory.putInt(arrayPtr, index, (int) value);
        }

        @Override
        public Object readElement(long arrayPtr, long index) {
            return NativeMemory.getInt(arrayPtr, index);
        }
    }
}
