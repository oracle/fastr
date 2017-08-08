/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

import sun.misc.Unsafe;

abstract class InteropRootNode extends RootNode {
    InteropRootNode() {
        super(RContext.getInstance().getLanguage());
    }

    @Override
    public final SourceSection getSourceSection() {
        return RSyntaxNode.INTERNAL;
    }
}

class UnsafeAdapter {
    public static final Unsafe UNSAFE = initUnsafe();

    private static Unsafe initUnsafe() {
        try {
            return Unsafe.getUnsafe();
        } catch (SecurityException se) {
            try {
                Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
                theUnsafe.setAccessible(true);
                return (Unsafe) theUnsafe.get(Unsafe.class);
            } catch (Exception e) {
                throw new RuntimeException("exception while trying to get Unsafe", e);
            }
        }
    }
}

public final class NativeDataAccess {
    private NativeDataAccess() {
        // no instances
    }

    private static final class NativeMirror {
        private final long id;
        private long dataAddress;

        NativeMirror() {
            this.id = counter.incrementAndGet();
        }

        @Override
        protected void finalize() throws Throwable {
            super.finalize();
            nativeMirrors.remove(id);
            if (dataAddress != 0) {
                UnsafeAdapter.UNSAFE.freeMemory(dataAddress);
                assert (dataAddress = 0xbadbad) != 0;
            }
        }
    }

    private static final AtomicLong counter = new AtomicLong(0xdef000000000000L);
    private static final ConcurrentHashMap<Long, WeakReference<RObject>> nativeMirrors = new ConcurrentHashMap<>();

    public static CallTarget createIsPointer() {
        return Truffle.getRuntime().createCallTarget(new InteropRootNode() {
            @Override
            public Object execute(VirtualFrame frame) {
                return ForeignAccess.getReceiver(frame) instanceof RObject;
            }
        });
    }

    public static CallTarget createAsPointer() {
        return Truffle.getRuntime().createCallTarget(new InteropRootNode() {
            @Override
            public Object execute(VirtualFrame frame) {
                Object arg = ForeignAccess.getReceiver(frame);
                if (arg instanceof RObject) {
                    RObject obj = (RObject) arg;
                    NativeMirror mirror = (NativeMirror) obj.getNativeMirror();
                    if (mirror == null) {
                        obj.setNativeMirror(mirror = new NativeMirror());
                    }
                    return mirror.id;
                }
                throw UnsupportedMessageException.raise(Message.AS_POINTER);
            }
        });
    }

    public static Object lookup(long address) {
        Object result = nativeMirrors.get(address);
        if (result == null) {
            CompilerDirectives.transferToInterpreter();
            throw RInternalError.shouldNotReachHere("unknown/stale native reference");
        }
        return result;
    }
}
