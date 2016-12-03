/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

static jmethodID GetRNGstate_MethodID;
static jmethodID PutRNGstate_MethodID;
static jmethodID UnifRand_MethodID;

void init_random(JNIEnv *env) {
	GetRNGstate_MethodID = checkGetMethodID(env, UpCallsRFFIClass, "GetRNGstate", "()V", 0);
	PutRNGstate_MethodID = checkGetMethodID(env, UpCallsRFFIClass, "PutRNGstate", "()V", 0);
	UnifRand_MethodID = checkGetMethodID(env, UpCallsRFFIClass, "unif_rand", "()D", 0);
}

void GetRNGstate() {
	JNIEnv *thisenv = getEnv();
	(*thisenv)->CallVoidMethod(thisenv, UpCallsRFFIObject, GetRNGstate_MethodID);
}

void PutRNGstate() {
	JNIEnv *thisenv = getEnv();
	(*thisenv)->CallVoidMethod(thisenv, UpCallsRFFIObject, PutRNGstate_MethodID);
}

double unif_rand() {
	JNIEnv *thisenv = getEnv();
	return (*thisenv)->CallDoubleMethod(thisenv, UpCallsRFFIObject, UnifRand_MethodID);
}

double norm_rand() {
	unimplemented("norm_rand");
	return 0;
}

double exp_rand() {
	unimplemented("exp_rand");
	return 0;
}
