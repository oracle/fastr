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
#ifndef RFFIUTILS_H
#define RFFIUTILS_H

#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include <limits.h>
#include <Rinternals.h>
#include <setjmp.h>
#include <Connections.h>

#define VALIDATE_REFS 1

JNIEnv *getEnv();
void setEnv(JNIEnv *env);

jclass checkFindClass(JNIEnv *env, const char *name);
jmethodID checkGetMethodID(JNIEnv *env, jclass klass, const char *name, const char *sig, int isStatic);
jfieldID checkGetFieldID(JNIEnv *env, jclass klass, const char *name, const char *sig, int isStatic);

// use for an unimplemented API function
void *unimplemented(char *msg);
// use for any fatal error
void fatalError(char *msg);
// makes a call to the VM with x as an argument (for debugger validation)
void validate(SEXP x);
// checks x against the list of global JNI refs, returning the global version if x matches (IsSameObject)
SEXP checkRef(JNIEnv *env, SEXP x);
// creates a global JNI global ref from x. If permanent is non-zero, calls to
// releaseGlobalRef are ignored and the global ref persists for the entire execution
// (used for the R global variables such as R_NilValue).
SEXP createGlobalRef(JNIEnv *env, SEXP x, int permanent);
// release a previously created JNI global ref
void releaseGlobalRef(JNIEnv *env, SEXP x);
// validate a JNI reference
void validateRef(JNIEnv *env, SEXP x, const char *msg);

// entering a top-level JNI call
void callEnter(JNIEnv *env, jmp_buf *error_exit);
// exiting a top-level JNI call
void callExit(JNIEnv *env);
// called by callExit to deallocate transient memory
void allocExit();

// returns the jmp_buf at the current call depth
jmp_buf *getErrorJmpBuf();

// Given the x denotes an R vector type, return a pointer to
// the data as a C array
void *getNativeArray(JNIEnv *env, SEXP x, SEXPTYPE type);
// Rare case where an operation changes the internal
// data and thus the old C array should be invalidated
void invalidateNativeArray(JNIEnv *env, SEXP oldObj);
// Should be called before up calling to arbitrary code, e.g. Rf_eval,
// to copy back the arrays into their Java counterparts
void updateNativeArrays(JNIEnv *env);

SEXP addGlobalRef(JNIEnv *env, SEXP obj, int permanent);

void init_utils(JNIEnv *env, jobject upCallsInstance);
void init_rmath(JNIEnv *env);
void init_variables(JNIEnv *env, jobjectArray initialValues);
void init_dynload(JNIEnv *env);
void init_internals(JNIEnv *env);
void init_random(JNIEnv *env);
void init_parse(JNIEnv *env);
void init_pcre(JNIEnv *env);
void init_c(JNIEnv *env);
void init_connections(JNIEnv *env);

void setEmbedded(void);

void setTempDir(JNIEnv *, jstring tempDir);

extern jclass UpCallsRFFIClass;
extern jclass JNIUpCallsRFFIImplClass;
extern jobject UpCallsRFFIObject;
extern FILE *traceFile;

// tracing/debugging support, set to 1 and recompile to enable
#define TRACE_UPCALLS 0    // trace upcalls
#define TRACE_REF_CACHE 0  // trace JNI reference cache
#define TRACE_NATIVE_ARRAYS 0     // trace generation of internal arrays
#define TRACE_ENABLED TRACE_UPCALLS || TRACE_REF_CACHE || TRACE_NATIVE_ARRAYS

#define TARGp "%s(%p)\n"
#define TARGpp "%s(%p, %p)\n"
#define TARGppp "%s(%p, %p, %p)\n"
#define TARGpd "%s(%p, %d)\n"
#define TARGppd "%s(%p, %p, %d)\n"
#define TARGs "%s(\"%s\")\n"
#define TARGps "%s(%p, \"%s\")\n"
#define TARGsdd "%s(\"%s\", %d, %d)\n"

#if TRACE_UPCALLS
#define TRACE(format, ...) fprintf(traceFile, format, __FUNCTION__, __VA_ARGS__)
#else
#define TRACE(format, ...)
#endif

#define _(Source) (Source)

// convert a string into a char*
jstring stringFromCharSXP(JNIEnv *thisenv, SEXP charsxp);
const char *stringToChars(JNIEnv *jniEnv, jstring string);


extern jmethodID INTEGER_MethodID;
extern jmethodID LOGICAL_MethodID;
extern jmethodID REAL_MethodID;
extern jmethodID RAW_MethodID;
extern jmethodID setCompleteMethodID;

extern int callDepth;

#endif /* RFFIUTILS_H */
