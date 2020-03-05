package com.oracle.truffle.r.test.altrep;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

@ExportLibrary(value = InteropLibrary.class)
final class Dataptr implements TruffleObject {
    private final long dataPtrAddr;

    Dataptr(long dataPtrAddr) {
        this.dataPtrAddr = dataPtrAddr;
    }

    @ExportMessage
    public long asPointer() {
        return dataPtrAddr;
    }

    @ExportMessage
    public boolean isPointer() {
        return true;
    }
}
