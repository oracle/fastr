/*
 *  R : A Computer Language for Statistical Data Analysis
 *  Copyright (C) 2012   The R Core Team.
 *
 *  This program is free software UNIMPLEMENTED you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation UNIMPLEMENTED either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY UNIMPLEMENTED without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program UNIMPLEMENTED if not, a copy is available at
 *  http://www.r-project.org/Licenses/
 */

#include <Rinternals.h>

#define UNIMPLEMENTED { error("unimplemented function at %s:%d", __FILE__, __LINE__); return NULL; }
#define RINTERNAL_CODE { error("function shoul be implemented in R code at %s:%d", __FILE__, __LINE__); return NULL; }

/* auxiliary */

/* Declarations for .Call entry points */


SEXP cutree(SEXP merge, SEXP which) UNIMPLEMENTED
SEXP Cdqrls(SEXP x, SEXP y, SEXP tol, SEXP chk) UNIMPLEMENTED
SEXP Cdist(SEXP x, SEXP method, SEXP attrs, SEXP p) UNIMPLEMENTED
SEXP r2dtable(SEXP n, SEXP r, SEXP c) UNIMPLEMENTED
SEXP cor(SEXP x, SEXP y, SEXP na_method, SEXP method) UNIMPLEMENTED
SEXP cov(SEXP x, SEXP y, SEXP na_method, SEXP method) UNIMPLEMENTED
SEXP updateform(SEXP old, SEXP new) UNIMPLEMENTED
SEXP fft(SEXP z, SEXP inverse) UNIMPLEMENTED
SEXP mvfft(SEXP z, SEXP inverse) UNIMPLEMENTED
SEXP nextn(SEXP n, SEXP factors) UNIMPLEMENTED

SEXP DoubleCentre(SEXP A) UNIMPLEMENTED
SEXP BinDist(SEXP x, SEXP weights, SEXP slo, SEXP sup, SEXP sn) UNIMPLEMENTED

SEXP do_dchisq(SEXP sa, SEXP sb, SEXP sI) UNIMPLEMENTED
SEXP do_dexp(SEXP sa, SEXP sb, SEXP sI) UNIMPLEMENTED
SEXP do_dgeom(SEXP sa, SEXP sb, SEXP sI) UNIMPLEMENTED
SEXP do_dpois(SEXP sa, SEXP sb, SEXP sI) UNIMPLEMENTED
SEXP do_dt(SEXP sa, SEXP sb, SEXP sI) UNIMPLEMENTED
SEXP do_dsignrank(SEXP sa, SEXP sb, SEXP sI) UNIMPLEMENTED
SEXP do_pchisq(SEXP sa, SEXP sb, SEXP sI, SEXP sJ) UNIMPLEMENTED
SEXP do_qchisq(SEXP sa, SEXP sb, SEXP sI, SEXP sJ) UNIMPLEMENTED
SEXP do_pexp(SEXP sa, SEXP sb, SEXP sI, SEXP sJ) UNIMPLEMENTED
SEXP do_qexp(SEXP sa, SEXP sb, SEXP sI, SEXP sJ) UNIMPLEMENTED
SEXP do_pgeom(SEXP sa, SEXP sb, SEXP sI, SEXP sJ) UNIMPLEMENTED
SEXP do_qgeom(SEXP sa, SEXP sb, SEXP sI, SEXP sJ) UNIMPLEMENTED
SEXP do_ppois(SEXP sa, SEXP sb, SEXP sI, SEXP sJ) UNIMPLEMENTED
SEXP do_qpois(SEXP sa, SEXP sb, SEXP sI, SEXP sJ) UNIMPLEMENTED
SEXP do_pt(SEXP sa, SEXP sb, SEXP sI, SEXP sJ) UNIMPLEMENTED
SEXP do_qt(SEXP sa, SEXP sb, SEXP sI, SEXP sJ) UNIMPLEMENTED
SEXP do_psignrank(SEXP sa, SEXP sb, SEXP sI, SEXP sJ) UNIMPLEMENTED
SEXP do_qsignrank(SEXP sa, SEXP sb, SEXP sI, SEXP sJ) UNIMPLEMENTED

