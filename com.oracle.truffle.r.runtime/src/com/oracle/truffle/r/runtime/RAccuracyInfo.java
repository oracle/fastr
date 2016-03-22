/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.runtime;

// Transcribed from GnuR src/main/platform.c

public class RAccuracyInfo {
    public final int ibeta;
    public final int it;
    public final int irnd;
    public final int ngrd;
    public final int machep;
    public final int negep;
    public final int iexp;
    public final int minexp;
    public final int maxexp;
    public final double eps;
    public final double epsneg;
    public final double xmin;
    public final double xmax;

    @SuppressFBWarnings(value = "FE_FLOATING_POINT_EQUALITY", justification = "the use of direct comparison is intended here")
    RAccuracyInfo() {
        int ibetaTemp;
        int itTemp;
        int irndTemp;
        int ngrdTemp;
        int machepTemp;
        int negepTemp;
        int iexpTemp;
        int minexpTemp;
        int maxexpTemp;
        double epsTemp;
        double epsnegTemp;
        double xminTemp;
        double xmaxTemp;

        double a;
        double b;
        double beta;
        double betain;
        double betah;
        double one;
        double t;
        double temp;
        double tempa;
        double temp1;
        double two;
        double y;
        double z;
        double zero;
        int i;
        int itemp;
        int iz;
        int j;
        int k;
        int mx;
        int nxres;

        one = 1;
        two = one + one;
        zero = one - one;

        /* determine ibeta, beta ala malcolm. */

        a = one;
        do {
            a = a + a;
            temp = a + one;
            temp1 = temp - a;
        } while (temp1 - one == zero);

        b = one;
        do {
            b = b + b;
            temp = a + b;
            itemp = (int) (temp - a);
        } while (itemp == 0);
        ibetaTemp = itemp;
        beta = ibetaTemp;

        /* determine it, irnd */

        itTemp = 0;
        b = one;
        do {
            itTemp = itTemp + 1;
            b = b * beta;
            temp = b + one;
            temp1 = temp - b;
        } while (temp1 - one == zero);

        irndTemp = 0;
        betah = beta / two;
        temp = a + betah;
        if (temp - a != zero) {
            irndTemp = 1;
        }
        tempa = a + beta;
        temp = tempa + betah;
        if (irndTemp == 0 && temp - tempa != zero) {
            irndTemp = 2;
        }

        /* determine negep, epsneg */

        negepTemp = itTemp + 3;
        betain = one / beta;
        a = one;
        for (i = 1; i <= negepTemp; i++) {
            a = a * betain;
        }
        b = a;
        for (;;) {
            temp = one - a;
            if (temp - one != zero) {
                break;
            }
            a = a * beta;
            negepTemp = negepTemp - 1;
        }
        negepTemp = -negepTemp;
        epsnegTemp = a;
        if (ibetaTemp != 2 && irndTemp != 0) {
            a = (a * (one + a)) / two;
            temp = one - a;
            if (temp - one != zero) {
                epsnegTemp = a;
            }
        }

        /* determine machep, eps */

        machepTemp = -itTemp - 3;
        a = b;
        for (;;) {
            temp = one + a;
            if (temp - one != zero) {
                break;
            }
            a = a * beta;
            machepTemp = machepTemp + 1;
        }
        epsTemp = a;
        temp = tempa + beta * (one + epsTemp);
        if (ibetaTemp != 2 && irndTemp != 0) {
            a = (a * (one + a)) / two;
            temp = one + a;
            if (temp - one != zero) {
                epsTemp = a;
            }
        }

        /* determine ngrd */

        ngrdTemp = 0;
        temp = one + epsTemp;
        if (irndTemp == 0 && temp * one - one != zero) {
            ngrdTemp = 1;
        }

        /* determine iexp, minexp, xmin */

        /* loop to determine largest i and k = 2**i such that */
        /* (1/beta) ** (2**(i)) */
        /* does not underflow. */
        /* exit from loop is signaled by an underflow. */

        i = 0;
        k = 1;
        z = betain;
        t = one + epsTemp;
        nxres = 0;
        for (;;) {
            y = z;
            z = y * y;

            /* check for underflow here */

            a = z * one;
            temp = z * t;
            if (a + a == zero || Math.abs(z) >= y) {
                break;
            }
            temp1 = temp * betain;
            if (temp1 * beta == z) {
                break;
            }
            i = i + 1;
            k = k + k;
        }
        if (ibetaTemp != 10) {
            iexpTemp = i + 1;
            mx = k + k;
        } else {
            /* this segment is for decimal machines only */

            iexpTemp = 2;
            iz = ibetaTemp;
            while (k >= iz) {
                iz = iz * ibetaTemp;
                iexpTemp = iexpTemp + 1;
            }
            mx = iz + iz - 1;
        }
        boolean broke = false;
        do {
            /* loop to determine minexp, xmin */
            /* exit from loop is signaled by an underflow */

            xminTemp = y;
            y = y * betain;

            /* check for underflow here */

            a = y * one;
            temp = y * t;
            if (a + a == zero || Math.abs(y) >= xminTemp) {
                broke = true;
                break;
            }
            k = k + 1;
            temp1 = temp * betain;
        } while (temp1 * beta != y);

        if (!broke) {
            nxres = 3;
            xminTemp = y;
        }

        minexpTemp = -k;

        /* determine maxexp, xmax */

        if (mx <= k + k - 3 && ibetaTemp != 10) {
            mx = mx + mx;
            iexpTemp = iexpTemp + 1;
        }
        maxexpTemp = mx + minexpTemp;

        /* adjust irnd to reflect partial underflow */

        irndTemp = irndTemp + nxres;

        /* adjust for ieee-style machines */

        if (irndTemp == 2 || irndTemp == 5) {
            maxexpTemp = maxexpTemp - 2;
        }

        /* adjust for non-ieee machines with partial underflow */

        if (irndTemp == 3 || irndTemp == 4) {
            maxexpTemp = maxexpTemp - itTemp;
        }

        /* adjust for machines with implicit leading bit in binary */
        /* significand, and machines with radix point at extreme */
        /* right of significand. */

        i = maxexpTemp + minexpTemp;
        if (ibetaTemp == 2 && i == 0) {
            maxexpTemp = maxexpTemp - 1;
        }
        if (i > 20) {
            maxexpTemp = maxexpTemp - 1;
        }
        if (a != y) {
            maxexpTemp = maxexpTemp - 2;
        }
        xmaxTemp = one - epsnegTemp;
        if (xmaxTemp * one != xmaxTemp) {
            xmaxTemp = one - beta * epsnegTemp;
        }
        xmaxTemp = xmaxTemp / (beta * beta * beta * xminTemp);
        i = maxexpTemp + minexpTemp + 3;
        if (i > 0) {
            for (j = 1; j <= i; j++) {
                if (ibetaTemp == 2) {
                    xmaxTemp = xmaxTemp + xmaxTemp;
                }
                if (ibetaTemp != 2) {
                    xmaxTemp = xmaxTemp * beta;
                }
            }
        }

        this.ibeta = ibetaTemp;
        this.it = itTemp;
        this.irnd = irndTemp;
        this.ngrd = ngrdTemp;
        this.machep = machepTemp;
        this.negep = negepTemp;
        this.iexp = iexpTemp;
        this.minexp = minexpTemp;
        this.maxexp = maxexpTemp;
        this.eps = epsTemp;
        this.epsneg = epsnegTemp;
        this.xmin = xminTemp;
        this.xmax = xmaxTemp;

    }

