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
package com.oracle.truffle.r.runtime.ffi;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.ffi.interop.NativeRawArray;

/**
 * zip compression/uncompression.
 */
public final class ZipRFFI {

    private final DownCallNodeFactory downCallNodeFactory;

    public ZipRFFI(DownCallNodeFactory downCallNodeFactory) {
        this.downCallNodeFactory = downCallNodeFactory;
    }

    public static final class CompressNode extends NativeCallNode {
        private CompressNode(DownCallNodeFactory factory) {
            super(factory.createDownCallNode());
        }

        /**
         * compress {@code source} into {@code dest}.
         *
         * @return standard return code (0 ok)
         */
        public int execute(byte[] dest, byte[] source) {
            NativeRawArray nativeDest = new NativeRawArray(dest);
            NativeRawArray nativeSource = new NativeRawArray(source);
            try {
                return (int) call(NativeFunction.compress, nativeDest, dest.length, nativeSource, source.length);
            } finally {
                nativeDest.getValue();
            }
        }

        public static CompressNode create() {
            return RFFIFactory.getZipRFFI().createCompressNode();
        }
    }

    public static final class UncompressNode extends NativeCallNode {
        private UncompressNode(DownCallNodeFactory factory) {
            super(factory.createDownCallNode());
        }

        /**
         * uncompress {@code source} into {@code dest}.
         *
         * @return standard return code (0 ok)
         */
        public int execute(byte[] dest, byte[] source) {
            NativeRawArray nativeDest = new NativeRawArray(dest);
            NativeRawArray nativeSource = new NativeRawArray(source);
            try {
                return (int) call(NativeFunction.uncompress, nativeDest, dest.length, nativeSource, source.length);
            } finally {
                nativeDest.getValue();
            }
        }
    }

    public CompressNode createCompressNode() {
        return new CompressNode(downCallNodeFactory);
    }

    public UncompressNode createUncompressNode() {
        return new UncompressNode(downCallNodeFactory);
    }

    // RootNodes for calling when not in Truffle context

    public static final class CompressRootNode extends RFFIRootNode<CompressNode> {
        protected CompressRootNode(CompressNode wrapped) {
            super(wrapped);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object[] args = frame.getArguments();
            return rffiNode.execute((byte[]) args[0], (byte[]) args[1]);
        }

        public static CallTarget create(RContext context) {
            return context.getOrCreateCachedCallTarget(CompressRootNode.class, () -> new CompressRootNode(context.getRFFI().zipRFFI.createCompressNode()).getCallTarget());
        }
    }

    public static final class UncompressRootNode extends RFFIRootNode<UncompressNode> {
        protected UncompressRootNode(UncompressNode wrapped) {
            super(wrapped);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object[] args = frame.getArguments();
            return rffiNode.execute((byte[]) args[0], (byte[]) args[1]);
        }

        public static CallTarget create(RContext context) {
            return context.getOrCreateCachedCallTarget(UncompressRootNode.class, () -> new UncompressRootNode(context.getRFFI().zipRFFI.createUncompressNode()).getCallTarget());
        }
    }
}