SEXP do_dbeta(SEXP sa, SEXP sb, SEXP sc, SEXP sI) UNIMPLEMENTED
SEXP do_dbinom(SEXP sa, SEXP sb, SEXP sc, SEXP sI) UNIMPLEMENTED
SEXP do_dcauchy(SEXP sa, SEXP sb, SEXP sc, SEXP sI) UNIMPLEMENTED
SEXP do_df(SEXP sa, SEXP sb, SEXP sc, SEXP sI) UNIMPLEMENTED
SEXP do_dgamma(SEXP sa, SEXP sb, SEXP sc, SEXP sI) UNIMPLEMENTED
SEXP do_dlnorm(SEXP sa, SEXP sb, SEXP sc, SEXP sI) UNIMPLEMENTED
SEXP do_dlogis(SEXP sa, SEXP sb, SEXP sc, SEXP sI) UNIMPLEMENTED
SEXP do_dnbinom(SEXP sa, SEXP sb, SEXP sc, SEXP sI) UNIMPLEMENTED
SEXP do_dnbinom_mu(SEXP sa, SEXP sb, SEXP sc, SEXP sI) UNIMPLEMENTED
SEXP do_dnorm(SEXP sa, SEXP sb, SEXP sc, SEXP sI) UNIMPLEMENTED
SEXP do_dweibull(SEXP sa, SEXP sb, SEXP sc, SEXP sI) UNIMPLEMENTED
SEXP do_dunif(SEXP sa, SEXP sb, SEXP sc, SEXP sI) UNIMPLEMENTED
SEXP do_dnt(SEXP sa, SEXP sb, SEXP sc, SEXP sI) UNIMPLEMENTED
SEXP do_dnchisq(SEXP sa, SEXP sb, SEXP sc, SEXP sI) UNIMPLEMENTED
SEXP do_dwilcox(SEXP sa, SEXP sb, SEXP sc, SEXP sI) UNIMPLEMENTED
SEXP do_pbeta(SEXP sa, SEXP sb, SEXP sc, SEXP sI, SEXP sJ) UNIMPLEMENTED
SEXP do_qbeta(SEXP sa, SEXP sb, SEXP sc, SEXP sI, SEXP sJ) UNIMPLEMENTED
SEXP do_pbinom(SEXP sa, SEXP sb, SEXP sc, SEXP sI, SEXP sJ) UNIMPLEMENTED
SEXP do_qbinom(SEXP sa, SEXP sb, SEXP sc, SEXP sI, SEXP sJ) UNIMPLEMENTED
SEXP do_pcauchy(SEXP sa, SEXP sb, SEXP sc, SEXP sI, SEXP sJ) UNIMPLEMENTED
SEXP do_qcauchy(SEXP sa, SEXP sb, SEXP sc, SEXP sI, SEXP sJ) UNIMPLEMENTED
SEXP do_pf(SEXP sa, SEXP sb, SEXP sc, SEXP sI, SEXP sJ) UNIMPLEMENTED
SEXP do_qf(SEXP sa, SEXP sb, SEXP sc, SEXP sI, SEXP sJ) UNIMPLEMENTED
SEXP do_pgamma(SEXP sa, SEXP sb, SEXP sc, SEXP sI, SEXP sJ) UNIMPLEMENTED
SEXP do_qgamma(SEXP sa, SEXP sb, SEXP sc, SEXP sI, SEXP sJ) UNIMPLEMENTED
SEXP do_plnorm(SEXP sa, SEXP sb, SEXP sc, SEXP sI, SEXP sJ) UNIMPLEMENTED
SEXP do_qlnorm(SEXP sa, SEXP sb, SEXP sc, SEXP sI, SEXP sJ) UNIMPLEMENTED
SEXP do_plogis(SEXP sa, SEXP sb, SEXP sc, SEXP sI, SEXP sJ) UNIMPLEMENTED
SEXP do_qlogis(SEXP sa, SEXP sb, SEXP sc, SEXP sI, SEXP sJ) UNIMPLEMENTED
SEXP do_pnbinom(SEXP sa, SEXP sb, SEXP sc, SEXP sI, SEXP sJ) UNIMPLEMENTED
SEXP do_qnbinom(SEXP sa, SEXP sb, SEXP sc, SEXP sI, SEXP sJ) UNIMPLEMENTED
SEXP do_pnbinom_mu(SEXP sa, SEXP sb, SEXP sc, SEXP sI, SEXP sJ) UNIMPLEMENTED
SEXP do_qnbinom_mu(SEXP sa, SEXP sb, SEXP sc, SEXP sI, SEXP sJ) UNIMPLEMENTED
SEXP do_pnorm(SEXP sa, SEXP sb, SEXP sc, SEXP sI, SEXP sJ) UNIMPLEMENTED
SEXP do_qnorm(SEXP sa, SEXP sb, SEXP sc, SEXP sI, SEXP sJ) UNIMPLEMENTED
SEXP do_pweibull(SEXP sa, SEXP sb, SEXP sc, SEXP sI, SEXP sJ) UNIMPLEMENTED
SEXP do_qweibull(SEXP sa, SEXP sb, SEXP sc, SEXP sI, SEXP sJ) UNIMPLEMENTED
SEXP do_punif(SEXP sa, SEXP sb, SEXP sc, SEXP sI, SEXP sJ) UNIMPLEMENTED
SEXP do_qunif(SEXP sa, SEXP sb, SEXP sc, SEXP sI, SEXP sJ) UNIMPLEMENTED
SEXP do_pnt(SEXP sa, SEXP sb, SEXP sc, SEXP sI, SEXP sJ) UNIMPLEMENTED
SEXP do_qnt(SEXP sa, SEXP sb, SEXP sc, SEXP sI, SEXP sJ) UNIMPLEMENTED
SEXP do_pnchisq(SEXP sa, SEXP sb, SEXP sc, SEXP sI, SEXP sJ) UNIMPLEMENTED
SEXP do_qnchisq(SEXP sa, SEXP sb, SEXP sc, SEXP sI, SEXP sJ) UNIMPLEMENTED
SEXP do_pwilcox(SEXP sa, SEXP sb, SEXP sc, SEXP sI, SEXP sJ) UNIMPLEMENTED
SEXP do_qwilcox(SEXP sa, SEXP sb, SEXP sc, SEXP sI, SEXP sJ) UNIMPLEMENTED

