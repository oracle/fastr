/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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

#include <stdlib.h>
#include <string.h>
#include <limits.h>
#include <Rinternals.h>
#include <trufflenfi.h>

extern void init_memory();

extern void init_utils(TruffleEnv *env);

char *ensure_truffle_chararray(const char *x);

void *ensure_string(const char *x);

// use for an unimplemented API function
void *unimplemented(const char *msg) __attribute__((noreturn));

// use to immediately exit from the current .Call/.Fortran
void exitCall() __attribute__((noreturn));

// use for any fatal error
void fatalError(const char *msg) __attribute__((noreturn));

// Boilerplate methods for the actual calls

typedef void (*callvoid0func)(void);

typedef void (*callvoid1func)(SEXP arg1);

void set_shutdown_phase(unsigned char value);

int is_shutdown_phase();

void dot_call_void0(callvoid0func);

void dot_call_void1(callvoid1func, SEXP);


typedef SEXP (*call0func)();

typedef SEXP (*call1func)(SEXP arg1);

typedef SEXP (*call2func)(SEXP arg1, SEXP arg2);

typedef SEXP (*call3func)(SEXP arg1, SEXP arg2, SEXP arg3);

typedef SEXP (*call4func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4);

typedef SEXP (*call5func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5);

typedef SEXP (*call6func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6);

typedef SEXP (*call7func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7);

typedef SEXP (*call8func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8);

typedef SEXP (*call9func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
                          SEXP arg9);

typedef SEXP (*call10func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
                           SEXP arg9, SEXP arg10);

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
                           SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                           SEXP arg16,
                           SEXP arg17);

typedef SEXP (*call18func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
                           SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                           SEXP arg16,
                           SEXP arg17, SEXP arg18);

typedef SEXP (*call19func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
                           SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                           SEXP arg16,
                           SEXP arg17, SEXP arg18, SEXP arg19);

typedef SEXP (*call20func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
                           SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                           SEXP arg16,
                           SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20);

typedef SEXP (*call21func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
                           SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                           SEXP arg16,
                           SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21);

typedef SEXP (*call22func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
                           SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                           SEXP arg16,
                           SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22);

typedef SEXP (*call23func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
                           SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                           SEXP arg16,
                           SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23);

typedef SEXP (*call24func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
                           SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                           SEXP arg16,
                           SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                           SEXP arg24
);

typedef SEXP (*call25func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
                           SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                           SEXP arg16,
                           SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                           SEXP arg24,
                           SEXP arg25);

typedef SEXP (*call26func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
                           SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                           SEXP arg16,
                           SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                           SEXP arg24,
                           SEXP arg25, SEXP arg26);

typedef SEXP (*call27func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
                           SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                           SEXP arg16,
                           SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                           SEXP arg24,
                           SEXP arg25, SEXP arg26, SEXP arg27);

typedef SEXP (*call28func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
                           SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                           SEXP arg16,
                           SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                           SEXP arg24,
                           SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28);

typedef SEXP (*call29func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
                           SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                           SEXP arg16,
                           SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                           SEXP arg24,
                           SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29);

typedef SEXP (*call30func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
                           SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                           SEXP arg16,
                           SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                           SEXP arg24,
                           SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30);

typedef SEXP (*call31func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
                           SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                           SEXP arg16,
                           SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                           SEXP arg24,
                           SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31);

typedef SEXP (*call32func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
                           SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                           SEXP arg16,
                           SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                           SEXP arg24,
                           SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                           SEXP arg32
);

typedef SEXP (*call33func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
                           SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                           SEXP arg16,
                           SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                           SEXP arg24,
                           SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                           SEXP arg32,
                           SEXP arg33);

typedef SEXP (*call34func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
                           SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                           SEXP arg16,
                           SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                           SEXP arg24,
                           SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                           SEXP arg32,
                           SEXP arg33, SEXP arg34);

typedef SEXP (*call35func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
                           SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                           SEXP arg16,
                           SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                           SEXP arg24,
                           SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                           SEXP arg32,
                           SEXP arg33, SEXP arg34, SEXP arg35);

typedef SEXP (*call36func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
                           SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                           SEXP arg16,
                           SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                           SEXP arg24,
                           SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                           SEXP arg32,
                           SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36);

typedef SEXP (*call37func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
                           SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                           SEXP arg16,
                           SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                           SEXP arg24,
                           SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                           SEXP arg32,
                           SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37);

typedef SEXP (*call38func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
                           SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                           SEXP arg16,
                           SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                           SEXP arg24,
                           SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                           SEXP arg32,
                           SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38);

