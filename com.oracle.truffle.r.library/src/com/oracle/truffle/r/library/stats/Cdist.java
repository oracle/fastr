/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1995-2014, The R Core Team
 * Copyright (c) 2002-2008, The R Foundation
 * Copyright (c) 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.stats;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.instanceOf;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RAttributes;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

public abstract class Cdist extends RExternalBuiltinNode.Arg4 {
    private static final NACheck naCheck = NACheck.create();

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.arg(0).asDoubleVector();
        casts.arg(1).asIntegerVector().findFirst();
        casts.arg(2).mustBe(instanceOf(RList.class));
        casts.arg(3).asDoubleVector().findFirst();
    }

    @Specialization
    protected RDoubleVector cdist(RAbstractDoubleVector x, int method, RList list, double p) {
        int nr = RRuntime.nrows(x);
        int nc = RRuntime.ncols(x);
        int n = nr * (nr - 1) / 2; /* avoid int overflow for N ~ 50,000 */
        double[] ans = new double[n];
        RDoubleVector xm = x.materialize();
        rdistance(xm.getDataWithoutCopying(), nr, nc, ans, false, method, p);
        RDoubleVector result = RDataFactory.createDoubleVector(ans, naCheck.neverSeenNA());
        RAttributes resultAttrs = result.initAttributes();
        RStringVector names = (RStringVector) list.getAttr(RRuntime.NAMES_ATTR_KEY);
        for (int i = 0; i < names.getLength(); i++) {
            String name = names.getDataAt(i);
            Object listValue = list.getDataAt(i);
            if (name.equals(RRuntime.CLASS_ATTR_KEY)) {
                result.setClassAttr(listValue instanceof RStringVector ? (RStringVector) listValue : RDataFactory.createStringVectorFromScalar((String) listValue));
            } else {
                resultAttrs.put(name, listValue);
            }
        }

        return result;
    }

    private static boolean bothNonNAN(double a, double b) {
        return !RRuntime.isNAorNaN(a) && !RRuntime.isNAorNaN(b);
    }

    private static boolean bothFinite(double a, double b) {
        return RRuntime.isFinite(a) && RRuntime.isFinite(b);
    }

    private static void rdistance(double[] x, int nr, int nc, double[] d, boolean diag, int method, double p) {
        int ij; /* can exceed 2^31 - 1, but Java can't handle that */
        if (method < 1 || method > Method.values().length) {
            throw RError.error(RError.SHOW_CALLER2, RError.Message.GENERIC, "distance(): invalid distance");
        }
        //
        Method m = Method.values()[method - 1];
        if (m == Method.MINKOWSKI) {
            if (!RRuntime.isFinite(p) || p <= 0) {
                throw RError.error(RError.SHOW_CALLER2, RError.Message.GENERIC, "distance(): invalid p");
            }
        }
        int dc = diag ? 0 : 1; /* diag=1: we do the diagonal */
        ij = 0;
        naCheck.enable(true);
        for (int j = 0; j <= nr; j++) {
            for (int i = j + dc; i < nr; i++) {
                double r = m.dist(x, nr, nc, i, j, p);
                naCheck.check(r);
                d[ij++] = r;
            }
        }
    }

    // Checkstyle: stop parameter assignment check

    private enum Method {
        EUCLIDEAN {
            @Override
            public double dist(double[] x, int nr, int nc, int i1, int i2, double p) {
                double dev, dist;
                int count, j;

                count = 0;
                dist = 0;
                for (j = 0; j < nc; j++) {
                    if (bothNonNAN(x[i1], x[i2])) {
                        dev = (x[i1] - x[i2]);
                        if (!RRuntime.isNAorNaN(dev)) {
                            dist += dev * dev;
                            count++;
                        }
                    }
                    i1 += nr;
                    i2 += nr;
                }
                if (count == 0)
                    return RRuntime.DOUBLE_NA;
                if (count != nc)
                    dist /= ((double) count / nc);
                return Math.sqrt(dist);

            }

        },
        MAXIMUM {
            @Override
            public double dist(double[] x, int nr, int nc, int i1, int i2, double p) {
                double dev, dist;
                int count, j;

                count = 0;
                dist = -Double.MAX_VALUE;
                for (j = 0; j < nc; j++) {
                    if (bothNonNAN(x[i1], x[i2])) {
                        dev = Math.abs(x[i1] - x[i2]);
                        if (!RRuntime.isNAorNaN(dev)) {
                            if (dev > dist)
                                dist = dev;
                            count++;
                        }
                    }
                    i1 += nr;
                    i2 += nr;
                }
                if (count == 0)
                    return RRuntime.DOUBLE_NA;
                return dist;

            }

        },
        MANHATTAN {
            @Override
            public double dist(double[] x, int nr, int nc, int i1, int i2, double p) {
                double dev, dist;
                int count, j;

                count = 0;
                dist = 0;
                for (j = 0; j < nc; j++) {
                    if (bothNonNAN(x[i1], x[i2])) {
                        dev = Math.abs(x[i1] - x[i2]);
                        if (!RRuntime.isNAorNaN(dev)) {
                            dist += dev;
                            count++;
                        }
                    }
                    i1 += nr;
                    i2 += nr;
                }
                if (count == 0)
                    return RRuntime.DOUBLE_NA;
                if (count != nc)
                    dist /= ((double) count / nc);
                return dist;

            }

        },
        CANBERRA {
            @Override
            public double dist(double[] x, int nr, int nc, int i1, int i2, double p) {
                double dev, dist, sum, diff;
                int count, j;

                count = 0;
                dist = 0;
                for (j = 0; j < nc; j++) {
                    if (bothNonNAN(x[i1], x[i2])) {
                        sum = Math.abs(x[i1] + x[i2]);
                        diff = Math.abs(x[i1] - x[i2]);
                        if (sum > Double.MIN_VALUE || diff > Double.MIN_VALUE) {
                            dev = diff / sum;
                            if (!RRuntime.isNAorNaN(dev) ||
                                            (!RRuntime.isFinite(diff) && diff == sum &&
                                                            /* use Inf = lim x -> oo */ ((dev = 1.) != 0))) {
                                dist += dev;
                                count++;
                            }
                        }
                    }
                    i1 += nr;
                    i2 += nr;
                }
                if (count == 0)
                    return RRuntime.DOUBLE_NA;
                if (count != nc)
                    dist /= ((double) count / nc);
                return dist;

            }

        },
        BINARY {
            @Override
            public double dist(double[] x, int nr, int nc, int i1, int i2, double p) {
                int total, count, dist;
                int j;

                total = 0;
                count = 0;
                dist = 0;

                for (j = 0; j < nc; j++) {
                    if (bothNonNAN(x[i1], x[i2])) {
                        if (!bothFinite(x[i1], x[i2])) {
                            RError.warning(RError.SHOW_CALLER2, RError.Message.GENERIC, "treating non-finite values as NA");
                        } else {
                            if (x[i1] != 0. || x[i2] != 0.) {
                                count++;
                                if (!(x[i1] != 0. && x[i2] != 0.))
                                    dist++;
                            }
                            total++;
                        }
                    }
                    i1 += nr;
                    i2 += nr;
                }

                if (total == 0)
                    return RRuntime.DOUBLE_NA;
                if (count == 0)
                    return 0;
                return (double) dist / count;

            }

        },
        MINKOWSKI {
            @Override
            public double dist(double[] x, int nr, int nc, int i1, int i2, double p) {
                double dev, dist;
                int count, j;

                count = 0;
                dist = 0;
                for (j = 0; j < nc; j++) {
                    if (bothNonNAN(x[i1], x[i2])) {
                        dev = (x[i1] - x[i2]);
                        if (!RRuntime.isNAorNaN(dev)) {
                            dist += Math.pow(Math.abs(dev), p);
                            count++;
                        }
                    }
                    i1 += nr;
                    i2 += nr;
                }
                if (count == 0)
                    return RRuntime.DOUBLE_NA;
                if (count != nc)
                    dist /= ((double) count / nc);
                return Math.pow(dist, 1.0 / p);
            }

        };

        public abstract double dist(double[] x, int nr, int nc, int i1, int i2, double p);
    }

}
