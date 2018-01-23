/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2002--2012, The R Core Team
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

/* **********************************************************************
 * === This was 'sort()' in  gamfit's  mysort.f  [or sortdi() in sortdi.f ] :
 * was at end of  modreg/src/ppr.f
 * Translated by f2c (version 20010821) and f2c-clean,v 1.9 2000/01/13 13:46:53
 * then manually by Martin Maechler
*/

// Copied from GNU R, TODO: convert to Java up-call

#include <Defn.h>
#include <Internal.h>
#include <Rmath.h>

/* These are exposed in Utils.h and are misguidely in the API */
void qsort4_(double *v, int *indx, int *ii, int *jj)
{
    R_qsort_I(v, indx, *ii, *jj);
}

void qsort3_(double *v, int *ii, int *jj)
{
    R_qsort(v, *ii, *jj);
}

#define qsort_Index
#define INTt int
#define INDt int

#define NUMERIC double
void R_qsort_I(double *v, int *I, int i, int j)
#include "qsort-body.templ"
#undef NUMERIC

#define NUMERIC int
void R_qsort_int_I(int *v, int *I, int i, int j)
#include "qsort-body.templ"
#undef NUMERIC

#undef INTt
#undef INDt

#undef qsort_Index

#define NUMERIC double
void R_qsort(double *v, size_t i, size_t j)
#include "qsort-body.templ"
#undef NUMERIC

#define NUMERIC int
void R_qsort_int(int *v, size_t i, size_t j)
#include "qsort-body.templ"
#undef NUMERIC
