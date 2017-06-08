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

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.java.JavaInterop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.ffi.ZipRFFI;

public class TruffleNFI_Zip implements ZipRFFI {

    private static class TruffleNFI_CompressNode extends ZipRFFI.CompressNode {
        @Child private Node message = NFIFunction.compress.createMessage();

        @Override
        public int execute(byte[] dest, byte[] source) {
            long[] destlen = new long[]{dest.length};
            try {
                int result = (int) ForeignAccess.sendExecute(message, NFIFunction.compress.getFunction(),
                                JavaInterop.asTruffleObject(dest), JavaInterop.asTruffleObject(destlen),
                                JavaInterop.asTruffleObject(source), JavaInterop.asTruffleObject(source.length));
                return result;
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    private static class TruffleNFI_UncompressNode extends ZipRFFI.UncompressNode {
        @Child private Node message = NFIFunction.uncompress.createMessage();

        @Override
        public int execute(byte[] dest, byte[] source) {
            long[] destlen = new long[]{dest.length};
            try {
                int result = (int) ForeignAccess.sendExecute(message, NFIFunction.uncompress.getFunction(),
                                JavaInterop.asTruffleObject(dest), JavaInterop.asTruffleObject(destlen),
                                JavaInterop.asTruffleObject(source), JavaInterop.asTruffleObject(source.length));
                return result;
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
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
