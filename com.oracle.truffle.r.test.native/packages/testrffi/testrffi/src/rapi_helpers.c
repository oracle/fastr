/*
 * Copyright (c) 2019, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

// Helpers for tests in rapiTests.R

#define USE_RINTERNALS
#include <R.h>
#include <Rdefines.h>
#include <Rinterface.h>
#include <Rinternals.h>
#include <Rmath.h>
#include <R_ext/Connections.h>
#include <R_ext/Parse.h>
#include <string.h>
#include <stdint.h>
#include "rapi_helpers.h"

void rapi_dotC(int* idata, double* ddata, void* arbitrary) {
  if (idata[0] != 42) printf("ERROR: idata[0] == 42 failed\n");
  if (ddata[0] != 3.14) printf("ERROR: ddata[0] == 3.14 failed\n");
  if (TYPEOF((SEXP) arbitrary) != 3 /*closure*/) printf("ERROR: 'arbitrary' is not a closure\n");
  idata[0] = 1;
  ddata[0] = 0.5;
}

SEXP rapi_dotCall(SEXP arg1, SEXP arg2) {
  return ScalarInteger(TYPEOF(arg1) + TYPEOF(arg2));
}

SEXP rapi_dotExternal(SEXP args) {
  return args;
}

SEXP rapi_dotExternal2(SEXP call, SEXP op, SEXP args, SEXP rho) {
  return args;
}