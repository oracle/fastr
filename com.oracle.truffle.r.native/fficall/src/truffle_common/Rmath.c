/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "../truffle_nfi/rffiutils.h"
#include "rffi_upcalls.h"

double Rf_dnorm(double a, double b, double c, int d) {
    return ((call_Rf_dnorm) callbacks[Rf_dnorm4_x])(a, b, c, d);
}

double Rf_dnorm4(double a, double b, double c, int d) {
    return Rf_dnorm(a, b, c, d);
}

double Rf_pnorm(double a, double b, double c, int d, int e) {
    return ((call_Rf_pnorm) callbacks[Rf_pnorm5_x])(a, b, c, d, e);
}

double Rf_pnorm5(double a, double b, double c, int d, int e) {
    return Rf_pnorm(a, b, c, d, e);
}

double Rf_qnorm(double a, double b, double c, int d, int e) {
    return ((call_Rf_qnorm) callbacks[Rf_qnorm5_x])(a, b, c, d, e);
}

double Rf_qnorm5(double a, double b, double c, int d, int e) {
    return Rf_qnorm(a, b, c, d, e);
}

double Rf_rnorm(double a, double b) {
    return ((call_Rf_rnorm) callbacks[Rf_rnorm_x])(a, b);
}

void Rf_pnorm_both(double a, double * b, double * c, int d, int e) {
    unimplemented("Rf_rnorm");
//    return ((call_Rf_pnorm_both) callbacks[Rf_pnorm_both_x])(a, b, c, d, e);
}

double Rf_dunif(double a, double b, double c, int d) {
    return ((call_Rf_dunif) callbacks[Rf_dunif_x])(a, b, c, d);
}

double Rf_punif(double a, double b, double c, int d, int e) {
    return ((call_Rf_punif) callbacks[Rf_punif_x])(a, b, c, d, e);
}

double Rf_qunif(double a, double b, double c, int d, int e) {
    return ((call_Rf_qunif) callbacks[Rf_qunif_x])(a, b, c, d, e);
}

double Rf_runif(double a, double b) {
    return ((call_Rf_runif) callbacks[Rf_runif_x])(a, b);
}

double Rf_dgamma(double a, double b, double c, int d) {
    return ((call_Rf_dgamma) callbacks[Rf_dgamma_x])(a, b, c, d);
}

double Rf_pgamma(double a, double b, double c, int d, int e) {
    return ((call_Rf_pgamma) callbacks[Rf_pgamma_x])(a, b, c, d, e);
}

double Rf_qgamma(double a, double b, double c, int d, int e) {
    return ((call_Rf_qgamma) callbacks[Rf_qgamma_x])(a, b, c, d, e);
}

double Rf_rgamma(double a, double b) {
    return ((call_Rf_rgamma) callbacks[Rf_rgamma_x])(a, b);
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
    return ((call_Rf_dbeta) callbacks[Rf_dbeta_x])(a, b, c, d);
}

double Rf_pbeta(double a, double b, double c, int d, int e) {
    return ((call_Rf_pbeta) callbacks[Rf_pbeta_x])(a, b, c, d, e);
}

double Rf_qbeta(double a, double b, double c, int d, int e) {
    return ((call_Rf_qbeta) callbacks[Rf_qbeta_x])(a, b, c, d, e);
}

double Rf_rbeta(double a, double b) {
    return ((call_Rf_rbeta) callbacks[Rf_rbeta_x])(a, b);
}

double Rf_dlnorm(double a, double b, double c, int d) {
    return ((call_Rf_dlnorm) callbacks[Rf_dlnorm_x])(a, b, c, d);
}

double Rf_plnorm(double a, double b, double c, int d, int e) {
    return ((call_Rf_plnorm) callbacks[Rf_plnorm_x])(a, b, c, d, e);
}

double Rf_qlnorm(double a, double b, double c, int d, int e) {
    return ((call_Rf_qlnorm) callbacks[Rf_qlnorm_x])(a, b, c, d, e);
}

double Rf_rlnorm(double a, double b) {
    return ((call_Rf_rlnorm) callbacks[Rf_rlnorm_x])(a, b);
}

double Rf_dchisq(double a, double b, int c) {
    return ((call_Rf_dchisq) callbacks[Rf_dchisq_x])(a, b, c);
}

