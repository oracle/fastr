/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
#include <rffiutils.h>

double Rf_dnorm(double a, double b, double c, int d) {
    unimplemented("Rf_dnorm");
    return 0;
}

double Rf_dnorm4(double a, double b, double c, int d) {
    return Rf_dnorm(a, b, c, d);
}

double Rf_pnorm(double a, double b, double c, int d, int e) {
    unimplemented("Rf_pnorm");
    return 0;
}

double Rf_pnorm5(double a, double b, double c, int d, int e) {
    return Rf_pnorm(a, b, c, d, e);
}

double Rf_qnorm(double a, double b, double c, int d, int e) {
    unimplemented("Rf_qnorm");
    return 0;
}

double Rf_qnorm5(double a, double b, double c, int d, int e) {
    return Rf_qnorm(a, b, c, d, e);
}

double Rf_rnorm(double a, double b) {
    unimplemented("Rf_rnorm");
    return 0;
}

void Rf_pnorm_both(double a, double * b, double * c, int d, int e) {
    unimplemented("Rf_pnorm_both");
}

double Rf_dunif(double a, double b, double c, int d) {
	return ((call_Rf_dunif) callbacks[Rf_dunif])(a, b, c, d);
}

double Rf_punif(double a, double b, double c, int d, int e) {
	return ((call_Rf_punif) callbacks[Rf_punif])(a, b, c, d, e);
}

double Rf_qunif(double a, double b, double c, int d, int e) {
	return ((call_Rf_qunif) callbacks[Rf_qunif])(a, b, c, d, e);
}

double Rf_runif(double a, double b) {
	return ((call_Rf_runif) callbacks[Rf_runif])(a, b);
}

double Rf_dgamma(double a, double b, double c, int d) {
    unimplemented("Rf_dgamma");
    return 0;
}

double Rf_pgamma(double a, double b, double c, int d, int e) {
    unimplemented("Rf_pgamma");
    return 0;
}

double Rf_qgamma(double a, double b, double c, int d, int e) {
    unimplemented("Rf_qgamma");
    return 0;
}

double Rf_rgamma(double a, double b) {
    unimplemented("Rf_rgamma");
    return 0;
}

double Rf_log1pmx(double a) {
    unimplemented("Rf_log1pmx");
    return 0;
}

double Rf_log1pexp(double a) {
    unimplemented("Rf_log1pexp");
    return 0;
}

double Rf_lgamma1p(double a) {
    unimplemented("Rf_lgamma1p");
    return 0;
}

double Rf_logspace_add(double a, double b) {
    unimplemented("Rf_logspace_add");
    return 0;
}

double Rf_logspace_sub(double a, double b) {
    unimplemented("Rf_logspace_sub");
    return 0;
}

double Rf_dbeta(double a, double b, double c, int d) {
    unimplemented("Rf_dbeta");
    return 0;
}

double Rf_pbeta(double a, double b, double c, int d, int e) {
    unimplemented("Rf_pbeta");
    return 0;
}

double Rf_qbeta(double a, double b, double c, int d, int e) {
    unimplemented("Rf_qbeta");
    return 0;
}

double Rf_rbeta(double a, double b) {
    unimplemented("Rf_rbeta");
    return 0;
}

double Rf_dlnorm(double a, double b, double c, int d) {
    unimplemented("Rf_dlnorm");
    return 0;
}

double Rf_plnorm(double a, double b, double c, int d, int e) {
    unimplemented("Rf_plnorm");
    return 0;
}

double Rf_qlnorm(double a, double b, double c, int d, int e) {
    unimplemented("Rf_qlnorm");
    return 0;
}

double Rf_rlnorm(double a, double b) {
    unimplemented("Rf_rlnorm");
    return 0;
}

double Rf_dchisq(double a, double b, int c) {
    unimplemented("Rf_dchisq");
    return 0;
}

double Rf_pchisq(double a, double b, int c, int d) {
    unimplemented("Rf_pchisq");
    return 0;
}

double Rf_qchisq(double a, double b, int c, int d) {
    unimplemented("Rf_qchisq");
    return 0;
}

double Rf_rchisq(double a) {
    unimplemented("Rf_rchisq");
    return 0;
}

