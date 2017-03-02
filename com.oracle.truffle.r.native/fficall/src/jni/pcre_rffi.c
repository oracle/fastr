/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1997-2015,  The R Core Team
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

#include <rffiutils.h>

#define PCRE_INFO_CAPTURECOUNT       2
#define PCRE_INFO_NAMEENTRYSIZE      7
#define PCRE_INFO_NAMECOUNT          8
#define PCRE_INFO_NAMETABLE          9

char *pcre_maketables();
void *pcre_compile(char *pattern, int options, char **errorMessage, int *errOffset, char *tables);
int  pcre_exec(void *code, void *extra, char* subject, int subjectLength, int startOffset, int options, int *ovector, int ovecSize);
int pcre_fullinfo(void *code, void *extra, int what, void *where);
void pcre_free(void *code);

jclass JNI_PCRE_ResultClass;
jmethodID ResultClassConstructorID;

void init_pcre(JNIEnv *env) {
	JNI_PCRE_ResultClass = checkFindClass(env, "com/oracle/truffle/r/runtime/ffi/PCRERFFI$Result");
	ResultClassConstructorID = checkGetMethodID(env, JNI_PCRE_ResultClass, "<init>", "(JLjava/lang/String;I)V", 0);
}

JNIEXPORT jlong JNICALL
Java_com_oracle_truffle_r_ffi_impl_jni_JNI_1PCRE_nativeMaketables(JNIEnv *env, jclass c) {
	return (jlong) pcre_maketables();
}

JNIEXPORT jobject JNICALL
Java_com_oracle_truffle_r_ffi_impl_jni_JNI_1PCRE_nativeCompile(JNIEnv *env, jclass c, jstring pattern, jint options, jlong tables) {
	const char *patternChars = (*env)->GetStringUTFChars(env, pattern, NULL);
	char *errorMessage;
	int errOffset;
	void *pcre_result = pcre_compile((char *) patternChars, options, &errorMessage, &errOffset, (char*) tables);
	jstring stringErrorMessage = NULL;
	if (pcre_result == NULL) {
	    stringErrorMessage = (*env)->NewStringUTF(env, errorMessage);
	}
	jobject result = (*env)->NewObject(env, JNI_PCRE_ResultClass, ResultClassConstructorID, pcre_result, stringErrorMessage, errOffset);
	return result;
}

JNIEXPORT jint JNICALL
Java_com_oracle_truffle_r_ffi_impl_jni_JNI_1PCRE_nativeGetCaptureCount(JNIEnv *env, jclass c, jlong code, jlong extra) {
    int captureCount;
	int rc = pcre_fullinfo((void *)code, (void *)extra, PCRE_INFO_CAPTURECOUNT, &captureCount);
    return rc < 0 ? rc : captureCount;
}

JNIEXPORT jint JNICALL
Java_com_oracle_truffle_r_ffi_impl_jni_JNI_1PCRE_nativeGetCaptureNames(JNIEnv *env, jclass c, jlong code, jlong extra, jobjectArray ret) {
    int nameCount;
    int nameEntrySize;
    char* nameTable;
    int res;
	res = pcre_fullinfo((void *) code, (void *) extra, PCRE_INFO_NAMECOUNT, &nameCount);
    if (res < 0) {
        return res;
    }
    res = pcre_fullinfo((void *) code, (void *) extra, PCRE_INFO_NAMEENTRYSIZE, &nameEntrySize);
    if (res < 0) {
        return res;
    }
	res = pcre_fullinfo((void *) code,(void *) extra, PCRE_INFO_NAMETABLE, &nameTable);
    if (res < 0) {
        return res;
    }
    // from GNU R's grep.c
	for(int i = 0; i < nameCount; i++) {
	    char* entry = nameTable + nameEntrySize * i;
	    int captureNum = (entry[0] << 8) + entry[1] - 1;
        (*env)->SetObjectArrayElement(env, ret, captureNum, (*env)->NewStringUTF(env, entry + 2));
    }
    return res;
}

JNIEXPORT jint JNICALL
Java_com_oracle_truffle_r_ffi_impl_jni_JNI_1PCRE_nativeExec(JNIEnv *env, jclass c, jlong code, jlong extra, jstring subject,
	jint startOffset, jint options, jintArray ovector, jint ovectorLen) {
	const char *subjectChars = (*env)->GetStringUTFChars(env, subject, NULL);
	int subjectLength = (*env)->GetStringUTFLength(env, subject);
	int* ovectorElems = (*env)->GetIntArrayElements(env, ovector, NULL);

	int rc = pcre_exec((void *) code,(void *) extra, (char *) subjectChars, subjectLength, startOffset, options,
			ovectorElems, ovectorLen);
	(*env)->ReleaseIntArrayElements(env, ovector, ovectorElems, 0);
	(*env)->ReleaseStringUTFChars(env, subject, subjectChars);
	return rc;
}
