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

extern void ilaver_(int *major, int *minor, int *patch);

void call_lapack_ilaver(int* version) {
	int major;
	int minor;
	int patch;
	ilaver_(&major, &minor, &patch);
	version[0] = major;
	version[1] = minor;
	version[2] = patch;
}

extern int dgeev_(char *jobVL, char *jobVR, int *n, double *a, int *lda, double *wr, double *wi, double *vl, int *ldvl, double *vr, int *ldvr, double *work, int *lwork, int *info);

int call_lapack_dgeev(char jobVL, char jobVR, int n, double *a, int lda, double *wr, double *wi, double *vl, int ldvl, double *vr, int ldvr, double *work, int lwork) {
    int info;
    dgeev_(&jobVL, &jobVR, &n, a, &lda, wr, wi, vl, &ldvl, vr, &ldvr, work, &lwork, &info);
    return info;
}

extern int dgeqp3_(int *m, int *n, double *a, int *lda, int *jpvt, double *tau, double *work, int *lwork, int *info);


int call_lapack_dgeqp3(int m, int n, double *a, int lda, int *jpvt, double *tau, double *work, int lwork) {
    int info;
    dgeqp3_(&m, &n, a, &lda, jpvt, tau, work, &lwork, &info);
    return info;
}

extern int dormqr_(char *side, char *trans, int *m, int *n, int *k, double *a, int *lda, double *tau, double *c, int *ldc, double *work, int *lwork, int *info);

int call_lapack_dormqr(char side, char trans, int m, int n, int k, double *a, int lda, double *tau, double *c, int ldc, double *work, int lwork) {
    int info;
    dormqr_(&side, &trans, &m, &n, &k, a, &lda, tau, c, &ldc, work, &lwork, &info);
    return info;
}

extern int dtrtrs_(char *uplo, char *trans, char *diag, int *n, int *nrhs, double *a, int *lda, double *b, int *ldb, int *info);

int call_lapack_dtrtrs(char uplo, char trans, char diag, int n, int nrhs, double *a, int lda, double *b, int ldb) {
    int info;
    dtrtrs_(&uplo, &trans, &diag, &n, &nrhs, a, &lda, b, &ldb, &info);
    return info;
}

extern int dgetrf_(int *m, int *n, double *a, int *lda, int *ipiv, int *info);

int call_lapack_dgetrf(int m, int n, double *a, int lda, int *ipiv) {
    int info;
    dgetrf_(&m, &n, a, &lda, ipiv, &info);
    return info;
}

extern int dpotrf_(char *uplo, int *n, double *a, int *lda, int *info);

int call_lapack_dpotrf(char uplo, int n, double *a, int lda) {
    int info;
    dpotrf_(&uplo, &n, a, &lda, &info);
    return info;
}

extern int dpotri_(char *uplo, int *n, double *a, int *lda, int *info);

int call_lapack_dpotri(char uplo, int n, double *a, int lda) {
    int info;
    dpotri_(&uplo, &n, a, &lda, &info);
    return info;
}

extern int dpstrf_(char *uplo, int *n, double *a, int *lda, int *piv, int *rank, double *tol, double *work, int *info);

int call_lapack_dpstrf(char uplo, int n, double *a, int lda, int *piv, int *rank, double tol, double *work) {
    int info;
    dpstrf_(&uplo, &n, a, &lda, piv, rank, &tol, work, &info);
    return info;
}

extern int dgesv_(int *n, int *nrhs, double *a, int *lda, int *ipiv, double *b, int *ldb, int *info);

int call_lapack_dgesv(int n, int nrhs, double *a, int lda, int *ipiv, double *b, int ldb) {
    int info;
    dgesv_(&n, &nrhs, a, &lda, ipiv, b, &ldb, &info);
    return info;
}

extern double dlange_(char *norm, int *m, int *n, double *a, int *lda, double *work);

double call_lapack_dlange(char norm, int m, int n, double *a, int lda, double *work) {
    double info = dlange_(&norm, &m, &n, a, &lda, work);
    return info;
}

extern int dgecon_(char *norm, int *n, double *a, int *lda, double *anorm, double *rcond, double *work, int *iwork, int *info);

int call_lapack_dgecon(char norm, int n, double *a, int lda, double anorm, double *rcond, double *work, int *iwork) {
    int info;
    dgecon_(&norm, &n, a, &lda, &anorm, rcond, work, iwork, &info);
    return info;
}

extern int dsyevr_(char *jobz, char *range, char *uplo, int *n, double* a, int *lda, double *vl, double *vu, int *il, int *iu, double *abstol, int* m, double* w,
                double* z, int *ldz, int* isuppz, double* work, int *lwork, int* iwork, int *liwork, int* info);

int call_lapack_dsyevr(char jobz, char range, char uplo, int n, double *a, int lda, double vl, double vu, int il, int iu, double abstol, int *m, double *w,
		                    double *z, int ldz, int *isuppz, double *work, int lwork, int *iwork, int liwork) {
    int info;
    dsyevr_(&jobz, &range, &uplo, &n, a, &lda, &vl, &vu, &il, &iu, &abstol, m, w,
            z, &ldz, isuppz, work, &lwork, iwork, &liwork, &info);
    return info;
}