SEXP do_dhyper(SEXP sa, SEXP sb, SEXP sc, SEXP sd, SEXP sI) UNIMPLEMENTED
SEXP do_dnbeta(SEXP sa, SEXP sb, SEXP sc, SEXP sd, SEXP sI) UNIMPLEMENTED
SEXP do_dnf(SEXP sa, SEXP sb, SEXP sc, SEXP sd, SEXP sI) UNIMPLEMENTED
SEXP do_phyper(SEXP sa, SEXP sb, SEXP sc, SEXP sd, SEXP sI, SEXP sJ) UNIMPLEMENTED
SEXP do_qhyper(SEXP sa, SEXP sb, SEXP sc, SEXP sd, SEXP sI, SEXP sJ) UNIMPLEMENTED
SEXP do_pnbeta(SEXP sa, SEXP sb, SEXP sc, SEXP sd, SEXP sI, SEXP sJ) UNIMPLEMENTED
SEXP do_qnbeta(SEXP sa, SEXP sb, SEXP sc, SEXP sd, SEXP sI, SEXP sJ) UNIMPLEMENTED
SEXP do_pnf(SEXP sa, SEXP sb, SEXP sc, SEXP sd, SEXP sI, SEXP sJ) UNIMPLEMENTED
SEXP do_qnf(SEXP sa, SEXP sb, SEXP sc, SEXP sd, SEXP sI, SEXP sJ) UNIMPLEMENTED
SEXP do_ptukey(SEXP sa, SEXP sb, SEXP sc, SEXP sd, SEXP sI, SEXP sJ) UNIMPLEMENTED
SEXP do_qtukey(SEXP sa, SEXP sb, SEXP sc, SEXP sd, SEXP sI, SEXP sJ) UNIMPLEMENTED

