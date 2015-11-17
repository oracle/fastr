/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
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

#include "rffiutils.h"
#include <stdlib.h>

// FastR: selected functions from arithmetic.c:

/*
 *  R : A Computer Language for Statistical Data Analysis
 *  Copyright (C) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 *  Copyright (C) 1998--2013	    The R Core Team.
 *  Copyright (C) 2003--2015	    The R Foundation
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, a copy is available at
 *  http://www.r-project.org/Licenses/
 */

typedef union
{
    double value;
    unsigned int word[2];
} ieee_double;

#ifdef WORDS_BIGENDIAN
static const int hw = 0;
static const int lw = 1;
#else  /* !WORDS_BIGENDIAN */
static const int hw = 1;
static const int lw = 0;
#endif /* WORDS_BIGENDIAN */


static double R_ValueOfNA(void)
{
    /* The gcc shipping with Fedora 9 gets this wrong without
     * the volatile declaration. Thanks to Marc Schwartz. */
    volatile ieee_double x;
    x.word[hw] = 0x7ff00000;
    x.word[lw] = 1954;
    return x.value;
}

int R_IsNA(double x)
{
    if (isnan(x)) {
	ieee_double y;
	y.value = x;
	return (y.word[lw] == 1954);
    }
    return 0;
}

int R_IsNaN(double x)
{
    if (isnan(x)) {
	ieee_double y;
	y.value = x;
	return (y.word[lw] != 1954);
    }
    return 0;
}

/* Mainly for use in packages */
int R_finite(double x)
{
#ifdef HAVE_WORKING_ISFINITE
    return isfinite(x);
#else
    return (!isnan(x) & (x != R_PosInf) & (x != R_NegInf));
#endif
}

#undef _
#include <nmath.h>

# define R_rint rint

double fround(double x, double digits) {
#define MAX_DIGITS DBL_MAX_10_EXP
    /* = 308 (IEEE); was till R 0.99: (DBL_DIG - 1) */
    /* Note that large digits make sense for very small numbers */
    LDOUBLE pow10, sgn, intx;
    int dig;

    if (ISNAN(x) || ISNAN(digits))
	return x + digits;
    if(!R_FINITE(x)) return x;

    if(digits == ML_POSINF) return x;
    else if(digits == ML_NEGINF) return 0.0;

    if (digits > MAX_DIGITS) digits = MAX_DIGITS;
    dig = (int)floor(digits + 0.5);
    if(x < 0.) {
	sgn = -1.;
	x = -x;
    } else
	sgn = 1.;
    if (dig == 0) {
	return (double)(sgn * R_rint(x));
    } else if (dig > 0) {
        pow10 = R_pow_di(10., dig);
	intx = floor(x);
	return (double)(sgn * (intx + R_rint((double)((x-intx) * pow10)) / pow10));
    } else {
        pow10 = R_pow_di(10., -dig);
        return (double)(sgn * R_rint((double)(x/pow10)) * pow10);
    }
}

double Rexp10(double x) {
	return pow(10.0, x);
}

static R_INLINE double R_POW(double x, double y) /* handle x ^ 2 inline */
{
    return y == 2.0 ? x * x : R_pow(x, y);
}

double R_pow_di(double x, int n)
{
    double xn = 1.0;

    if (ISNAN(x)) return x;
    if (n == NA_INTEGER) return NA_REAL;

    if (n != 0) {
	if (!R_FINITE(x)) return R_POW(x, (double)n);

	Rboolean is_neg = (n < 0);
	if(is_neg) n = -n;
	for(;;) {
	    if(n & 01) xn *= x;
	    if(n >>= 1) x *= x; else break;
	}
        if(is_neg) xn = 1. / xn;
    }
    return xn;
}

double R_pow(double x, double y) {
	unimplemented("R_pow");
    return 0;
}


