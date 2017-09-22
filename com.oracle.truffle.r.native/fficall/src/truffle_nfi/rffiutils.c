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

#include <setjmp.h>
#include <stdlib.h>
#include <stdio.h>

#include <rffiutils.h>
#include <trufflenfi.h>

void *unimplemented(const char *f) {
    printf("unimplemented %s\n", f);
    exit(1);
}

static TruffleContext *truffleContext = NULL;

void init_utils(TruffleEnv *env) {
    if (truffleContext == NULL) {
        truffleContext = (*env)->getTruffleContext(env);
    }
}

static unsigned char shutdown_phase = 0;

#define ERROR_JMP_BUF_STACK_SIZE 32
static jmp_buf *callErrorJmpBufStack[ERROR_JMP_BUF_STACK_SIZE];
static int callErrorJmpBufStackIndex = 0;

void exitCall() {
    longjmp(*callErrorJmpBufStack[callErrorJmpBufStackIndex - 1], 1);
}

static void pushJmpBuf(jmp_buf *buf) {
    if (callErrorJmpBufStackIndex == ERROR_JMP_BUF_STACK_SIZE) {
        fprintf(stderr, "Maximum native call stack size ERROR_JMP_BUF_STACK_SIZE exceeded. Update the constant ERROR%s.\n", "_JMP_BUF_STACK_SIZE");
        exit(1);
    }
    callErrorJmpBufStack[callErrorJmpBufStackIndex++] = buf;
}

static void popJmpBuf() {
    callErrorJmpBufStackIndex--;
}

#define DO_CALL_VOID(call)          \
    jmp_buf error_jmpbuf;           \
    pushJmpBuf(&error_jmpbuf);      \
    if (!setjmp(error_jmpbuf)) {    \
        call;                       \
    }                               \
    popJmpBuf();

#define DO_CALL(call)               \
    jmp_buf error_jmpbuf;           \
    pushJmpBuf(&error_jmpbuf);      \
    SEXP result;                    \
    if (!setjmp(error_jmpbuf)) {    \
        result = call;              \
    }                               \
    popJmpBuf();                    \
    return result;

void set_shutdown_phase(unsigned char value) {
	shutdown_phase = value;
}

int is_shutdown_phase() {
	return shutdown_phase;
}

void dot_call_void0(callvoid0func fun) {
    DO_CALL_VOID(fun());
}

void dot_call_void1(callvoid1func fun, SEXP arg1) {
    DO_CALL_VOID(fun(arg1));
}

SEXP dot_call0(call0func fun) {
    DO_CALL(fun());
}

SEXP dot_call1(call1func fun, SEXP arg0) {
    DO_CALL(fun(arg0));
}

SEXP dot_call2(call2func fun, SEXP arg0, SEXP arg1) {
    DO_CALL(fun(arg0, arg1));
}

SEXP dot_call3(call3func fun, SEXP arg0, SEXP arg1, SEXP arg2) {
    DO_CALL(fun(arg0, arg1, arg2));
}

SEXP dot_call4(call4func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3) {
    DO_CALL(fun(arg0, arg1, arg2, arg3));
}

SEXP dot_call5(call5func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4));
}

SEXP dot_call6(call6func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4, arg5));
}

SEXP dot_call7(call7func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4, arg5, arg6));
}

SEXP dot_call8(call8func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7));
}

SEXP dot_call9(call9func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7,
               SEXP arg8) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8));
}

SEXP dot_call10(call10func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7,
                SEXP arg8, SEXP arg9) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9));
}

SEXP dot_call11(call11func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7,
                SEXP arg8, SEXP arg9, SEXP arg10) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10));
}

SEXP dot_call12(call12func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7,
                SEXP arg8, SEXP arg9, SEXP arg10, SEXP arg11) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11));
}

SEXP dot_call13(call13func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7,
                SEXP arg8, SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12));
}

SEXP dot_call14(call14func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7,
                SEXP arg8, SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13));
}

SEXP dot_call15(call15func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7,
                SEXP arg8, SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14));
}

SEXP dot_call16(call16func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7,
                SEXP arg8, SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15));
}

SEXP dot_call17(call17func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7,
                SEXP arg8, SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                SEXP arg16) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15,
                arg16));
}

