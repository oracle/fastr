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

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.CompilerDirectives.ValueType;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.Utils;

/**
 * Denotes an R "symbol" or "name". Its rep is a {@code String} but it's a different type in the
 * Truffle sense.
 */
@ValueType
@ExportLibrary(InteropLibrary.class)
public final class RSymbol extends RAttributeStorage {

    /**
     * Note: GnuR caches all symbols and some packages rely on their identity. Moreover, the cached
     * symbols are never garbage collected. This table corresponds to {@code R_SymbolTable} in GNUR.
     * Note: we could rewrite this to some concurrent implementation of identity hash map since the
     * strings are interned.
     */
    private static final ConcurrentHashMap<String, RSymbol> symbolTable = new ConcurrentHashMap<>(2551);

    public static final RSymbol MISSING = RDataFactory.createSymbol("");

    private CharSXPWrapper nameWrapper;

    private RSymbol(String name) {
        this.nameWrapper = CharSXPWrapper.createInterned(name);
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    boolean isPointer() {
        return true;
    }

    @ExportMessage
    long asPointer() {
        return NativeDataAccess.asPointer(this);
    }

    @ExportMessage
    void toNative() {
        NativeDataAccess.asPointer(this);
    }

    @TruffleBoundary
    public static RSymbol install(String name, Function<RSymbol, RSymbol> reportAllocation) {
        assert Utils.isInterned(name);
        if (reportAllocation == null) {
            return symbolTable.computeIfAbsent(name, FastRSymbolAllocator.INSTANCE);
        } else {
            return symbolTable.computeIfAbsent(name, new RSymbolAllocator(reportAllocation));
        }
    }

    @Override
    public RType getRType() {
        return RType.Symbol;
    }

    public String getName() {
        return nameWrapper.getContents();
    }

    public CharSXPWrapper getWrappedName() {
        return nameWrapper;
    }

    @Override
    public String toString() {
        return getName();
    }

    public boolean isMissing() {
        return getName().isEmpty();
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this.getName());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof RSymbol) {
            return Utils.identityEquals(((RSymbol) obj).getName(), this.nameWrapper.getContents());
        }
        return false;
    }

    private static final class FastRSymbolAllocator implements Function<String, RSymbol> {
        static final FastRSymbolAllocator INSTANCE = new FastRSymbolAllocator();

        @Override
        public RSymbol apply(String s) {
            return new RSymbol(s);
        }
    }

    private static final class RSymbolAllocator implements Function<String, RSymbol> {
        private final Function<RSymbol, RSymbol> reportAllocation;

        RSymbolAllocator(Function<RSymbol, RSymbol> reportAllocation) {
            this.reportAllocation = reportAllocation;
        }

        @Override
        public RSymbol apply(String n) {
            RSymbol result = new RSymbol(n);
            if (reportAllocation != null) {
                result = reportAllocation.apply(result);
            }
            return result;
        }
    }
}
