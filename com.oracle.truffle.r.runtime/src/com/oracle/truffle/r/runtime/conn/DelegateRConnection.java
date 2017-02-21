package com.oracle.truffle.r.runtime.conn;

import java.io.IOException;
import java.nio.channels.Channel;

import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.BaseRConnection;
import com.sun.istack.internal.NotNull;

abstract class DelegateRConnection extends RConnection {
    protected BaseRConnection base;

    DelegateRConnection(@NotNull BaseRConnection base) {
        this.base = base;
    }

    @Override
    public int getDescriptor() {
        return base.getDescriptor();
    }

    @Override
    public boolean isTextMode() {
        return base.isTextMode();
    }

    @Override
    public boolean isOpen() {
        return base.isOpen();
    }

    @Override
    public RConnection forceOpen(String modeString) throws IOException {
        return base.forceOpen(modeString);
    }

    @Override
    protected long seekInternal(long offset, SeekMode seekMode, SeekRWMode seekRWMode) throws IOException {
        assert !isSeekable();
        throw RError.error(RError.SHOW_CALLER2, RError.Message.UNSEEKABLE_CONNECTION);
    }

    public abstract Channel getChannel();

}