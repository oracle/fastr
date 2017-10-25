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

#include "port.h"

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
