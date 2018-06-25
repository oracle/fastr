/*
 *  R : A Computer Language for Statistical Data Analysis
 *  Copyright (C) 1999--2014  The R Core Team.
 *  Copyright (c) 2018, 2018, Oracle and/or its affiliates
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
 *  https://www.R-project.org/Licenses/
 */
#include <rffiutils.h>
#include <R_ext/Applic.h>

void Rdqags(integr_fn f, void *ex, double *a, double *b,
	    double *epsabs, double *epsrel,
	    double *result, double *abserr, int *neval, int *ier,
	    int *limit, int *lenw, int *last, int *iwork, double *work) {
	unimplemented("Rdqags");
}


void Rdqagi(integr_fn f, void *ex, double *bound, int *inf,
	    double *epsabs, double *epsrel,
	    double *result, double *abserr, int *neval, int *ier,
	    int *limit, int *lenw, int *last,
	    int *iwork, double *work) {
	unimplemented("Rdqagi");
}

static double * vect(int n) {
    return (double *)R_alloc(n, sizeof(double));
}

static double ** Lmatrix(int n) {
    double **m = (double **) R_alloc(n, sizeof(double *));
    for (int i = 0; i < n; i++)
	    m[i] = (double *) R_alloc((i + 1), sizeof(double));
    return m;
}

#define stepredn	0.2
#define acctol		0.0001
#define reltest		10.0

void vmmin(int n0, double *b, double *Fmin, optimfn fminfn, optimgr fmingr,
      int maxit, int trace, int *mask,
      double abstol, double reltol, int nREPORT, void *ex,
      int *fncount, int *grcount, int *fail) {
    Rboolean accpoint, enough;
    double *g, *t, *X, *c, **B;
    int   count, funcount, gradcount;
    double f, gradproj;
    int   i, j, ilast, iter = 0;
    double s, steplength;
    double D1, D2;
    int  n, *l;

    if (maxit <= 0) {
        *fail = 0;
        *Fmin = fminfn(n0, b, ex);
        *fncount = *grcount = 0;
        return;
    }

    if (nREPORT <= 0)
	    error(_("REPORT must be > 0 (method = \"BFGS\")"));

    l = (int *) R_alloc(n0, sizeof(int));
    n = 0;
    for (i = 0; i < n0; i++) if (mask[i]) l[n++] = i;
    g = vect(n0);
    t = vect(n);
    X = vect(n);
    c = vect(n);
    B = Lmatrix(n);
    f = fminfn(n0, b, ex);
    if (!R_FINITE(f))
	    error(_("initial value in 'vmmin' is not finite"));
    if (trace) Rprintf("initial  value %f \n", f);
    *Fmin = f;
    funcount = gradcount = 1;
    fmingr(n0, b, g, ex);
    iter++;
    ilast = gradcount;

    do {
        if (ilast == gradcount) {
            for (i = 0; i < n; i++) {
            for (j = 0; j < i; j++)
                B[i][j] = 0.0;
                B[i][i] = 1.0;
            }
        }
        for (i = 0; i < n; i++) {
            X[i] = b[l[i]];
            c[i] = g[l[i]];
        }
        gradproj = 0.0;
        for (i = 0; i < n; i++) {
            s = 0.0;
            for (j = 0; j <= i; j++) s -= B[i][j] * g[l[j]];
            for (j = i + 1; j < n; j++) s -= B[j][i] * g[l[j]];
            t[i] = s;
            gradproj += s * g[l[i]];
        }

        if (gradproj < 0.0) {	/* search direction is downhill */
            steplength = 1.0;
            accpoint = FALSE;
            do {
                count = 0;
                for (i = 0; i < n; i++) {
                    b[l[i]] = X[i] + steplength * t[i];
                    if (reltest + X[i] == reltest + b[l[i]]) /* no change */
                    count++;
                }
                if (count < n) {
                    f = fminfn(n0, b, ex);
                    funcount++;
                    accpoint = R_FINITE(f) &&
                    (f <= *Fmin + gradproj * steplength * acctol);
                    if (!accpoint) {
                    steplength *= stepredn;
                    }
                }
            } while (!(count == n || accpoint));
            enough = (f > abstol) &&
            fabs(f - *Fmin) > reltol * (fabs(*Fmin) + reltol);
            /* stop if value if small or if relative change is low */
            if (!enough) {
                count = n;
                *Fmin = f;
            }
            if (count < n) {/* making progress */
                *Fmin = f;
                fmingr(n0, b, g, ex);
                gradcount++;
                iter++;
                D1 = 0.0;
                for (i = 0; i < n; i++) {
                    t[i] = steplength * t[i];
                    c[i] = g[l[i]] - c[i];
                    D1 += t[i] * c[i];
                }
                if (D1 > 0) {
                    D2 = 0.0;
                    for (i = 0; i < n; i++) {
                    s = 0.0;
                    for (j = 0; j <= i; j++)
                        s += B[i][j] * c[j];
                    for (j = i + 1; j < n; j++)
                        s += B[j][i] * c[j];
                    X[i] = s;
                    D2 += s * c[i];
                    }
                    D2 = 1.0 + D2 / D1;
                    for (i = 0; i < n; i++) {
                    for (j = 0; j <= i; j++)
                        B[i][j] += (D2 * t[i] * t[j]
                            - X[i] * t[j] - t[i] * X[j]) / D1;
                    }
                } else {	/* D1 < 0 */
                    ilast = gradcount;
                }
            } else {	/* no progress */
                if (ilast < gradcount) {
                    count = 0;
                    ilast = gradcount;
                }
            }
        } else {		/* uphill search */
            count = 0;
            if (ilast == gradcount) count = n;
            else ilast = gradcount;
            /* Resets unless has just been reset */
        }
        if (trace && (iter % nREPORT == 0))
            Rprintf("iter%4d value %f\n", iter, f);
        if (iter >= maxit) break;
        if (gradcount - ilast > 2 * n)
            ilast = gradcount;	/* periodic restart */
    } while (count != n || ilast != gradcount);
    if (trace) {
	    Rprintf("final  value %f \n", *Fmin);
        if (iter < maxit) Rprintf("converged\n");
        else Rprintf("stopped after %i iterations\n", iter);
    }
    *fail = (iter < maxit) ? 0 : 1;
    *fncount = funcount;
    *grcount = gradcount;
}

void
optif9(int nr, int n, double *x, fcn_p fcn, fcn_p d1fcn, d2fcn_p d2fcn,
       void *state, double *typsiz, double fscale, int method,
       int iexp, int *msg, int ndigit, int itnlim, int iagflg, int iahflg,
       double dlt, double gradtl, double stepmx, double steptl,
       double *xpls, double *fpls, double *gpls, int *itrmcd, double *a,
       double *wrk, int *itncnt) {
	unimplemented("optif9");
}
