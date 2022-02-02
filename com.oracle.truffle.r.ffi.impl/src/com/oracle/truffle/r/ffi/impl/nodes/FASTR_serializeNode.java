/*
 * Copyright (c) 2021, 2022, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.truffle.r.ffi.impl.nodes;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.nfi.api.SignatureLibrary;
import com.oracle.truffle.r.ffi.impl.nfi.TruffleNFI_Context;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RSerialize;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.TruffleRLanguage;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.ffi.interop.NativeCharArray;

@GenerateUncached
public abstract class FASTR_serializeNode extends FFIUpCallNode.Arg5 {
    // void OutBytes(R_outpstream_t stream, void *buffer, int n)
    private static final String outBytesFuncSignature = "(pointer, pointer, sint32): void";

    public static FASTR_serializeNode create() {
        return FASTR_serializeNodeGen.create();
    }

    @GenerateUncached
    abstract static class BindSignatureNode extends Node {

        abstract Object execute(Object function);

        @TruffleBoundary
        static Object createSignature() {
            return TruffleNFI_Context.parseSignature(outBytesFuncSignature);
        }

        @Specialization
        Object doBind(Object function,
                        @Cached(value = "createSignature()", allowUncached = true) Object signature,
                        @CachedLibrary("signature") SignatureLibrary signatures) {
            return signatures.bind(signature, function);
        }
    }

    /**
     * Serializes object into buffer of bytes and passes this buffer into outBytesFunc native
     * callback.
     *
     * FIXME: We serialize to XDR format no matter what the caller specified, therefore type
     * parameter is intentionally ignored.
     *
     * @param object Object to be serialized.
     * @param type Type of serialization, e.g., XDR or BINARY. Currently, only XDR is supported.
     * @param version Version of serialization. Can be either 2 or 3.
     * @param stream A native object of type R_outpstream_t. Defined in Rinternals.h. Contains
     *            pointers to various callback functions into which we pass serialized byte array.
     * @param outBytesFunc Pointer to native function callback. This is a member of R_outpstream_t
     *            struct.
     * @return RNull, bytes are passed into outBytesFunc native function.
     */
    @Specialization
    protected Object doIt(Object object, @SuppressWarnings("unused") int type, int version, Object stream, Object outBytesFunc,
                    @CachedContext(TruffleRLanguage.class) TruffleLanguage.ContextReference<RContext> ctxRef,
                    @Cached BindSignatureNode bind,
                    @CachedLibrary(limit = "getInteropLibraryCacheSize()") InteropLibrary interopLibrary) {

        Object outBytesFuncExecutable = null;
        if (!interopLibrary.isExecutable(outBytesFunc)) {
            outBytesFuncExecutable = bind.execute(outBytesFunc);
        } else {
            outBytesFuncExecutable = outBytesFunc;
        }
        assert outBytesFuncExecutable != null;
        assert interopLibrary.isExecutable(outBytesFuncExecutable);

        // TODO: Pass type instead of RSerialize.XDR
        byte[] serializedBuff = RSerialize.serialize(ctxRef.get(), object, RSerialize.XDR, version, null);
        NativeCharArray nativeBuff = new NativeCharArray(serializedBuff);

        try {
            interopLibrary.execute(outBytesFuncExecutable, stream, nativeBuff, serializedBuff.length);
        } catch (Exception e) {
            throw RInternalError.shouldNotReachHere(e);
        }
        return RNull.instance;
    }
}