SEXP dot_call18(call18func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7,
                SEXP arg8, SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                SEXP arg16, SEXP arg17) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15,
                arg16, arg17));
}

SEXP dot_call19(call19func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7,
                SEXP arg8, SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                SEXP arg16, SEXP arg17, SEXP arg18) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15,
                arg16, arg17, arg18));
}

SEXP dot_call20(call20func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7,
                SEXP arg8, SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                SEXP arg16, SEXP arg17, SEXP arg18, SEXP arg19) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15,
                arg16, arg17, arg18, arg19));
}

SEXP dot_call21(call21func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7,
                SEXP arg8, SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                SEXP arg16, SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15,
                arg16, arg17, arg18, arg19, arg20));
}

SEXP dot_call22(call22func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7,
                SEXP arg8, SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                SEXP arg16, SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15,
                arg16, arg17, arg18, arg19, arg20, arg21));
}

SEXP dot_call23(call23func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7,
                SEXP arg8, SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                SEXP arg16, SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15,
                arg16, arg17, arg18, arg19, arg20, arg21, arg22));
}

SEXP dot_call24(call24func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7,
                SEXP arg8, SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                SEXP arg16, SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15,
                arg16, arg17, arg18, arg19, arg20, arg21, arg22, arg23));
}

SEXP dot_call25(call25func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7,
                SEXP arg8, SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                SEXP arg16, SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                SEXP arg24) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15,
                arg16, arg17, arg18, arg19, arg20, arg21, arg22, arg23, arg24));
}

SEXP dot_call26(call26func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7,
                SEXP arg8, SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                SEXP arg16, SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                SEXP arg24, SEXP arg25) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15,
                arg16, arg17, arg18, arg19, arg20, arg21, arg22, arg23, arg24, arg25));
}

SEXP dot_call27(call27func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7,
                SEXP arg8, SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                SEXP arg16, SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                SEXP arg24, SEXP arg25, SEXP arg26) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15,
                arg16, arg17, arg18, arg19, arg20, arg21, arg22, arg23, arg24, arg25, arg26));
}

SEXP dot_call28(call28func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7,
                SEXP arg8, SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                SEXP arg16, SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                SEXP arg24, SEXP arg25, SEXP arg26, SEXP arg27) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15,
                arg16, arg17, arg18, arg19, arg20, arg21, arg22, arg23, arg24, arg25, arg26, arg27));
}

SEXP dot_call29(call29func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7,
                SEXP arg8, SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                SEXP arg16, SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                SEXP arg24, SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15,
                arg16, arg17, arg18, arg19, arg20, arg21, arg22, arg23, arg24, arg25, arg26, arg27, arg28));
}

SEXP dot_call30(call30func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7,
                SEXP arg8, SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                SEXP arg16, SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                SEXP arg24, SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15,
                arg16, arg17, arg18, arg19, arg20, arg21, arg22, arg23, arg24, arg25, arg26, arg27, arg28, arg29));
}

SEXP dot_call31(call31func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7,
                SEXP arg8, SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                SEXP arg16, SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                SEXP arg24, SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15,
                arg16, arg17, arg18, arg19, arg20, arg21, arg22, arg23, arg24, arg25, arg26, arg27, arg28, arg29,
                arg30));
}

SEXP dot_call32(call32func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7,
                SEXP arg8, SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                SEXP arg16, SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                SEXP arg24, SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15,
                arg16, arg17, arg18, arg19, arg20, arg21, arg22, arg23, arg24, arg25, arg26, arg27, arg28, arg29, arg30,
                arg31));
}

SEXP dot_call33(call33func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7,
                SEXP arg8, SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                SEXP arg16, SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                SEXP arg24, SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                SEXP arg32) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15,
                arg16, arg17, arg18, arg19, arg20, arg21, arg22, arg23, arg24, arg25, arg26, arg27, arg28, arg29, arg30,
                arg31, arg32));
}

SEXP dot_call34(call34func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7,
                SEXP arg8, SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                SEXP arg16, SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                SEXP arg24, SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                SEXP arg32, SEXP arg33) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15,
                arg16, arg17, arg18, arg19, arg20, arg21, arg22, arg23, arg24, arg25, arg26, arg27, arg28, arg29, arg30,
                arg31, arg32, arg33));
}

