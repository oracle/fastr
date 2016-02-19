/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1998-2013, The R Core Team
 * Copyright (c) 2003-2015, The R Foundation
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.stats;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

// inspired by arithmetic.c

public final class StatsFunctions {
    private StatsFunctions() {
        // private
    }

    public interface Function3_2 {
        double evaluate(double a, double b, double c, boolean x, boolean y);
    }

    public interface Function3_1 extends Function3_2 {
        default double evaluate(double a, double b, double c, boolean x, boolean y) {
            return evaluate(a, b, c, x);
        }

        double evaluate(double a, double b, double c, boolean x);
    }

    private static RAbstractDoubleVector evaluate3(Node node, Function3_2 function, RAbstractDoubleVector a, RAbstractDoubleVector b, RAbstractDoubleVector c, boolean x, boolean y,
                    BranchProfile nan, NACheck aCheck, NACheck bCheck, NACheck cCheck) {
        int aLength = a.getLength();
        int bLength = b.getLength();
        int cLength = c.getLength();
        if (aLength == 0 || bLength == 0 || cLength == 0) {
            return RDataFactory.createEmptyDoubleVector();
        }
        int length = Math.max(aLength, Math.max(bLength, cLength));
        RNode.reportWork(node, length);
        double[] result = new double[length];

        boolean complete = true;
        boolean nans = false;
        aCheck.enable(a);
        bCheck.enable(b);
        cCheck.enable(c);
        for (int i = 0; i < length; i++) {
            double aValue = a.getDataAt(i % aLength);
            double bValue = b.getDataAt(i % bLength);
            double cValue = c.getDataAt(i % cLength);
            double value;
            if (Double.isNaN(aValue) || Double.isNaN(bValue) || Double.isNaN(cValue)) {
                nan.enter();
                if (aCheck.check(aValue) || bCheck.check(bValue) || cCheck.check(cValue)) {
                    value = RRuntime.DOUBLE_NA;
                    complete = false;
                } else {
                    value = Double.NaN;
                }
            } else {
                value = function.evaluate(aValue, bValue, cValue, x, y);
                if (Double.isNaN(value)) {
                    nan.enter();
                    nans = true;
                }
            }
            result[i] = value;
        }
        if (nans) {
            RError.warning(RError.SHOW_CALLER, RError.Message.NAN_PRODUCED);
        }
        return RDataFactory.createDoubleVector(result, complete);
    }

    public abstract static class Function3_2Node extends RExternalBuiltinNode.Arg5 {
        private final Function3_2 function;

        public Function3_2Node(Function3_2 function) {
            this.function = function;
        }

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.toDouble(0).toDouble(1).toDouble(2).firstBoolean(3).firstBoolean(4);
        }

        @Specialization
        protected RAbstractDoubleVector evaluate(RAbstractDoubleVector a, RAbstractDoubleVector b, RAbstractDoubleVector c, boolean x, boolean y, //
                        @Cached("create()") BranchProfile nan, //
                        @Cached("create()") NACheck aCheck, //
                        @Cached("create()") NACheck bCheck, //
                        @Cached("create()") NACheck cCheck) {
            return evaluate3(this, function, a, b, c, x, y, nan, aCheck, bCheck, cCheck);
        }
    }

    public abstract static class Function3_1Node extends RExternalBuiltinNode.Arg4 {
        private final Function3_2 function;

        public Function3_1Node(Function3_1 function) {
            this.function = function;
        }

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.toDouble(0).toDouble(1).toDouble(2).firstBoolean(3);
        }

        @Specialization
        protected RAbstractDoubleVector evaluate(RAbstractDoubleVector a, RAbstractDoubleVector b, RAbstractDoubleVector c, boolean x, //
                        @Cached("create()") BranchProfile nan, //
                        @Cached("create()") NACheck aCheck, //
                        @Cached("create()") NACheck bCheck, //
                        @Cached("create()") NACheck cCheck) {
            return evaluate3(this, function, a, b, c, x, false /* dummy */, nan, aCheck, bCheck, cCheck);
        }
    }
}
