/*
 *  R : A Computer Language for Statistical Data Analysis
 *  Copyright (C) 2001-12   The R Core Team.
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

#include "ts.h"

void multi_burg(int *pn, double *x, int *pomax, int *pnser, double *coef,
		double *pacf, double *var, double *aic, int *porder,
		int *useaic, int *vmethod) { }
void multi_yw(double *acf, int *pn, int *pomax, int *pnser, double *coef,
	      double *pacf, double *var, double *aic, int *porder,
	      int *puseaic) { }
void HoltWinters (double *x, int *xl, double *alpha, double *beta,
		  double *gamma, int *start_time, int *seasonal, int *period,
		  int *dotrend, int *doseasonal,
		  double *a, double *b, double *s, double *SSE, double *level,
		  double *trend, double *season) { }

void starma(Starma G, int *ifault) { }

void karma(Starma G, double *sumlog, double *ssq, int iupd, int *nit) { }

void forkal(Starma G, int id, int il, double *delta, double *y,
	    double *amse, int *ifault) { }

SEXP setup_starma(SEXP na, SEXP x, SEXP pn, SEXP xreg, SEXP pm,
		  SEXP dt, SEXP ptrans, SEXP sncond) { return NULL; }
SEXP free_starma(SEXP pG) { return NULL; }
SEXP set_trans(SEXP pG, SEXP ptrans) { return NULL; }
SEXP arma0fa(SEXP pG, SEXP inparams) { return NULL; }
SEXP get_s2(SEXP pG) { return NULL; }
SEXP get_resid(SEXP pG) { return NULL; }
SEXP Dotrans(SEXP pG, SEXP x) { return NULL; }
SEXP arma0_kfore(SEXP pG, SEXP pd, SEXP psd, SEXP n_ahead) { return NULL; }
SEXP Starma_method(SEXP pG, SEXP method) { return NULL; }
SEXP Gradtrans(SEXP pG, SEXP x) { return NULL; }
SEXP Invtrans(SEXP pG, SEXP x) { return NULL; }

SEXP ARMAtoMA(SEXP ar, SEXP ma, SEXP lag_max) { return NULL; }

SEXP KalmanLike(SEXP sy, SEXP mod, SEXP sUP, SEXP op, SEXP fast) { return NULL; }
SEXP KalmanFore(SEXP nahead, SEXP mod, SEXP fast) { return NULL; }
SEXP KalmanSmooth(SEXP sy, SEXP mod, SEXP sUP) { return NULL; }
SEXP ARIMA_undoPars(SEXP sin, SEXP sarma) { return NULL; }
SEXP ARIMA_transPars(SEXP sin, SEXP sarma, SEXP strans) { return NULL; }
SEXP ARIMA_Invtrans(SEXP in, SEXP sarma) { return NULL; }
SEXP ARIMA_Gradtrans(SEXP in, SEXP sarma) { return NULL; }
SEXP ARIMA_Like(SEXP sy, SEXP mod, SEXP sUP, SEXP giveResid) { return NULL; }
SEXP ARIMA_CSS(SEXP sy, SEXP sarma, SEXP sPhi, SEXP sTheta, SEXP sncond,
	       SEXP giveResid) { return NULL; }
SEXP TSconv(SEXP a, SEXP b) { return NULL; }
SEXP getQ0(SEXP sPhi, SEXP sTheta) { return NULL; }
SEXP getQ0bis(SEXP sPhi, SEXP sTheta, SEXP sTol) { return NULL; }

SEXP acf(SEXP x, SEXP lmax, SEXP sCor) { return NULL; }
SEXP pacf1(SEXP acf, SEXP lmax) { return NULL; }
SEXP ar2ma(SEXP ar, SEXP npsi) { return NULL; }
SEXP Burg(SEXP x, SEXP order) { return NULL; }
SEXP pp_sum(SEXP u, SEXP sl) { return NULL; }
SEXP intgrt_vec(SEXP x, SEXP xi, SEXP slag) { return NULL; }