double Rf_dnchisq(double a, double b, double c, int d) {
    unimplemented("Rf_dnchisq");
    return 0;
}

double Rf_pnchisq(double a, double b, double c, int d, int e) {
    unimplemented("Rf_pnchisq");
    return 0;
}

double Rf_qnchisq(double a, double b, double c, int d, int e) {
    unimplemented("Rf_qnchisq");
    return 0;
}

double Rf_rnchisq(double a, double b) {
    unimplemented("Rf_rnchisq");
    return 0;
}

double Rf_df(double a, double b, double c, int d) {
    unimplemented("Rf_df");
    return 0;
}

double Rf_pf(double a, double b, double c, int d, int e) {
    unimplemented("Rf_pf");
    return 0;
}

double Rf_qf(double a, double b, double c, int d, int e) {
    unimplemented("Rf_qf");
    return 0;
}

double Rf_rf(double a, double b) {
    unimplemented("Rf_rf");
    return 0;
}

double Rf_dt(double a, double b, int c) {
    unimplemented("Rf_dt");
    return 0;
}

double Rf_pt(double a, double b, int c, int d) {
    unimplemented("Rf_pt");
    return 0;
}

double Rf_qt(double a, double b, int c, int d) {
    unimplemented("Rf_qt");
    return 0;
}

double Rf_rt(double a) {
    unimplemented("Rf_rt");
    return 0;
}

double Rf_dbinom(double a, double b, double c, int d) {
    unimplemented("Rf_dbinom");
    return 0;
}

double Rf_pbinom(double a, double b, double c, int d, int e) {
    unimplemented("Rf_pbinom");
    return 0;
}

double Rf_qbinom(double a, double b, double c, int d, int e) {
    unimplemented("Rf_qbinom");
    return 0;
}

double Rf_rbinom(double a, double b) {
    unimplemented("Rf_rbinom");
    return 0;
}

void Rf_rmultinom(int a, double* b, int c, int* d) {
    unimplemented("Rf_rmultinom");
}

double Rf_dcauchy(double a, double b, double c, int d) {
    unimplemented("Rf_dcauchy");
    return 0;
}

double Rf_pcauchy(double a, double b, double c, int d, int e) {
    unimplemented("Rf_pcauchy");
    return 0;
}

double Rf_qcauchy(double a, double b, double c, int d, int e) {
    unimplemented("Rf_qcauchy");
    return 0;
}

double Rf_rcauchy(double a, double b) {
    unimplemented("Rf_rcauchy");
    return 0;
}

double Rf_dexp(double a, double b, int c) {
    unimplemented("Rf_dexp");
    return 0;
}

double Rf_pexp(double a, double b, int c, int d) {
    unimplemented("Rf_pexp");
    return 0;
}

double Rf_qexp(double a, double b, int c, int d) {
    unimplemented("Rf_qexp");
    return 0;
}

double Rf_rexp(double a) {
    unimplemented("Rf_rexp");
    return 0;
}

double Rf_dgeom(double a, double b, int c) {
    unimplemented("Rf_dgeom");
    return 0;
}

double Rf_pgeom(double a, double b, int c, int d) {
    unimplemented("Rf_pgeom");
    return 0;
}

double Rf_qgeom(double a, double b, int c, int d) {
    unimplemented("Rf_qgeom");
    return 0;
}

double Rf_rgeom(double a) {
    unimplemented("Rf_rgeom");
    return 0;
}

double Rf_dhyper(double a, double b, double c, double d, int e) {
    unimplemented("Rf_dhyper");
    return 0;
}

double Rf_phyper(double a, double b, double c, double d, int e, int f) {
    unimplemented("Rf_phyper");
    return 0;
}

double Rf_qhyper(double a, double b, double c, double d, int e, int f) {
    unimplemented("Rf_qhyper");
    return 0;
}

double Rf_rhyper(double a, double b, double c) {
    unimplemented("Rf_rhyper");
    return 0;
}

double Rf_dnbinom(double a, double b, double c, int d) {
    unimplemented("Rf_dnbinom");
    return 0;
}

double Rf_pnbinom(double a, double b, double c, int d, int e) {
    unimplemented("Rf_pnbinom");
    return 0;
}

