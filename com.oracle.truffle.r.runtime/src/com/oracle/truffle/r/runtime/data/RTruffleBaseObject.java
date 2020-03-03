package com.oracle.truffle.r.runtime.data;

import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.runtime.context.TruffleRLanguage;

/**
 * This class should serve only for code sharing purposes to avoid implementing base interop messages in every R object that can participate in interop. However, not all R object are required to extend this class, they are only required to implement {@link RTruffleObject}.
 */
@ExportLibrary(InteropLibrary.class)
public abstract class RTruffleBaseObject implements RTruffleObject {
    @ExportMessage
    @SuppressWarnings("static-method")
    public boolean hasLanguage() {
        return true;
    }

    @ExportMessage
    @SuppressWarnings("static-method")
    public Class<? extends TruffleLanguage<?>> getLanguage() {
        return TruffleRLanguage.class;
    }

    @ExportMessage
    @SuppressWarnings("static-method")
    public boolean hasMetaObject() {
        // convenient default, overridden in RBaseObject
        return false;
    }

    @ExportMessage
    public Object getMetaObject() throws UnsupportedMessageException {
        throw UnsupportedMessageException.create();
    }

    @ExportMessage
    public boolean hasSourceLocation() {
        // default for most of the R objects, overridden in RFunction
        return false;
    }

    @ExportMessage
    public SourceSection getSourceLocation() throws UnsupportedMessageException {
        throw UnsupportedMessageException.create();
    }

    @ExportMessage
    public Object toDisplayString(boolean allowSideEffects) {
        return null;
    }
}
