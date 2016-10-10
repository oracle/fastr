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

char *pcre_maketables();
void *pcre_compile(char * pattern, int options, char ** errorMessage, int *errOffset, char * tables);
int  pcre_exec(void * code, void *extra, char* subject, int subjectLength, int startOffset, int options, int *ovector, int ovecSize);

jclass JNI_PCRE_ResultClass;
jmethodID ResultClassConstructorID;

void init_pcre(JNIEnv *env) {
	JNI_PCRE_ResultClass = checkFindClass(env, "com/oracle/truffle/r/runtime/ffi/PCRERFFI$Result");
	ResultClassConstructorID = checkGetMethodID(env, JNI_PCRE_ResultClass, "<init>", "(JLjava/lang/String;I)V", 0);
}

JNIEXPORT jlong JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jni_JNI_1PCRE_nativeMaketables(JNIEnv *env, jclass c) {
	return (jlong) pcre_maketables();
}

JNIEXPORT jobject JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jni_JNI_1PCRE_nativeCompile(JNIEnv *env, jclass c, jstring pattern, jint options, jlong tables) {
	const char *patternChars = (*env)->GetStringUTFChars(env, pattern, NULL);
	char *errorMessage;
	int errOffset;
	void *pcre_result = pcre_compile(patternChars, options, &errorMessage, &errOffset, (char*) tables);
	jstring stringErrorMessage = NULL;
	if (pcre_result == NULL) {
	    stringErrorMessage = (*env)->NewStringUTF(env, errorMessage);
	}
	jobject result = (*env)->NewObject(env, JNI_PCRE_ResultClass, ResultClassConstructorID, pcre_result, stringErrorMessage, errOffset);
	return result;
}

JNIEXPORT jint JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jni_JNI_1PCRE_nativeExec(JNIEnv *env, jclass c,jlong code, jlong extra, jstring subject,
		jint startOffset, jint options, jintArray ovector, jint ovectorLen) {
	const char *subjectChars = (*env)->GetStringUTFChars(env, subject, NULL);
	int subjectLength = (*env)->GetStringUTFLength(env, subject);
	int* ovectorElems = (*env)->GetIntArrayElements(env, ovector, NULL);

	int rc = pcre_exec(code, extra, subjectChars, subjectLength, startOffset, options,
			ovectorElems, ovectorLen);
	(*env)->ReleaseIntArrayElements(env, ovector, ovectorElems, 0);
	(*env)->ReleaseStringUTFChars(env, subject, subjectChars);
	return rc;
}
