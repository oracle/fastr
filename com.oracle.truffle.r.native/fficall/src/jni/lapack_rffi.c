/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
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

extern void ilaver_(int *major, int *minor, int *patch);

JNIEXPORT void JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jni_JNI_1Lapack_native_1ilaver(JNIEnv *env, jclass klass, jintArray jversion) {
	int major;
	int minor;
	int patch;
	ilaver_(&major, &minor, &patch);
	int *version = (*env)->GetPrimitiveArrayCritical(env, jversion, NULL);
	version[0] = major;
	version[1] = minor;
	version[2] = patch;
	(*env)->ReleasePrimitiveArrayCritical(env, jversion, version, 0);
}


extern int dgeev_(char *jobVL, char *jobVR, int *n, double *a, int *lda, double *wr, double *wi, double *vl, int *ldvl, double *vr, int *ldvr, double *work, int *lwork, int *info);

JNIEXPORT jint JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jni_JNI_1Lapack_native_1dgeev(JNIEnv *env, jclass klass,
        char jobVL, char jobVR, int n, jdoubleArray ja, int lda, jdoubleArray jwr, jdoubleArray jwi, jdoubleArray jvl, int ldvl, jdoubleArray jvr, int ldvr, jdoubleArray jwork, int lwork) {
    double *a = (*env)->GetPrimitiveArrayCritical(env, ja, NULL);
    double *wr = (*env)->GetPrimitiveArrayCritical(env, jwr, NULL);
    double *wi = (*env)->GetPrimitiveArrayCritical(env, jwi, NULL);
    double *vl = jvl == NULL ? NULL : (*env)->GetPrimitiveArrayCritical(env, jvl, NULL);
    double *vr = jvr == NULL ? NULL : (*env)->GetPrimitiveArrayCritical(env, jvr, NULL);
    double *work = (*env)->GetPrimitiveArrayCritical(env, jwork, NULL);
    int info;
    dgeev_(&jobVL, &jobVR, &n, a, &lda, wr, wi, vl, &ldvl, vr, &ldvr, work, &lwork, &info);
    (*env)->ReleasePrimitiveArrayCritical(env, ja, a, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, jwr, wr, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, jwi, wi, 0);
    if (jvl != NULL) (*env)->ReleasePrimitiveArrayCritical(env, jvl, vl, 0);
    if (jvr != NULL) (*env)->ReleasePrimitiveArrayCritical(env, jvr, vr, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, jwork, work, 0);
    return info;
}

extern int dgeqp3_(int *m, int *n, double *a, int *lda, int *jpvt, double *tau, double *work, int *lwork, int *info);

JNIEXPORT jint JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jni_JNI_1Lapack_native_1dgeqp3(JNIEnv *env, jclass klass,
        int m, int n, jdoubleArray ja, int lda, jintArray jjpvt, jdoubleArray jtau, jdoubleArray jwork, int lwork) {
    double *a = (*env)->GetPrimitiveArrayCritical(env, ja, NULL);
    int *jpvt = (*env)->GetPrimitiveArrayCritical(env, jjpvt, NULL);
    double *tau = (*env)->GetPrimitiveArrayCritical(env, jtau, NULL);
    double *work = (*env)->GetPrimitiveArrayCritical(env, jwork, NULL);
    int info;
    dgeqp3_(&m, &n, a, &lda, jpvt, tau, work, &lwork, &info);
    (*env)->ReleasePrimitiveArrayCritical(env, ja, a, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, jjpvt, jpvt, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, jtau, tau, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, jwork, work, 0);
    return info;
}

extern int dormqr_(char *side, char *trans, int *m, int *n, int *k, double *a, int *lda, double *tau, double *c, int *ldc, double *work, int *lwork, int *info);

JNIEXPORT jint JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jni_JNI_1Lapack_native_1dormqr(JNIEnv *env, jclass klass,
        char side, char trans, int m, int n, int k, jdoubleArray ja, int lda, jdoubleArray jtau, jdoubleArray jc, int ldc, jdoubleArray jwork, int lwork) {
    double *a = (*env)->GetPrimitiveArrayCritical(env, ja, NULL);
    double *tau = (*env)->GetPrimitiveArrayCritical(env, jtau, NULL);
    double *c = (*env)->GetPrimitiveArrayCritical(env, jc, NULL);
    double *work = (*env)->GetPrimitiveArrayCritical(env, jwork, NULL);
    int info;
    dormqr_(&side, &trans, &m, &n, &k, a, &lda, tau, c, &ldc, work, &lwork, &info);
    (*env)->ReleasePrimitiveArrayCritical(env, ja, a, JNI_ABORT);
    (*env)->ReleasePrimitiveArrayCritical(env, jtau, tau, JNI_ABORT);
    (*env)->ReleasePrimitiveArrayCritical(env, jc, c, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, jwork, work, 0);
    return info;
}

