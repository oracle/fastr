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
import com.oracle.truffle.r.runtime.ffi.RApplRFFI;

public class TruffleNFI_RAppl implements RApplRFFI {

    private static class TruffleNFI_Dqrdc2Node extends Dqrdc2Node {
        @Child private Node message = NFIFunction.dqrdc2.createMessage();

        @Override
        public void execute(double[] x, int ldx, int n, int p, double tol, int[] rank, double[] qraux, int[] pivot, double[] work) {
            try {
                ForeignAccess.sendExecute(message, NFIFunction.dqrdc2.getFunction(),
                                JavaInterop.asTruffleObject(x),
                                ldx, n, p, tol,
                                JavaInterop.asTruffleObject(rank),
                                JavaInterop.asTruffleObject(qraux),
                                JavaInterop.asTruffleObject(pivot),
                                JavaInterop.asTruffleObject(work));
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    private static class TruffleNFI_DqrcfNode extends DqrcfNode {
        @Child private Node message = NFIFunction.dqrcf.createMessage();

        @Override
        public void execute(double[] x, int n, int k, double[] qraux, double[] y, int ny, double[] b, int[] info) {
            try {
                ForeignAccess.sendExecute(message, NFIFunction.dqrcf.getFunction(),
                                JavaInterop.asTruffleObject(x),
                                n, k,
                                JavaInterop.asTruffleObject(qraux),
                                JavaInterop.asTruffleObject(y),
                                ny,
                                JavaInterop.asTruffleObject(b),
                                JavaInterop.asTruffleObject(info));

            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    private static class TruffleNFI_DqrlsNode extends DqrlsNode {
        @Child private Node message = NFIFunction.dqrls.createMessage();

        @Override
        public void execute(double[] x, int n, int p, double[] y, int ny, double tol, double[] b, double[] rsd, double[] qty, int[] k, int[] jpvt, double[] qraux, double[] work) {
            try {
                ForeignAccess.sendExecute(message, NFIFunction.dqrls.getFunction(),
                                JavaInterop.asTruffleObject(x),
                                n, p,
                                JavaInterop.asTruffleObject(y),
                                ny, tol,
                                JavaInterop.asTruffleObject(b),
                                JavaInterop.asTruffleObject(rsd),
                                JavaInterop.asTruffleObject(qty),
                                JavaInterop.asTruffleObject(k),
                                JavaInterop.asTruffleObject(jpvt),
                                JavaInterop.asTruffleObject(qraux),
                                JavaInterop.asTruffleObject(work));

            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    @Override
    public Dqrdc2Node createDqrdc2Node() {
        return new TruffleNFI_Dqrdc2Node();
    }

    @Override
    public DqrcfNode createDqrcfNode() {
        return new TruffleNFI_DqrcfNode();
    }

    @Override
    public DqrlsNode createDqrlsNode() {
        return new TruffleNFI_DqrlsNode();
    }
}