double Rf_qnbinom(double a, double b, double c, int d, int e) {
    unimplemented("Rf_qnbinom");
    return 0;
}

double Rf_rnbinom(double a, double b) {
    unimplemented("Rf_rnbinom");
    return 0;
}

double Rf_dnbinom_mu(double a, double b, double c, int d) {
    unimplemented("Rf_dnbinom_mu");
    return 0;
}

double Rf_pnbinom_mu(double a, double b, double c, int d, int e) {
    unimplemented("Rf_pnbinom_mu");
    return 0;
}

double Rf_qnbinom_mu(double a, double b, double c, int d, int e) {
    unimplemented("Rf_qnbinom_mu");
    return 0;
}

double Rf_rnbinom_mu(double a, double b) {
    unimplemented("Rf_rnbinom_mu");
    return 0;
}

double Rf_dpois(double a, double b, int c) {
    unimplemented("Rf_dpois");
    return 0;
}

double Rf_ppois(double a, double b, int c, int d) {
    unimplemented("Rf_ppois");
    return 0;
}

double Rf_qpois(double a, double b, int c, int d) {
    unimplemented("Rf_qpois");
    return 0;
}

double Rf_rpois(double a) {
    unimplemented("Rf_rpois");
    return 0;
}

double Rf_dweibull(double a, double b, double c, int d) {
    unimplemented("Rf_dweibull");
    return 0;
}

double Rf_pweibull(double a, double b, double c, int d, int e) {
    unimplemented("Rf_pweibull");
    return 0;
}

double Rf_qweibull(double a, double b, double c, int d, int e) {
    unimplemented("Rf_qweibull");
    return 0;
}

double Rf_rweibull(double a, double b) {
    unimplemented("Rf_rweibull");
    return 0;
}

double Rf_dlogis(double a, double b, double c, int d) {
    unimplemented("Rf_dlogis");
    return 0;
}

double Rf_plogis(double a, double b, double c, int d, int e) {
    unimplemented("Rf_plogis");
    return 0;
}

double Rf_qlogis(double a, double b, double c, int d, int e) {
    unimplemented("Rf_qlogis");
    return 0;
}

double Rf_rlogis(double a, double b) {
    unimplemented("Rf_rlogis");
    return 0;
}

double Rf_dnbeta(double a, double b, double c, double d, int e) {
    unimplemented("Rf_dnbeta");
    return 0;
}

double Rf_pnbeta(double a, double b, double c, double d, int e, int f) {
    unimplemented("Rf_pnbeta");
    return 0;
}

double Rf_qnbeta(double a, double b, double c, double d, int e, int f) {
    unimplemented("Rf_qnbeta");
    return 0;
}

double Rf_rnbeta(double a, double b, double c) {
    unimplemented("Rf_rnbeta");
    return 0;
}

double Rf_dnf(double a, double b, double c, double d, int e) {
    unimplemented("Rf_dnf");
    return 0;
}

double Rf_pnf(double a, double b, double c, double d, int e, int f) {
    unimplemented("Rf_pnf");
    return 0;
}

double Rf_qnf(double a, double b, double c, double d, int e, int f) {
    unimplemented("Rf_qnf");
    return 0;
}

double Rf_dnt(double a, double b, double c, int d) {
    unimplemented("Rf_dnt");
    return 0;
}

double Rf_pnt(double a, double b, double c, int d, int e) {
    unimplemented("Rf_pnt");
    return 0;
}

double Rf_qnt(double a, double b, double c, int d, int e) {
    unimplemented("Rf_qnt");
    return 0;
}

double Rf_ptukey(double a, double b, double c, double d, int e, int f) {
    unimplemented("Rf_ptukey");
    return 0;
}

double Rf_qtukey(double a, double b, double c, double d, int e, int f) {
    unimplemented("Rf_qtukey");
    return 0;
}

double Rf_dwilcox(double a, double b, double c, int d) {
    unimplemented("Rf_dwilcox");
    return 0;
}

double Rf_pwilcox(double a, double b, double c, int d, int e) {
    unimplemented("Rf_pwilcox");
    return 0;
}

double Rf_qwilcox(double a, double b, double c, int d, int e) {
    unimplemented("Rf_qwilcox");
    return 0;
}

