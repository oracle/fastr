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

// JNI implementation of BaseRFFI

#include <rffiutils.h>

#include <sys/types.h>
#include <unistd.h>
#include <sys/stat.h>
#include <glob.h>
#include <sys/utsname.h>

#include <errno.h>

JNIEXPORT jint JNICALL
Java_com_oracle_truffle_r_ffi_impl_jni_JNI_1Base_native_1getpid(JNIEnv *env, jclass c) {
	pid_t pid = getpid();
	return (jint) pid;
}

JNIEXPORT jint JNICALL
Java_com_oracle_truffle_r_ffi_impl_jni_JNI_1Base_native_1getwd(JNIEnv *env, jclass c, jbyteArray jdest, int len) {
    char *dest = (*env)->GetPrimitiveArrayCritical(env, jdest, NULL);
    char *r = getcwd(dest, len);
    if (r == NULL) return 0;
    (*env)->ReleasePrimitiveArrayCritical(env, jdest, dest, 0);
    return 1;
}

JNIEXPORT jint JNICALL
Java_com_oracle_truffle_r_ffi_impl_jni_JNI_1Base_native_1setwd(JNIEnv *env, jclass c, jstring jdir) {
    const char *dir = (*env)->GetStringUTFChars(env, jdir, NULL);
    int rc = chdir(dir);
    (*env)->ReleaseStringUTFChars(env, jdir, dir);
    return rc;
}

JNIEXPORT jint JNICALL
Java_com_oracle_truffle_r_ffi_impl_jni_JNI_1Base_native_1mkdtemp(JNIEnv *env, jclass c, jbyteArray jtemplate) {
    char *template = (char*) (*env)->GetByteArrayElements(env, jtemplate, NULL);
    char *r = mkdtemp(template);
    int rc = 1;
    if (r == NULL) {
    	// printf("mkdtemp errno: %d\n", errno);
    	rc = 0;
    }
    (*env)->ReleaseByteArrayElements(env, jtemplate, (jbyte*) template, rc == 1 ? 0 : JNI_ABORT);
    return rc;
}

JNIEXPORT jint JNICALL
Java_com_oracle_truffle_r_ffi_impl_jni_JNI_1Base_native_1mkdir(JNIEnv *env, jclass c, jstring jdir, jint mode) {
    const char *dir = (*env)->GetStringUTFChars(env, jdir, NULL);
    int rc = mkdir(dir, mode);
    (*env)->ReleaseStringUTFChars(env, jdir, dir);
    return rc;
}

JNIEXPORT jint JNICALL
Java_com_oracle_truffle_r_ffi_impl_jni_JNI_1Base_native_1chmod(JNIEnv *env, jclass c, jstring jdir, jint mode) {
    const char *dir = (*env)->GetStringUTFChars(env, jdir, NULL);
    int rc = chmod(dir, mode);
    (*env)->ReleaseStringUTFChars(env, jdir, dir);
    return rc;
}

JNIEXPORT jlong JNICALL
Java_com_oracle_truffle_r_ffi_impl_jni_JNI_1Base_native_1strtol(JNIEnv *env, jclass c, jstring js, jint base, jintArray jerrno) {
    const char *s = (*env)->GetStringUTFChars(env, js, NULL);
    jlong rc = strtol(s, NULL, base);
    if (errno) {
    	int *cerrno = (*env)->GetPrimitiveArrayCritical(env, jerrno, NULL);
    	*cerrno = errno;
    	(*env)->ReleasePrimitiveArrayCritical(env, jerrno, cerrno, 0);
    }
    (*env)->ReleaseStringUTFChars(env, js, s);
    return rc;
}

JNIEXPORT jstring JNICALL
Java_com_oracle_truffle_r_ffi_impl_jni_JNI_1Base_native_1readlink(JNIEnv *env, jclass c, jstring jpath, jintArray jerrno) {
    const char *path = (*env)->GetStringUTFChars(env, jpath, NULL);
    char buf[4096];
    int len = readlink(path, buf, 4096);
    jstring result = NULL;
    if (len == -1) {
    	int *cerrno = (*env)->GetPrimitiveArrayCritical(env, jerrno, NULL);
    	*cerrno = errno;
    	(*env)->ReleasePrimitiveArrayCritical(env, jerrno, cerrno, 0);
    } else {
    	buf[len] = 0;
    	result = (*env)->NewStringUTF(env, buf);
    }
    (*env)->ReleaseStringUTFChars(env, jpath, path);
    return result;
}


static jmethodID addPathID = 0;

JNIEXPORT void JNICALL
Java_com_oracle_truffle_r_ffi_impl_jni_JNI_1Glob_doglob(JNIEnv *env, jobject obj, jstring pattern) {
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

JNIEXPORT void JNICALL
Java_com_oracle_truffle_r_ffi_impl_jni_JNI_1UtsName_getutsname(JNIEnv *env, jobject obj) {
	struct utsname name;

	uname(&name);
	jstring sysname = (*env)->NewStringUTF(env, name.sysname);
	jstring release = (*env)->NewStringUTF(env, name.release);
	jstring version = (*env)->NewStringUTF(env, name.version);
	jstring machine = (*env)->NewStringUTF(env, name.machine);
	jstring nodename = (*env)->NewStringUTF(env, name.nodename);

	jclass klass = (*env)->GetObjectClass(env, obj);

	jfieldID sysnameId = checkGetFieldID(env, klass, "sysname", "Ljava/lang/String;", 0);
	jfieldID releaseId = checkGetFieldID(env, klass, "release", "Ljava/lang/String;", 0);
	jfieldID versionId = checkGetFieldID(env, klass, "version", "Ljava/lang/String;", 0);
	jfieldID machineId = checkGetFieldID(env, klass, "machine", "Ljava/lang/String;", 0);
	jfieldID nodenameId = checkGetFieldID(env, klass, "nodename", "Ljava/lang/String;", 0);

	(*env)->SetObjectField(env, obj, sysnameId, sysname);
	(*env)->SetObjectField(env, obj, releaseId, release);
	(*env)->SetObjectField(env, obj, versionId, version);
	(*env)->SetObjectField(env, obj, machineId, machine);
	(*env)->SetObjectField(env, obj, nodenameId, nodename);

}