extern int dtrtrs_(char *uplo, char *trans, char *diag, int *n, int *nrhs, double *a, int *lda, double *b, int *ldb, int *info);

JNIEXPORT jint JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jni_JNI_1Lapack_native_1dtrtrs(JNIEnv *env, jclass klass,
        char uplo, char trans, char diag, int n, int nrhs, jdoubleArray ja, int lda, jdoubleArray jb, int ldb) {
    double *a = (*env)->GetPrimitiveArrayCritical(env, ja, NULL);
    double *b = (*env)->GetPrimitiveArrayCritical(env, jb, NULL);
    int info;
    dtrtrs_(&uplo, &trans, &diag, &n, &nrhs, a, &lda, b, &ldb, &info);
    (*env)->ReleasePrimitiveArrayCritical(env, ja, a, JNI_ABORT);
    (*env)->ReleasePrimitiveArrayCritical(env, jb, b, 0);
    return info;
}

extern int dgetrf_(int *m, int *n, double *a, int *lda, int *ipiv, int *info);

JNIEXPORT jint JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jni_JNI_1Lapack_native_1dgetrf(JNIEnv *env, jclass klass,
        int m, int n, jdoubleArray ja, int lda, jintArray jipiv) {
    double *a = (*env)->GetPrimitiveArrayCritical(env, ja, NULL);
    int *ipiv = (*env)->GetPrimitiveArrayCritical(env, jipiv, NULL);
    int info;
    dgetrf_(&m, &n, a, &lda, ipiv, &info);
    (*env)->ReleasePrimitiveArrayCritical(env, ja, a, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, jipiv, ipiv, 0);
    return info;
}

extern int dpotrf_(char *uplo, int *n, double *a, int *lda, int *info);

JNIEXPORT jint JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jni_JNI_1Lapack_native_1dpotrf(JNIEnv *env, jclass klass,
        char uplo, int n, jdoubleArray ja, int lda) {
    double *a = (*env)->GetPrimitiveArrayCritical(env, ja, NULL);
    int info;
    dpotrf_(&uplo, &n, a, &lda, &info);
    (*env)->ReleasePrimitiveArrayCritical(env, ja, a, 0);
    return info;
}

extern int dpotri_(char *uplo, int *n, double *a, int *lda, int *info);

JNIEXPORT jint JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jni_JNI_1Lapack_native_1dpotri(JNIEnv *env, jclass klass,
        char uplo, int n, jdoubleArray ja, int lda) {
    double *a = (*env)->GetPrimitiveArrayCritical(env, ja, NULL);
    int info;
    dpotri_(&uplo, &n, a, &lda, &info);
    (*env)->ReleasePrimitiveArrayCritical(env, ja, a, 0);
    return info;
}

extern int dpstrf_(char *uplo, int *n, double *a, int *lda, int *piv, int *rank, double *tol, double *work, int *info);

JNIEXPORT jint JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jni_JNI_1Lapack_native_1dpstrf(JNIEnv *env, jclass klass,
        char uplo, int n, jdoubleArray ja, int lda, jintArray jpiv, jintArray jrank, double tol, jdoubleArray jwork) {
    double *a = (*env)->GetPrimitiveArrayCritical(env, ja, NULL);
    int *piv = (*env)->GetPrimitiveArrayCritical(env, jpiv, NULL);
    int *rank = (*env)->GetPrimitiveArrayCritical(env, jrank, NULL);
    double *work = (*env)->GetPrimitiveArrayCritical(env, jwork, NULL);
    int info;
    dpstrf_(&uplo, &n, a, &lda, piv, rank, &tol, work, &info);
    (*env)->ReleasePrimitiveArrayCritical(env, ja, a, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, jpiv, piv, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, jrank, rank, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, jwork, work, 0);
    return info;
}

extern int dgesv_(int *n, int *nrhs, double *a, int *lda, int *ipiv, double *b, int *ldb, int *info);

