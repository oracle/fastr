/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data;

import java.nio.charset.StandardCharsets;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.Utils;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Internally GNU R distinguishes "strings" and "vectors of strings" using the {@code CHARSXP} and
 * {@code STRSXP} types, respectively. Although this difference is invisible at the R level, it
 * manifests itself in the R FFI as several functions traffic in the {@code CHARSXP} type. Since
 * FastR already uses {@code String} to denote a length-1 string vector, it cannot be used to
 * represent a {@code CHARSXP}, so this class exists to do so.
 *
 * As opposed to Strings on the Java side, the native side "Strings" should be treated as array of
 * bytes. {@link CharSXPWrapper} wraps the byte array, but does not add the '\0' at the end of it.
 *
 * N.B. Use limited to RFFI implementations.
 */
public final class CharSXPWrapper extends RObject implements RTruffleObject, RTypedValue {
    private static final CharSXPWrapper NA = new CharSXPWrapper(RRuntime.STRING_NA);
    private int typedValueInfo = ASCII_MASK_SHIFTED;
    private String contents;
    private byte[] bytes;
    private static final Map<CharSXPWrapper, WeakReference<CharSXPWrapper>> instances = new WeakHashMap<>(2048);

    private CharSXPWrapper(String contents) {
        this.contents = contents;
    }

    @TruffleBoundary
    public String getContents() {
        if (this == NA) {
            // The NA string may have been moved to the native space if someone called R_CHAR on it,
            // but on the Java side, it should still look like NA string, i.e. RRuntime.isNA should
            // be true for its contents
            return RRuntime.STRING_NA;
        }
        // WARNING:
        // we keep and use the contents value even in cases when contents got allocated and could be
        // accessed via NativeDataAccess.getData():
        // - when used with RSymbol the String has to be interned - NDA.getData() will create a new
        // instance if already allocated
        // - the contents field is also used in equals() and hashCode()
        assert !NativeDataAccess.isAllocated(this) || contents.equals(NativeDataAccess.getData(this, contents));
        return contents;
    }

    @TruffleBoundary
    public byte getByteAt(int index) {
        return NativeDataAccess.getDataAt(this, getBytes(), index);
    }

    @TruffleBoundary
    public int getLength() {
        return NativeDataAccess.getDataLength(this, getBytes());
    }

    @Override
    public String toString() {
        return "CHARSXP(" + getContents() + ")";
    }

    public void setTruelength(int truelength) {
        NativeDataAccess.setTrueDataLength(this, truelength);
    }

    public int getTruelength() {
        return NativeDataAccess.getTrueDataLength(this);
    }

    public static CharSXPWrapper create(String contents) {
        return create(contents, false);
    }

    public static CharSXPWrapper createInterned(String contents) {
        assert Utils.isInterned(contents);
        return create(contents, true);
    }

    private static CharSXPWrapper create(String contents, boolean intern) {
        assert !intern || Utils.isInterned(contents);
        if (RRuntime.isNA(contents)) {
            return NA;
        } else {
            CharSXPWrapper cachedWrapper;
            CharSXPWrapper newWrapper = new CharSXPWrapper(contents);
            synchronized (instances) {
                WeakReference<CharSXPWrapper> wr = instances.get(newWrapper);
                if (wr != null) {
                    cachedWrapper = wr.get();
                    if (cachedWrapper != null) {
                        if (intern) {
                            cachedWrapper.contents = contents;
                        }
                        return cachedWrapper;
                    }
                }
                instances.put(newWrapper, new WeakReference<>(newWrapper));
            }
            return newWrapper;
        }
    }

    public long allocateNativeContents() {
        try {
            return NativeDataAccess.allocateNativeContents(this, getBytes());
        } finally {
            bytes = null;
        }
    }

    private byte[] getBytes() {
        if (bytes == null && !NativeDataAccess.isAllocated(this)) {
            bytes = contents.getBytes(StandardCharsets.UTF_8);
        }
        return bytes;
    }

    @Override
    public int hashCode() {
        return this.contents.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CharSXPWrapper other = (CharSXPWrapper) obj;
        return this.contents.equals(other.contents);
    }

    @Override
    public RType getRType() {
        return RType.Char;
    }

    @Override
    public int getTypedValueInfo() {
        return typedValueInfo;
    }

    @Override
    public void setTypedValueInfo(int value) {
        typedValueInfo = value;
    }

}
