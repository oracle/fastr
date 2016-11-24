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

#include <rffiutils.h>
#include <string.h>
#include <setjmp.h>

JNIEXPORT void JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jni_JNI_1Call_initialize(JNIEnv *env, jclass c,
		jobjectArray initialValues) {
	init_utils(env); // must be first
	init_variables(env, initialValues);
	init_dynload(env);
	init_internals(env);
	init_rmath(env);
	init_random(env);
	init_parse(env);
	init_pcre(env);
	init_c(env);
}

JNIEXPORT void JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jni_JNI_1Call_nativeSetTempDir(JNIEnv *env, jclass c, jstring tempDir) {
	setTempDir(env, tempDir);
}


// Boilerplate methods for the actual calls

typedef SEXP (*call0func)();
typedef SEXP (*call1func)(SEXP arg1);
typedef SEXP (*call2func)(SEXP arg1, SEXP arg2);
typedef SEXP (*call3func)(SEXP arg1, SEXP arg2, SEXP arg3);
typedef SEXP (*call4func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4);
typedef SEXP (*call5func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5);
typedef SEXP (*call6func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6);
typedef SEXP (*call7func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7);
typedef SEXP (*call8func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8);
typedef SEXP (*call9func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8, SEXP arg9);
typedef SEXP (*call10func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8, SEXP arg9, SEXP arg10);
typedef SEXP (*call11func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
        SEXP arg9, SEXP arg10, SEXP arg11);
typedef SEXP (*call12func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
        SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12);
typedef SEXP (*call13func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
        SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13);
typedef SEXP (*call14func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
        SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14);
typedef SEXP (*call15func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
        SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15);
typedef SEXP (*call16func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
        SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15, SEXP arg16
        );
typedef SEXP (*call17func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
        SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15, SEXP arg16,
        SEXP arg17);
typedef SEXP (*call18func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
        SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15, SEXP arg16,
        SEXP arg17, SEXP arg18);
typedef SEXP (*call19func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
        SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15, SEXP arg16,
        SEXP arg17, SEXP arg18, SEXP arg19);
typedef SEXP (*call20func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
        SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15, SEXP arg16,
        SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20);
typedef SEXP (*call21func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
        SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15, SEXP arg16,
        SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21);
typedef SEXP (*call22func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
        SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15, SEXP arg16,
        SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22);
typedef SEXP (*call23func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
        SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15, SEXP arg16,
        SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23);
typedef SEXP (*call24func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
        SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15, SEXP arg16,
        SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23, SEXP arg24
        );
typedef SEXP (*call25func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
        SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15, SEXP arg16,
        SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23, SEXP arg24,
        SEXP arg25);
typedef SEXP (*call26func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
        SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15, SEXP arg16,
        SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23, SEXP arg24,
        SEXP arg25, SEXP arg26);
typedef SEXP (*call27func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
        SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15, SEXP arg16,
        SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23, SEXP arg24,
        SEXP arg25, SEXP arg26, SEXP arg27);
typedef SEXP (*call28func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
        SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15, SEXP arg16,
        SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23, SEXP arg24,
        SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28);
typedef SEXP (*call29func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
        SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15, SEXP arg16,
        SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23, SEXP arg24,
        SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29);
typedef SEXP (*call30func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
        SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15, SEXP arg16,
        SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23, SEXP arg24,
        SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30);
typedef SEXP (*call31func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
        SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15, SEXP arg16,
        SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23, SEXP arg24,
        SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31);
typedef SEXP (*call32func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
        SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15, SEXP arg16,
        SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23, SEXP arg24,
        SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31, SEXP arg32
        );
typedef SEXP (*call33func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
        SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15, SEXP arg16,
        SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23, SEXP arg24,
        SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31, SEXP arg32,
        SEXP arg33);
typedef SEXP (*call34func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
        SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15, SEXP arg16,
        SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23, SEXP arg24,
        SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31, SEXP arg32,
        SEXP arg33, SEXP arg34);
typedef SEXP (*call35func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
        SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15, SEXP arg16,
        SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23, SEXP arg24,
        SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31, SEXP arg32,
        SEXP arg33, SEXP arg34, SEXP arg35);
typedef SEXP (*call36func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
        SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15, SEXP arg16,
        SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23, SEXP arg24,
        SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31, SEXP arg32,
        SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36);
typedef SEXP (*call37func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
        SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15, SEXP arg16,
        SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23, SEXP arg24,
        SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31, SEXP arg32,
        SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37);
typedef SEXP (*call38func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
        SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15, SEXP arg16,
        SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23, SEXP arg24,
        SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31, SEXP arg32,
        SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38);
typedef SEXP (*call39func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
        SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15, SEXP arg16,
        SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23, SEXP arg24,
        SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31, SEXP arg32,
        SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39);
typedef SEXP (*call40func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
        SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15, SEXP arg16,
        SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23, SEXP arg24,
        SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31, SEXP arg32,
        SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39, SEXP arg40
        );
typedef SEXP (*call41func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
        SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15, SEXP arg16,
        SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23, SEXP arg24,
        SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31, SEXP arg32,
        SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39, SEXP arg40,
        SEXP arg41);
typedef SEXP (*call42func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
        SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15, SEXP arg16,
        SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23, SEXP arg24,
        SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31, SEXP arg32,
        SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39, SEXP arg40,
        SEXP arg41, SEXP arg42);
typedef SEXP (*call43func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
        SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15, SEXP arg16,
        SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23, SEXP arg24,
        SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31, SEXP arg32,
        SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39, SEXP arg40,
        SEXP arg41, SEXP arg42, SEXP arg43);
typedef SEXP (*call44func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
        SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15, SEXP arg16,
        SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23, SEXP arg24,
        SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31, SEXP arg32,
        SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39, SEXP arg40,
        SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44);
typedef SEXP (*call45func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
        SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15, SEXP arg16,
        SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23, SEXP arg24,
        SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31, SEXP arg32,
        SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39, SEXP arg40,
        SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44, SEXP arg45);
typedef SEXP (*call46func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
        SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15, SEXP arg16,
        SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23, SEXP arg24,
        SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31, SEXP arg32,
        SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39, SEXP arg40,
        SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44, SEXP arg45, SEXP arg46);
typedef SEXP (*call47func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
        SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15, SEXP arg16,
        SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23, SEXP arg24,
        SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31, SEXP arg32,
        SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39, SEXP arg40,
        SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44, SEXP arg45, SEXP arg46, SEXP arg47);
typedef SEXP (*call48func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
        SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15, SEXP arg16,
        SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23, SEXP arg24,
        SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31, SEXP arg32,
        SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39, SEXP arg40,
        SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44, SEXP arg45, SEXP arg46, SEXP arg47, SEXP arg48
        );
typedef SEXP (*call49func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
        SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15, SEXP arg16,
        SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23, SEXP arg24,
        SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31, SEXP arg32,
        SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39, SEXP arg40,
        SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44, SEXP arg45, SEXP arg46, SEXP arg47, SEXP arg48,
        SEXP arg49);
typedef SEXP (*call50func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
        SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15, SEXP arg16,
        SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23, SEXP arg24,
        SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31, SEXP arg32,
        SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39, SEXP arg40,
        SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44, SEXP arg45, SEXP arg46, SEXP arg47, SEXP arg48,
        SEXP arg49, SEXP arg50);
typedef SEXP (*call51func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
        SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15, SEXP arg16,
        SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23, SEXP arg24,
        SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31, SEXP arg32,
        SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39, SEXP arg40,
        SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44, SEXP arg45, SEXP arg46, SEXP arg47, SEXP arg48,
        SEXP arg49, SEXP arg50, SEXP arg51);
typedef SEXP (*call52func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
        SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15, SEXP arg16,
        SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23, SEXP arg24,
        SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31, SEXP arg32,
        SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39, SEXP arg40,
        SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44, SEXP arg45, SEXP arg46, SEXP arg47, SEXP arg48,
        SEXP arg49, SEXP arg50, SEXP arg51, SEXP arg52);
