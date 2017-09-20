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

import com.oracle.truffle.r.runtime.ffi.StatsRFFI;

public class TruffleNFI_Stats implements StatsRFFI {

    private static class TruffleNFI_FactorNode extends TruffleNFI_DownCallNode implements FactorNode {
        @Override
        protected NativeFunction getFunction() {
            return NativeFunction.fft_factor;
        }

        @Override
        public void execute(int n, int[] pmaxf, int[] pmaxp) {
            call(n, pmaxf, pmaxp);
        }
    }

    private static class TruffleNFI_WorkNode extends TruffleNFI_DownCallNode implements WorkNode {
        @Override
        protected NativeFunction getFunction() {
            return NativeFunction.fft_work;
        }

        @Override
        public int execute(double[] a, int nseg, int n, int nspn, int isn, double[] work, int[] iwork) {
            return (int) call(a, nseg, n, nspn, isn, work, iwork);
        }
    }

    @Override
    public FactorNode createFactorNode() {
        return new TruffleNFI_FactorNode();
    }

    @Override
    public WorkNode createWorkNode() {
        return new TruffleNFI_WorkNode();
    }
}