JNIEXPORT jint JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jni_JNI_1Lapack_native_1dgesv(JNIEnv *env, jclass klass,
        int n, int nrhs, jdoubleArray ja, int lda, jintArray jipiv, jdoubleArray jb, int ldb) {
    double *a = (*env)->GetPrimitiveArrayCritical(env, ja, NULL);
    int *ipiv = (*env)->GetPrimitiveArrayCritical(env, jipiv, NULL);
    double *b = (*env)->GetPrimitiveArrayCritical(env, jb, NULL);
    int info;
    dgesv_(&n, &nrhs, a, &lda, ipiv, b, &ldb, &info);
    (*env)->ReleasePrimitiveArrayCritical(env, ja, a, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, jipiv, ipiv, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, jb, b, 0);
    return info;
}

extern double dlange_(char *norm, int *m, int *n, double *a, int *lda, double *work);

JNIEXPORT jdouble JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jni_JNI_1Lapack_native_1dlange(JNIEnv *env, jclass klass,
        char norm, int m, int n, jdoubleArray ja, int lda, jdoubleArray jwork) {
    double *a = (*env)->GetPrimitiveArrayCritical(env, ja, NULL);
    double *work = jwork == NULL ? NULL : (*env)->GetPrimitiveArrayCritical(env, jwork, NULL);
    double info = dlange_(&norm, &m, &n, a, &lda, work);
    (*env)->ReleasePrimitiveArrayCritical(env, ja, a, JNI_ABORT);
    if (jwork != NULL) (*env)->ReleasePrimitiveArrayCritical(env, jwork, work, JNI_ABORT);
    return info;
}

extern int dgecon_(char *norm, int *n, double *a, int *lda, double *anorm, double *rcond, double *work, int *iwork, int *info);

JNIEXPORT jint JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jni_JNI_1Lapack_native_1dgecon(JNIEnv *env, jclass klass,
        char norm, int n, jdoubleArray ja, int lda, double anorm, jdoubleArray jrcond, jdoubleArray jwork, jintArray jiwork) {
    double *a = (*env)->GetPrimitiveArrayCritical(env, ja, NULL);
    double *rcond = (*env)->GetPrimitiveArrayCritical(env, jrcond, NULL);
    double *work = (*env)->GetPrimitiveArrayCritical(env, jwork, NULL);
    int *iwork = (*env)->GetPrimitiveArrayCritical(env, jiwork, NULL);
    int info;
    dgecon_(&norm, &n, a, &lda, &anorm, rcond, work, iwork, &info);
    (*env)->ReleasePrimitiveArrayCritical(env, ja, a, JNI_ABORT);
    (*env)->ReleasePrimitiveArrayCritical(env, jrcond, rcond, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, jwork, work, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, jiwork, iwork, 0);
    return info;
}

extern int dsyevr_(char *jobz, char *range, char *uplo, int *n, double* a, int *lda, double *vl, double *vu, int *il, int *iu, double *abstol, int* m, double* w,
                double* z, int *ldz, int* isuppz, double* work, int *lwork, int* iwork, int *liwork, int* info);

JNIEXPORT jint JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jni_JNI_1Lapack_native_1dsyevr(JNIEnv *env, jclass klass,
		char jobz, char range, char uplo, int n, jdoubleArray ja, int lda, double vl, double vu, int il, int iu, double abstol, jintArray jm, jdoubleArray jw,
		                    jdoubleArray jz, int ldz, jintArray jisuppz, jdoubleArray jwork, int lwork, jintArray jiwork, int liwork) {
    double *a = (*env)->GetPrimitiveArrayCritical(env, ja, NULL);
    int *m = (*env)->GetPrimitiveArrayCritical(env, jm, NULL);
    double *w = (*env)->GetPrimitiveArrayCritical(env, jw, NULL);
    double *z = jz == NULL ? NULL : (*env)->GetPrimitiveArrayCritical(env, jz, NULL);
    int *isuppz = (*env)->GetPrimitiveArrayCritical(env, jisuppz, NULL);
    double *work = (*env)->GetPrimitiveArrayCritical(env, jwork, NULL);
    int *iwork = (*env)->GetPrimitiveArrayCritical(env, jiwork, NULL);
    int info;
    dsyevr_(&jobz, &range, &uplo, &n, a, &lda, &vl, &vu, &il, &iu, &abstol, m, w,
            z, &ldz, isuppz, work, &lwork, iwork, &liwork, &info);
    (*env)->ReleasePrimitiveArrayCritical(env, ja, a, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, jm, m, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, jw, w, 0);
    if (jz != NULL) (*env)->ReleasePrimitiveArrayCritical(env, jz, z, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, jisuppz, isuppz, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, jwork, work, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, jiwork, iwork, 0);
    return info;
}
