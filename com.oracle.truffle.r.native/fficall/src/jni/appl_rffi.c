/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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

JNIEXPORT void JNICALL
Java_com_oracle_truffle_r_ffi_impl_jni_JNI_1RAppl_native_1dqrdc2(JNIEnv *env, jclass c,
		jdoubleArray jx, jint ldx, jint n, jint p, jdouble tol, jintArray jrank, jdoubleArray jqraux,
		jintArray jpivot, jdoubleArray jwork) {
	double *x = (*env)->GetPrimitiveArrayCritical(env, jx, NULL);
	int *rank = (*env)->GetPrimitiveArrayCritical(env, jrank, NULL);
	double *qraux = (*env)->GetPrimitiveArrayCritical(env, jqraux, NULL);
	int *pivot = (*env)->GetPrimitiveArrayCritical(env, jpivot, NULL);
	double *work = (*env)->GetPrimitiveArrayCritical(env, jwork, NULL);
	dqrdc2_(x, &ldx, &n, &p, &tol, rank, qraux, pivot, work);
	(*env)->ReleasePrimitiveArrayCritical(env, jx, x, 0);
	(*env)->ReleasePrimitiveArrayCritical(env, jrank, rank, 0);
	(*env)->ReleasePrimitiveArrayCritical(env, jqraux, qraux, 0);
	(*env)->ReleasePrimitiveArrayCritical(env, jpivot, pivot, 0);
	(*env)->ReleasePrimitiveArrayCritical(env, jwork, work, 0);
}

JNIEXPORT void JNICALL
Java_com_oracle_truffle_r_ffi_impl_jni_JNI_1RAppl_native_1dqrcf(JNIEnv *env, jclass c,
		jdoubleArray jx, jint n, jint k, jdoubleArray jqraux, jdoubleArray jy, jint ny, jdoubleArray jb, jintArray jinfo) {
	double *x = (*env)->GetPrimitiveArrayCritical(env, jx, NULL);
	double *qraux = (*env)->GetPrimitiveArrayCritical(env, jqraux, NULL);
	double *y = (*env)->GetPrimitiveArrayCritical(env, jy, NULL);
	double *b = (*env)->GetPrimitiveArrayCritical(env, jb, NULL);
	int *info = (*env)->GetPrimitiveArrayCritical(env, jinfo, NULL);
	dqrcf_(x, &n, &k, qraux, y, &ny, b, info);
	(*env)->ReleasePrimitiveArrayCritical(env, jx, x, 0);
	(*env)->ReleasePrimitiveArrayCritical(env, jqraux, qraux, 0);
	(*env)->ReleasePrimitiveArrayCritical(env, jy, y, 0);
	(*env)->ReleasePrimitiveArrayCritical(env, jb, b, 0);
	(*env)->ReleasePrimitiveArrayCritical(env, jinfo, info, 0);
}

JNIEXPORT void JNICALL
Java_com_oracle_truffle_r_ffi_impl_jni_JNI_1RAppl_native_1dqrls(JNIEnv *env, jclass c,
		jdoubleArray jx, int n, int p, jdoubleArray jy, int ny, double tol, jdoubleArray jb,
		jdoubleArray jrsd, jdoubleArray jqty, jintArray jk, jintArray jjpvt, jdoubleArray jqraux, jdoubleArray jwork) {
	double *x = (*env)->GetPrimitiveArrayCritical(env, jx, NULL);
	double *y = (*env)->GetPrimitiveArrayCritical(env, jy, NULL);
	double *b = (*env)->GetPrimitiveArrayCritical(env, jb, NULL);
	double *rsd = (*env)->GetPrimitiveArrayCritical(env, jrsd, NULL);
	double *qty = (*env)->GetPrimitiveArrayCritical(env, jqty, NULL);
	int *k = (*env)->GetPrimitiveArrayCritical(env, jk, NULL);
	int *jpvt = (*env)->GetPrimitiveArrayCritical(env, jjpvt, NULL);
	double *qraux = (*env)->GetPrimitiveArrayCritical(env, jqraux, NULL);
	double *work = (*env)->GetPrimitiveArrayCritical(env, jwork, NULL);
	dqrls_(x, &n, &p, y, &ny, &tol, b, rsd, qty, k, jpvt, qraux, work);
	(*env)->ReleasePrimitiveArrayCritical(env, jx, x, 0);
	(*env)->ReleasePrimitiveArrayCritical(env, jy, y, 0);
	(*env)->ReleasePrimitiveArrayCritical(env, jb, b, 0);
	(*env)->ReleasePrimitiveArrayCritical(env, jrsd, rsd, 0);
	(*env)->ReleasePrimitiveArrayCritical(env, jqty, qty, 0);
	(*env)->ReleasePrimitiveArrayCritical(env, jk, k, 0);
	(*env)->ReleasePrimitiveArrayCritical(env, jjpvt, jpvt, 0);
	(*env)->ReleasePrimitiveArrayCritical(env, jqraux, qraux, 0);
	(*env)->ReleasePrimitiveArrayCritical(env, jwork, work, 0);
}
