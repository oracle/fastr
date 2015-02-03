/*
 *  R : A Computer Language for Statistical Data Analysis
 *  Copyright (C) 2005   The R Core Team.
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

#ifndef PORT_PORT_H
#define PORT_PORT_H

/* Header file for the C utilities to accompany the Fortran
 * optimization routines for the port library.
 *
 * Copyright (C) 2005-5  the R Core Team
 * Licensed under the GNU General Public License, version 2 or later.
 */

#include <Rinternals.h>
#include <R_ext/RS.h>

#ifdef ENABLE_NLS
#include <libintl.h>
#define _(String) dgettext ("stats", String)
#else
#define _(String) (String)
#endif

/* PORT interface functions - reverse communication */

/* DRMNF(D, FX, IV, LIV, LV, N, V, X) */
void F77_NAME(drmnf)(double d[], double *fx,
			    int iv[], int *liv, int *lv, int *n, double v[], double x[]) {  }

/* DRMNG(D, FX, G, IV, LIV, LV, N, V, X) */
void F77_NAME(drmng)(double d[], double *fx, double g[],
		int iv[], int *liv, int *lv, int *n, double v[], double x[]) {  }

/* DRMNH(D, FX, G, H, IV, LH, LIV, LV, N, V, X) */
void F77_NAME(drmnh)(double d[], double *fx, double g[], double h[],
			    int iv[], int *lh, int *liv, int *lv, int *n, double v[], double x[]) {  }

/* DRMNFB(B, D, FX, IV, LIV, LV, N, V, X) */
void F77_NAME(drmnfb)(double b[], double d[], double *fx,
		int *liv, int *lv, int *n, double v[], double x[]) {  }

/* DRMNGB(B, D, FX, G, IV, LIV, LV, N, V, X) */
void F77_NAME(drmngb)(double b[], double d[], double *fx, double g[],
		int iv[], int *liv, int *lv, int *n, double v[], double x[]) {  }

/* DRMNH(B, D, FX, G, H, IV, LH, LIV, LV, N, V, X) */
void F77_NAME(drmnhb)(double b[], double d[], double *fx, double g[], double h[],
		int iv[], int *lh, int *liv, int *lv, int *n, double v[], double x[]) {  }

/* DRN2GB(B, D, DR, IV, LIV, LV, N, ND, N1, N2, P, R, RD, V, X) */
void F77_NAME(drn2gb)(double b[], double d[], double dr[],
			     int iv[], int *liv, int *lv, int *n, int *nd, int *n1, int *n2, int *p,
			     double r[], double rd[], double v[], double x[]) {  }

/* DRN2G(D, DR, IV, LIV, LV, N, ND, N1, N2, P, R, RD, V, X) */
void F77_NAME(drn2g)(double d[], double dr[],
		int iv[], int *liv, int *lv, int *n, int *nd, int *n1, int *n2, int *p,
		double r[], double rd[], double v[], double x[]) {  }

/* DRNSGB(A, ALF, B, C, DA, IN, IV, L, L1, LA, LIV, LV, N, NDA, P, V, Y) */
void F77_NAME(drnsgb)(double a[], double alf[], double b[], double c [], double da[],
			     int in[], int iv[], int *l, int *l1, int *la, int *liv,
			     int *lv, int *n, int *nda, int *p,
			     double v[], double y[]) {  }

/* DRNSG(A, ALF, C, DA, IN, IV, L, L1, LA, LIV, LV, N, NDA, P, V, Y) */
void F77_NAME(drnsg)(double a[], double alf[], double c[], double da[],
		int in[], int iv[], int *l, int *l1, int *la, int *liv,
		int *lv, int *n, int *nda, int *p,
		double v[], double y[]) {  }

SEXP port_ivset(SEXP kind, SEXP iv, SEXP v) { return NULL; }

SEXP port_nlminb(SEXP fn, SEXP gr, SEXP hs, SEXP rho,
		 SEXP lowerb, SEXP upperb, SEXP d, SEXP iv, SEXP v) { return NULL; }

SEXP port_nlsb(SEXP m, SEXP d, SEXP gg, SEXP iv, SEXP v,
	       SEXP lowerb, SEXP upperb) { return NULL; }

void Rf_divset(int alg, int iv[], int liv, int lv, double v[]) {  }

void
nlminb_iterate(double b[], double d[], double fx, double g[], double h[],
	       int iv[], int liv, int lv, int n, double v[], double x[]) {  }

void
nlsb_iterate(double b[], double d[], double dr[], int iv[], int liv,
	     int lv, int n, int nd, int p, double r[], double rd[],
	     double v[], double x[]) {  }

#endif
