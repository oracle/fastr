/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

static jmethodID Rf_matchMethodID;

void init_unique(JNIEnv *env) {

	Rf_matchMethodID = checkGetMethodID(env, UpCallsRFFIClass, "Rf_match", "(Ljava/lang/Object;Ljava/lang/Object;I)Ljava/lang/Object;", 0);
}

SEXP Rf_matchE(SEXP itable, SEXP ix, int nmatch, SEXP env)
{
	unimplemented("Rf_matchE");
}

/* used from other code, not here: */
SEXP Rf_match(SEXP itable, SEXP ix, int nmatch)
{
	TRACE(TARGppd, itable, ix, nmatch);
	JNIEnv *thisenv = getEnv();
	SEXP result = (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, Rf_matchMethodID, itable, ix, nmatch);
	return checkRef(thisenv, result);
}

