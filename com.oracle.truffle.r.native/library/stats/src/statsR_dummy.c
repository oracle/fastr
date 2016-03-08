/*
 *  R : A Computer Language for Statistical Data Analysis
 *  Copyright (C) 2012   The R Core Team.
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

#include <Rinternals.h>

#define UNIMPLEMENTED { error("unimplemented function at %s:%d", __FILE__, __LINE__); return NULL; }

SEXP getListElement(SEXP list, char *str) UNIMPLEMENTED

/* Declarations for .Call entry points */

SEXP logit_link(SEXP mu) UNIMPLEMENTED
SEXP logit_linkinv(SEXP eta) UNIMPLEMENTED
SEXP logit_mu_eta(SEXP eta) UNIMPLEMENTED
SEXP binomial_dev_resids(SEXP y, SEXP mu, SEXP wt) UNIMPLEMENTED

SEXP cutree(SEXP merge, SEXP which) UNIMPLEMENTED
SEXP rWishart(SEXP ns, SEXP nuP, SEXP scal) UNIMPLEMENTED
SEXP Cdqrls(SEXP x, SEXP y, SEXP tol, SEXP chk) UNIMPLEMENTED
SEXP Cdist(SEXP x, SEXP method, SEXP attrs, SEXP p) UNIMPLEMENTED
SEXP r2dtable(SEXP n, SEXP r, SEXP c) UNIMPLEMENTED
SEXP cor(SEXP x, SEXP y, SEXP na_method, SEXP method) UNIMPLEMENTED
SEXP cov(SEXP x, SEXP y, SEXP na_method, SEXP method) UNIMPLEMENTED
SEXP updateform(SEXP old, SEXP new) UNIMPLEMENTED
SEXP fft(SEXP z, SEXP inverse) UNIMPLEMENTED
SEXP mvfft(SEXP z, SEXP inverse) UNIMPLEMENTED
SEXP nextn(SEXP n, SEXP factors) UNIMPLEMENTED

SEXP cfilter(SEXP sx, SEXP sfilter, SEXP ssides, SEXP scircular) UNIMPLEMENTED
SEXP rfilter(SEXP x, SEXP filter, SEXP out) UNIMPLEMENTED
SEXP lowess(SEXP x, SEXP y, SEXP sf, SEXP siter, SEXP sdelta) UNIMPLEMENTED
SEXP DoubleCentre(SEXP A) UNIMPLEMENTED
SEXP BinDist(SEXP x, SEXP weights, SEXP slo, SEXP sup, SEXP sn) UNIMPLEMENTED

/* Declarations for .External[2] entry points */

SEXP compcases(SEXP args) UNIMPLEMENTED
SEXP doD(SEXP args) UNIMPLEMENTED
SEXP deriv(SEXP args) UNIMPLEMENTED
SEXP modelframe(SEXP call, SEXP op, SEXP args, SEXP rho) UNIMPLEMENTED
SEXP modelmatrix(SEXP call, SEXP op, SEXP args, SEXP rho) UNIMPLEMENTED
SEXP termsform(SEXP args) UNIMPLEMENTED
SEXP do_fmin(SEXP call, SEXP op, SEXP args, SEXP rho) UNIMPLEMENTED
SEXP nlm(SEXP call, SEXP op, SEXP args, SEXP rho) UNIMPLEMENTED
SEXP zeroin2(SEXP call, SEXP op, SEXP args, SEXP rho) UNIMPLEMENTED
SEXP optim(SEXP call, SEXP op, SEXP args, SEXP rho) UNIMPLEMENTED
SEXP optimhess(SEXP call, SEXP op, SEXP args, SEXP rho) UNIMPLEMENTED
SEXP Rmultinom(SEXP args) UNIMPLEMENTED
SEXP call_dqagi(SEXP x) UNIMPLEMENTED
SEXP call_dqags(SEXP x) UNIMPLEMENTED
SEXP Random1(SEXP args) UNIMPLEMENTED
SEXP Random2(SEXP args) UNIMPLEMENTED
SEXP Random3(SEXP args) UNIMPLEMENTED
SEXP distn2(SEXP args) UNIMPLEMENTED
SEXP distn3(SEXP args) UNIMPLEMENTED
SEXP distn4(SEXP args) UNIMPLEMENTED

SEXP Rsm(SEXP x, SEXP stype, SEXP send) UNIMPLEMENTED
SEXP tukeyline(SEXP x, SEXP y, SEXP call) UNIMPLEMENTED
SEXP runmed(SEXP x, SEXP stype, SEXP sk, SEXP end, SEXP print_level) UNIMPLEMENTED
SEXP influence(SEXP mqr, SEXP do_coef, SEXP e, SEXP stol) UNIMPLEMENTED

SEXP pSmirnov2x(SEXP statistic, SEXP snx, SEXP sny) UNIMPLEMENTED
SEXP pKolmogorov2x(SEXP statistic, SEXP sn) UNIMPLEMENTED
SEXP pKS2(SEXP sn, SEXP stol) UNIMPLEMENTED

SEXP ksmooth(SEXP x, SEXP y, SEXP snp, SEXP skrn, SEXP sbw) UNIMPLEMENTED

SEXP SplineCoef(SEXP method, SEXP x, SEXP y) UNIMPLEMENTED
SEXP SplineEval(SEXP xout, SEXP z) UNIMPLEMENTED

SEXP ApproxTest(SEXP x, SEXP y, SEXP method, SEXP sf) UNIMPLEMENTED
SEXP Approx(SEXP x, SEXP y, SEXP v, SEXP method,
	    SEXP yleft, SEXP yright, SEXP sf) UNIMPLEMENTED

SEXP LogLin(SEXP dtab, SEXP conf, SEXP table, SEXP start,
	    SEXP snmar, SEXP eps, SEXP iter) UNIMPLEMENTED

SEXP pAnsari(SEXP q, SEXP sm, SEXP sn) UNIMPLEMENTED
SEXP qAnsari(SEXP p, SEXP sm, SEXP sn) UNIMPLEMENTED
SEXP pKendall(SEXP q, SEXP sn) UNIMPLEMENTED
SEXP pRho(SEXP q, SEXP sn, SEXP lower) UNIMPLEMENTED
SEXP SWilk(SEXP x) UNIMPLEMENTED

SEXP bw_den(SEXP nbin, SEXP sx) UNIMPLEMENTED
SEXP bw_ucv(SEXP sn, SEXP sd, SEXP cnt, SEXP sh) UNIMPLEMENTED
SEXP bw_bcv(SEXP sn, SEXP sd, SEXP cnt, SEXP sh) UNIMPLEMENTED
SEXP bw_phi4(SEXP sn, SEXP sd, SEXP cnt, SEXP sh) UNIMPLEMENTED
SEXP bw_phi6(SEXP sn, SEXP sd, SEXP cnt, SEXP sh) UNIMPLEMENTED

SEXP Fexact(SEXP x, SEXP pars, SEXP work, SEXP smult) UNIMPLEMENTED
SEXP Fisher_sim(SEXP sr, SEXP sc, SEXP sB) UNIMPLEMENTED
SEXP chisq_sim(SEXP sr, SEXP sc, SEXP sB, SEXP E) UNIMPLEMENTED
SEXP d2x2xk(SEXP sK, SEXP sm, SEXP sn, SEXP st, SEXP srn) UNIMPLEMENTED

SEXP stats_signrank_free(void) UNIMPLEMENTED
SEXP stats_wilcox_free(void) UNIMPLEMENTED
