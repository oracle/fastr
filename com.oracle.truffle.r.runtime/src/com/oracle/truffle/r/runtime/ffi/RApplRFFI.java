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

import com.oracle.truffle.api.nodes.Node;

/**
 * Collection of statically typed methods (from Linpack and elsewhere) that are built in to a GnuR
 * implementation and factored out into a separate library in FastR. This corresponds to the
 * {@code libappl} library in GnuR.
 */
public interface RApplRFFI {
    abstract class Dqrdc2Node extends Node {
        public abstract void execute(double[] x, int ldx, int n, int p, double tol, int[] rank, double[] qraux, int[] pivot, double[] work);

        public static Dqrdc2Node create() {
            return RFFIFactory.getRFFI().getRApplRFFI().createDqrdc2Node();
        }
    }

    abstract class DqrcfNode extends Node {
        public abstract void execute(double[] x, int n, int k, double[] qraux, double[] y, int ny, double[] b, int[] info);

        public static Dqrdc2Node create() {
            return RFFIFactory.getRFFI().getRApplRFFI().createDqrdc2Node();
        }
    }

    abstract class DqrlsNode extends Node {
        public abstract void execute(double[] x, int n, int p, double[] y, int ny, double tol, double[] b, double[] rsd, double[] qty, int[] k, int[] jpvt, double[] qraux, double[] work);

        public static DqrlsNode create() {
            return RFFIFactory.getRFFI().getRApplRFFI().createDqrlsNode();
        }
    }

    abstract class DqrqtyNode extends Node {
        public abstract void execute(double[] x, int n, int k, double[] qraux, double[] y, int ny, double[] qty);

        public static DqrqtyNode create() {

            return RFFIFactory.getRFFI().getRApplRFFI().createDqrqtyNode();
        }
    }

    abstract class DqrqyNode extends Node {
        public abstract void execute(double[] x, int n, int k, double[] qraux, double[] y, int ny, double[] qy);

        public static DqrqyNode create() {
            return RFFIFactory.getRFFI().getRApplRFFI().createDqrqyNode();
        }
    }

    abstract class DqrrsdNode extends Node {
        public abstract void execute(double[] x, int n, int k, double[] qraux, double[] y, int ny, double[] rsd);

        public static DqrrsdNode create() {
            return RFFIFactory.getRFFI().getRApplRFFI().createDqrrsdNode();
        }
    }

    abstract class DqrxbNode extends Node {
        public abstract void execute(double[] x, int n, int k, double[] qraux, double[] y, int ny, double[] xb);

        public static DqrxbNode create() {
            return RFFIFactory.getRFFI().getRApplRFFI().createDqrxbNode();
        }
    }

    Dqrdc2Node createDqrdc2Node();

    DqrcfNode createDqrcfNode();

    DqrlsNode createDqrlsNode();

    DqrqtyNode createDqrqtyNode();

    DqrqyNode createDqrqyNode();

    DqrrsdNode createDqrrsdNode();

    DqrxbNode createDqrxbNode();
}
