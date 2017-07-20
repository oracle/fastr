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
package com.oracle.truffle.r.runtime.ffi;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

/**
 * zip compression/uncompression.
 */
public interface ZipRFFI {

    abstract class CompressNode extends Node {
        /**
         * compress {@code source} into {@code dest}.
         *
         * @return standard return code (0 ok)
         */
        public abstract int execute(byte[] dest, byte[] source);

        public static CompressNode create() {
            return RFFIFactory.getRFFI().getZipRFFI().createCompressNode();
        }
    }

    abstract class UncompressNode extends Node {
        /**
         * uncompress {@code source} into {@code dest}.
         *
         * @return standard return code (0 ok)
         */
        public abstract int execute(byte[] dest, byte[] source);
    }

    CompressNode createCompressNode();

    UncompressNode createUncompressNode();

    // RootNodes for calling when not in Truffle context

    final class CompressRootNode extends RFFIRootNode<CompressNode> {
        private static CompressRootNode compressRootNode;

        private CompressRootNode() {
            super(RFFIFactory.getRFFI().getZipRFFI().createCompressNode());
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object[] args = frame.getArguments();
            return rffiNode.execute((byte[]) args[0], (byte[]) args[1]);
        }

        public static CompressRootNode create() {
            if (compressRootNode == null) {
                compressRootNode = new CompressRootNode();
            }
            return compressRootNode;
        }
    }

    final class UncompressRootNode extends RFFIRootNode<UncompressNode> {
        private static UncompressRootNode uncompressRootNode;

        private UncompressRootNode() {
            super(RFFIFactory.getRFFI().getZipRFFI().createUncompressNode());
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object[] args = frame.getArguments();
            return rffiNode.execute((byte[]) args[0], (byte[]) args[1]);
        }

        public static UncompressRootNode create() {
            if (uncompressRootNode == null) {
                uncompressRootNode = new UncompressRootNode();
            }
            return uncompressRootNode;
        }
    }
}