typedef SEXP (*call53func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
        SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15, SEXP arg16,
        SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23, SEXP arg24,
        SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31, SEXP arg32,
        SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39, SEXP arg40,
        SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44, SEXP arg45, SEXP arg46, SEXP arg47, SEXP arg48,
        SEXP arg49, SEXP arg50, SEXP arg51, SEXP arg52, SEXP arg53);
typedef SEXP (*call54func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
        SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15, SEXP arg16,
        SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23, SEXP arg24,
        SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31, SEXP arg32,
        SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39, SEXP arg40,
        SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44, SEXP arg45, SEXP arg46, SEXP arg47, SEXP arg48,
        SEXP arg49, SEXP arg50, SEXP arg51, SEXP arg52, SEXP arg53, SEXP arg54);
typedef SEXP (*call55func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
        SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15, SEXP arg16,
        SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23, SEXP arg24,
        SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31, SEXP arg32,
        SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39, SEXP arg40,
        SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44, SEXP arg45, SEXP arg46, SEXP arg47, SEXP arg48,
        SEXP arg49, SEXP arg50, SEXP arg51, SEXP arg52, SEXP arg53, SEXP arg54, SEXP arg55);
typedef SEXP (*call56func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
        SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15, SEXP arg16,
        SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23, SEXP arg24,
        SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31, SEXP arg32,
        SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39, SEXP arg40,
        SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44, SEXP arg45, SEXP arg46, SEXP arg47, SEXP arg48,
        SEXP arg49, SEXP arg50, SEXP arg51, SEXP arg52, SEXP arg53, SEXP arg54, SEXP arg55, SEXP arg56
        );
typedef SEXP (*call57func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
        SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15, SEXP arg16,
        SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23, SEXP arg24,
        SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31, SEXP arg32,
        SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39, SEXP arg40,
        SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44, SEXP arg45, SEXP arg46, SEXP arg47, SEXP arg48,
        SEXP arg49, SEXP arg50, SEXP arg51, SEXP arg52, SEXP arg53, SEXP arg54, SEXP arg55, SEXP arg56,
        SEXP arg57);
typedef SEXP (*call58func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
        SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15, SEXP arg16,
        SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23, SEXP arg24,
        SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31, SEXP arg32,
        SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39, SEXP arg40,
        SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44, SEXP arg45, SEXP arg46, SEXP arg47, SEXP arg48,
        SEXP arg49, SEXP arg50, SEXP arg51, SEXP arg52, SEXP arg53, SEXP arg54, SEXP arg55, SEXP arg56,
        SEXP arg57, SEXP arg58);
typedef SEXP (*call59func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
        SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15, SEXP arg16,
        SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23, SEXP arg24,
        SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31, SEXP arg32,
        SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39, SEXP arg40,
        SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44, SEXP arg45, SEXP arg46, SEXP arg47, SEXP arg48,
        SEXP arg49, SEXP arg50, SEXP arg51, SEXP arg52, SEXP arg53, SEXP arg54, SEXP arg55, SEXP arg56,
        SEXP arg57, SEXP arg58, SEXP arg59);
typedef SEXP (*call60func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
        SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15, SEXP arg16,
        SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23, SEXP arg24,
        SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31, SEXP arg32,
        SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39, SEXP arg40,
        SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44, SEXP arg45, SEXP arg46, SEXP arg47, SEXP arg48,
        SEXP arg49, SEXP arg50, SEXP arg51, SEXP arg52, SEXP arg53, SEXP arg54, SEXP arg55, SEXP arg56,
        SEXP arg57, SEXP arg58, SEXP arg59, SEXP arg60);
typedef SEXP (*call61func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
        SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15, SEXP arg16,
        SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23, SEXP arg24,
        SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31, SEXP arg32,
        SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39, SEXP arg40,
        SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44, SEXP arg45, SEXP arg46, SEXP arg47, SEXP arg48,
        SEXP arg49, SEXP arg50, SEXP arg51, SEXP arg52, SEXP arg53, SEXP arg54, SEXP arg55, SEXP arg56,
        SEXP arg57, SEXP arg58, SEXP arg59, SEXP arg60, SEXP arg61);
typedef SEXP (*call62func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
        SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15, SEXP arg16,
        SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23, SEXP arg24,
        SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31, SEXP arg32,
        SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39, SEXP arg40,
        SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44, SEXP arg45, SEXP arg46, SEXP arg47, SEXP arg48,
        SEXP arg49, SEXP arg50, SEXP arg51, SEXP arg52, SEXP arg53, SEXP arg54, SEXP arg55, SEXP arg56,
        SEXP arg57, SEXP arg58, SEXP arg59, SEXP arg60, SEXP arg61, SEXP arg62);
typedef SEXP (*call63func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
        SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15, SEXP arg16,
        SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23, SEXP arg24,
        SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31, SEXP arg32,
        SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39, SEXP arg40,
        SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44, SEXP arg45, SEXP arg46, SEXP arg47, SEXP arg48,
        SEXP arg49, SEXP arg50, SEXP arg51, SEXP arg52, SEXP arg53, SEXP arg54, SEXP arg55, SEXP arg56,
        SEXP arg57, SEXP arg58, SEXP arg59, SEXP arg60, SEXP arg61, SEXP arg62, SEXP arg63);
typedef SEXP (*call64func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
        SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15, SEXP arg16,
        SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23, SEXP arg24,
        SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31, SEXP arg32,
        SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39, SEXP arg40,
        SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44, SEXP arg45, SEXP arg46, SEXP arg47, SEXP arg48,
        SEXP arg49, SEXP arg50, SEXP arg51, SEXP arg52, SEXP arg53, SEXP arg54, SEXP arg55, SEXP arg56,
        SEXP arg57, SEXP arg58, SEXP arg59, SEXP arg60, SEXP arg61, SEXP arg62, SEXP arg63, SEXP arg64
        );

JNIEXPORT jobject JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jni_JNI_1Call_call0(JNIEnv *env, jclass c, jlong address) {
	jmp_buf error_jmpbuf;
	jobject result = NULL;
	callEnter(env, &error_jmpbuf);
	if (!setjmp(error_jmpbuf)) {
		call0func call0 = (call0func) address;
		result = (*call0)();
	}
	callExit(env);
	return result;
}

JNIEXPORT jobject JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jni_JNI_1Call_call1(JNIEnv *env, jclass c, jlong address, jobject arg1) {
	jmp_buf error_jmpbuf;
	jobject result = NULL;
	callEnter(env, &error_jmpbuf);
	if (!setjmp(error_jmpbuf)) {
		call1func call1 = (call1func) address;
		result = (*call1)(checkRef(env, arg1));
	}
	callExit(env);
	return result;
}

JNIEXPORT jobject JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jni_JNI_1Call_call2(JNIEnv *env, jclass c, jlong address, jobject arg1, jobject arg2) {
	jmp_buf error_jmpbuf;
	jobject result = NULL;
	callEnter(env, &error_jmpbuf);
	if (!setjmp(error_jmpbuf)) {
		call2func call2 = (call2func) address;
		result = (*call2)(checkRef(env, arg1), checkRef(env, arg2));
	}
	callExit(env);
	return result;
}

JNIEXPORT jobject JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jni_JNI_1Call_call3(JNIEnv *env, jclass c, jlong address, jobject arg1, jobject arg2,
		jobject arg3) {
	jmp_buf error_jmpbuf;
	jobject result = NULL;
	callEnter(env, &error_jmpbuf);
	if (!setjmp(error_jmpbuf)) {
		call3func call3 = (call3func) address;
		result = (*call3)(checkRef(env, arg1), checkRef(env, checkRef(env, arg2)), checkRef(env, arg3));
	}
	callExit(env);
	return result;
}

