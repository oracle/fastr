/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.ffi.impl.nfi;

import com.oracle.truffle.api.interop.java.JavaInterop;
import com.oracle.truffle.r.runtime.ffi.NativeFunction;
import com.oracle.truffle.r.runtime.ffi.ZipRFFI;

public class TruffleNFI_Zip implements ZipRFFI {

    private static class TruffleNFI_CompressNode extends TruffleNFI_DownCallNode implements ZipRFFI.CompressNode {
        @Override
        protected NativeFunction getFunction() {
            return NativeFunction.compress;
        }

        @Override
        public int execute(byte[] dest, byte[] source) {
            return (int) call(JavaInterop.asTruffleObject(dest), (long) dest.length, JavaInterop.asTruffleObject(source), source.length);
        }
    }

    private static class TruffleNFI_UncompressNode extends TruffleNFI_DownCallNode implements ZipRFFI.UncompressNode {
        @Override
        protected NativeFunction getFunction() {
            return NativeFunction.uncompress;
        }

        @Override
        public int execute(byte[] dest, byte[] source) {
            return (int) call(JavaInterop.asTruffleObject(dest), (long) dest.length, JavaInterop.asTruffleObject(source), source.length);
        }
    }

    @Override
    public CompressNode createCompressNode() {
        return new TruffleNFI_CompressNode();
    }

    @Override
    public UncompressNode createUncompressNode() {
        return new TruffleNFI_UncompressNode();
    }
}