SEXP dot_call35(call35func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7,
                SEXP arg8, SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                SEXP arg16, SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                SEXP arg24, SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                SEXP arg32, SEXP arg33, SEXP arg34) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15,
                arg16, arg17, arg18, arg19, arg20, arg21, arg22, arg23, arg24, arg25, arg26, arg27, arg28, arg29, arg30,
                arg31, arg32, arg33, arg34));
}

SEXP dot_call36(call36func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7,
                SEXP arg8, SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                SEXP arg16, SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                SEXP arg24, SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                SEXP arg32, SEXP arg33, SEXP arg34, SEXP arg35) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15,
                arg16, arg17, arg18, arg19, arg20, arg21, arg22, arg23, arg24, arg25, arg26, arg27, arg28, arg29, arg30,
                arg31, arg32, arg33, arg34, arg35));
}

SEXP dot_call37(call37func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7,
                SEXP arg8, SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                SEXP arg16, SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                SEXP arg24, SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                SEXP arg32, SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15,
                arg16, arg17, arg18, arg19, arg20, arg21, arg22, arg23, arg24, arg25, arg26, arg27, arg28, arg29, arg30,
                arg31, arg32, arg33, arg34, arg35, arg36));
}

SEXP dot_call38(call38func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7,
                SEXP arg8, SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                SEXP arg16, SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                SEXP arg24, SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                SEXP arg32, SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15,
                arg16, arg17, arg18, arg19, arg20, arg21, arg22, arg23, arg24, arg25, arg26, arg27, arg28, arg29, arg30,
                arg31, arg32, arg33, arg34, arg35, arg36, arg37));
}

SEXP dot_call39(call39func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7,
                SEXP arg8, SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                SEXP arg16, SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                SEXP arg24, SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                SEXP arg32, SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15,
                arg16, arg17, arg18, arg19, arg20, arg21, arg22, arg23, arg24, arg25, arg26, arg27, arg28, arg29, arg30,
                arg31, arg32, arg33, arg34, arg35, arg36, arg37, arg38));
}

SEXP dot_call40(call40func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7,
                SEXP arg8, SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                SEXP arg16, SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                SEXP arg24, SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                SEXP arg32, SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15,
                arg16, arg17, arg18, arg19, arg20, arg21, arg22, arg23, arg24, arg25, arg26, arg27, arg28, arg29, arg30,
                arg31, arg32, arg33, arg34, arg35, arg36, arg37, arg38, arg39));
}

SEXP dot_call41(call41func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7,
                SEXP arg8, SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                SEXP arg16, SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                SEXP arg24, SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                SEXP arg32, SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39,
                SEXP arg40) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15,
                arg16, arg17, arg18, arg19, arg20, arg21, arg22, arg23, arg24, arg25, arg26, arg27, arg28, arg29, arg30,
                arg31, arg32, arg33, arg34, arg35, arg36, arg37, arg38, arg39, arg40));
}

SEXP dot_call42(call42func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7,
                SEXP arg8, SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                SEXP arg16, SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                SEXP arg24, SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                SEXP arg32, SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39,
                SEXP arg40, SEXP arg41) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15,
                arg16, arg17, arg18, arg19, arg20, arg21, arg22, arg23, arg24, arg25, arg26, arg27, arg28, arg29, arg30,
                arg31, arg32, arg33, arg34, arg35, arg36, arg37, arg38, arg39, arg40, arg41));
}

SEXP dot_call43(call43func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7,
                SEXP arg8, SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                SEXP arg16, SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                SEXP arg24, SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                SEXP arg32, SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39,
                SEXP arg40, SEXP arg41, SEXP arg42) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15,
                arg16, arg17, arg18, arg19, arg20, arg21, arg22, arg23, arg24, arg25, arg26, arg27, arg28, arg29, arg30,
                arg31, arg32, arg33, arg34, arg35, arg36, arg37, arg38, arg39, arg40, arg41, arg42));
}

SEXP dot_call44(call44func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7,
                SEXP arg8, SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                SEXP arg16, SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                SEXP arg24, SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                SEXP arg32, SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39,
                SEXP arg40, SEXP arg41, SEXP arg42, SEXP arg43) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15,
                arg16, arg17, arg18, arg19, arg20, arg21, arg22, arg23, arg24, arg25, arg26, arg27, arg28, arg29, arg30,
                arg31, arg32, arg33, arg34, arg35, arg36, arg37, arg38, arg39, arg40, arg41, arg42, arg43));
}