typedef SEXP (*call39func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
                           SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                           SEXP arg16,
                           SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                           SEXP arg24,
                           SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                           SEXP arg32,
                           SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39);

typedef SEXP (*call40func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
                           SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                           SEXP arg16,
                           SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                           SEXP arg24,
                           SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                           SEXP arg32,
                           SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39,
                           SEXP arg40
);

typedef SEXP (*call41func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
                           SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                           SEXP arg16,
                           SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                           SEXP arg24,
                           SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                           SEXP arg32,
                           SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39,
                           SEXP arg40,
                           SEXP arg41);

typedef SEXP (*call42func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
                           SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                           SEXP arg16,
                           SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                           SEXP arg24,
                           SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                           SEXP arg32,
                           SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39,
                           SEXP arg40,
                           SEXP arg41, SEXP arg42);

typedef SEXP (*call43func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
                           SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                           SEXP arg16,
                           SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                           SEXP arg24,
                           SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                           SEXP arg32,
                           SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39,
                           SEXP arg40,
                           SEXP arg41, SEXP arg42, SEXP arg43);

typedef SEXP (*call44func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
                           SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                           SEXP arg16,
                           SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                           SEXP arg24,
                           SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                           SEXP arg32,
                           SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39,
                           SEXP arg40,
                           SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44);

typedef SEXP (*call45func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
                           SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                           SEXP arg16,
                           SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                           SEXP arg24,
                           SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                           SEXP arg32,
                           SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39,
                           SEXP arg40,
                           SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44, SEXP arg45);

typedef SEXP (*call46func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
                           SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                           SEXP arg16,
                           SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                           SEXP arg24,
                           SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                           SEXP arg32,
                           SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39,
                           SEXP arg40,
                           SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44, SEXP arg45, SEXP arg46);

typedef SEXP (*call47func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
                           SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                           SEXP arg16,
                           SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                           SEXP arg24,
                           SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                           SEXP arg32,
                           SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39,
                           SEXP arg40,
                           SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44, SEXP arg45, SEXP arg46, SEXP arg47);

typedef SEXP (*call48func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
                           SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                           SEXP arg16,
                           SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                           SEXP arg24,
                           SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                           SEXP arg32,
                           SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39,
                           SEXP arg40,
                           SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44, SEXP arg45, SEXP arg46, SEXP arg47,
                           SEXP arg48
);

typedef SEXP (*call49func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
                           SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                           SEXP arg16,
                           SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                           SEXP arg24,
                           SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                           SEXP arg32,
                           SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39,
                           SEXP arg40,
                           SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44, SEXP arg45, SEXP arg46, SEXP arg47,
                           SEXP arg48,
                           SEXP arg49);

typedef SEXP (*call50func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
                           SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                           SEXP arg16,
                           SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                           SEXP arg24,
                           SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                           SEXP arg32,
                           SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39,
                           SEXP arg40,
                           SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44, SEXP arg45, SEXP arg46, SEXP arg47,
                           SEXP arg48,
                           SEXP arg49, SEXP arg50);

typedef SEXP (*call51func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
                           SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                           SEXP arg16,
                           SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                           SEXP arg24,
                           SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                           SEXP arg32,
                           SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39,
                           SEXP arg40,
                           SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44, SEXP arg45, SEXP arg46, SEXP arg47,
                           SEXP arg48,
                           SEXP arg49, SEXP arg50, SEXP arg51);

typedef SEXP (*call52func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
                           SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                           SEXP arg16,
                           SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                           SEXP arg24,
                           SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                           SEXP arg32,
                           SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39,
                           SEXP arg40,
                           SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44, SEXP arg45, SEXP arg46, SEXP arg47,
                           SEXP arg48,
                           SEXP arg49, SEXP arg50, SEXP arg51, SEXP arg52);

typedef SEXP (*call53func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
                           SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                           SEXP arg16,
                           SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                           SEXP arg24,
                           SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                           SEXP arg32,
                           SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39,
                           SEXP arg40,
                           SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44, SEXP arg45, SEXP arg46, SEXP arg47,
                           SEXP arg48,
                           SEXP arg49, SEXP arg50, SEXP arg51, SEXP arg52, SEXP arg53);

typedef SEXP (*call54func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
                           SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                           SEXP arg16,
                           SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                           SEXP arg24,
                           SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                           SEXP arg32,
                           SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39,
                           SEXP arg40,
                           SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44, SEXP arg45, SEXP arg46, SEXP arg47,
                           SEXP arg48,
                           SEXP arg49, SEXP arg50, SEXP arg51, SEXP arg52, SEXP arg53, SEXP arg54);

