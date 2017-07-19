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
package com.oracle.truffle.r.ffi.impl.jni;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.ffi.RApplRFFI;

public class JNI_RAppl implements RApplRFFI {
    private static class JNI_Dqrdc2Node extends Dqrdc2Node {
        @Override
        @TruffleBoundary
        public void execute(double[] x, int ldx, int n, int p, double tol, int[] rank, double[] qraux, int[] pivot, double[] work) {
            native_dqrdc2(x, ldx, n, p, tol, rank, qraux, pivot, work);
        }
    }

    private static class JNI_DqrcfNode extends DqrcfNode {

        @Override
        @TruffleBoundary
        public void execute(double[] x, int n, int k, double[] qraux, double[] y, int ny, double[] b, int[] info) {
            native_dqrcf(x, n, k, qraux, y, ny, b, info);
        }
    }

    private static class JNI_DqrlsNode extends DqrlsNode {
        @Override
        @TruffleBoundary
        public void execute(double[] x, int n, int p, double[] y, int ny, double tol, double[] b, double[] rsd, double[] qty, int[] k, int[] jpvt, double[] qraux, double[] work) {
            native_dqrls(x, n, p, y, ny, tol, b, rsd, qty, k, jpvt, qraux, work);
        }
    }

    private static class JNI_DqrqtyNode extends DqrqtyNode {
        @Override
        @TruffleBoundary
        public void execute(double[] x, int n, int k, double[] qraux, double[] y, int ny, double[] qty) {
            native_dqrqty(x, n, k, qraux, y, ny, qty);
        }
    }

    private static class JNI_DqrqyNode extends DqrqyNode {
        @Override
        @TruffleBoundary
        public void execute(double[] x, int n, int k, double[] qraux, double[] y, int ny, double[] qy) {
            native_dqrqy(x, n, k, qraux, y, ny, qy);
        }
    }

    private static class JNI_DqrrsdNode extends DqrrsdNode {
        @Override
        @TruffleBoundary
        public void execute(double[] x, int n, int k, double[] qraux, double[] y, int ny, double[] rsd) {
            native_dqrrsd(x, n, k, qraux, y, ny, rsd);
        }
    }

    private static class JNI_DqrxbNode extends DqrxbNode {
        @Override
        @TruffleBoundary
        public void execute(double[] x, int n, int k, double[] qraux, double[] y, int ny, double[] xb) {
            native_dqrxb(x, n, k, qraux, y, ny, xb);
        }
    }

    @Override
    public Dqrdc2Node createDqrdc2Node() {
        return new JNI_Dqrdc2Node();
    }

    @Override
    public DqrcfNode createDqrcfNode() {
        return new JNI_DqrcfNode();
    }

    @Override
    public DqrlsNode createDqrlsNode() {
        return new JNI_DqrlsNode();
    }

    @Override
    public DqrqtyNode createDqrqtyNode() {
        return new JNI_DqrqtyNode();
    }

    @Override
    public DqrqyNode createDqrqyNode() {
        return new JNI_DqrqyNode();
    }

    @Override
    public DqrrsdNode createDqrrsdNode() {
        return new JNI_DqrrsdNode();
    }

    @Override
    public DqrxbNode createDqrxbNode() {
        return new JNI_DqrxbNode();
    }

    // Checkstyle: stop method name

    private static native void native_dqrdc2(double[] x, int ldx, int n, int p, double tol, int[] rank, double[] qraux, int[] pivot, double[] work);

    private static native void native_dqrcf(double[] x, int n, int k, double[] qraux, double[] y, int ny, double[] b, int[] info);

    private static native void native_dqrls(double[] x, int n, int p, double[] y, int ny, double tol, double[] b, double[] rsd, double[] qty, int[] k, int[] jpvt, double[] qraux, double[] work);

    private static native void native_dqrqty(double[] x, int n, int k, double[] qraux, double[] y, int ny, double[] qty);

    private static native void native_dqrqy(double[] x, int n, int k, double[] qraux, double[] y, int ny, double[] qy);

    private static native void native_dqrrsd(double[] x, int n, int k, double[] qraux, double[] y, int ny, double[] rsd);

    private static native void native_dqrxb(double[] x, int n, int k, double[] qraux, double[] y, int ny, double[] xb);
}
