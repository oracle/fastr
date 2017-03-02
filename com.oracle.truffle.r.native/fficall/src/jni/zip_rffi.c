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

extern int compress(char *dest, long *destlen, char *source, long sourcelen);
extern int uncompress(char *dest, long *destlen, char *source, long sourcelen);

JNIEXPORT jint JNICALL
Java_com_oracle_truffle_r_ffi_impl_jni_JNI_1Zip_native_1compress(JNIEnv *env, jclass c,
		jbyteArray jdest, jlong destlen, jbyteArray jsource, jlong sourcelen) {
    char *dest = (*env)->GetPrimitiveArrayCritical(env, jdest, NULL);
    char *source = (*env)->GetPrimitiveArrayCritical(env, jsource, NULL);
    int rc = compress(dest, &destlen, source, sourcelen);
    (*env)->ReleasePrimitiveArrayCritical(env, jdest, dest, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, jsource, source, JNI_ABORT);
    return rc;
}

JNIEXPORT jint JNICALL
Java_com_oracle_truffle_r_ffi_impl_jni_JNI_1Zip_native_1uncompress(JNIEnv *env, jclass c,
		jbyteArray jdest, jlong destlen, jbyteArray jsource, jlong sourcelen) {
    char *dest = (*env)->GetPrimitiveArrayCritical(env, jdest, NULL);
    char *source = (*env)->GetPrimitiveArrayCritical(env, jsource, NULL);
    int rc = uncompress(dest, &destlen, source, sourcelen);
    (*env)->ReleasePrimitiveArrayCritical(env, jdest, dest, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, jsource, source, JNI_ABORT);
	return rc;
}
