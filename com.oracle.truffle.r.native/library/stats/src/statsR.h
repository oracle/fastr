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

/* auxiliary */
static void unimplemented_stats() {
	error("unimplemented stats .External");
}

SEXP getListElement(SEXP list, char *str) { unimplemented_stats(); return NULL; }

/* Declarations for .Call entry points */

SEXP logit_link(SEXP mu) { unimplemented_stats(); return NULL; }
SEXP logit_linkinv(SEXP eta) { unimplemented_stats(); return NULL; }
SEXP logit_mu_eta(SEXP eta) { unimplemented_stats(); return NULL; }
SEXP binomial_dev_resids(SEXP y, SEXP mu, SEXP wt) { unimplemented_stats(); return NULL; }

SEXP cutree(SEXP merge, SEXP which) { unimplemented_stats(); return NULL; }
SEXP rWishart(SEXP ns, SEXP nuP, SEXP scal) { unimplemented_stats(); return NULL; }
SEXP Cdqrls(SEXP x, SEXP y, SEXP tol, SEXP chk) { unimplemented_stats(); return NULL; }
SEXP Cdist(SEXP x, SEXP method, SEXP attrs, SEXP p) { unimplemented_stats(); return NULL; }
SEXP r2dtable(SEXP n, SEXP r, SEXP c) { unimplemented_stats(); return NULL; }
SEXP cor(SEXP x, SEXP y, SEXP na_method, SEXP method) { unimplemented_stats(); return NULL; }
SEXP cov(SEXP x, SEXP y, SEXP na_method, SEXP method) { unimplemented_stats(); return NULL; }
SEXP updateform(SEXP old, SEXP new) { unimplemented_stats(); return NULL; }
SEXP fft(SEXP z, SEXP inverse) { unimplemented_stats(); return NULL; }
SEXP mvfft(SEXP z, SEXP inverse) { unimplemented_stats(); return NULL; }
SEXP nextn(SEXP n, SEXP factors) { unimplemented_stats(); return NULL; }

SEXP cfilter(SEXP sx, SEXP sfilter, SEXP ssides, SEXP scircular) { unimplemented_stats(); return NULL; }
SEXP rfilter(SEXP x, SEXP filter, SEXP out) { unimplemented_stats(); return NULL; }
SEXP lowess(SEXP x, SEXP y, SEXP sf, SEXP siter, SEXP sdelta) { unimplemented_stats(); return NULL; }
SEXP DoubleCentre(SEXP A) { unimplemented_stats(); return NULL; }
SEXP BinDist(SEXP x, SEXP weights, SEXP slo, SEXP sup, SEXP sn) { unimplemented_stats(); return NULL; }

/* Declarations for .External[2] entry points */

SEXP compcases(SEXP args) { unimplemented_stats(); return NULL; }
SEXP doD(SEXP args) { unimplemented_stats(); return NULL; }
SEXP deriv(SEXP args) { unimplemented_stats(); return NULL; }
SEXP modelframe(SEXP call, SEXP op, SEXP args, SEXP rho) { unimplemented_stats(); return NULL; }
SEXP modelmatrix(SEXP call, SEXP op, SEXP args, SEXP rho) { unimplemented_stats(); return NULL; }
SEXP termsform(SEXP args) { unimplemented_stats(); return NULL; }
SEXP do_fmin(SEXP call, SEXP op, SEXP args, SEXP rho) { unimplemented_stats(); return NULL; }
SEXP nlm(SEXP call, SEXP op, SEXP args, SEXP rho) { unimplemented_stats(); return NULL; }
SEXP zeroin2(SEXP call, SEXP op, SEXP args, SEXP rho) { unimplemented_stats(); return NULL; }
SEXP optim(SEXP call, SEXP op, SEXP args, SEXP rho) { unimplemented_stats(); return NULL; }
SEXP optimhess(SEXP call, SEXP op, SEXP args, SEXP rho) { unimplemented_stats(); return NULL; }
SEXP Rmultinom(SEXP args) { unimplemented_stats(); return NULL; }
SEXP call_dqagi(SEXP x) { unimplemented_stats(); return NULL; }
SEXP call_dqags(SEXP x) { unimplemented_stats(); return NULL; }
SEXP Random1(SEXP args) { unimplemented_stats(); return NULL; }
SEXP Random2(SEXP args) { unimplemented_stats(); ; return NULL; }
SEXP Random3(SEXP args) { unimplemented_stats(); return NULL; }
SEXP distn2(SEXP args) { unimplemented_stats(); return NULL; }
SEXP distn3(SEXP args) { unimplemented_stats(); return NULL; }
SEXP distn4(SEXP args) { unimplemented_stats(); return NULL; }