    private static RAccuracyInfo accuracy;

    public static void initialize() {
        if (accuracy == null) {
            accuracy = new RAccuracyInfo();
        }
    }

    public static RAccuracyInfo get() {
        return accuracy;
    }

    public static void main(String[] args) {
        RAccuracyInfo accuracyInfo = new RAccuracyInfo();
        // Checkstyle: stop print method check
        System.out.printf("ibeta %d\n", accuracyInfo.ibeta);
        System.out.printf("it %d\n", accuracyInfo.it);
        System.out.printf("irnd %d\n", accuracyInfo.irnd);
        System.out.printf("ngrd %d\n", accuracyInfo.ngrd);
        System.out.printf("machep %d\n", accuracyInfo.machep);
        System.out.printf("negep %d\n", accuracyInfo.negep);
        System.out.printf("iexp %d\n", accuracyInfo.iexp);
        System.out.printf("minexp %d\n", accuracyInfo.minexp);
        System.out.printf("maxexp %d\n", accuracyInfo.maxexp);
        System.out.printf("negep %d\n", accuracyInfo.negep);
        System.out.printf("eps %e\n", accuracyInfo.eps);
        System.out.printf("epsneg %e\n", accuracyInfo.epsneg);
        System.out.printf("xmin %e\n", accuracyInfo.xmin);
        System.out.printf("xmax %e\n", accuracyInfo.xmax);

    }
}
