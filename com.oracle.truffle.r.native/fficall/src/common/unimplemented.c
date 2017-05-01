/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2015, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

#include <Rinternals.h>
#include <stdlib.h>

#include <R_ext/eventloop.h>

Rboolean known_to_be_latin1 = FALSE;
Rboolean known_to_be_utf8 = FALSE;

extern void *unimplemented(char *msg);

int R_cairoCdynload(int local, int now)
{
	unimplemented("R_cairoCdynload");
    return 0;
}

SEXP do_X11(SEXP call, SEXP op, SEXP args, SEXP rho)
{
	unimplemented("do_X11");
    return R_NilValue;
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

void *Rf_AdobeSymbol2utf8(char *work, const char *c0, size_t nwork) {
    unimplemented("Rf_AdobeSymbol2utf8");
    return NULL;
}

size_t Rf_ucstoutf8(char *s, const unsigned int wc) {
    unimplemented("Rf_ucstoutf8");
    return 0;
}

InputHandler *
addInputHandler(InputHandler *handlers, int fd, InputHandlerProc handler,
		int activity) {
    unimplemented("addInputHandler");
    return NULL;
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

void R_InitOutPStream(R_outpstream_t stream, R_pstream_data_t data, R_pstream_format_t type, int version,
		 void (*outchar)(R_outpstream_t, int), void (*outbytes)(R_outpstream_t, void *, int),
		 SEXP (*phook)(SEXP, SEXP), SEXP pdata)
{
	unimplemented("R_InitOutPStream");
}

void R_Serialize(SEXP s, R_outpstream_t stream)
{
	unimplemented("R_Serialize");
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
	unimplemented("Rf_checkArityCall");
}

SEXP NewEnvironment(SEXP a, SEXP b, SEXP c) {
	unimplemented("NewEnvironment");
	return NULL;
}

void* PRIMFUN(SEXP x) {
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

