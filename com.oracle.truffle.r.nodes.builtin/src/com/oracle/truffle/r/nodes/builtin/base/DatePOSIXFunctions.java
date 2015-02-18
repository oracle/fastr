/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2014, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

// from GnuR datatime.c

public class DatePOSIXFunctions {

    @RBuiltin(name = "Date2POSIXlt", kind = RBuiltinKind.INTERNAL, parameterNames = "x")
    public abstract static class Date2POSIXlt extends RBuiltinNode {

        private static final String[] LT_NAMES = new String[]{"sec", "min", "hour", "mday", "mon", "year", "wday", "yday", "isdst"};
        private static final RStringVector LT_NAMES_VEC = RDataFactory.createStringVector(LT_NAMES, RDataFactory.COMPLETE_VECTOR);
        private static final int[] DAYS_IN_MONTH = new int[]{31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        private static final RStringVector CLASS_ATTR = RDataFactory.createStringVector(new String[]{"POSIXlt", "POSIXt"}, RDataFactory.COMPLETE_VECTOR);

        @CreateCast("arguments")
        public RNode[] castArguments(RNode[] arguments) {
            arguments[0] = CastDoubleNodeGen.create(CastToVectorNodeGen.create(arguments[0], true, false, false, false), true, false, false);
            return arguments;
        }

        @Specialization
        protected RList doDate2POSIXlt(RDoubleVector x) {
            int xLen = x.getLength();
            Object[] data = new Object[LT_NAMES.length];
            for (int i = 0; i < data.length; i++) {
                data[i] = i == 0 ? new double[xLen] : new int[xLen];
            }
            boolean complete = RDataFactory.COMPLETE_VECTOR;
            for (int i = 0; i < xLen; i++) {
                double d = x.getDataAt(i);
                if (RRuntime.isFinite(d)) {
                    int day = (int) Math.floor(d);
                    int hour = 0;
                    int min = 0;
                    int sec = 0;
                    int wday;
                    /* weekday: 1970-01-01 was a Thursday */
                    if ((wday = ((4 + day) % 7)) < 0) {
                        wday += 7;
                    }

                    int tmp;
                    /* year & day within year */
                    int y = 1970;
                    if (day >= 0) {
                        for (; day >= (tmp = daysInYear(y)); day -= tmp, y++) {
                        }
                    } else {
                        for (; day < 0; --y, day += daysInYear(y)) {
                        }
                    }

                    int year = y - 1900;
                    y = year;
                    int yday = day;

                    /* month within year */
                    int mon;
                    for (mon = 0; day >= (tmp = (DAYS_IN_MONTH[mon]) + ((mon == 1 && isleap(y + 1900)) ? 1 : 0)); day -= tmp, mon++) {
                    }
                    int mday = day + 1;
                    int isdst = 0; /* no dst in GMT */

                    ((double[]) data[0])[i] = sec;
                    ((int[]) data[1])[i] = min;
                    ((int[]) data[2])[i] = hour;
                    ((int[]) data[3])[i] = mday;
                    ((int[]) data[4])[i] = mon;
                    ((int[]) data[5])[i] = year;
                    ((int[]) data[6])[i] = wday;
                    ((int[]) data[7])[i] = yday;
                    ((int[]) data[8])[i] = isdst;
                } else {
                    ((double[]) data[0])[i] = RRuntime.DOUBLE_NA;
                    for (int j = 1; j < 8; j++) {
                        ((int[]) data[j])[i] = RRuntime.INT_NA;
                    }
                    ((int[]) data[8])[i] = -1;
                    complete = false;
                }
            }
            for (int i = 0; i < data.length; i++) {
                if (i == 0) {
                    data[i] = RDataFactory.createDoubleVector((double[]) data[i], complete);
                } else {
                    data[i] = RDataFactory.createIntVector((int[]) data[i], i == 8 ? RDataFactory.COMPLETE_VECTOR : complete);
                }
            }
            RList result = RDataFactory.createList(data, LT_NAMES_VEC);
            result.setClassAttr(CLASS_ATTR);
            result.setAttr("tzone", "UTC");
            RStringVector xNames = x.getNames();
            if (xNames != null) {
                ((RIntVector) data[5]).copyNamesFrom(x);
            }
            return result;
        }

        private static boolean isleap(int y) {
            return (((y) % 4) == 0 && ((y) % 100) != 0) || ((y) % 400) == 0;
        }

        private static int daysInYear(int year) {
            return isleap(year) ? 366 : 365;
        }

    }
}