JNIEXPORT jobject JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jni_JNI_1Call_call4(JNIEnv *env, jclass c, jlong address, jobject arg1, jobject arg2,
		jobject arg3, jobject arg4) {
	jmp_buf error_jmpbuf;
	jobject result = NULL;
	callEnter(env, &error_jmpbuf);
	if (!setjmp(error_jmpbuf)) {
		call4func call4 = (call4func) address;
		result = (*call4)(checkRef(env, arg1), checkRef(env, arg2), checkRef(env, arg3), checkRef(env, arg4));
	}
	callExit(env);
	return result;
}

JNIEXPORT jobject JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jni_JNI_1Call_call5(JNIEnv *env, jclass c, jlong address, jobject arg1, jobject arg2,
		jobject arg3, jobject arg4, jobject arg5) {
	jmp_buf error_jmpbuf;
	jobject result = NULL;
	callEnter(env, &error_jmpbuf);
	if (!setjmp(error_jmpbuf)) {
		call5func call5 = (call5func) address;
		result = (*call5)(checkRef(env, arg1), checkRef(env, arg2), checkRef(env, arg3), checkRef(env, arg4), checkRef(env, arg5));
	}
	callExit(env);
	return result;
}

JNIEXPORT jobject JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jni_JNI_1Call_call6(JNIEnv *env, jclass c, jlong address, jobject arg1, jobject arg2,
		jobject arg3, jobject arg4, jobject arg5, jobject arg6) {
	jmp_buf error_jmpbuf;
	jobject result = NULL;
	callEnter(env, &error_jmpbuf);
	if (!setjmp(error_jmpbuf)) {
		call6func call6 = (call6func) address;
		result = (*call6)(checkRef(env, arg1), checkRef(env, arg2), checkRef(env, arg3), checkRef(env, arg4), checkRef(env, arg5), checkRef(env, arg6));
	}
	callExit(env);
	return result;
}

JNIEXPORT jobject JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jni_JNI_1Call_call7(JNIEnv *env, jclass c, jlong address, jobject arg1, jobject arg2,
		jobject arg3, jobject arg4, jobject arg5, jobject arg6, jobject arg7) {
	jmp_buf error_jmpbuf;
	jobject result = NULL;
	callEnter(env, &error_jmpbuf);
	if (!setjmp(error_jmpbuf)) {
		call7func call7 = (call7func) address;
		result = (*call7)(checkRef(env, arg1), checkRef(env, arg2), checkRef(env, arg3), checkRef(env, arg4), checkRef(env, arg5), checkRef(env, arg6), checkRef(env, arg7));
	}
	callExit(env);
	return result;
}

JNIEXPORT jobject JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jni_JNI_1Call_call8(JNIEnv *env, jclass c, jlong address, jobject arg1, jobject arg2,
		jobject arg3, jobject arg4, jobject arg5, jobject arg6, jobject arg7, jobject arg8) {
	jmp_buf error_jmpbuf;
	jobject result = NULL;
	callEnter(env, &error_jmpbuf);
	if (!setjmp(error_jmpbuf)) {
		call8func call8 = (call8func) address;
		result = (*call8)(checkRef(env, arg1), checkRef(env, arg2), checkRef(env, arg3), checkRef(env, arg4), checkRef(env, arg5), checkRef(env, arg6), checkRef(env, arg7), checkRef(env, arg8));
	}
	callExit(env);
	return result;
}

JNIEXPORT jobject JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jni_JNI_1Call_call9(JNIEnv *env, jclass c, jlong address, jobject arg1, jobject arg2,
		jobject arg3, jobject arg4, jobject arg5, jobject arg6, jobject arg7, jobject arg8, jobject arg9) {
	jmp_buf error_jmpbuf;
	jobject result = NULL;
	callEnter(env, &error_jmpbuf);
	if (!setjmp(error_jmpbuf)) {
		call9func call9 = (call9func) address;
		result = (*call9)(checkRef(env, arg1), checkRef(env, arg2), checkRef(env, arg3), checkRef(env, arg4), checkRef(env, arg5), checkRef(env, arg6), checkRef(env, arg7), checkRef(env, arg8), checkRef(env, arg9));
	}
	callExit(env);
	return result;
}

