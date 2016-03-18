/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.ffi.jnr;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.ffi.ZipRFFI;

import jnr.ffi.LibraryLoader;
import jnr.ffi.annotations.In;
import jnr.ffi.annotations.Out;

/**
 * Zip support using JNR.
 */
public class JNR_Zip implements ZipRFFI {
    public interface Zip {
        int compress(@Out byte[] dest, long[] destlen, @In byte[] source, long sourcelen);

        int uncompress(@Out byte[] dest, long[] destlen, @In byte[] source, long sourcelen);
    }

    private static class ZipProvider {
        private static Zip zip;

        @TruffleBoundary
        private static Zip createAndLoadLib() {
            return LibraryLoader.create(Zip.class).load("z");
        }

        static Zip zip() {
            if (zip == null) {
                zip = createAndLoadLib();
            }
            return zip;
        }
    }

    private static Zip zip() {
        return ZipProvider.zip();
    }

    @Override
    @TruffleBoundary
    public int compress(byte[] dest, long[] destlen, byte[] source) {
        return zip().compress(dest, destlen, source, source.length);
    }

    @Override
    @TruffleBoundary
    public int uncompress(byte[] dest, long[] destlen, byte[] source) {
        return zip().uncompress(dest, destlen, source, source.length);
    }
}
