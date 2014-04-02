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

import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.ops.*;

@RBuiltin("order")
public abstract class Order extends RBuiltinNode {

    private static final Object[] PARAMETER_NAMES = new Object[]{"x", "tie"};

    @Override
    public Object[] getParameterNames() {
        return PARAMETER_NAMES;
    }

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance)};
    }

    @Child protected BooleanOperation eq = BinaryCompare.EQUAL.create();
    @Child protected BooleanOperation lt = BinaryCompare.LESS_THAN.create();
    @Child protected BooleanOperation le = BinaryCompare.LESS_EQUAL.create();
    @Child protected BooleanOperation ge = BinaryCompare.GREATER_EQUAL.create();
    @Child protected BooleanOperation gt = BinaryCompare.GREATER_THAN.create();

    // specialisations for one parameter

    @Specialization(order = 10)
    public RIntVector order(RStringVector x, @SuppressWarnings("unused") RMissing tie) {
        controlVisibility();
        String[] xs = x.getDataCopy();
        int[] ord = ordArray(xs.length);
        stringSort(xs, ord, null, 0, xs.length - 1);
        return RDataFactory.createIntVector(ord, RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization(order = 20)
    public RIntVector order(RDoubleVector x, @SuppressWarnings("unused") RMissing tie) {
        controlVisibility();
        double[] xs = x.getDataCopy();
        int[] ord = ordArray(xs.length);
        doubleSort(xs, ord, null, 0, xs.length - 1);
        return RDataFactory.createIntVector(ord, RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization(order = 30)
    public RIntVector order(RIntVector x, @SuppressWarnings("unused") RMissing tie) {
        controlVisibility();
        int[] xs = x.getDataCopy();
        int[] ord = ordArray(xs.length);
        intSort(xs, ord, null, 0, xs.length - 1);
        return RDataFactory.createIntVector(ord, RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization(order = 31)
    public RIntVector order(RIntVector x, @SuppressWarnings("unused") RNull nul) {
        controlVisibility();
        return order(x, RMissing.instance);
    }

    // specialisations for vector and tie parameters

    @Specialization(order = 100)
    public RIntVector order(RIntVector x, RStringVector tie) {
        controlVisibility();
        int[] t = order(tie, RMissing.instance).getDataCopy();
        int[] xs = x.getDataCopy();
        int[] ord = ordArray(xs.length);
        intSort(xs, ord, t, 0, xs.length - 1);
        fixTies(xs, ord, t);
        return RDataFactory.createIntVector(ord, RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization(order = 110)
    public RIntVector order(RDoubleVector x, RStringVector tie) {
        controlVisibility();
        int[] t = order(tie, RMissing.instance).getDataCopy();
        double[] xs = x.getDataCopy();
        int[] ord = ordArray(xs.length);
        doubleSort(xs, ord, t, 0, xs.length - 1);
        fixTies(xs, ord, t);
        return RDataFactory.createIntVector(ord, RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization(order = 120)
    public RIntVector order(RDoubleVector x, RDoubleVector tie) {
        controlVisibility();
        int[] t = order(tie, RMissing.instance).getDataCopy();
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
