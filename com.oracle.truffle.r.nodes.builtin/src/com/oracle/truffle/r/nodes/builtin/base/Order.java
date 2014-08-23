/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.ops.*;

@RBuiltin(name = "order", kind = SUBSTITUTE, parameterNames = {"x", "tie"})
// TODO INTERNAL
public abstract class Order extends RBuiltinNode {

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance)};
    }

    public abstract Object executeDoubleVector(VirtualFrame frame, Object vec, RMissing tie);

    @Child private BooleanOperation eq = BinaryCompare.EQUAL.create();
    @Child private BooleanOperation lt = BinaryCompare.LESS_THAN.create();
    @Child private BooleanOperation le = BinaryCompare.LESS_EQUAL.create();
    @Child private BooleanOperation ge = BinaryCompare.GREATER_EQUAL.create();
    @Child private BooleanOperation gt = BinaryCompare.GREATER_THAN.create();

    // specialisations for one parameter

    @Specialization
    protected RIntVector order(RStringVector x, @SuppressWarnings("unused") RMissing tie) {
        controlVisibility();
        String[] xs = x.getDataCopy();
        int[] ord = ordArray(xs.length);
        stringSort(xs, ord, null, 0, xs.length - 1);
        return RDataFactory.createIntVector(ord, RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization
    protected RIntVector order(RDoubleVector x, @SuppressWarnings("unused") RMissing tie) {
        controlVisibility();
        double[] xs = x.getDataCopy();
        int[] ord = ordArray(xs.length);
        doubleSort(xs, ord, null, 0, xs.length - 1);
        return RDataFactory.createIntVector(ord, RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization
    protected RIntVector order(RIntVector x, @SuppressWarnings("unused") RMissing tie) {
        controlVisibility();
        int[] xs = x.getDataCopy();
        int[] ord = ordArray(xs.length);
        intSort(xs, ord, null, 0, xs.length - 1);
        return RDataFactory.createIntVector(ord, RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization
    protected RIntVector order(RIntVector x, @SuppressWarnings("unused") RNull nul) {
        controlVisibility();
        return order(x, RMissing.instance);
    }

    @Specialization
    protected RIntVector order(RComplexVector x, @SuppressWarnings("unused") RMissing tie) {
        controlVisibility();
        double[] xs = x.getDataCopy();
        int[] ord = ordArray(x.getLength());
        complexSort(xs, ord, null, 0, x.getLength() - 1);
        return RDataFactory.createIntVector(ord, RDataFactory.COMPLETE_VECTOR);
    }

    // specialisations for vector and tie parameters

    @Specialization
    protected RIntVector order(RIntVector x, RStringVector tie) {
        controlVisibility();
        int[] t = order(tie, RMissing.instance).getDataWithoutCopying();
        int[] xs = x.getDataCopy();
        int[] ord = ordArray(xs.length);
        intSort(xs, ord, t, 0, xs.length - 1);
        fixTies(xs, ord, t);
        return RDataFactory.createIntVector(ord, RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization
    protected RIntVector order(RDoubleVector x, RStringVector tie) {
        controlVisibility();
        int[] t = order(tie, RMissing.instance).getDataWithoutCopying();
        double[] xs = x.getDataCopy();
        int[] ord = ordArray(xs.length);
        doubleSort(xs, ord, t, 0, xs.length - 1);
        fixTies(xs, ord, t);
        return RDataFactory.createIntVector(ord, RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization
    protected RIntVector order(RDoubleVector x, RDoubleVector tie) {
        controlVisibility();
        int[] t = order(tie, RMissing.instance).getDataWithoutCopying();
        double[] xs = x.getDataCopy();
        int[] ord = ordArray(xs.length);
        doubleSort(xs, ord, t, 0, xs.length - 1);
        fixTies(xs, ord, t);
        return RDataFactory.createIntVector(ord, RDataFactory.COMPLETE_VECTOR);
    }

    // sorting

    @SlowPath
    protected void stringSort(String[] x, int[] o, int[] t, int l, int r) {
        if (l < r) {
            int i = l;
            int j = r - 1;
            String p = x[r];
            do {
                while (le.op(x[i], p) == RRuntime.LOGICAL_TRUE && i < r) {
                    ++i;
                }
                while (ge.op(x[j], p) == RRuntime.LOGICAL_TRUE && j > l) {
                    --j;
                }
                if (i < j) {
                    String st = x[i];
                    x[i] = x[j];
                    x[j] = st;
                    swaps(o, t, i, j);
                }
            } while (i < j);
            if (gt.op(x[i], p) == RRuntime.LOGICAL_TRUE) {
                String st = x[i];
                x[i] = x[r];
                x[r] = st;
                swaps(o, t, i, r);
            }
            stringSort(x, o, t, l, i - 1);
            stringSort(x, o, t, i + 1, r);
        }
    }

    @SlowPath
    protected void doubleSort(double[] x, int[] o, int[] t, int l, int r) {
        if (l < r) {
            int i = l;
            int j = r - 1;
            double p = x[r];
            do {
                while (le.op(x[i], p) == RRuntime.LOGICAL_TRUE && i < r) {
                    ++i;
                }
                while (ge.op(x[j], p) == RRuntime.LOGICAL_TRUE && j > l) {
                    --j;
                }
                if (i < j) {
                    double st = x[i];
                    x[i] = x[j];
                    x[j] = st;
                    swaps(o, t, i, j);
                }
            } while (i < j);
            if (gt.op(x[i], p) == RRuntime.LOGICAL_TRUE) {
                double st = x[i];
                x[i] = x[r];
                x[r] = st;
                swaps(o, t, i, r);
            }
            doubleSort(x, o, t, l, i - 1);
            doubleSort(x, o, t, i + 1, r);
        }
    }

    @SlowPath
    protected void intSort(int[] x, int[] o, int[] t, int l, int r) {
        if (l < r) {
            int i = l;
            int j = r - 1;
            double p = x[r];
            do {
                while (le.op(x[i], p) == RRuntime.LOGICAL_TRUE && i < r) {
                    ++i;
                }
                while (ge.op(x[j], p) == RRuntime.LOGICAL_TRUE && j > l) {
                    --j;
                }
                if (i < j) {
                    int st = x[i];
                    x[i] = x[j];
                    x[j] = st;
                    swaps(o, t, i, j);
                }
            } while (i < j);
            if (gt.op(x[i], p) == RRuntime.LOGICAL_TRUE) {
                int st = x[i];
                x[i] = x[r];
                x[r] = st;
                swaps(o, t, i, r);
            }
            intSort(x, o, t, l, i - 1);
            intSort(x, o, t, i + 1, r);
        }
    }

    @SlowPath
    protected void complexSort(double[] x, int[] o, int[] t, int l, int r) {
        if (l < r) {
            int i = l;
            int j = r - 1;
            double pr = x[2 * r];
            double pi = x[2 * r + 1];
            do {
                while (complexLE(x[2 * i], x[2 * i + 1], pr, pi) && i < r) {
                    ++i;
                }
                while (complexGE(x[2 * j], x[2 * j + 1], pr, pi) && j > l) {
                    --j;
                }
                if (i < j) {
                    double str = x[2 * i];
                    double sti = x[2 * i + 1];
                    x[2 * i] = x[2 * j];
                    x[2 * i + 1] = x[2 * j + 1];
                    x[2 * j] = str;
                    x[2 * j + 1] = sti;
                    swaps(o, t, i, j);
                }
            } while (i < j);
            if (complexGT(x[2 * i], x[2 * i + 1], pr, pi)) {
                double str = x[2 * i];
                double sti = x[2 * i + 1];
                x[2 * i] = x[2 * r];
                x[2 * i + 1] = x[2 * r + 1];
                x[2 * r] = str;
                x[2 * r + 1] = sti;
                swaps(o, t, i, r);
            }
            complexSort(x, o, t, l, i - 1);
            complexSort(x, o, t, i + 1, r);
        }
    }

    private boolean complexLE(double xr, double xi, double yr, double yi) {
        if (eq.op(xr, yr) == RRuntime.LOGICAL_TRUE) {
            return le.op(xi, yi) == RRuntime.LOGICAL_TRUE;
        }
        return le.op(xr, yr) == RRuntime.LOGICAL_TRUE;
    }

    private boolean complexGE(double xr, double xi, double yr, double yi) {
        if (eq.op(xr, yr) == RRuntime.LOGICAL_TRUE) {
            return ge.op(xi, yi) == RRuntime.LOGICAL_TRUE;
        }
        return ge.op(xr, yr) == RRuntime.LOGICAL_TRUE;
    }

    private boolean complexGT(double xr, double xi, double yr, double yi) {
        if (eq.op(xr, yr) == RRuntime.LOGICAL_TRUE) {
            return gt.op(xi, yi) == RRuntime.LOGICAL_TRUE;
        }
        return gt.op(xr, yr) == RRuntime.LOGICAL_TRUE;
    }

    // applying tie decisions

    private void fixTies(double[] xs, int[] ord, int[] t) {
        int i = 0;
        while (i < ord.length - 1) {
            while (eq.op(xs[i], xs[i + 1]) == RRuntime.LOGICAL_TRUE && t[i] > t[i + 1]) {
                int j = i;
                while (j < ord.length - 1 && eq.op(xs[j], xs[j + 1]) == RRuntime.LOGICAL_TRUE) {
                    if (t[j] > t[j + 1]) {
                        double xt = xs[j];
                        xs[j] = xs[j + 1];
                        xs[j + 1] = xt;
                        swaps(ord, t, j, j + 1);
                    }
                    ++j;
                }
            }
            ++i;
        }
    }

    private void fixTies(int[] xs, int[] ord, int[] t) {
        int i = 0;
        while (i < ord.length - 1) {
            while (eq.op(xs[i], xs[i + 1]) == RRuntime.LOGICAL_TRUE && t[i] > t[i + 1]) {
                int j = i;
                while (j < ord.length - 1 && eq.op(xs[j], xs[j + 1]) == RRuntime.LOGICAL_TRUE) {
                    if (t[j] > t[j + 1]) {
                        int xt = xs[j];
                        xs[j] = xs[j + 1];
                        xs[j + 1] = xt;
                        swaps(ord, t, j, j + 1);
                    }
                    ++j;
                }
            }
            ++i;
        }
    }

    // helpers

    protected static int[] ordArray(int length) {
        int[] r = new int[length];
        for (int i = 0; i < length; ++i) {
            r[i] = i + 1;
        }
        return r;
    }

    private static void swaps(int[] a, int[] b, int i, int j) {
        int t;
        t = a[i];
        a[i] = a[j];
        a[j] = t;
        if (b != null) {
            t = b[i];
            b[i] = b[j];
            b[j] = t;
        }
    }

}