double Rf_rwilcox(double a, double b) {
    unimplemented("Rf_rwilcox");
    return 0;
}

double Rf_dsignrank(double a, double b, int c) {
    unimplemented("Rf_dsignrank");
    return 0;
}

double Rf_psignrank(double a, double b, int c, int d) {
    unimplemented("Rf_psignrank");
    return 0;
}

double Rf_qsignrank(double a, double b, int c, int d) {
    unimplemented("Rf_qsignrank");
    return 0;
}

double Rf_rsignrank(double a) {
    unimplemented("Rf_rsignrank");
    return 0;
}

double Rf_gammafn(double a) {
    unimplemented("Rf_gammafn");
    return 0;
}

double Rf_lgammafn(double a) {
    unimplemented("Rf_lgammafn");
    return 0;
}

double Rf_lgammafn_sign(double a, int* b) {
    unimplemented("Rf_lgammafn_sign");
    return 0;
}

void Rf_dpsifn(double a, int b, int c, int d, double* e, int* f, int* g) {
    unimplemented("Rf_dpsifn");
}

double Rf_psigamma(double a, double b) {
    unimplemented("Rf_psigamma");
    return 0;
}

double Rf_digamma(double a) {
    unimplemented("Rf_digamma");
    return 0;
}

double Rf_trigamma(double a) {
    unimplemented("Rf_trigamma");
    return 0;
}

double Rf_tetragamma(double a) {
    unimplemented("Rf_tetragamma");
    return 0;
}

double Rf_pentagamma(double a) {
    unimplemented("Rf_pentagamma");
    return 0;
}

double Rf_beta(double a, double b) {
    unimplemented("Rf_beta");
    return 0;
}

double Rf_lbeta(double a, double b) {
    unimplemented("Rf_lbeta");
    return 0;
}

double Rf_choose(double a, double b) {
    unimplemented("Rf_choose");
    return 0;
}

double Rf_lchoose(double a, double b) {
    unimplemented("Rf_lchoose");
    return 0;
}

double Rf_bessel_i(double a, double b, double c) {
    unimplemented("Rf_bessel_i");
    return 0;
}

double Rf_bessel_j(double a, double b) {
    unimplemented("Rf_bessel_j");
    return 0;
}

double Rf_bessel_k(double a, double b, double c) {
    unimplemented("Rf_bessel_k");
    return 0;
}

double Rf_bessel_y(double a, double b) {
    unimplemented("Rf_bessel_y");
    return 0;
}

double Rf_bessel_i_ex(double a, double b, double c, double * d) {
    unimplemented("Rf_bessel_i_ex");
    return 0;
}

double Rf_bessel_j_ex(double a, double b, double * c) {
    unimplemented("Rf_bessel_j_ex");
    return 0;
}

double Rf_bessel_k_ex(double a, double b, double c, double * d) {
    unimplemented("Rf_bessel_k_ex");
    return 0;
}

double Rf_bessel_y_ex(double a, double b, double * c) {
    unimplemented("Rf_bessel_y_ex");
    return 0;
}

int Rf_imax2(int x, int y) {
    return x > y ? x : y;
}

int Rf_imin2(int x, int y) {
    return x > y ? y : x;
}

double Rf_fmax2(double x, double y) {
    return x > y ? x : y;
}

double Rf_fmin2(double x, double y) {
    return x > y ? y : x;
}

double Rf_sign(double a) {
    unimplemented("Rf_sign");
    return 0;
}

double Rf_fprec(double a, double b) {
    unimplemented("Rf_fprec");
    return 0;
}

double Rf_fsign(double a, double b) {
#ifdef IEEE_754
    if (ISNAN(a) || ISNAN(b))
	return a + b;
#endif
    return ((b >= 0) ? fabs(a) : -fabs(b));
}

double Rf_ftrunc(double a) {
    unimplemented("Rf_ftrunc");
    return 0;
}

double Rf_cospi(double a) {
    unimplemented("Rf_cospi");
    return 0;
}

double Rf_sinpi(double a) {
    unimplemented("Rf_sinpi");
    return 0;
}

double Rf_tanpi(double a) {
    unimplemented("Rf_tanpi");
    return 0;
}