JNIEXPORT jobject JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jni_JNI_1Call_call(JNIEnv *env, jclass c, jlong address, jobjectArray args) {
	jmp_buf error_jmpbuf;
	jobject result = NULL;
	callEnter(env, &error_jmpbuf);
	jsize len = (*env)->GetArrayLength(env, args);
	SEXP jargs[64];
	for (int i = 0; i < len; i++) {
		jargs[i] = checkRef(env, (*env)->GetObjectArrayElement(env, args, i));
	}
	switch (len) {
    case 10: {
        if (!setjmp(error_jmpbuf)) {
            call10func call10 = (call10func) address;
            result = (*call10)(jargs[0], jargs[1], jargs[2], jargs[3], jargs[4], jargs[5], jargs[6], jargs[7],
                jargs[8], jargs[9]);
        }
        callExit(env);
        return result;
    }

    case 11: {
        if (!setjmp(error_jmpbuf)) {
            call11func call11 = (call11func) address;
            result = (*call11)(jargs[0], jargs[1], jargs[2], jargs[3], jargs[4], jargs[5], jargs[6], jargs[7],
                jargs[8], jargs[9], jargs[10]);
        }
        callExit(env);
        return result;
    }

    case 12: {
        if (!setjmp(error_jmpbuf)) {
            call12func call12 = (call12func) address;
            result = (*call12)(jargs[0], jargs[1], jargs[2], jargs[3], jargs[4], jargs[5], jargs[6], jargs[7],
                jargs[8], jargs[9], jargs[10], jargs[11]);
        }
        callExit(env);
        return result;
    }

    case 13: {
        if (!setjmp(error_jmpbuf)) {
            call13func call13 = (call13func) address;
            result = (*call13)(jargs[0], jargs[1], jargs[2], jargs[3], jargs[4], jargs[5], jargs[6], jargs[7],
                jargs[8], jargs[9], jargs[10], jargs[11], jargs[12]);
        }
        callExit(env);
        return result;
    }

    case 14: {
        if (!setjmp(error_jmpbuf)) {
            call14func call14 = (call14func) address;
            result = (*call14)(jargs[0], jargs[1], jargs[2], jargs[3], jargs[4], jargs[5], jargs[6], jargs[7],
                jargs[8], jargs[9], jargs[10], jargs[11], jargs[12], jargs[13]);
        }
        callExit(env);
        return result;
    }

    case 15: {
        if (!setjmp(error_jmpbuf)) {
            call15func call15 = (call15func) address;
            result = (*call15)(jargs[0], jargs[1], jargs[2], jargs[3], jargs[4], jargs[5], jargs[6], jargs[7],
                jargs[8], jargs[9], jargs[10], jargs[11], jargs[12], jargs[13], jargs[14]);
        }
        callExit(env);
        return result;
    }

    case 16: {
        if (!setjmp(error_jmpbuf)) {
            call16func call16 = (call16func) address;
            result = (*call16)(jargs[0], jargs[1], jargs[2], jargs[3], jargs[4], jargs[5], jargs[6], jargs[7],
                jargs[8], jargs[9], jargs[10], jargs[11], jargs[12], jargs[13], jargs[14], jargs[15]
                );
        }
        callExit(env);
        return result;
    }

    case 17: {
        if (!setjmp(error_jmpbuf)) {
            call17func call17 = (call17func) address;
            result = (*call17)(jargs[0], jargs[1], jargs[2], jargs[3], jargs[4], jargs[5], jargs[6], jargs[7],
                jargs[8], jargs[9], jargs[10], jargs[11], jargs[12], jargs[13], jargs[14], jargs[15],
                jargs[16]);
        }
        callExit(env);
        return result;
    }

    case 18: {
        if (!setjmp(error_jmpbuf)) {
            call18func call18 = (call18func) address;
            result = (*call18)(jargs[0], jargs[1], jargs[2], jargs[3], jargs[4], jargs[5], jargs[6], jargs[7],
                jargs[8], jargs[9], jargs[10], jargs[11], jargs[12], jargs[13], jargs[14], jargs[15],
                jargs[16], jargs[17]);
        }
        callExit(env);
        return result;
    }

    case 19: {
        if (!setjmp(error_jmpbuf)) {
            call19func call19 = (call19func) address;
            result = (*call19)(jargs[0], jargs[1], jargs[2], jargs[3], jargs[4], jargs[5], jargs[6], jargs[7],
                jargs[8], jargs[9], jargs[10], jargs[11], jargs[12], jargs[13], jargs[14], jargs[15],
                jargs[16], jargs[17], jargs[18]);
        }
        callExit(env);
        return result;
    }

    case 20: {
        if (!setjmp(error_jmpbuf)) {
            call20func call20 = (call20func) address;
            result = (*call20)(jargs[0], jargs[1], jargs[2], jargs[3], jargs[4], jargs[5], jargs[6], jargs[7],
                jargs[8], jargs[9], jargs[10], jargs[11], jargs[12], jargs[13], jargs[14], jargs[15],
                jargs[16], jargs[17], jargs[18], jargs[19]);
        }
        callExit(env);
        return result;
    }

    case 21: {
        if (!setjmp(error_jmpbuf)) {
            call21func call21 = (call21func) address;
            result = (*call21)(jargs[0], jargs[1], jargs[2], jargs[3], jargs[4], jargs[5], jargs[6], jargs[7],
                jargs[8], jargs[9], jargs[10], jargs[11], jargs[12], jargs[13], jargs[14], jargs[15],
                jargs[16], jargs[17], jargs[18], jargs[19], jargs[20]);
        }
        callExit(env);
        return result;
    }

    case 22: {
        if (!setjmp(error_jmpbuf)) {
            call22func call22 = (call22func) address;
            result = (*call22)(jargs[0], jargs[1], jargs[2], jargs[3], jargs[4], jargs[5], jargs[6], jargs[7],
                jargs[8], jargs[9], jargs[10], jargs[11], jargs[12], jargs[13], jargs[14], jargs[15],
                jargs[16], jargs[17], jargs[18], jargs[19], jargs[20], jargs[21]);
        }
        callExit(env);
        return result;
    }

    case 23: {
        if (!setjmp(error_jmpbuf)) {
            call23func call23 = (call23func) address;
            result = (*call23)(jargs[0], jargs[1], jargs[2], jargs[3], jargs[4], jargs[5], jargs[6], jargs[7],
                jargs[8], jargs[9], jargs[10], jargs[11], jargs[12], jargs[13], jargs[14], jargs[15],
                jargs[16], jargs[17], jargs[18], jargs[19], jargs[20], jargs[21], jargs[22]);
        }
        callExit(env);
        return result;
    }

    case 24: {
        if (!setjmp(error_jmpbuf)) {
            call24func call24 = (call24func) address;
            result = (*call24)(jargs[0], jargs[1], jargs[2], jargs[3], jargs[4], jargs[5], jargs[6], jargs[7],
                jargs[8], jargs[9], jargs[10], jargs[11], jargs[12], jargs[13], jargs[14], jargs[15],
                jargs[16], jargs[17], jargs[18], jargs[19], jargs[20], jargs[21], jargs[22], jargs[23]
                );
        }
        callExit(env);
        return result;
    }

    case 25: {
        if (!setjmp(error_jmpbuf)) {
            call25func call25 = (call25func) address;
            result = (*call25)(jargs[0], jargs[1], jargs[2], jargs[3], jargs[4], jargs[5], jargs[6], jargs[7],
                jargs[8], jargs[9], jargs[10], jargs[11], jargs[12], jargs[13], jargs[14], jargs[15],
                jargs[16], jargs[17], jargs[18], jargs[19], jargs[20], jargs[21], jargs[22], jargs[23],
                jargs[24]);
        }
        callExit(env);
        return result;
    }

    case 26: {
        if (!setjmp(error_jmpbuf)) {
            call26func call26 = (call26func) address;
            result = (*call26)(jargs[0], jargs[1], jargs[2], jargs[3], jargs[4], jargs[5], jargs[6], jargs[7],
                jargs[8], jargs[9], jargs[10], jargs[11], jargs[12], jargs[13], jargs[14], jargs[15],
                jargs[16], jargs[17], jargs[18], jargs[19], jargs[20], jargs[21], jargs[22], jargs[23],
                jargs[24], jargs[25]);
        }
        callExit(env);
        return result;
    }

    case 27: {
        if (!setjmp(error_jmpbuf)) {
            call27func call27 = (call27func) address;
            result = (*call27)(jargs[0], jargs[1], jargs[2], jargs[3], jargs[4], jargs[5], jargs[6], jargs[7],
                jargs[8], jargs[9], jargs[10], jargs[11], jargs[12], jargs[13], jargs[14], jargs[15],
                jargs[16], jargs[17], jargs[18], jargs[19], jargs[20], jargs[21], jargs[22], jargs[23],
                jargs[24], jargs[25], jargs[26]);
        }
        callExit(env);
        return result;
    }

    case 28: {
        if (!setjmp(error_jmpbuf)) {
            call28func call28 = (call28func) address;
            result = (*call28)(jargs[0], jargs[1], jargs[2], jargs[3], jargs[4], jargs[5], jargs[6], jargs[7],
                jargs[8], jargs[9], jargs[10], jargs[11], jargs[12], jargs[13], jargs[14], jargs[15],
                jargs[16], jargs[17], jargs[18], jargs[19], jargs[20], jargs[21], jargs[22], jargs[23],
                jargs[24], jargs[25], jargs[26], jargs[27]);
        }
        callExit(env);
        return result;
    }

    case 29: {
        if (!setjmp(error_jmpbuf)) {
            call29func call29 = (call29func) address;
            result = (*call29)(jargs[0], jargs[1], jargs[2], jargs[3], jargs[4], jargs[5], jargs[6], jargs[7],
                jargs[8], jargs[9], jargs[10], jargs[11], jargs[12], jargs[13], jargs[14], jargs[15],
                jargs[16], jargs[17], jargs[18], jargs[19], jargs[20], jargs[21], jargs[22], jargs[23],
                jargs[24], jargs[25], jargs[26], jargs[27], jargs[28]);
        }
        callExit(env);
        return result;
    }

    case 30: {
        if (!setjmp(error_jmpbuf)) {
            call30func call30 = (call30func) address;
            result = (*call30)(jargs[0], jargs[1], jargs[2], jargs[3], jargs[4], jargs[5], jargs[6], jargs[7],
                jargs[8], jargs[9], jargs[10], jargs[11], jargs[12], jargs[13], jargs[14], jargs[15],
                jargs[16], jargs[17], jargs[18], jargs[19], jargs[20], jargs[21], jargs[22], jargs[23],
                jargs[24], jargs[25], jargs[26], jargs[27], jargs[28], jargs[29]);
        }
        callExit(env);
        return result;
    }

    case 31: {
        if (!setjmp(error_jmpbuf)) {
            call31func call31 = (call31func) address;
            result = (*call31)(jargs[0], jargs[1], jargs[2], jargs[3], jargs[4], jargs[5], jargs[6], jargs[7],
                jargs[8], jargs[9], jargs[10], jargs[11], jargs[12], jargs[13], jargs[14], jargs[15],
                jargs[16], jargs[17], jargs[18], jargs[19], jargs[20], jargs[21], jargs[22], jargs[23],
                jargs[24], jargs[25], jargs[26], jargs[27], jargs[28], jargs[29], jargs[30]);
        }
        callExit(env);
        return result;
    }

    case 32: {
        if (!setjmp(error_jmpbuf)) {
            call32func call32 = (call32func) address;
            result = (*call32)(jargs[0], jargs[1], jargs[2], jargs[3], jargs[4], jargs[5], jargs[6], jargs[7],
                jargs[8], jargs[9], jargs[10], jargs[11], jargs[12], jargs[13], jargs[14], jargs[15],
                jargs[16], jargs[17], jargs[18], jargs[19], jargs[20], jargs[21], jargs[22], jargs[23],
                jargs[24], jargs[25], jargs[26], jargs[27], jargs[28], jargs[29], jargs[30], jargs[31]
                );
        }
        callExit(env);
        return result;
    }

    case 33: {
        if (!setjmp(error_jmpbuf)) {
            call33func call33 = (call33func) address;
            result = (*call33)(jargs[0], jargs[1], jargs[2], jargs[3], jargs[4], jargs[5], jargs[6], jargs[7],
                jargs[8], jargs[9], jargs[10], jargs[11], jargs[12], jargs[13], jargs[14], jargs[15],
                jargs[16], jargs[17], jargs[18], jargs[19], jargs[20], jargs[21], jargs[22], jargs[23],
                jargs[24], jargs[25], jargs[26], jargs[27], jargs[28], jargs[29], jargs[30], jargs[31],
                jargs[32]);
        }
        callExit(env);
        return result;
    }

    case 34: {
        if (!setjmp(error_jmpbuf)) {
            call34func call34 = (call34func) address;
            result = (*call34)(jargs[0], jargs[1], jargs[2], jargs[3], jargs[4], jargs[5], jargs[6], jargs[7],
                jargs[8], jargs[9], jargs[10], jargs[11], jargs[12], jargs[13], jargs[14], jargs[15],
                jargs[16], jargs[17], jargs[18], jargs[19], jargs[20], jargs[21], jargs[22], jargs[23],
                jargs[24], jargs[25], jargs[26], jargs[27], jargs[28], jargs[29], jargs[30], jargs[31],
                jargs[32], jargs[33]);
        }
        callExit(env);
        return result;
    }

    case 35: {
        if (!setjmp(error_jmpbuf)) {
            call35func call35 = (call35func) address;
            result = (*call35)(jargs[0], jargs[1], jargs[2], jargs[3], jargs[4], jargs[5], jargs[6], jargs[7],
                jargs[8], jargs[9], jargs[10], jargs[11], jargs[12], jargs[13], jargs[14], jargs[15],
                jargs[16], jargs[17], jargs[18], jargs[19], jargs[20], jargs[21], jargs[22], jargs[23],
                jargs[24], jargs[25], jargs[26], jargs[27], jargs[28], jargs[29], jargs[30], jargs[31],
                jargs[32], jargs[33], jargs[34]);
        }
        callExit(env);
        return result;
    }

    case 36: {
        if (!setjmp(error_jmpbuf)) {
            call36func call36 = (call36func) address;
            result = (*call36)(jargs[0], jargs[1], jargs[2], jargs[3], jargs[4], jargs[5], jargs[6], jargs[7],
                jargs[8], jargs[9], jargs[10], jargs[11], jargs[12], jargs[13], jargs[14], jargs[15],
                jargs[16], jargs[17], jargs[18], jargs[19], jargs[20], jargs[21], jargs[22], jargs[23],
                jargs[24], jargs[25], jargs[26], jargs[27], jargs[28], jargs[29], jargs[30], jargs[31],
                jargs[32], jargs[33], jargs[34], jargs[35]);
        }
        callExit(env);
        return result;
    }

    case 37: {
        if (!setjmp(error_jmpbuf)) {
            call37func call37 = (call37func) address;
            result = (*call37)(jargs[0], jargs[1], jargs[2], jargs[3], jargs[4], jargs[5], jargs[6], jargs[7],
                jargs[8], jargs[9], jargs[10], jargs[11], jargs[12], jargs[13], jargs[14], jargs[15],
                jargs[16], jargs[17], jargs[18], jargs[19], jargs[20], jargs[21], jargs[22], jargs[23],
                jargs[24], jargs[25], jargs[26], jargs[27], jargs[28], jargs[29], jargs[30], jargs[31],
                jargs[32], jargs[33], jargs[34], jargs[35], jargs[36]);
        }
        callExit(env);
        return result;
    }

    case 38: {
        if (!setjmp(error_jmpbuf)) {
            call38func call38 = (call38func) address;
            result = (*call38)(jargs[0], jargs[1], jargs[2], jargs[3], jargs[4], jargs[5], jargs[6], jargs[7],
                jargs[8], jargs[9], jargs[10], jargs[11], jargs[12], jargs[13], jargs[14], jargs[15],
                jargs[16], jargs[17], jargs[18], jargs[19], jargs[20], jargs[21], jargs[22], jargs[23],
                jargs[24], jargs[25], jargs[26], jargs[27], jargs[28], jargs[29], jargs[30], jargs[31],
                jargs[32], jargs[33], jargs[34], jargs[35], jargs[36], jargs[37]);
        }
        callExit(env);
        return result;
    }

    case 39: {
        if (!setjmp(error_jmpbuf)) {
            call39func call39 = (call39func) address;
            result = (*call39)(jargs[0], jargs[1], jargs[2], jargs[3], jargs[4], jargs[5], jargs[6], jargs[7],
                jargs[8], jargs[9], jargs[10], jargs[11], jargs[12], jargs[13], jargs[14], jargs[15],
                jargs[16], jargs[17], jargs[18], jargs[19], jargs[20], jargs[21], jargs[22], jargs[23],
                jargs[24], jargs[25], jargs[26], jargs[27], jargs[28], jargs[29], jargs[30], jargs[31],
                jargs[32], jargs[33], jargs[34], jargs[35], jargs[36], jargs[37], jargs[38]);
        }
        callExit(env);
        return result;
    }

    case 40: {
        if (!setjmp(error_jmpbuf)) {
            call40func call40 = (call40func) address;
            result = (*call40)(jargs[0], jargs[1], jargs[2], jargs[3], jargs[4], jargs[5], jargs[6], jargs[7],
                jargs[8], jargs[9], jargs[10], jargs[11], jargs[12], jargs[13], jargs[14], jargs[15],
                jargs[16], jargs[17], jargs[18], jargs[19], jargs[20], jargs[21], jargs[22], jargs[23],
                jargs[24], jargs[25], jargs[26], jargs[27], jargs[28], jargs[29], jargs[30], jargs[31],
                jargs[32], jargs[33], jargs[34], jargs[35], jargs[36], jargs[37], jargs[38], jargs[39]
                );
        }
        callExit(env);
        return result;
    }

    case 41: {
        if (!setjmp(error_jmpbuf)) {
            call41func call41 = (call41func) address;
            result = (*call41)(jargs[0], jargs[1], jargs[2], jargs[3], jargs[4], jargs[5], jargs[6], jargs[7],
                jargs[8], jargs[9], jargs[10], jargs[11], jargs[12], jargs[13], jargs[14], jargs[15],
                jargs[16], jargs[17], jargs[18], jargs[19], jargs[20], jargs[21], jargs[22], jargs[23],
                jargs[24], jargs[25], jargs[26], jargs[27], jargs[28], jargs[29], jargs[30], jargs[31],
                jargs[32], jargs[33], jargs[34], jargs[35], jargs[36], jargs[37], jargs[38], jargs[39],
                jargs[40]);
        }
        callExit(env);
        return result;
    }

    case 42: {
        if (!setjmp(error_jmpbuf)) {
            call42func call42 = (call42func) address;
            result = (*call42)(jargs[0], jargs[1], jargs[2], jargs[3], jargs[4], jargs[5], jargs[6], jargs[7],
                jargs[8], jargs[9], jargs[10], jargs[11], jargs[12], jargs[13], jargs[14], jargs[15],
                jargs[16], jargs[17], jargs[18], jargs[19], jargs[20], jargs[21], jargs[22], jargs[23],
                jargs[24], jargs[25], jargs[26], jargs[27], jargs[28], jargs[29], jargs[30], jargs[31],
                jargs[32], jargs[33], jargs[34], jargs[35], jargs[36], jargs[37], jargs[38], jargs[39],
                jargs[40], jargs[41]);
        }
        callExit(env);
        return result;
    }

    case 43: {
        if (!setjmp(error_jmpbuf)) {
            call43func call43 = (call43func) address;
            result = (*call43)(jargs[0], jargs[1], jargs[2], jargs[3], jargs[4], jargs[5], jargs[6], jargs[7],
                jargs[8], jargs[9], jargs[10], jargs[11], jargs[12], jargs[13], jargs[14], jargs[15],
                jargs[16], jargs[17], jargs[18], jargs[19], jargs[20], jargs[21], jargs[22], jargs[23],
                jargs[24], jargs[25], jargs[26], jargs[27], jargs[28], jargs[29], jargs[30], jargs[31],
                jargs[32], jargs[33], jargs[34], jargs[35], jargs[36], jargs[37], jargs[38], jargs[39],
                jargs[40], jargs[41], jargs[42]);
        }
        callExit(env);
        return result;
    }

    case 44: {
        if (!setjmp(error_jmpbuf)) {
            call44func call44 = (call44func) address;
            result = (*call44)(jargs[0], jargs[1], jargs[2], jargs[3], jargs[4], jargs[5], jargs[6], jargs[7],
                jargs[8], jargs[9], jargs[10], jargs[11], jargs[12], jargs[13], jargs[14], jargs[15],
                jargs[16], jargs[17], jargs[18], jargs[19], jargs[20], jargs[21], jargs[22], jargs[23],
                jargs[24], jargs[25], jargs[26], jargs[27], jargs[28], jargs[29], jargs[30], jargs[31],
                jargs[32], jargs[33], jargs[34], jargs[35], jargs[36], jargs[37], jargs[38], jargs[39],
                jargs[40], jargs[41], jargs[42], jargs[43]);
        }
        callExit(env);
        return result;
    }

    case 45: {
        if (!setjmp(error_jmpbuf)) {
            call45func call45 = (call45func) address;
            result = (*call45)(jargs[0], jargs[1], jargs[2], jargs[3], jargs[4], jargs[5], jargs[6], jargs[7],
                jargs[8], jargs[9], jargs[10], jargs[11], jargs[12], jargs[13], jargs[14], jargs[15],
                jargs[16], jargs[17], jargs[18], jargs[19], jargs[20], jargs[21], jargs[22], jargs[23],
                jargs[24], jargs[25], jargs[26], jargs[27], jargs[28], jargs[29], jargs[30], jargs[31],
                jargs[32], jargs[33], jargs[34], jargs[35], jargs[36], jargs[37], jargs[38], jargs[39],
                jargs[40], jargs[41], jargs[42], jargs[43], jargs[44]);
        }
        callExit(env);
        return result;
    }

    case 46: {
        if (!setjmp(error_jmpbuf)) {
            call46func call46 = (call46func) address;
            result = (*call46)(jargs[0], jargs[1], jargs[2], jargs[3], jargs[4], jargs[5], jargs[6], jargs[7],
                jargs[8], jargs[9], jargs[10], jargs[11], jargs[12], jargs[13], jargs[14], jargs[15],
                jargs[16], jargs[17], jargs[18], jargs[19], jargs[20], jargs[21], jargs[22], jargs[23],
                jargs[24], jargs[25], jargs[26], jargs[27], jargs[28], jargs[29], jargs[30], jargs[31],
                jargs[32], jargs[33], jargs[34], jargs[35], jargs[36], jargs[37], jargs[38], jargs[39],
                jargs[40], jargs[41], jargs[42], jargs[43], jargs[44], jargs[45]);
        }
        callExit(env);
        return result;
    }

    case 47: {
        if (!setjmp(error_jmpbuf)) {
            call47func call47 = (call47func) address;
            result = (*call47)(jargs[0], jargs[1], jargs[2], jargs[3], jargs[4], jargs[5], jargs[6], jargs[7],
                jargs[8], jargs[9], jargs[10], jargs[11], jargs[12], jargs[13], jargs[14], jargs[15],
                jargs[16], jargs[17], jargs[18], jargs[19], jargs[20], jargs[21], jargs[22], jargs[23],
                jargs[24], jargs[25], jargs[26], jargs[27], jargs[28], jargs[29], jargs[30], jargs[31],
                jargs[32], jargs[33], jargs[34], jargs[35], jargs[36], jargs[37], jargs[38], jargs[39],
                jargs[40], jargs[41], jargs[42], jargs[43], jargs[44], jargs[45], jargs[46]);
        }
        callExit(env);
        return result;
    }

    case 48: {
        if (!setjmp(error_jmpbuf)) {
            call48func call48 = (call48func) address;
            result = (*call48)(jargs[0], jargs[1], jargs[2], jargs[3], jargs[4], jargs[5], jargs[6], jargs[7],
                jargs[8], jargs[9], jargs[10], jargs[11], jargs[12], jargs[13], jargs[14], jargs[15],
                jargs[16], jargs[17], jargs[18], jargs[19], jargs[20], jargs[21], jargs[22], jargs[23],
                jargs[24], jargs[25], jargs[26], jargs[27], jargs[28], jargs[29], jargs[30], jargs[31],
                jargs[32], jargs[33], jargs[34], jargs[35], jargs[36], jargs[37], jargs[38], jargs[39],
                jargs[40], jargs[41], jargs[42], jargs[43], jargs[44], jargs[45], jargs[46], jargs[47]
                );
        }
        callExit(env);
        return result;
    }

    case 49: {
        if (!setjmp(error_jmpbuf)) {
            call49func call49 = (call49func) address;
            result = (*call49)(jargs[0], jargs[1], jargs[2], jargs[3], jargs[4], jargs[5], jargs[6], jargs[7],
                jargs[8], jargs[9], jargs[10], jargs[11], jargs[12], jargs[13], jargs[14], jargs[15],
                jargs[16], jargs[17], jargs[18], jargs[19], jargs[20], jargs[21], jargs[22], jargs[23],
                jargs[24], jargs[25], jargs[26], jargs[27], jargs[28], jargs[29], jargs[30], jargs[31],
                jargs[32], jargs[33], jargs[34], jargs[35], jargs[36], jargs[37], jargs[38], jargs[39],
                jargs[40], jargs[41], jargs[42], jargs[43], jargs[44], jargs[45], jargs[46], jargs[47],
                jargs[48]);
        }
        callExit(env);
        return result;
    }

    case 50: {
        if (!setjmp(error_jmpbuf)) {
            call50func call50 = (call50func) address;
            result = (*call50)(jargs[0], jargs[1], jargs[2], jargs[3], jargs[4], jargs[5], jargs[6], jargs[7],
                jargs[8], jargs[9], jargs[10], jargs[11], jargs[12], jargs[13], jargs[14], jargs[15],
                jargs[16], jargs[17], jargs[18], jargs[19], jargs[20], jargs[21], jargs[22], jargs[23],
                jargs[24], jargs[25], jargs[26], jargs[27], jargs[28], jargs[29], jargs[30], jargs[31],
                jargs[32], jargs[33], jargs[34], jargs[35], jargs[36], jargs[37], jargs[38], jargs[39],
                jargs[40], jargs[41], jargs[42], jargs[43], jargs[44], jargs[45], jargs[46], jargs[47],
                jargs[48], jargs[49]);
        }
        callExit(env);
        return result;
    }

    case 51: {
        if (!setjmp(error_jmpbuf)) {
            call51func call51 = (call51func) address;
            result = (*call51)(jargs[0], jargs[1], jargs[2], jargs[3], jargs[4], jargs[5], jargs[6], jargs[7],
                jargs[8], jargs[9], jargs[10], jargs[11], jargs[12], jargs[13], jargs[14], jargs[15],
                jargs[16], jargs[17], jargs[18], jargs[19], jargs[20], jargs[21], jargs[22], jargs[23],
                jargs[24], jargs[25], jargs[26], jargs[27], jargs[28], jargs[29], jargs[30], jargs[31],
                jargs[32], jargs[33], jargs[34], jargs[35], jargs[36], jargs[37], jargs[38], jargs[39],
                jargs[40], jargs[41], jargs[42], jargs[43], jargs[44], jargs[45], jargs[46], jargs[47],
                jargs[48], jargs[49], jargs[50]);
        }
        callExit(env);
        return result;
    }

    case 52: {
        if (!setjmp(error_jmpbuf)) {
            call52func call52 = (call52func) address;
            result = (*call52)(jargs[0], jargs[1], jargs[2], jargs[3], jargs[4], jargs[5], jargs[6], jargs[7],
                jargs[8], jargs[9], jargs[10], jargs[11], jargs[12], jargs[13], jargs[14], jargs[15],
                jargs[16], jargs[17], jargs[18], jargs[19], jargs[20], jargs[21], jargs[22], jargs[23],
                jargs[24], jargs[25], jargs[26], jargs[27], jargs[28], jargs[29], jargs[30], jargs[31],
                jargs[32], jargs[33], jargs[34], jargs[35], jargs[36], jargs[37], jargs[38], jargs[39],
                jargs[40], jargs[41], jargs[42], jargs[43], jargs[44], jargs[45], jargs[46], jargs[47],
                jargs[48], jargs[49], jargs[50], jargs[51]);
        }
        callExit(env);
        return result;
    }

    case 53: {
        if (!setjmp(error_jmpbuf)) {
            call53func call53 = (call53func) address;
            result = (*call53)(jargs[0], jargs[1], jargs[2], jargs[3], jargs[4], jargs[5], jargs[6], jargs[7],
                jargs[8], jargs[9], jargs[10], jargs[11], jargs[12], jargs[13], jargs[14], jargs[15],
                jargs[16], jargs[17], jargs[18], jargs[19], jargs[20], jargs[21], jargs[22], jargs[23],
                jargs[24], jargs[25], jargs[26], jargs[27], jargs[28], jargs[29], jargs[30], jargs[31],
                jargs[32], jargs[33], jargs[34], jargs[35], jargs[36], jargs[37], jargs[38], jargs[39],
                jargs[40], jargs[41], jargs[42], jargs[43], jargs[44], jargs[45], jargs[46], jargs[47],
                jargs[48], jargs[49], jargs[50], jargs[51], jargs[52]);
        }
        callExit(env);
        return result;
    }

    case 54: {
        if (!setjmp(error_jmpbuf)) {
            call54func call54 = (call54func) address;
            result = (*call54)(jargs[0], jargs[1], jargs[2], jargs[3], jargs[4], jargs[5], jargs[6], jargs[7],
                jargs[8], jargs[9], jargs[10], jargs[11], jargs[12], jargs[13], jargs[14], jargs[15],
                jargs[16], jargs[17], jargs[18], jargs[19], jargs[20], jargs[21], jargs[22], jargs[23],
                jargs[24], jargs[25], jargs[26], jargs[27], jargs[28], jargs[29], jargs[30], jargs[31],
                jargs[32], jargs[33], jargs[34], jargs[35], jargs[36], jargs[37], jargs[38], jargs[39],
                jargs[40], jargs[41], jargs[42], jargs[43], jargs[44], jargs[45], jargs[46], jargs[47],
                jargs[48], jargs[49], jargs[50], jargs[51], jargs[52], jargs[53]);
        }
        callExit(env);
        return result;
    }

    case 55: {
        if (!setjmp(error_jmpbuf)) {
            call55func call55 = (call55func) address;
            result = (*call55)(jargs[0], jargs[1], jargs[2], jargs[3], jargs[4], jargs[5], jargs[6], jargs[7],
                jargs[8], jargs[9], jargs[10], jargs[11], jargs[12], jargs[13], jargs[14], jargs[15],
                jargs[16], jargs[17], jargs[18], jargs[19], jargs[20], jargs[21], jargs[22], jargs[23],
                jargs[24], jargs[25], jargs[26], jargs[27], jargs[28], jargs[29], jargs[30], jargs[31],
                jargs[32], jargs[33], jargs[34], jargs[35], jargs[36], jargs[37], jargs[38], jargs[39],
                jargs[40], jargs[41], jargs[42], jargs[43], jargs[44], jargs[45], jargs[46], jargs[47],
                jargs[48], jargs[49], jargs[50], jargs[51], jargs[52], jargs[53], jargs[54]);
        }
        callExit(env);
        return result;
    }

    case 56: {
        if (!setjmp(error_jmpbuf)) {
            call56func call56 = (call56func) address;
            result = (*call56)(jargs[0], jargs[1], jargs[2], jargs[3], jargs[4], jargs[5], jargs[6], jargs[7],
                jargs[8], jargs[9], jargs[10], jargs[11], jargs[12], jargs[13], jargs[14], jargs[15],
                jargs[16], jargs[17], jargs[18], jargs[19], jargs[20], jargs[21], jargs[22], jargs[23],
                jargs[24], jargs[25], jargs[26], jargs[27], jargs[28], jargs[29], jargs[30], jargs[31],
                jargs[32], jargs[33], jargs[34], jargs[35], jargs[36], jargs[37], jargs[38], jargs[39],
                jargs[40], jargs[41], jargs[42], jargs[43], jargs[44], jargs[45], jargs[46], jargs[47],
                jargs[48], jargs[49], jargs[50], jargs[51], jargs[52], jargs[53], jargs[54], jargs[55]
                );
        }
        callExit(env);
        return result;
    }

    case 57: {
        if (!setjmp(error_jmpbuf)) {
            call57func call57 = (call57func) address;
            result = (*call57)(jargs[0], jargs[1], jargs[2], jargs[3], jargs[4], jargs[5], jargs[6], jargs[7],
                jargs[8], jargs[9], jargs[10], jargs[11], jargs[12], jargs[13], jargs[14], jargs[15],
                jargs[16], jargs[17], jargs[18], jargs[19], jargs[20], jargs[21], jargs[22], jargs[23],
                jargs[24], jargs[25], jargs[26], jargs[27], jargs[28], jargs[29], jargs[30], jargs[31],
                jargs[32], jargs[33], jargs[34], jargs[35], jargs[36], jargs[37], jargs[38], jargs[39],
                jargs[40], jargs[41], jargs[42], jargs[43], jargs[44], jargs[45], jargs[46], jargs[47],
                jargs[48], jargs[49], jargs[50], jargs[51], jargs[52], jargs[53], jargs[54], jargs[55],
                jargs[56]);
        }
        callExit(env);
        return result;
    }

    case 58: {
        if (!setjmp(error_jmpbuf)) {
            call58func call58 = (call58func) address;
            result = (*call58)(jargs[0], jargs[1], jargs[2], jargs[3], jargs[4], jargs[5], jargs[6], jargs[7],
                jargs[8], jargs[9], jargs[10], jargs[11], jargs[12], jargs[13], jargs[14], jargs[15],
                jargs[16], jargs[17], jargs[18], jargs[19], jargs[20], jargs[21], jargs[22], jargs[23],
                jargs[24], jargs[25], jargs[26], jargs[27], jargs[28], jargs[29], jargs[30], jargs[31],
                jargs[32], jargs[33], jargs[34], jargs[35], jargs[36], jargs[37], jargs[38], jargs[39],
                jargs[40], jargs[41], jargs[42], jargs[43], jargs[44], jargs[45], jargs[46], jargs[47],
                jargs[48], jargs[49], jargs[50], jargs[51], jargs[52], jargs[53], jargs[54], jargs[55],
                jargs[56], jargs[57]);
        }
        callExit(env);
        return result;
    }

    case 59: {
        if (!setjmp(error_jmpbuf)) {
            call59func call59 = (call59func) address;
            result = (*call59)(jargs[0], jargs[1], jargs[2], jargs[3], jargs[4], jargs[5], jargs[6], jargs[7],
                jargs[8], jargs[9], jargs[10], jargs[11], jargs[12], jargs[13], jargs[14], jargs[15],
                jargs[16], jargs[17], jargs[18], jargs[19], jargs[20], jargs[21], jargs[22], jargs[23],
                jargs[24], jargs[25], jargs[26], jargs[27], jargs[28], jargs[29], jargs[30], jargs[31],
                jargs[32], jargs[33], jargs[34], jargs[35], jargs[36], jargs[37], jargs[38], jargs[39],
                jargs[40], jargs[41], jargs[42], jargs[43], jargs[44], jargs[45], jargs[46], jargs[47],
                jargs[48], jargs[49], jargs[50], jargs[51], jargs[52], jargs[53], jargs[54], jargs[55],
                jargs[56], jargs[57], jargs[58]);
        }
        callExit(env);
        return result;
    }

    case 60: {
        if (!setjmp(error_jmpbuf)) {
            call60func call60 = (call60func) address;
            result = (*call60)(jargs[0], jargs[1], jargs[2], jargs[3], jargs[4], jargs[5], jargs[6], jargs[7],
                jargs[8], jargs[9], jargs[10], jargs[11], jargs[12], jargs[13], jargs[14], jargs[15],
                jargs[16], jargs[17], jargs[18], jargs[19], jargs[20], jargs[21], jargs[22], jargs[23],
                jargs[24], jargs[25], jargs[26], jargs[27], jargs[28], jargs[29], jargs[30], jargs[31],
                jargs[32], jargs[33], jargs[34], jargs[35], jargs[36], jargs[37], jargs[38], jargs[39],
                jargs[40], jargs[41], jargs[42], jargs[43], jargs[44], jargs[45], jargs[46], jargs[47],
                jargs[48], jargs[49], jargs[50], jargs[51], jargs[52], jargs[53], jargs[54], jargs[55],
                jargs[56], jargs[57], jargs[58], jargs[59]);
        }
        callExit(env);
        return result;
    }

    case 61: {
        if (!setjmp(error_jmpbuf)) {
            call61func call61 = (call61func) address;
            result = (*call61)(jargs[0], jargs[1], jargs[2], jargs[3], jargs[4], jargs[5], jargs[6], jargs[7],
                jargs[8], jargs[9], jargs[10], jargs[11], jargs[12], jargs[13], jargs[14], jargs[15],
                jargs[16], jargs[17], jargs[18], jargs[19], jargs[20], jargs[21], jargs[22], jargs[23],
                jargs[24], jargs[25], jargs[26], jargs[27], jargs[28], jargs[29], jargs[30], jargs[31],
                jargs[32], jargs[33], jargs[34], jargs[35], jargs[36], jargs[37], jargs[38], jargs[39],
                jargs[40], jargs[41], jargs[42], jargs[43], jargs[44], jargs[45], jargs[46], jargs[47],
                jargs[48], jargs[49], jargs[50], jargs[51], jargs[52], jargs[53], jargs[54], jargs[55],
                jargs[56], jargs[57], jargs[58], jargs[59], jargs[60]);
        }
        callExit(env);
        return result;
    }

    case 62: {
        if (!setjmp(error_jmpbuf)) {
            call62func call62 = (call62func) address;
            result = (*call62)(jargs[0], jargs[1], jargs[2], jargs[3], jargs[4], jargs[5], jargs[6], jargs[7],
                jargs[8], jargs[9], jargs[10], jargs[11], jargs[12], jargs[13], jargs[14], jargs[15],
                jargs[16], jargs[17], jargs[18], jargs[19], jargs[20], jargs[21], jargs[22], jargs[23],
                jargs[24], jargs[25], jargs[26], jargs[27], jargs[28], jargs[29], jargs[30], jargs[31],
                jargs[32], jargs[33], jargs[34], jargs[35], jargs[36], jargs[37], jargs[38], jargs[39],
                jargs[40], jargs[41], jargs[42], jargs[43], jargs[44], jargs[45], jargs[46], jargs[47],
                jargs[48], jargs[49], jargs[50], jargs[51], jargs[52], jargs[53], jargs[54], jargs[55],
                jargs[56], jargs[57], jargs[58], jargs[59], jargs[60], jargs[61]);
        }
        callExit(env);
        return result;
    }

    case 63: {
        if (!setjmp(error_jmpbuf)) {
            call63func call63 = (call63func) address;
            result = (*call63)(jargs[0], jargs[1], jargs[2], jargs[3], jargs[4], jargs[5], jargs[6], jargs[7],
                jargs[8], jargs[9], jargs[10], jargs[11], jargs[12], jargs[13], jargs[14], jargs[15],
                jargs[16], jargs[17], jargs[18], jargs[19], jargs[20], jargs[21], jargs[22], jargs[23],
                jargs[24], jargs[25], jargs[26], jargs[27], jargs[28], jargs[29], jargs[30], jargs[31],
                jargs[32], jargs[33], jargs[34], jargs[35], jargs[36], jargs[37], jargs[38], jargs[39],
                jargs[40], jargs[41], jargs[42], jargs[43], jargs[44], jargs[45], jargs[46], jargs[47],
                jargs[48], jargs[49], jargs[50], jargs[51], jargs[52], jargs[53], jargs[54], jargs[55],
                jargs[56], jargs[57], jargs[58], jargs[59], jargs[60], jargs[61], jargs[62]);
        }
        callExit(env);
        return result;
    }

    case 64: {
        if (!setjmp(error_jmpbuf)) {
            call64func call64 = (call64func) address;
            result = (*call64)(jargs[0], jargs[1], jargs[2], jargs[3], jargs[4], jargs[5], jargs[6], jargs[7],
                jargs[8], jargs[9], jargs[10], jargs[11], jargs[12], jargs[13], jargs[14], jargs[15],
                jargs[16], jargs[17], jargs[18], jargs[19], jargs[20], jargs[21], jargs[22], jargs[23],
                jargs[24], jargs[25], jargs[26], jargs[27], jargs[28], jargs[29], jargs[30], jargs[31],
                jargs[32], jargs[33], jargs[34], jargs[35], jargs[36], jargs[37], jargs[38], jargs[39],
                jargs[40], jargs[41], jargs[42], jargs[43], jargs[44], jargs[45], jargs[46], jargs[47],
                jargs[48], jargs[49], jargs[50], jargs[51], jargs[52], jargs[53], jargs[54], jargs[55],
                jargs[56], jargs[57], jargs[58], jargs[59], jargs[60], jargs[61], jargs[62], jargs[63]
                );
        }
        callExit(env);
        return result;
    }


	default:
		(*env)->FatalError(env, "call(JNI): unimplemented number of arguments");
		return NULL;
	}
}

typedef void (*callVoid1func)(SEXP arg1);

JNIEXPORT void JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jni_JNI_1Call_callVoid1(JNIEnv *env, jclass c, jlong address, jobject arg1) {
	jmp_buf error_jmpbuf;
	callEnter(env, &error_jmpbuf);
	if (!setjmp(error_jmpbuf)) {
		callVoid1func call1 = (callVoid1func) address;
		(*call1)(arg1);
	}
	callExit(env);
}

typedef void (*callVoid0func)();

JNIEXPORT void JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jni_JNI_1Call_callVoid0(JNIEnv *env, jclass c, jlong address) {
	jmp_buf error_jmpbuf;
	callEnter(env, &error_jmpbuf);
	if (!setjmp(error_jmpbuf)) {
		callVoid0func call1 = (callVoid0func) address;
		(*call1)();
	}
	callExit(env);
}

#include <Rinterface.h>

JNIEXPORT void JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jni_JNI_1Call_nativeSetInteractive(JNIEnv *env, jclass c, jboolean interactive) {
	R_Interactive = interactive;
}