SEXP do_rchisq(SEXP sn, SEXP sa) UNIMPLEMENTED
SEXP do_rexp(SEXP sn, SEXP sa) UNIMPLEMENTED
SEXP do_rgeom(SEXP sn, SEXP sa) UNIMPLEMENTED
SEXP do_rpois(SEXP sn, SEXP sa) UNIMPLEMENTED
SEXP do_rt(SEXP sn, SEXP sa) UNIMPLEMENTED
SEXP do_rsignrank(SEXP sn, SEXP sa) UNIMPLEMENTED

SEXP do_rbeta(SEXP sn, SEXP sa, SEXP sb) UNIMPLEMENTED
SEXP do_rbinom(SEXP sn, SEXP sa, SEXP sb) UNIMPLEMENTED
SEXP do_rcauchy(SEXP sn, SEXP sa, SEXP sb) UNIMPLEMENTED
SEXP do_rf(SEXP sn, SEXP sa, SEXP sb) UNIMPLEMENTED
SEXP do_rgamma(SEXP sn, SEXP sa, SEXP sb) UNIMPLEMENTED
SEXP do_rlnorm(SEXP sn, SEXP sa, SEXP sb) UNIMPLEMENTED
SEXP do_rlogis(SEXP sn, SEXP sa, SEXP sb) UNIMPLEMENTED
SEXP do_rnbinom(SEXP sn, SEXP sa, SEXP sb) UNIMPLEMENTED
SEXP do_rnorm(SEXP sn, SEXP sa, SEXP sb) UNIMPLEMENTED
SEXP do_runif(SEXP sn, SEXP sa, SEXP sb) UNIMPLEMENTED
SEXP do_rweibull(SEXP sn, SEXP sa, SEXP sb) UNIMPLEMENTED
SEXP do_rwilcox(SEXP sn, SEXP sa, SEXP sb) UNIMPLEMENTED
SEXP do_rnchisq(SEXP sn, SEXP sa, SEXP sb) UNIMPLEMENTED
SEXP do_rnbinom_mu(SEXP sn, SEXP sa, SEXP sb) UNIMPLEMENTED

SEXP do_rhyper(SEXP sn, SEXP sa, SEXP sb, SEXP sc) UNIMPLEMENTED

SEXP do_rmultinom(SEXP sn, SEXP ssize, SEXP sprob) UNIMPLEMENTED

/* Declarations for .External[2] entry points */

SEXP compcases(SEXP args) UNIMPLEMENTED
SEXP doD(SEXP args) UNIMPLEMENTED
SEXP deriv(SEXP args) UNIMPLEMENTED
SEXP modelframe(SEXP call, SEXP op, SEXP args, SEXP rho) RINTERNAL_CODE
SEXP modelmatrix(SEXP call, SEXP op, SEXP args, SEXP rho) RINTERNAL_CODE
SEXP termsform(SEXP args) RINTERNAL_CODE
SEXP call_dqagi(SEXP x) UNIMPLEMENTED
SEXP call_dqags(SEXP x) UNIMPLEMENTED

SEXP influence(SEXP mqr, SEXP do_coef, SEXP e, SEXP stol) UNIMPLEMENTED



SEXP SplineCoef(SEXP method, SEXP x, SEXP y) UNIMPLEMENTED
SEXP SplineEval(SEXP xout, SEXP z) UNIMPLEMENTED

SEXP ApproxTest(SEXP x, SEXP y, SEXP method, SEXP sf) UNIMPLEMENTED
SEXP Approx(SEXP x, SEXP y, SEXP v, SEXP method,
	    SEXP yleft, SEXP yright, SEXP sf) UNIMPLEMENTED

SEXP Fisher_sim(SEXP sr, SEXP sc, SEXP sB) UNIMPLEMENTED
SEXP chisq_sim(SEXP sr, SEXP sc, SEXP sB, SEXP E) UNIMPLEMENTED

SEXP stats_signrank_free(void) UNIMPLEMENTED
SEXP stats_wilcox_free(void) UNIMPLEMENTED
