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

typedef void (*fft_factor)(int n, int *pmaxf, int *pmaxp);
typedef Rboolean (*fft_work)(double *a, int nseg, int n, int nspn, int isn,
		  double *work, int *iwork);

JNIEXPORT void JNICALL
Java_com_oracle_truffle_r_ffi_impl_jni_JNI_1Stats_native_1fft_1factor(JNIEnv *env, jclass c, jlong address,
		jint n, jintArray jpmaxf, jintArray jpmaxp) {
	fft_factor f = (fft_factor) address;
	int *pmaxf = (*env)->GetPrimitiveArrayCritical(env, jpmaxf, NULL);
	int *pmaxp = (*env)->GetPrimitiveArrayCritical(env, jpmaxp, NULL);
	f(n, pmaxp, pmaxf);
	(*env)->ReleasePrimitiveArrayCritical(env, jpmaxf, pmaxf, 0);
	(*env)->ReleasePrimitiveArrayCritical(env, jpmaxp, pmaxp, 0);
}

JNIEXPORT int JNICALL
Java_com_oracle_truffle_r_ffi_impl_jni_JNI_1Stats_native_1fft_1work(JNIEnv *env, jclass c, jlong address,
		jdoubleArray ja, int nseg, int n, int nsps, int isn, jdoubleArray jwork, jintArray jiwork) {
	fft_work f = (fft_work) address;
	double *a = (*env)->GetPrimitiveArrayCritical(env, ja, NULL);
	double *work = (*env)->GetPrimitiveArrayCritical(env, jwork, NULL);
	int *iwork = (*env)->GetPrimitiveArrayCritical(env, jiwork, NULL);
	int res = f(a, nseg, n, nsps, isn, work, iwork);
	(*env)->ReleasePrimitiveArrayCritical(env, ja, a, 0);
	(*env)->ReleasePrimitiveArrayCritical(env, jwork, work, JNI_ABORT);
	(*env)->ReleasePrimitiveArrayCritical(env, jiwork, iwork, JNI_ABORT);
	return res;
}
