/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
#include <string.h>
#include <glob.h>
#include <jni.h>

static jmethodID addPathID = 0;

JNIEXPORT void JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jnr_JNIGlob_doglob(JNIEnv *env, jobject obj, jstring pattern) {
	glob_t globstruct;

	if (addPathID == 0) {
		addPathID = (*env)->GetMethodID(env, (*env)->GetObjectClass(env, obj), "addPath", "(Ljava/lang/String;)V");
	}

	const char *patternChars = (*env)->GetStringUTFChars(env, pattern, NULL);
	int rc = glob(patternChars, 0, NULL, &globstruct);
	if (rc == 0) {
		int i;
		for (i = 0; i < globstruct.gl_pathc; i++) {
			char *path = globstruct.gl_pathv[i];
			jstring pathString = (*env)->NewStringUTF(env, path);
			(*env)->CallVoidMethod(env, obj, addPathID, pathString);
		}
	}
}