typedef SEXP (*call55func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
                           SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                           SEXP arg16,
                           SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                           SEXP arg24,
                           SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                           SEXP arg32,
                           SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39,
                           SEXP arg40,
                           SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44, SEXP arg45, SEXP arg46, SEXP arg47,
                           SEXP arg48,
                           SEXP arg49, SEXP arg50, SEXP arg51, SEXP arg52, SEXP arg53, SEXP arg54, SEXP arg55);

typedef SEXP (*call56func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
                           SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                           SEXP arg16,
                           SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                           SEXP arg24,
                           SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                           SEXP arg32,
                           SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39,
                           SEXP arg40,
                           SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44, SEXP arg45, SEXP arg46, SEXP arg47,
                           SEXP arg48,
                           SEXP arg49, SEXP arg50, SEXP arg51, SEXP arg52, SEXP arg53, SEXP arg54, SEXP arg55,
                           SEXP arg56
);

typedef SEXP (*call57func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
                           SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                           SEXP arg16,
                           SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                           SEXP arg24,
                           SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                           SEXP arg32,
                           SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39,
                           SEXP arg40,
                           SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44, SEXP arg45, SEXP arg46, SEXP arg47,
                           SEXP arg48,
                           SEXP arg49, SEXP arg50, SEXP arg51, SEXP arg52, SEXP arg53, SEXP arg54, SEXP arg55,
                           SEXP arg56,
                           SEXP arg57);

typedef SEXP (*call58func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
                           SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                           SEXP arg16,
                           SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                           SEXP arg24,
                           SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                           SEXP arg32,
                           SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39,
                           SEXP arg40,
                           SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44, SEXP arg45, SEXP arg46, SEXP arg47,
                           SEXP arg48,
                           SEXP arg49, SEXP arg50, SEXP arg51, SEXP arg52, SEXP arg53, SEXP arg54, SEXP arg55,
                           SEXP arg56,
                           SEXP arg57, SEXP arg58);

typedef SEXP (*call59func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
                           SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                           SEXP arg16,
                           SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                           SEXP arg24,
                           SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                           SEXP arg32,
                           SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39,
                           SEXP arg40,
                           SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44, SEXP arg45, SEXP arg46, SEXP arg47,
                           SEXP arg48,
                           SEXP arg49, SEXP arg50, SEXP arg51, SEXP arg52, SEXP arg53, SEXP arg54, SEXP arg55,
                           SEXP arg56,
                           SEXP arg57, SEXP arg58, SEXP arg59);

typedef SEXP (*call60func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
                           SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                           SEXP arg16,
                           SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                           SEXP arg24,
                           SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                           SEXP arg32,
                           SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39,
                           SEXP arg40,
                           SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44, SEXP arg45, SEXP arg46, SEXP arg47,
                           SEXP arg48,
                           SEXP arg49, SEXP arg50, SEXP arg51, SEXP arg52, SEXP arg53, SEXP arg54, SEXP arg55,
                           SEXP arg56,
                           SEXP arg57, SEXP arg58, SEXP arg59, SEXP arg60);

typedef SEXP (*call61func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
                           SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                           SEXP arg16,
                           SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                           SEXP arg24,
                           SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                           SEXP arg32,
                           SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39,
                           SEXP arg40,
                           SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44, SEXP arg45, SEXP arg46, SEXP arg47,
                           SEXP arg48,
                           SEXP arg49, SEXP arg50, SEXP arg51, SEXP arg52, SEXP arg53, SEXP arg54, SEXP arg55,
                           SEXP arg56,
                           SEXP arg57, SEXP arg58, SEXP arg59, SEXP arg60, SEXP arg61);

typedef SEXP (*call62func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
                           SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                           SEXP arg16,
                           SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                           SEXP arg24,
                           SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                           SEXP arg32,
                           SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39,
                           SEXP arg40,
                           SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44, SEXP arg45, SEXP arg46, SEXP arg47,
                           SEXP arg48,
                           SEXP arg49, SEXP arg50, SEXP arg51, SEXP arg52, SEXP arg53, SEXP arg54, SEXP arg55,
                           SEXP arg56,
                           SEXP arg57, SEXP arg58, SEXP arg59, SEXP arg60, SEXP arg61, SEXP arg62);