SEXP Rsm(SEXP x, SEXP stype, SEXP send) { unimplemented_stats(); return NULL; }
SEXP tukeyline(SEXP x, SEXP y, SEXP call) { unimplemented_stats(); return NULL; }
SEXP runmed(SEXP x, SEXP stype, SEXP sk, SEXP end, SEXP print_level) { unimplemented_stats(); return NULL; }
SEXP influence(SEXP mqr, SEXP do_coef, SEXP e, SEXP stol) { unimplemented_stats(); return NULL; }

SEXP pSmirnov2x(SEXP statistic, SEXP snx, SEXP sny) { unimplemented_stats(); return NULL; }
SEXP pKolmogorov2x(SEXP statistic, SEXP sn) { unimplemented_stats(); return NULL; }
SEXP pKS2(SEXP sn, SEXP stol) { unimplemented_stats(); return NULL; }

SEXP ksmooth(SEXP x, SEXP y, SEXP snp, SEXP skrn, SEXP sbw) { unimplemented_stats(); return NULL; }

SEXP SplineCoef(SEXP method, SEXP x, SEXP y) { unimplemented_stats(); return NULL; }
SEXP SplineEval(SEXP xout, SEXP z) { unimplemented_stats(); return NULL; }

SEXP ApproxTest(SEXP x, SEXP y, SEXP method, SEXP sf) { unimplemented_stats(); return NULL; }
SEXP Approx(SEXP x, SEXP y, SEXP v, SEXP method,
	    SEXP yleft, SEXP yright, SEXP sf) { unimplemented_stats(); return NULL; }

SEXP LogLin(SEXP dtab, SEXP conf, SEXP table, SEXP start,
	    SEXP snmar, SEXP eps, SEXP iter) { unimplemented_stats(); return NULL; }

SEXP pAnsari(SEXP q, SEXP sm, SEXP sn) { unimplemented_stats(); return NULL; }
SEXP qAnsari(SEXP p, SEXP sm, SEXP sn) { unimplemented_stats(); return NULL; }
SEXP pKendall(SEXP q, SEXP sn) { unimplemented_stats(); return NULL; }
SEXP pRho(SEXP q, SEXP sn, SEXP lower) { unimplemented_stats(); return NULL; }
SEXP SWilk(SEXP x) { unimplemented_stats(); return NULL; }

SEXP bw_den(SEXP nbin, SEXP sx) { unimplemented_stats(); return NULL; }
SEXP bw_ucv(SEXP sn, SEXP sd, SEXP cnt, SEXP sh) { unimplemented_stats(); return NULL; }
SEXP bw_bcv(SEXP sn, SEXP sd, SEXP cnt, SEXP sh) { unimplemented_stats(); return NULL; }
SEXP bw_phi4(SEXP sn, SEXP sd, SEXP cnt, SEXP sh) { unimplemented_stats(); return NULL; }
SEXP bw_phi6(SEXP sn, SEXP sd, SEXP cnt, SEXP sh) { unimplemented_stats(); return NULL; }

SEXP Fexact(SEXP x, SEXP pars, SEXP work, SEXP smult) { unimplemented_stats(); return NULL; }
SEXP Fisher_sim(SEXP sr, SEXP sc, SEXP sB) { unimplemented_stats(); return NULL; }
SEXP chisq_sim(SEXP sr, SEXP sc, SEXP sB, SEXP E) { unimplemented_stats(); return NULL; }
SEXP d2x2xk(SEXP sK, SEXP sm, SEXP sn, SEXP st, SEXP srn) { unimplemented_stats(); return NULL; }

SEXP stats_signrank_free(void) { unimplemented_stats(); return NULL; }
SEXP stats_wilcox_free(void) { unimplemented_stats(); return NULL; }
