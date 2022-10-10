/*
 * Copyright (c) 1995-2015, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, 2022, Oracle and/or its affiliates
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, a copy is available at
 * https://www.R-project.org/Licenses/
 */

#include <Rinternals.h>
#include <stdlib.h>
#include <rlocale.h>

#include <R_ext/eventloop.h>
#include <R_ext/GraphicsEngine.h>
#include <Defn.h>


Rboolean known_to_be_latin1 = FALSE;
Rboolean known_to_be_utf8 = FALSE;

extern void *unimplemented(char *msg);

int R_cairoCdynload(int local, int now)
{
	unimplemented("R_cairoCdynload");
    return 0;
}

SEXP do_saveplot(SEXP call, SEXP op, SEXP args, SEXP rho)
{
	unimplemented("do_saveplot");
    return R_NilValue;
}


SEXP do_getGraphicsEvent(SEXP call, SEXP op, SEXP args, SEXP env)
{
    unimplemented("do_getGraphicsEvent");
    return R_NilValue;
}


SEXP do_setGraphicsEventEnv(SEXP call, SEXP op, SEXP args, SEXP env)
{
    unimplemented("do_setGraphicsEventEnv");
    return R_NilValue;
}

SEXP do_getGraphicsEventEnv(SEXP call, SEXP op, SEXP args, SEXP env)
{
    unimplemented("do_getGraphicsEventEnv");
    return R_NilValue;
}

void *Rf_AdobeSymbol2utf8(char *out, const char *in, size_t nwork, Rboolean usePUA) {
    unimplemented("Rf_AdobeSymbol2utf8");
    return NULL;
}

size_t ucstoutf8(char *s, const unsigned int wc) {
    unimplemented("Rf_ucstoutf8");
    return 0;
}

size_t Rf_ucstoutf8(char *s, const unsigned int wc) {
    unimplemented("Rf_ucstoutf8");
    return 0;
}

size_t mbtoucs(unsigned int *wc, const char *s, size_t n) {
    unimplemented("mbtoucs");
    return 0;
}

void setup_RdotApp(void) {
	unimplemented("setup_RdotApp");
}

const char *Rf_EncodeComplex(Rcomplex x, int wr, int dr, int er, int wi, int di, int ei, char cdec)
{
	unimplemented("Rf_EncodeComplex");
	return NULL;
}

const char *Rf_EncodeInteger(int x, int w)
{
	unimplemented("Rf_EncodeInteger");
	return NULL;
}

const char *Rf_EncodeLogical(int x, int w)
{
	unimplemented("Rf_EncodeLogical");
	return NULL;
}

void R_InitInPStream(R_inpstream_t stream, R_pstream_data_t data, R_pstream_format_t type,
		int (*inchar)(R_inpstream_t), void (*inbytes)(R_inpstream_t, void *, int), SEXP (*phook)(SEXP, SEXP), SEXP pdata)
{
	unimplemented("R_InitInPStream");
}

SEXP R_Unserialize(R_inpstream_t stream)
{
	unimplemented("R_Unserialize");
	return NULL;
}

SEXP R_getS4DataSlot(SEXP obj, SEXPTYPE type) {
	unimplemented("R_getS4DataSlot");
	return NULL;
}

void Rf_checkArityCall(SEXP a, SEXP b, SEXP c) {
	//unimplemented("Rf_checkArityCall");
}

SEXP NewEnvironment(SEXP a, SEXP b, SEXP c) {
	unimplemented("NewEnvironment");
	return NULL;
}

SEXP coerceToSymbol(SEXP v) {
	unimplemented("coerceToSymbol");
	return NULL;
}

int IntegerFromString(SEXP a, int* b) {
	unimplemented("IntegerFromString");
	return 0;
}

int Scollate(SEXP a, SEXP b) {
	unimplemented("Scollate(");
	return 0;
}

// used in main/format.c
void z_prec_r(Rcomplex *r, Rcomplex *x, double digits) {
    unimplemented("z_prec_r");
}

int Rf_AdobeSymbol2ucs2(int n) {
    unimplemented("Rf_AdobeSymbol2ucs2");
    return 0;
}

size_t Mbrtowc(wchar_t *wc, const char *s, size_t n, mbstate_t *ps) {
    unimplemented("Mbrtowc");
    return 0;
}

double R_GE_VStrHeight(const char *s, cetype_t enc, const pGEcontext gc, pGEDevDesc dd) {
    unimplemented("R_GE_VStrHeight");
    return 0;
}

void setulb(int n, int m, double *x, double *l, double *u, int *nbd,
	    double *f, double *g, double factr, double *pgtol,
	    double *wa, int * iwa, char *task, int iprint, int *isave) {
    unimplemented("setulb");	    
}

void R_GE_VText(double x, double y, const char * const s, cetype_t enc,
		double x_justify, double y_justify, double rotation,
		const pGEcontext gc, pGEDevDesc dd) {
    unimplemented("R_GE_VText");	    		
}

double R_GE_VStrWidth(const char *s, cetype_t enc, const pGEcontext gc, pGEDevDesc dd) {
    unimplemented("R_GE_VStrWidth");	    		
    return 0;
}

double EXP(double x) {
    unimplemented("EXP");
    return 0;
}

double LOG(double x) {
    unimplemented("LOG");
    return 0;
}

R_wchar_t Rf_utf8toucs32(wchar_t high, const char *s) {
    unimplemented("Rf_utf8toucs32");	
	return 0;
}

int DispatchOrEval(SEXP call, SEXP op, const char *generic, SEXP args,
		   SEXP rho, SEXP *ans, int dropmissing, int argsevald) {
    unimplemented("DispatchOrEval");	
	return 0;
}