SEXP dot_call45(call45func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7,
                SEXP arg8, SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                SEXP arg16, SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                SEXP arg24, SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                SEXP arg32, SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39,
                SEXP arg40, SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15,
                arg16, arg17, arg18, arg19, arg20, arg21, arg22, arg23, arg24, arg25, arg26, arg27, arg28, arg29, arg30,
                arg31, arg32, arg33, arg34, arg35, arg36, arg37, arg38, arg39, arg40, arg41, arg42, arg43, arg44));
}

SEXP dot_call46(call46func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7,
                SEXP arg8, SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                SEXP arg16, SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                SEXP arg24, SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                SEXP arg32, SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39,
                SEXP arg40, SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44, SEXP arg45) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15,
                arg16, arg17, arg18, arg19, arg20, arg21, arg22, arg23, arg24, arg25, arg26, arg27, arg28, arg29, arg30,
                arg31, arg32, arg33, arg34, arg35, arg36, arg37, arg38, arg39, arg40, arg41, arg42, arg43, arg44,
                arg45));
}

SEXP dot_call47(call47func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7,
                SEXP arg8, SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                SEXP arg16, SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                SEXP arg24, SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                SEXP arg32, SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39,
                SEXP arg40, SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44, SEXP arg45, SEXP arg46) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15,
                arg16, arg17, arg18, arg19, arg20, arg21, arg22, arg23, arg24, arg25, arg26, arg27, arg28, arg29, arg30,
                arg31, arg32, arg33, arg34, arg35, arg36, arg37, arg38, arg39, arg40, arg41, arg42, arg43, arg44, arg45,
                arg46));
}

SEXP dot_call48(call48func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7,
                SEXP arg8, SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                SEXP arg16, SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                SEXP arg24, SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                SEXP arg32, SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39,
                SEXP arg40, SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44, SEXP arg45, SEXP arg46, SEXP arg47) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15,
                arg16, arg17, arg18, arg19, arg20, arg21, arg22, arg23, arg24, arg25, arg26, arg27, arg28, arg29, arg30,
                arg31, arg32, arg33, arg34, arg35, arg36, arg37, arg38, arg39, arg40, arg41, arg42, arg43, arg44, arg45,
                arg46, arg47));
}

SEXP dot_call49(call49func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7,
                SEXP arg8, SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                SEXP arg16, SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                SEXP arg24, SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                SEXP arg32, SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39,
                SEXP arg40, SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44, SEXP arg45, SEXP arg46, SEXP arg47,
                SEXP arg48) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15,
                arg16, arg17, arg18, arg19, arg20, arg21, arg22, arg23, arg24, arg25, arg26, arg27, arg28, arg29, arg30,
                arg31, arg32, arg33, arg34, arg35, arg36, arg37, arg38, arg39, arg40, arg41, arg42, arg43, arg44, arg45,
                arg46, arg47, arg48));
}

SEXP dot_call50(call50func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7,
                SEXP arg8, SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                SEXP arg16, SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                SEXP arg24, SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                SEXP arg32, SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39,
                SEXP arg40, SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44, SEXP arg45, SEXP arg46, SEXP arg47,
                SEXP arg48, SEXP arg49) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15,
                arg16, arg17, arg18, arg19, arg20, arg21, arg22, arg23, arg24, arg25, arg26, arg27, arg28, arg29, arg30,
                arg31, arg32, arg33, arg34, arg35, arg36, arg37, arg38, arg39, arg40, arg41, arg42, arg43, arg44, arg45,
                arg46, arg47, arg48, arg49));
}

SEXP dot_call51(call51func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7,
                SEXP arg8, SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                SEXP arg16, SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                SEXP arg24, SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                SEXP arg32, SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39,
                SEXP arg40, SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44, SEXP arg45, SEXP arg46, SEXP arg47,
                SEXP arg48, SEXP arg49, SEXP arg50) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15,
                arg16, arg17, arg18, arg19, arg20, arg21, arg22, arg23, arg24, arg25, arg26, arg27, arg28, arg29, arg30,
                arg31, arg32, arg33, arg34, arg35, arg36, arg37, arg38, arg39, arg40, arg41, arg42, arg43, arg44, arg45,
                arg46, arg47, arg48, arg49, arg50));
}

