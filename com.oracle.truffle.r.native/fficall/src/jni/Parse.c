/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
#include <R_ext/Parse.h>

static jmethodID parseMethodID;
static jclass parseResultClass;
static jfieldID parseStatusFieldID;
static jfieldID parseExprFieldID;


void init_parse(JNIEnv *env) {
	parseMethodID = checkGetMethodID(env, UpCallsRFFIClass, "R_ParseVector", "(Ljava/lang/Object;ILjava/lang/Object;)Ljava/lang/Object;", 0);
	parseResultClass = checkFindClass(env, "com/oracle/truffle/r/ffi/impl/common/ParseResult");
	parseStatusFieldID = checkGetFieldID(env, parseResultClass, "parseStatus", "I", 0);
	parseExprFieldID = checkGetFieldID(env, parseResultClass, "expr", "Ljava/lang/Object;", 0);
}

SEXP R_ParseVector(SEXP text, int n, ParseStatus *z, SEXP srcfile) {
	JNIEnv *env = getEnv();
	jobject result = (*env)->CallObjectMethod(env, UpCallsRFFIObject, parseMethodID, text, n, srcfile);
	*z = (*env)->GetIntField(env, result, parseStatusFieldID);
    return (*env)->GetObjectField(env, result, parseExprFieldID);
}