double Rf_pchisq(double a, double b, int c, int d) {
    return ((call_Rf_pchisq) callbacks[Rf_pchisq_x])(a, b, c, d);
}

double Rf_qchisq(double a, double b, int c, int d) {
    return ((call_Rf_qchisq) callbacks[Rf_qchisq_x])(a, b, c, d);
}

double Rf_rchisq(double a) {
    return ((call_Rf_rchisq) callbacks[Rf_rchisq_x])(a);
}

double Rf_dnchisq(double a, double b, double c, int d) {
    return ((call_Rf_dnchisq) callbacks[Rf_dnchisq_x])(a, b, c, d);
}

double Rf_pnchisq(double a, double b, double c, int d, int e) {
    return ((call_Rf_pnchisq) callbacks[Rf_pnchisq_x])(a, b, c, d, e);
}

double Rf_qnchisq(double a, double b, double c, int d, int e) {
    return ((call_Rf_qnchisq) callbacks[Rf_qnchisq_x])(a, b, c, d, e);
}

double Rf_rnchisq(double a, double b) {
    return ((call_Rf_rnchisq) callbacks[Rf_rnchisq_x])(a, b);
}

double Rf_df(double a, double b, double c, int d) {
    return ((call_Rf_df) callbacks[Rf_df_x])(a, b, c, d);
}

double Rf_pf(double a, double b, double c, int d, int e) {
    return ((call_Rf_pf) callbacks[Rf_pf_x])(a, b, c, d, e);
}

double Rf_qf(double a, double b, double c, int d, int e) {
    return ((call_Rf_qf) callbacks[Rf_qf_x])(a, b, c, d, e);
}

double Rf_rf(double a, double b) {
    return ((call_Rf_rf) callbacks[Rf_rf_x])(a, b);
}

double Rf_dt(double a, double b, int c) {
    return ((call_Rf_dt) callbacks[Rf_dt_x])(a, b, c);
}

double Rf_pt(double a, double b, int c, int d) {
    return ((call_Rf_pt) callbacks[Rf_pt_x])(a, b, c, d);
}

double Rf_qt(double a, double b, int c, int d) {
    return ((call_Rf_qt) callbacks[Rf_qt_x])(a, b, c, d);
}

double Rf_rt(double a) {
    return ((call_Rf_rt) callbacks[Rf_rt_x])(a);
}

double Rf_dbinom(double a, double b, double c, int d) {
    return ((call_Rf_dbinom) callbacks[Rf_dbinom_x])(a, b, c, d);
}

double Rf_pbinom(double a, double b, double c, int d, int e) {
    return ((call_Rf_pbinom) callbacks[Rf_pbinom_x])(a, b, c, d, e);
}

double Rf_qbinom(double a, double b, double c, int d, int e) {
    return ((call_Rf_qbinom) callbacks[Rf_qbinom_x])(a, b, c, d, e);
}

double Rf_rbinom(double a, double b) {
    return ((call_Rf_rbinom) callbacks[Rf_rbinom_x])(a, b);
}

void Rf_rmultinom(int a, double* b, int c, int* d) {
    ((call_Rf_rmultinom) callbacks[Rf_rmultinom_x])(a, b, c, d);
}

double Rf_dcauchy(double a, double b, double c, int d) {
    return ((call_Rf_dcauchy) callbacks[Rf_dcauchy_x])(a, b, c, d);
}

double Rf_pcauchy(double a, double b, double c, int d, int e) {
    return ((call_Rf_pcauchy) callbacks[Rf_pcauchy_x])(a, b, c, d, e);
}

double Rf_qcauchy(double a, double b, double c, int d, int e) {
    return ((call_Rf_qcauchy) callbacks[Rf_qcauchy_x])(a, b, c, d, e);
}

double Rf_rcauchy(double a, double b) {
    return ((call_Rf_rcauchy) callbacks[Rf_rcauchy_x])(a, b);
}

double Rf_dexp(double a, double b, int c) {
    return ((call_Rf_dexp) callbacks[Rf_dexp_x])(a, b, c);
}

double Rf_pexp(double a, double b, int c, int d) {
    return ((call_Rf_pexp) callbacks[Rf_pexp_x])(a, b, c, d);
}

double Rf_qexp(double a, double b, int c, int d) {
    return ((call_Rf_qexp) callbacks[Rf_qexp_x])(a, b, c, d);
}