SEXP dot_call52(call52func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7,
                SEXP arg8, SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                SEXP arg16, SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                SEXP arg24, SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                SEXP arg32, SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39,
                SEXP arg40, SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44, SEXP arg45, SEXP arg46, SEXP arg47,
                SEXP arg48, SEXP arg49, SEXP arg50, SEXP arg51) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15,
                arg16, arg17, arg18, arg19, arg20, arg21, arg22, arg23, arg24, arg25, arg26, arg27, arg28, arg29, arg30,
                arg31, arg32, arg33, arg34, arg35, arg36, arg37, arg38, arg39, arg40, arg41, arg42, arg43, arg44, arg45,
                arg46, arg47, arg48, arg49, arg50, arg51));
}

SEXP dot_call53(call53func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7,
                SEXP arg8, SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                SEXP arg16, SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                SEXP arg24, SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                SEXP arg32, SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39,
                SEXP arg40, SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44, SEXP arg45, SEXP arg46, SEXP arg47,
                SEXP arg48, SEXP arg49, SEXP arg50, SEXP arg51, SEXP arg52) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15,
                arg16, arg17, arg18, arg19, arg20, arg21, arg22, arg23, arg24, arg25, arg26, arg27, arg28, arg29, arg30,
                arg31, arg32, arg33, arg34, arg35, arg36, arg37, arg38, arg39, arg40, arg41, arg42, arg43, arg44, arg45,
                arg46, arg47, arg48, arg49, arg50, arg51, arg52));
}

SEXP dot_call54(call54func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7,
                SEXP arg8, SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                SEXP arg16, SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                SEXP arg24, SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                SEXP arg32, SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39,
                SEXP arg40, SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44, SEXP arg45, SEXP arg46, SEXP arg47,
                SEXP arg48, SEXP arg49, SEXP arg50, SEXP arg51, SEXP arg52, SEXP arg53) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15,
                arg16, arg17, arg18, arg19, arg20, arg21, arg22, arg23, arg24, arg25, arg26, arg27, arg28, arg29, arg30,
                arg31, arg32, arg33, arg34, arg35, arg36, arg37, arg38, arg39, arg40, arg41, arg42, arg43, arg44, arg45,
                arg46, arg47, arg48, arg49, arg50, arg51, arg52, arg53));
}

SEXP dot_call55(call55func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7,
                SEXP arg8, SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                SEXP arg16, SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                SEXP arg24, SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                SEXP arg32, SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39,
                SEXP arg40, SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44, SEXP arg45, SEXP arg46, SEXP arg47,
                SEXP arg48, SEXP arg49, SEXP arg50, SEXP arg51, SEXP arg52, SEXP arg53, SEXP arg54) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15,
                arg16, arg17, arg18, arg19, arg20, arg21, arg22, arg23, arg24, arg25, arg26, arg27, arg28, arg29, arg30,
                arg31, arg32, arg33, arg34, arg35, arg36, arg37, arg38, arg39, arg40, arg41, arg42, arg43, arg44, arg45,
                arg46, arg47, arg48, arg49, arg50, arg51, arg52, arg53, arg54));
}

SEXP dot_call56(call56func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7,
                SEXP arg8, SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                SEXP arg16, SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                SEXP arg24, SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                SEXP arg32, SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39,
                SEXP arg40, SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44, SEXP arg45, SEXP arg46, SEXP arg47,
                SEXP arg48, SEXP arg49, SEXP arg50, SEXP arg51, SEXP arg52, SEXP arg53, SEXP arg54, SEXP arg55) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15,
                arg16, arg17, arg18, arg19, arg20, arg21, arg22, arg23, arg24, arg25, arg26, arg27, arg28, arg29, arg30,
                arg31, arg32, arg33, arg34, arg35, arg36, arg37, arg38, arg39, arg40, arg41, arg42, arg43, arg44, arg45,
                arg46, arg47, arg48, arg49, arg50, arg51, arg52, arg53, arg54, arg55));
}

