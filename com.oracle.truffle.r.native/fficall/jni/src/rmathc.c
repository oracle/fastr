/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
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
#include "rffiutils.h"

void init_rmath(JNIEnv *env) {

}

double Rf_choose(double x, double y) {
    unimplemented("Rf_choose");
    return 0;
}

double Rf_lchoose(double x, double y) {
    unimplemented("Rf_lchoose");
    return 0;
}

double Rf_dbeta(double x, double y, double z, int w) {
    unimplemented("Rf_dbeta");
    return 0;
}

double Rf_pbeta(double x, double y, double z, int w, int v) {
    unimplemented("Rf_pbeta");
    return 0;
}

double Rf_qbeta(double x, double y, double z, int w, int v) {
    unimplemented("Rf_qbeta");
    return 0;
}

double Rf_rbeta(double x, double y) {
    unimplemented("Rf_rbeta");
    return 0;
}

double Rf_dnorm4(double a, double b, double c, int d) {
    unimplemented("Rf_dnorm4");
    return 0;
}

double Rf_pnorm5(double x, double y, double z, int w, int v) {
    unimplemented("Rf_pnorm5");
    return 0;
}

int Rf_imax2(int x, int y) {
    return x > y ? x : y;
}

int Rf_imin2(int x, int y) {
    return x > y ? y : x;
}

double Rf_fmax2(double x, double y) {
    return x > y ? x : y;
}

double Rf_fmin2(double x, double y) {
    return x > y ? y : x;
}

double Rf_sign(double x) {
    unimplemented("Rf_sign");
    return 0;
}

double Rf_runif(double x, double y) {
    unimplemented("Rf_runif");
    return 0;
}

double Rf_gammafn(double x) {
    unimplemented("Rf_gammafn");
    return 0;
}

double Rf_lgammafn(double x) {
    unimplemented("Rf_lgammafn");
    return 0;
}

double Rf_lgammafn_sign(double x, int*y) {
    unimplemented("Rf_lgammafn_sign");
    return 0;
}

double R_pow(double x, double y) {
    unimplemented("R_pow");
    return 0;
}

double Rf_dchisq(double x, double y, int z) {
    unimplemented("Rf_dchisq");
    return 0;
}

double Rf_pchisq(double x, double y, int z, int w) {
    unimplemented("Rf_pchisq");
    return 0;
}

double Rf_qchisq(double x, double y, int z, int w) {
    unimplemented("Rf_qchisq");
    return 0;
}

double Rf_rchisq(double x) {
    unimplemented("Rf_rchisq");
    return 0;
}

double Rf_dexp(double x, double y, int z) {
    unimplemented("Rf_dexp");
    return 0;
}
