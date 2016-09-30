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

// These functions, while defined in JNI_Base are stored in a seperate library, jniboot
// in order to be able to bootstrap the system as libR has to be loaded using these functions.

#include <dlfcn.h>
#include <jni.h>

// It seems that an internal (JVM) dlsym call can occur between a call to these functions and dlerror
// (probably resolving the JNI dlerror symbol, so we capture it here (N.B. depends on single
// threaded limitation).

static char *last_dlerror;

JNIEXPORT jlong JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jni_JNI_1Base_native_1dlopen(JNIEnv *env, jclass c, jstring jpath, jboolean local, jboolean now) {
    const char *path = (*env)->GetStringUTFChars(env, jpath, NULL);
    int flags = (local ? RTLD_LOCAL : RTLD_GLOBAL) | (now ? RTLD_NOW : RTLD_LAZY);
    void *handle = dlopen(path, flags);
    last_dlerror = dlerror();
    (*env)->ReleaseStringUTFChars(env, jpath, path);
    return (jlong) handle;
}

JNIEXPORT jlong JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jni_JNI_1Base_native_1dlsym(JNIEnv *env, jclass c, jlong handle, jstring jsymbol) {
    const char *symbol = (*env)->GetStringUTFChars(env, jsymbol, NULL);
    void *address = dlsym((void *)handle, symbol);
	last_dlerror = dlerror();
    (*env)->ReleaseStringUTFChars(env, jsymbol, symbol);
    return (jlong) address;
}

JNIEXPORT jint JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jni_JNI_1Base_native_1dlclose(JNIEnv *env, jclass c, jlong handle) {
    int rc = dlclose((void *)handle);
    return rc;
}

JNIEXPORT jobject JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jni_JNI_1Base_native_1dlerror(JNIEnv *env, jclass c) {
    char *err = last_dlerror;
    if (err == NULL) {
    	return NULL;
    } else {
    	return (*env)->NewStringUTF(env, err);
    }
}