SEXP dot_call57(call57func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7,
                SEXP arg8, SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                SEXP arg16, SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                SEXP arg24, SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                SEXP arg32, SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39,
                SEXP arg40, SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44, SEXP arg45, SEXP arg46, SEXP arg47,
                SEXP arg48, SEXP arg49, SEXP arg50, SEXP arg51, SEXP arg52, SEXP arg53, SEXP arg54, SEXP arg55,
                SEXP arg56) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15,
                arg16, arg17, arg18, arg19, arg20, arg21, arg22, arg23, arg24, arg25, arg26, arg27, arg28, arg29, arg30,
                arg31, arg32, arg33, arg34, arg35, arg36, arg37, arg38, arg39, arg40, arg41, arg42, arg43, arg44, arg45,
                arg46, arg47, arg48, arg49, arg50, arg51, arg52, arg53, arg54, arg55, arg56));
}

SEXP dot_call58(call58func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7,
                SEXP arg8, SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                SEXP arg16, SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                SEXP arg24, SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                SEXP arg32, SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39,
                SEXP arg40, SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44, SEXP arg45, SEXP arg46, SEXP arg47,
                SEXP arg48, SEXP arg49, SEXP arg50, SEXP arg51, SEXP arg52, SEXP arg53, SEXP arg54, SEXP arg55,
                SEXP arg56, SEXP arg57) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15,
                arg16, arg17, arg18, arg19, arg20, arg21, arg22, arg23, arg24, arg25, arg26, arg27, arg28, arg29, arg30,
                arg31, arg32, arg33, arg34, arg35, arg36, arg37, arg38, arg39, arg40, arg41, arg42, arg43, arg44, arg45,
                arg46, arg47, arg48, arg49, arg50, arg51, arg52, arg53, arg54, arg55, arg56, arg57));
}

SEXP dot_call59(call59func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7,
                SEXP arg8, SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                SEXP arg16, SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                SEXP arg24, SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                SEXP arg32, SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39,
                SEXP arg40, SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44, SEXP arg45, SEXP arg46, SEXP arg47,
                SEXP arg48, SEXP arg49, SEXP arg50, SEXP arg51, SEXP arg52, SEXP arg53, SEXP arg54, SEXP arg55,
                SEXP arg56, SEXP arg57, SEXP arg58) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15,
                arg16, arg17, arg18, arg19, arg20, arg21, arg22, arg23, arg24, arg25, arg26, arg27, arg28, arg29, arg30,
                arg31, arg32, arg33, arg34, arg35, arg36, arg37, arg38, arg39, arg40, arg41, arg42, arg43, arg44, arg45,
                arg46, arg47, arg48, arg49, arg50, arg51, arg52, arg53, arg54, arg55, arg56, arg57, arg58));
}

SEXP dot_call60(call60func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7,
                SEXP arg8, SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                SEXP arg16, SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                SEXP arg24, SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                SEXP arg32, SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39,
                SEXP arg40, SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44, SEXP arg45, SEXP arg46, SEXP arg47,
                SEXP arg48, SEXP arg49, SEXP arg50, SEXP arg51, SEXP arg52, SEXP arg53, SEXP arg54, SEXP arg55,
                SEXP arg56, SEXP arg57, SEXP arg58, SEXP arg59) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15,
                arg16, arg17, arg18, arg19, arg20, arg21, arg22, arg23, arg24, arg25, arg26, arg27, arg28, arg29, arg30,
                arg31, arg32, arg33, arg34, arg35, arg36, arg37, arg38, arg39, arg40, arg41, arg42, arg43, arg44, arg45,
                arg46, arg47, arg48, arg49, arg50, arg51, arg52, arg53, arg54, arg55, arg56, arg57, arg58, arg59));
}

SEXP dot_call61(call61func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7,
                SEXP arg8, SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                SEXP arg16, SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                SEXP arg24, SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                SEXP arg32, SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39,
                SEXP arg40, SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44, SEXP arg45, SEXP arg46, SEXP arg47,
                SEXP arg48, SEXP arg49, SEXP arg50, SEXP arg51, SEXP arg52, SEXP arg53, SEXP arg54, SEXP arg55,
                SEXP arg56, SEXP arg57, SEXP arg58, SEXP arg59, SEXP arg60) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15,
                arg16, arg17, arg18, arg19, arg20, arg21, arg22, arg23, arg24, arg25, arg26, arg27, arg28, arg29, arg30,
                arg31, arg32, arg33, arg34, arg35, arg36, arg37, arg38, arg39, arg40, arg41, arg42, arg43, arg44, arg45,
                arg46, arg47, arg48, arg49, arg50, arg51, arg52, arg53, arg54, arg55, arg56, arg57, arg58, arg59,
                arg60));
}