typedef SEXP (*call63func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
                           SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                           SEXP arg16,
                           SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                           SEXP arg24,
                           SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                           SEXP arg32,
                           SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39,
                           SEXP arg40,
                           SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44, SEXP arg45, SEXP arg46, SEXP arg47,
                           SEXP arg48,
                           SEXP arg49, SEXP arg50, SEXP arg51, SEXP arg52, SEXP arg53, SEXP arg54, SEXP arg55,
                           SEXP arg56,
                           SEXP arg57, SEXP arg58, SEXP arg59, SEXP arg60, SEXP arg61, SEXP arg62, SEXP arg63);

typedef SEXP (*call64func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8,
                           SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                           SEXP arg16,
                           SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                           SEXP arg24,
                           SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                           SEXP arg32,
                           SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39,
                           SEXP arg40,
                           SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44, SEXP arg45, SEXP arg46, SEXP arg47,
                           SEXP arg48,
                           SEXP arg49, SEXP arg50, SEXP arg51, SEXP arg52, SEXP arg53, SEXP arg54, SEXP arg55,
                           SEXP arg56,
                           SEXP arg57, SEXP arg58, SEXP arg59, SEXP arg60, SEXP arg61, SEXP arg62, SEXP arg63,
                           SEXP arg64
);

SEXP dot_call0(call0func);

SEXP dot_call1(call1func, SEXP);

SEXP dot_call2(call2func, SEXP, SEXP);

SEXP dot_call3(call3func, SEXP, SEXP, SEXP);

SEXP dot_call4(call4func, SEXP, SEXP, SEXP, SEXP);

SEXP dot_call5(call5func, SEXP, SEXP, SEXP, SEXP, SEXP);

SEXP dot_call6(call6func, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP);

SEXP dot_call7(call7func, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP);

SEXP dot_call8(call8func, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP);

SEXP dot_call9(call9func, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP);

SEXP dot_call10(call10func, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP);

SEXP dot_call11(call11func, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP);

SEXP dot_call12(call12func, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP);

SEXP dot_call13(call13func, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP);

SEXP dot_call14(call14func, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP);

SEXP dot_call15(call15func, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP);

SEXP
dot_call16(call16func, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP);

SEXP
dot_call17(call17func, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP);

SEXP
dot_call18(call18func, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP);

SEXP
dot_call19(call19func, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP);

SEXP
dot_call20(call20func, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP);

SEXP
dot_call21(call21func, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP);

SEXP
dot_call22(call22func, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP);

SEXP
dot_call23(call23func, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP);

SEXP
dot_call24(call24func, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP);

SEXP
dot_call25(call25func, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP);

SEXP
dot_call26(call26func, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP);

SEXP
dot_call27(call27func, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP);

SEXP
dot_call28(call28func, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP);

SEXP
dot_call29(call29func, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP);

SEXP
dot_call30(call30func, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP);

SEXP
dot_call31(call31func, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP);

SEXP
dot_call32(call32func, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP);

SEXP
dot_call33(call33func, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP);

SEXP
dot_call34(call34func, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP);

SEXP
dot_call35(call35func, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP);

SEXP
dot_call36(call36func, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP);

SEXP
dot_call37(call37func, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP);

SEXP
dot_call38(call38func, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP);

SEXP
dot_call39(call39func, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP);

SEXP
dot_call40(call40func, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP);

SEXP
dot_call41(call41func, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP);

SEXP
dot_call42(call42func, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP);

SEXP
dot_call43(call43func, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP);

SEXP
dot_call44(call44func, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP);

SEXP
dot_call45(call45func, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP);

SEXP
dot_call46(call46func, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP);

SEXP
dot_call47(call47func, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP);

SEXP
dot_call48(call48func, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP);

SEXP
dot_call49(call49func, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP);

SEXP
dot_call50(call50func, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP);

SEXP
dot_call51(call51func, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP);

SEXP
dot_call52(call52func, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP);

SEXP
dot_call53(call53func, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP);

SEXP
dot_call54(call54func, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP);

SEXP
dot_call55(call55func, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP);

SEXP
dot_call56(call56func, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP);

SEXP
dot_call57(call57func, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP);

SEXP
dot_call58(call58func, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP);

SEXP
dot_call59(call59func, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP);

SEXP
dot_call60(call60func, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP);

SEXP
dot_call61(call61func, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP);

SEXP
dot_call62(call62func, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP);

SEXP
dot_call63(call63func, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP);

SEXP
dot_call64(call64func, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP,
           SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP, SEXP);

#endif
