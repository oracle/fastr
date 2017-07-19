/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
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

extern void dqrdc2_(double *x, int *ldx, int *n, int *p, double *tol, int *rank, double *qraux, int* pivot, double *work);
extern void dqrcf_(double *x, int *n, int *k, double *qraux, double *y, int *ny, double *b, int* info);
extern void dqrls_(double *x, int *n, int *p, double *y, int *ny, double *tol, double *b, double *rsd, double *qty, int *k, int *jpvt, double *qraux, double *work);
extern void dqrqty_(double *x, int *n, int *k, double *qraux, double *y, int *ny, double *qty);
extern void dqrqy_(double *x, int *n, int *k, double *qraux, double *y, int *ny, double *qy);
extern void dqrrsd_(double *x, int *n, int *k, double *qraux, double *y, int *ny, double *rsd);
extern void dqrxb_(double *x, int *n, int *k, double *qraux, double *y, int *ny, double *xb);

void call_appl_dqrdc2(double *x, int ldx, int n, int p, double tol, int *rank, double *qraux, int* pivot, double *work) {
	dqrdc2_(x, &ldx, &n, &p, &tol, rank, qraux, pivot, work);
}

void call_appl_dqrcf(double *x, int n, int k, double *qraux, double *y, int ny, double *b, int* info) {
	dqrcf_(x, &n, &k, qraux, y, &ny, b, info);
}

void call_appl_dqrls(double *x, int n, int p, double *y, int ny, double tol, double *b, double *rsd, double *qty, int *k, int *jpvt, double *qraux, double *work) {
	dqrls_(x, &n, &p, y, &ny, &tol, b, rsd, qty, k, jpvt, qraux, work);
}

void call_appl_dqrqty(double *x, int n, int k, double *qraux, double *y, int ny, double *qty) {
	dqrqty_(x, &n, &k, qraux, y, &ny, qty);
}

void call_appl_dqrqy(double *x, int n, int k, double *qraux, double *y, int ny, double *qy) {
	dqrqy_(x, &n, &k, qraux, y, &ny, qy);
}

void call_appl_dqrrsd(double *x, int n, int k, double *qraux, double *y, int ny, double *rsd) {
	dqrrsd_(x, &n, &k, qraux, y, &ny, rsd);
}

void call_appl_dqrxb(double *x, int n, int k, double *qraux, double *y, int ny, double *xb) {
	dqrxb_(x, &n, &k, qraux, y, &ny, xb);
}
