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
#ifndef RFFIUTILS_H
#define RFFIUTILS_H

#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include <limits.h>
#include <Rinternals.h>
#include <setjmp.h>

#define VALIDATE_REFS 1

JNIEnv *getEnv();
void setEnv(JNIEnv *env);

jclass checkFindClass(JNIEnv *env, const char *name);
jmethodID checkGetMethodID(JNIEnv *env, jclass klass, const char *name, const char *sig, int isStatic);
jfieldID checkGetFieldID(JNIEnv *env, jclass klass, const char *name, const char *sig, int isStatic);
extern jmethodID createSymbolMethodID;

// use for an unimplemented API function
void *unimplemented(char *msg);
// use for any fatal error
void fatalError(char *msg);
// makes a call to the VM with x as an argument (for debugger validation)
void validate(SEXP x);
// checks x against the list of canonical (named) refs, returning the canonical version if a match
SEXP checkRef(JNIEnv *env, SEXP x);
// creates a JNI global ref from x for slot index of the named refs table
SEXP mkNamedGlobalRef(JNIEnv *env, int index, SEXP x);
// validate a JNI reference
void validateRef(JNIEnv *env, SEXP x, const char *msg);

// entering a top-level JNI call
void callEnter(JNIEnv *env, jmp_buf *error_exit);
// exiting a top-level JNI call
void callExit(JNIEnv *env);
// called by callExit to deallocate transient memory
void allocExit();

jmp_buf *getErrorJmpBuf();

// find an object for which we have cached the internal rep
void *findCopiedObject(JNIEnv *env, SEXP x);
// add a new object to the internal rep cache
void addCopiedObject(JNIEnv *env, SEXP x, SEXPTYPE type, void *jArray, void *data);
void invalidateCopiedObject(JNIEnv *env, SEXP oldObj);

void init_rmath(JNIEnv *env);
void init_variables(JNIEnv *env, jobjectArray initialValues);
void init_dynload(JNIEnv *env);
void init_internals(JNIEnv *env);
void init_random(JNIEnv *env);
void init_utils(JNIEnv *env);

void setTempDir(JNIEnv *, jstring tempDir);

extern jclass RDataFactoryClass;
extern jclass CallRFFIHelperClass;
extern jclass RRuntimeClass;

#define TRACE_UPCALLS 0

#define TARG1 "%s(%p)\n"
#define TARG2 "%s(%p, %p)\n"
#define TARG2d "%s(%p, %d)\n"

#if TRACE_UPCALLS
#define TRACE(format, ...) printf(format, __FUNCTION__, __VA_ARGS__)
#else
#define TRACE(format, ...)
#endif

#define _(Source) (Source)

#endif /* RFFIUTILS_H */
