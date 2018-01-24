/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.ffi;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInterface;
import com.oracle.truffle.r.runtime.context.RContext;

/**
 * zip compression/uncompression.
 */
public interface ZipRFFI {

    interface CompressNode extends NodeInterface {
        /**
         * compress {@code source} into {@code dest}.
         *
         * @return standard return code (0 ok)
         */
        int execute(byte[] dest, byte[] source);

        static CompressNode create() {
            return RFFIFactory.getZipRFFI().createCompressNode();
        }
    }

    interface UncompressNode extends NodeInterface {
        /**
         * uncompress {@code source} into {@code dest}.
         *
         * @return standard return code (0 ok)
         */
        int execute(byte[] dest, byte[] source);
    }

    CompressNode createCompressNode();

    UncompressNode createUncompressNode();

    // RootNodes for calling when not in Truffle context

    final class CompressRootNode extends RFFIRootNode<CompressNode> {
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

    final class UncompressRootNode extends RFFIRootNode<UncompressNode> {
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