double Rf_rexp(double a) {
    return ((call_Rf_rexp) callbacks[Rf_rexp_x])(a);
}

double Rf_dgeom(double a, double b, int c) {
    return ((call_Rf_dgeom) callbacks[Rf_dgeom_x])(a, b, c);
}

double Rf_pgeom(double a, double b, int c, int d) {
    return ((call_Rf_pgeom) callbacks[Rf_pgeom_x])(a, b, c, d);
}

double Rf_qgeom(double a, double b, int c, int d) {
    return ((call_Rf_qgeom) callbacks[Rf_qgeom_x])(a, b, c, d);
}

double Rf_rgeom(double a) {
    return ((call_Rf_rgeom) callbacks[Rf_rgeom_x])(a);
}

double Rf_dhyper(double a, double b, double c, double d, int e) {
    return ((call_Rf_dhyper) callbacks[Rf_dhyper_x])(a, b, c, d, e);
}

double Rf_phyper(double a, double b, double c, double d, int e, int f) {
    return ((call_Rf_phyper) callbacks[Rf_phyper_x])(a, b, c, d, e, f);
}

double Rf_qhyper(double a, double b, double c, double d, int e, int f) {
    return ((call_Rf_qhyper) callbacks[Rf_qhyper_x])(a, b, c, d, e, f);
}

double Rf_rhyper(double a, double b, double c) {
    return ((call_Rf_rhyper) callbacks[Rf_rhyper_x])(a, b, c);
}

double Rf_dnbinom(double a, double b, double c, int d) {
    return ((call_Rf_dnbinom) callbacks[Rf_dnbinom_x])(a, b, c, d);
}

double Rf_pnbinom(double a, double b, double c, int d, int e) {
    return ((call_Rf_pnbinom) callbacks[Rf_pnbinom_x])(a, b, c, d, e);
}

double Rf_qnbinom(double a, double b, double c, int d, int e) {
    return ((call_Rf_qnbinom) callbacks[Rf_qnbinom_x])(a, b, c, d, e);
}

double Rf_rnbinom(double a, double b) {
    return ((call_Rf_rnbinom) callbacks[Rf_rnbinom_x])(a, b);
}

double Rf_dnbinom_mu(double a, double b, double c, int d) {
    return ((call_Rf_dnbinom_mu) callbacks[Rf_dnbinom_mu_x])(a, b, c, d);
}

double Rf_pnbinom_mu(double a, double b, double c, int d, int e) {
    return ((call_Rf_pnbinom_mu) callbacks[Rf_pnbinom_mu_x])(a, b, c, d, e);
}

double Rf_qnbinom_mu(double a, double b, double c, int d, int e) {
    return ((call_Rf_qnbinom_mu) callbacks[Rf_qnbinom_mu_x])(a, b, c, d, e);
}

double Rf_rnbinom_mu(double a, double b) {
    return ((call_Rf_rnbinom_mu) callbacks[Rf_rnbinom_mu_x])(a, b);
}

double Rf_dpois(double a, double b, int c) {
    return ((call_Rf_dpois) callbacks[Rf_dpois_x])(a, b, c);
}

double Rf_ppois(double a, double b, int c, int d) {
    return ((call_Rf_ppois) callbacks[Rf_ppois_x])(a, b, c, d);
}

double Rf_qpois(double a, double b, int c, int d) {
    return ((call_Rf_qpois) callbacks[Rf_qpois_x])(a, b, c, d);
}

double Rf_rpois(double a) {
    return ((call_Rf_rpois) callbacks[Rf_rpois_x])(a);
}

double Rf_dweibull(double a, double b, double c, int d) {
    return ((call_Rf_dweibull) callbacks[Rf_dweibull_x])(a, b, c, d);
}

double Rf_pweibull(double a, double b, double c, int d, int e) {
    return ((call_Rf_pweibull) callbacks[Rf_pweibull_x])(a, b, c, d, e);
}

double Rf_qweibull(double a, double b, double c, int d, int e) {
    return ((call_Rf_qweibull) callbacks[Rf_qweibull_x])(a, b, c, d, e);
}

double Rf_rweibull(double a, double b) {
    return ((call_Rf_rweibull) callbacks[Rf_rweibull_x])(a, b);
}