SEXP dot_call62(call62func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7,
                SEXP arg8, SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                SEXP arg16, SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                SEXP arg24, SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                SEXP arg32, SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39,
                SEXP arg40, SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44, SEXP arg45, SEXP arg46, SEXP arg47,
                SEXP arg48, SEXP arg49, SEXP arg50, SEXP arg51, SEXP arg52, SEXP arg53, SEXP arg54, SEXP arg55,
                SEXP arg56, SEXP arg57, SEXP arg58, SEXP arg59, SEXP arg60, SEXP arg61) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15,
                arg16, arg17, arg18, arg19, arg20, arg21, arg22, arg23, arg24, arg25, arg26, arg27, arg28, arg29, arg30,
                arg31, arg32, arg33, arg34, arg35, arg36, arg37, arg38, arg39, arg40, arg41, arg42, arg43, arg44, arg45,
                arg46, arg47, arg48, arg49, arg50, arg51, arg52, arg53, arg54, arg55, arg56, arg57, arg58, arg59, arg60,
                arg61));
}

SEXP dot_call63(call63func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7,
                SEXP arg8, SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                SEXP arg16, SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                SEXP arg24, SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                SEXP arg32, SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39,
                SEXP arg40, SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44, SEXP arg45, SEXP arg46, SEXP arg47,
                SEXP arg48, SEXP arg49, SEXP arg50, SEXP arg51, SEXP arg52, SEXP arg53, SEXP arg54, SEXP arg55,
                SEXP arg56, SEXP arg57, SEXP arg58, SEXP arg59, SEXP arg60, SEXP arg61, SEXP arg62) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15,
                arg16, arg17, arg18, arg19, arg20, arg21, arg22, arg23, arg24, arg25, arg26, arg27, arg28, arg29, arg30,
                arg31, arg32, arg33, arg34, arg35, arg36, arg37, arg38, arg39, arg40, arg41, arg42, arg43, arg44, arg45,
                arg46, arg47, arg48, arg49, arg50, arg51, arg52, arg53, arg54, arg55, arg56, arg57, arg58, arg59, arg60,
                arg61, arg62));
}

SEXP dot_call64(call64func fun, SEXP arg0, SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7,
                SEXP arg8, SEXP arg9, SEXP arg10, SEXP arg11, SEXP arg12, SEXP arg13, SEXP arg14, SEXP arg15,
                SEXP arg16, SEXP arg17, SEXP arg18, SEXP arg19, SEXP arg20, SEXP arg21, SEXP arg22, SEXP arg23,
                SEXP arg24, SEXP arg25, SEXP arg26, SEXP arg27, SEXP arg28, SEXP arg29, SEXP arg30, SEXP arg31,
                SEXP arg32, SEXP arg33, SEXP arg34, SEXP arg35, SEXP arg36, SEXP arg37, SEXP arg38, SEXP arg39,
                SEXP arg40, SEXP arg41, SEXP arg42, SEXP arg43, SEXP arg44, SEXP arg45, SEXP arg46, SEXP arg47,
                SEXP arg48, SEXP arg49, SEXP arg50, SEXP arg51, SEXP arg52, SEXP arg53, SEXP arg54, SEXP arg55,
                SEXP arg56, SEXP arg57, SEXP arg58, SEXP arg59, SEXP arg60, SEXP arg61, SEXP arg62, SEXP arg63) {
    DO_CALL(fun(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15,
                arg16, arg17, arg18, arg19, arg20, arg21, arg22, arg23, arg24, arg25, arg26, arg27, arg28, arg29, arg30,
                arg31, arg32, arg33, arg34, arg35, arg36, arg37, arg38, arg39, arg40, arg41, arg42, arg43, arg44, arg45,
                arg46, arg47, arg48, arg49, arg50, arg51, arg52, arg53, arg54, arg55, arg56, arg57, arg58, arg59, arg60,
                arg61, arg62, arg63));
}