double Rf_dlogis(double a, double b, double c, int d) {
    return ((call_Rf_dlogis) callbacks[Rf_dlogis_x])(a, b, c, d);
}

double Rf_plogis(double a, double b, double c, int d, int e) {
    return ((call_Rf_plogis) callbacks[Rf_plogis_x])(a, b, c, d, e);
}

double Rf_qlogis(double a, double b, double c, int d, int e) {
    return ((call_Rf_qlogis) callbacks[Rf_qlogis_x])(a, b, c, d, e);
}

double Rf_rlogis(double a, double b) {
    return ((call_Rf_rlogis) callbacks[Rf_rlogis_x])(a, b);
}

double Rf_dnbeta(double a, double b, double c, double d, int e) {
    return ((call_Rf_dnbeta) callbacks[Rf_dnbeta_x])(a, b, c, d, e);
}

double Rf_pnbeta(double a, double b, double c, double d, int e, int f) {
    return ((call_Rf_pnbeta) callbacks[Rf_pnbeta_x])(a, b, c, d, e, f);
}

double Rf_qnbeta(double a, double b, double c, double d, int e, int f) {
    return ((call_Rf_qnbeta) callbacks[Rf_qnbeta_x])(a, b, c, d, e, f);
}

double Rf_rnbeta(double a, double b, double c) {
    unimplemented("Rf_rnbeta");
    return 0;
}

double Rf_dnf(double a, double b, double c, double d, int e) {
    return ((call_Rf_dnf) callbacks[Rf_dnf_x])(a, b, c, d, e);
}

double Rf_pnf(double a, double b, double c, double d, int e, int f) {
    return ((call_Rf_pnf) callbacks[Rf_pnf_x])(a, b, c, d, e, f);
}

double Rf_qnf(double a, double b, double c, double d, int e, int f) {
    return ((call_Rf_qnf) callbacks[Rf_qnf_x])(a, b, c, d, e, f);
}

double Rf_dnt(double a, double b, double c, int d) {
    return ((call_Rf_dnt) callbacks[Rf_dnt_x])(a, b, c, d);
}

double Rf_pnt(double a, double b, double c, int d, int e) {
    return ((call_Rf_pnt) callbacks[Rf_pnt_x])(a, b, c, d, e);
}

double Rf_qnt(double a, double b, double c, int d, int e) {
    return ((call_Rf_qnt) callbacks[Rf_qnt_x])(a, b, c, d, e);
}

double Rf_ptukey(double a, double b, double c, double d, int e, int f) {
    return ((call_Rf_ptukey) callbacks[Rf_ptukey_x])(a, b, c, d, e, f);
}

double Rf_qtukey(double a, double b, double c, double d, int e, int f) {
    return ((call_Rf_qtukey) callbacks[Rf_qtukey_x])(a, b, c, d, e, f);
}

double Rf_dwilcox(double a, double b, double c, int d) {
    return ((call_Rf_dwilcox) callbacks[Rf_dwilcox_x])(a, b, c, d);
}

double Rf_pwilcox(double a, double b, double c, int d, int e) {
    return ((call_Rf_pwilcox) callbacks[Rf_pwilcox_x])(a, b, c, d, e);
}

double Rf_qwilcox(double a, double b, double c, int d, int e) {
    return ((call_Rf_qwilcox) callbacks[Rf_qwilcox_x])(a, b, c, d, e);
}

double Rf_rwilcox(double a, double b) {
    return ((call_Rf_rwilcox) callbacks[Rf_rwilcox_x])(a, b);
}

double Rf_dsignrank(double a, double b, int c) {
    return ((call_Rf_dsignrank) callbacks[Rf_dsignrank_x])(a, b, c);
}

double Rf_psignrank(double a, double b, int c, int d) {
    return ((call_Rf_psignrank) callbacks[Rf_psignrank_x])(a, b, c, d);
}

double Rf_qsignrank(double a, double b, int c, int d) {
    return ((call_Rf_qsignrank) callbacks[Rf_qsignrank_x])(a, b, c, d);
}

double Rf_rsignrank(double a) {
    return ((call_Rf_rsignrank) callbacks[Rf_rsignrank_x])(a);
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
    return ((call_Rf_ftrunc) callbacks[Rf_ftrunc_x])(a);
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

