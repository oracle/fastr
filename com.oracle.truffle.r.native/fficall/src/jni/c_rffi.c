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
#include <stdbool.h>

static jclass intArrayClass;
static jclass doubleArrayClass;

void init_c(JNIEnv *env) {
	intArrayClass = checkFindClass(env, "[I");
	doubleArrayClass = checkFindClass(env, "[D");
}

typedef void (*c0func)();
typedef void (*c1func)(void *arg1);
typedef void (*c2func)(void *arg1, void *arg2);
typedef void (*c3func)(void *arg1, void *arg2, void *arg3);
typedef void (*c4func)(void *arg1, void *arg2, void *arg3, void *arg4);
typedef void (*c5func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5);
typedef void (*c6func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6);
typedef void (*c7func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6, void *arg7);
typedef void (*c8func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6, void *arg7, void *arg8);
typedef void (*c9func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6, void *arg7, void *arg8,
        void *arg9);
typedef void (*c10func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6, void *arg7, void *arg8,
        void *arg9, void *arg10);
typedef void (*c11func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6, void *arg7, void *arg8,
        void *arg9, void *arg10, void *arg11);
typedef void (*c12func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6, void *arg7, void *arg8,
        void *arg9, void *arg10, void *arg11, void *arg12);
typedef void (*c13func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6, void *arg7, void *arg8,
        void *arg9, void *arg10, void *arg11, void *arg12, void *arg13);
typedef void (*c14func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6, void *arg7, void *arg8,
        void *arg9, void *arg10, void *arg11, void *arg12, void *arg13, void *arg14);
typedef void (*c15func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6, void *arg7, void *arg8,
        void *arg9, void *arg10, void *arg11, void *arg12, void *arg13, void *arg14, void *arg15);
typedef void (*c16func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6, void *arg7, void *arg8,
        void *arg9, void *arg10, void *arg11, void *arg12, void *arg13, void *arg14, void *arg15, void *arg16);
typedef void (*c17func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6, void *arg7, void *arg8,
        void *arg9, void *arg10, void *arg11, void *arg12, void *arg13, void *arg14, void *arg15, void *arg16,
        void *arg17);
typedef void (*c18func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6, void *arg7, void *arg8,
        void *arg9, void *arg10, void *arg11, void *arg12, void *arg13, void *arg14, void *arg15, void *arg16,
        void *arg17, void *arg18);
typedef void (*c19func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6, void *arg7, void *arg8,
        void *arg9, void *arg10, void *arg11, void *arg12, void *arg13, void *arg14, void *arg15, void *arg16,
        void *arg17, void *arg18, void *arg19);
typedef void (*c20func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6, void *arg7, void *arg8,
        void *arg9, void *arg10, void *arg11, void *arg12, void *arg13, void *arg14, void *arg15, void *arg16,
        void *arg17, void *arg18, void *arg19, void *arg20);
typedef void (*c21func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6, void *arg7, void *arg8,
        void *arg9, void *arg10, void *arg11, void *arg12, void *arg13, void *arg14, void *arg15, void *arg16,
        void *arg17, void *arg18, void *arg19, void *arg20, void *arg21);
typedef void (*c22func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6, void *arg7, void *arg8,
        void *arg9, void *arg10, void *arg11, void *arg12, void *arg13, void *arg14, void *arg15, void *arg16,
        void *arg17, void *arg18, void *arg19, void *arg20, void *arg21, void *arg22);
typedef void (*c23func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6, void *arg7, void *arg8,
        void *arg9, void *arg10, void *arg11, void *arg12, void *arg13, void *arg14, void *arg15, void *arg16,
        void *arg17, void *arg18, void *arg19, void *arg20, void *arg21, void *arg22, void *arg23);
typedef void (*c24func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6, void *arg7, void *arg8,
        void *arg9, void *arg10, void *arg11, void *arg12, void *arg13, void *arg14, void *arg15, void *arg16,
        void *arg17, void *arg18, void *arg19, void *arg20, void *arg21, void *arg22, void *arg23, void *arg24);
typedef void (*c25func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6, void *arg7, void *arg8,
        void *arg9, void *arg10, void *arg11, void *arg12, void *arg13, void *arg14, void *arg15, void *arg16,
        void *arg17, void *arg18, void *arg19, void *arg20, void *arg21, void *arg22, void *arg23, void *arg24,
        void *arg25);
typedef void (*c26func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6, void *arg7, void *arg8,
        void *arg9, void *arg10, void *arg11, void *arg12, void *arg13, void *arg14, void *arg15, void *arg16,
        void *arg17, void *arg18, void *arg19, void *arg20, void *arg21, void *arg22, void *arg23, void *arg24,
        void *arg25, void *arg26);
typedef void (*c27func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6, void *arg7, void *arg8,
        void *arg9, void *arg10, void *arg11, void *arg12, void *arg13, void *arg14, void *arg15, void *arg16,
        void *arg17, void *arg18, void *arg19, void *arg20, void *arg21, void *arg22, void *arg23, void *arg24,
        void *arg25, void *arg26, void *arg27);
typedef void (*c28func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6, void *arg7, void *arg8,
        void *arg9, void *arg10, void *arg11, void *arg12, void *arg13, void *arg14, void *arg15, void *arg16,
        void *arg17, void *arg18, void *arg19, void *arg20, void *arg21, void *arg22, void *arg23, void *arg24,
        void *arg25, void *arg26, void *arg27, void *arg28);
typedef void (*c29func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6, void *arg7, void *arg8,
        void *arg9, void *arg10, void *arg11, void *arg12, void *arg13, void *arg14, void *arg15, void *arg16,
        void *arg17, void *arg18, void *arg19, void *arg20, void *arg21, void *arg22, void *arg23, void *arg24,
        void *arg25, void *arg26, void *arg27, void *arg28, void *arg29);
typedef void (*c30func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6, void *arg7, void *arg8,
        void *arg9, void *arg10, void *arg11, void *arg12, void *arg13, void *arg14, void *arg15, void *arg16,
        void *arg17, void *arg18, void *arg19, void *arg20, void *arg21, void *arg22, void *arg23, void *arg24,
        void *arg25, void *arg26, void *arg27, void *arg28, void *arg29, void *arg30);
typedef void (*c31func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6, void *arg7, void *arg8,
        void *arg9, void *arg10, void *arg11, void *arg12, void *arg13, void *arg14, void *arg15, void *arg16,
        void *arg17, void *arg18, void *arg19, void *arg20, void *arg21, void *arg22, void *arg23, void *arg24,
        void *arg25, void *arg26, void *arg27, void *arg28, void *arg29, void *arg30, void *arg31);
typedef void (*c32func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6, void *arg7, void *arg8,
        void *arg9, void *arg10, void *arg11, void *arg12, void *arg13, void *arg14, void *arg15, void *arg16,
        void *arg17, void *arg18, void *arg19, void *arg20, void *arg21, void *arg22, void *arg23, void *arg24,
        void *arg25, void *arg26, void *arg27, void *arg28, void *arg29, void *arg30, void *arg31, void *arg32);
typedef void (*c33func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6, void *arg7, void *arg8,
        void *arg9, void *arg10, void *arg11, void *arg12, void *arg13, void *arg14, void *arg15, void *arg16,
        void *arg17, void *arg18, void *arg19, void *arg20, void *arg21, void *arg22, void *arg23, void *arg24,
        void *arg25, void *arg26, void *arg27, void *arg28, void *arg29, void *arg30, void *arg31, void *arg32,
        void *arg33);
typedef void (*c34func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6, void *arg7, void *arg8,
        void *arg9, void *arg10, void *arg11, void *arg12, void *arg13, void *arg14, void *arg15, void *arg16,
        void *arg17, void *arg18, void *arg19, void *arg20, void *arg21, void *arg22, void *arg23, void *arg24,
        void *arg25, void *arg26, void *arg27, void *arg28, void *arg29, void *arg30, void *arg31, void *arg32,
        void *arg33, void *arg34);
typedef void (*c35func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6, void *arg7, void *arg8,
        void *arg9, void *arg10, void *arg11, void *arg12, void *arg13, void *arg14, void *arg15, void *arg16,
        void *arg17, void *arg18, void *arg19, void *arg20, void *arg21, void *arg22, void *arg23, void *arg24,
        void *arg25, void *arg26, void *arg27, void *arg28, void *arg29, void *arg30, void *arg31, void *arg32,
        void *arg33, void *arg34, void *arg35);
typedef void (*c36func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6, void *arg7, void *arg8,
        void *arg9, void *arg10, void *arg11, void *arg12, void *arg13, void *arg14, void *arg15, void *arg16,
        void *arg17, void *arg18, void *arg19, void *arg20, void *arg21, void *arg22, void *arg23, void *arg24,
        void *arg25, void *arg26, void *arg27, void *arg28, void *arg29, void *arg30, void *arg31, void *arg32,
        void *arg33, void *arg34, void *arg35, void *arg36);
typedef void (*c37func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6, void *arg7, void *arg8,
        void *arg9, void *arg10, void *arg11, void *arg12, void *arg13, void *arg14, void *arg15, void *arg16,
        void *arg17, void *arg18, void *arg19, void *arg20, void *arg21, void *arg22, void *arg23, void *arg24,
        void *arg25, void *arg26, void *arg27, void *arg28, void *arg29, void *arg30, void *arg31, void *arg32,
        void *arg33, void *arg34, void *arg35, void *arg36, void *arg37);
typedef void (*c38func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6, void *arg7, void *arg8,
        void *arg9, void *arg10, void *arg11, void *arg12, void *arg13, void *arg14, void *arg15, void *arg16,
        void *arg17, void *arg18, void *arg19, void *arg20, void *arg21, void *arg22, void *arg23, void *arg24,
        void *arg25, void *arg26, void *arg27, void *arg28, void *arg29, void *arg30, void *arg31, void *arg32,
        void *arg33, void *arg34, void *arg35, void *arg36, void *arg37, void *arg38);
typedef void (*c39func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6, void *arg7, void *arg8,
        void *arg9, void *arg10, void *arg11, void *arg12, void *arg13, void *arg14, void *arg15, void *arg16,
        void *arg17, void *arg18, void *arg19, void *arg20, void *arg21, void *arg22, void *arg23, void *arg24,
        void *arg25, void *arg26, void *arg27, void *arg28, void *arg29, void *arg30, void *arg31, void *arg32,
        void *arg33, void *arg34, void *arg35, void *arg36, void *arg37, void *arg38, void *arg39);
typedef void (*c40func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6, void *arg7, void *arg8,
        void *arg9, void *arg10, void *arg11, void *arg12, void *arg13, void *arg14, void *arg15, void *arg16,
        void *arg17, void *arg18, void *arg19, void *arg20, void *arg21, void *arg22, void *arg23, void *arg24,
        void *arg25, void *arg26, void *arg27, void *arg28, void *arg29, void *arg30, void *arg31, void *arg32,
        void *arg33, void *arg34, void *arg35, void *arg36, void *arg37, void *arg38, void *arg39, void *arg40);
typedef void (*c41func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6, void *arg7, void *arg8,
        void *arg9, void *arg10, void *arg11, void *arg12, void *arg13, void *arg14, void *arg15, void *arg16,
        void *arg17, void *arg18, void *arg19, void *arg20, void *arg21, void *arg22, void *arg23, void *arg24,
        void *arg25, void *arg26, void *arg27, void *arg28, void *arg29, void *arg30, void *arg31, void *arg32,
        void *arg33, void *arg34, void *arg35, void *arg36, void *arg37, void *arg38, void *arg39, void *arg40,
        void *arg41);
typedef void (*c42func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6, void *arg7, void *arg8,
        void *arg9, void *arg10, void *arg11, void *arg12, void *arg13, void *arg14, void *arg15, void *arg16,
        void *arg17, void *arg18, void *arg19, void *arg20, void *arg21, void *arg22, void *arg23, void *arg24,
        void *arg25, void *arg26, void *arg27, void *arg28, void *arg29, void *arg30, void *arg31, void *arg32,
        void *arg33, void *arg34, void *arg35, void *arg36, void *arg37, void *arg38, void *arg39, void *arg40,
        void *arg41, void *arg42);
typedef void (*c43func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6, void *arg7, void *arg8,
        void *arg9, void *arg10, void *arg11, void *arg12, void *arg13, void *arg14, void *arg15, void *arg16,
        void *arg17, void *arg18, void *arg19, void *arg20, void *arg21, void *arg22, void *arg23, void *arg24,
        void *arg25, void *arg26, void *arg27, void *arg28, void *arg29, void *arg30, void *arg31, void *arg32,
        void *arg33, void *arg34, void *arg35, void *arg36, void *arg37, void *arg38, void *arg39, void *arg40,
        void *arg41, void *arg42, void *arg43);
typedef void (*c44func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6, void *arg7, void *arg8,
        void *arg9, void *arg10, void *arg11, void *arg12, void *arg13, void *arg14, void *arg15, void *arg16,
        void *arg17, void *arg18, void *arg19, void *arg20, void *arg21, void *arg22, void *arg23, void *arg24,
        void *arg25, void *arg26, void *arg27, void *arg28, void *arg29, void *arg30, void *arg31, void *arg32,
        void *arg33, void *arg34, void *arg35, void *arg36, void *arg37, void *arg38, void *arg39, void *arg40,
        void *arg41, void *arg42, void *arg43, void *arg44);
typedef void (*c45func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6, void *arg7, void *arg8,
        void *arg9, void *arg10, void *arg11, void *arg12, void *arg13, void *arg14, void *arg15, void *arg16,
        void *arg17, void *arg18, void *arg19, void *arg20, void *arg21, void *arg22, void *arg23, void *arg24,
        void *arg25, void *arg26, void *arg27, void *arg28, void *arg29, void *arg30, void *arg31, void *arg32,
        void *arg33, void *arg34, void *arg35, void *arg36, void *arg37, void *arg38, void *arg39, void *arg40,
        void *arg41, void *arg42, void *arg43, void *arg44, void *arg45);
typedef void (*c46func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6, void *arg7, void *arg8,
        void *arg9, void *arg10, void *arg11, void *arg12, void *arg13, void *arg14, void *arg15, void *arg16,
        void *arg17, void *arg18, void *arg19, void *arg20, void *arg21, void *arg22, void *arg23, void *arg24,
        void *arg25, void *arg26, void *arg27, void *arg28, void *arg29, void *arg30, void *arg31, void *arg32,
        void *arg33, void *arg34, void *arg35, void *arg36, void *arg37, void *arg38, void *arg39, void *arg40,
        void *arg41, void *arg42, void *arg43, void *arg44, void *arg45, void *arg46);
typedef void (*c47func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6, void *arg7, void *arg8,
        void *arg9, void *arg10, void *arg11, void *arg12, void *arg13, void *arg14, void *arg15, void *arg16,
        void *arg17, void *arg18, void *arg19, void *arg20, void *arg21, void *arg22, void *arg23, void *arg24,
        void *arg25, void *arg26, void *arg27, void *arg28, void *arg29, void *arg30, void *arg31, void *arg32,
        void *arg33, void *arg34, void *arg35, void *arg36, void *arg37, void *arg38, void *arg39, void *arg40,
        void *arg41, void *arg42, void *arg43, void *arg44, void *arg45, void *arg46, void *arg47);
typedef void (*c48func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6, void *arg7, void *arg8,
        void *arg9, void *arg10, void *arg11, void *arg12, void *arg13, void *arg14, void *arg15, void *arg16,
        void *arg17, void *arg18, void *arg19, void *arg20, void *arg21, void *arg22, void *arg23, void *arg24,
        void *arg25, void *arg26, void *arg27, void *arg28, void *arg29, void *arg30, void *arg31, void *arg32,
        void *arg33, void *arg34, void *arg35, void *arg36, void *arg37, void *arg38, void *arg39, void *arg40,
        void *arg41, void *arg42, void *arg43, void *arg44, void *arg45, void *arg46, void *arg47, void *arg48);
typedef void (*c49func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6, void *arg7, void *arg8,
        void *arg9, void *arg10, void *arg11, void *arg12, void *arg13, void *arg14, void *arg15, void *arg16,
        void *arg17, void *arg18, void *arg19, void *arg20, void *arg21, void *arg22, void *arg23, void *arg24,
        void *arg25, void *arg26, void *arg27, void *arg28, void *arg29, void *arg30, void *arg31, void *arg32,
        void *arg33, void *arg34, void *arg35, void *arg36, void *arg37, void *arg38, void *arg39, void *arg40,
        void *arg41, void *arg42, void *arg43, void *arg44, void *arg45, void *arg46, void *arg47, void *arg48,
        void *arg49);
typedef void (*c50func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6, void *arg7, void *arg8,
        void *arg9, void *arg10, void *arg11, void *arg12, void *arg13, void *arg14, void *arg15, void *arg16,
        void *arg17, void *arg18, void *arg19, void *arg20, void *arg21, void *arg22, void *arg23, void *arg24,
        void *arg25, void *arg26, void *arg27, void *arg28, void *arg29, void *arg30, void *arg31, void *arg32,
        void *arg33, void *arg34, void *arg35, void *arg36, void *arg37, void *arg38, void *arg39, void *arg40,
        void *arg41, void *arg42, void *arg43, void *arg44, void *arg45, void *arg46, void *arg47, void *arg48,
        void *arg49, void *arg50);
typedef void (*c51func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6, void *arg7, void *arg8,
        void *arg9, void *arg10, void *arg11, void *arg12, void *arg13, void *arg14, void *arg15, void *arg16,
        void *arg17, void *arg18, void *arg19, void *arg20, void *arg21, void *arg22, void *arg23, void *arg24,
        void *arg25, void *arg26, void *arg27, void *arg28, void *arg29, void *arg30, void *arg31, void *arg32,
        void *arg33, void *arg34, void *arg35, void *arg36, void *arg37, void *arg38, void *arg39, void *arg40,
        void *arg41, void *arg42, void *arg43, void *arg44, void *arg45, void *arg46, void *arg47, void *arg48,
        void *arg49, void *arg50, void *arg51);
typedef void (*c52func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6, void *arg7, void *arg8,
        void *arg9, void *arg10, void *arg11, void *arg12, void *arg13, void *arg14, void *arg15, void *arg16,
        void *arg17, void *arg18, void *arg19, void *arg20, void *arg21, void *arg22, void *arg23, void *arg24,
        void *arg25, void *arg26, void *arg27, void *arg28, void *arg29, void *arg30, void *arg31, void *arg32,
        void *arg33, void *arg34, void *arg35, void *arg36, void *arg37, void *arg38, void *arg39, void *arg40,
        void *arg41, void *arg42, void *arg43, void *arg44, void *arg45, void *arg46, void *arg47, void *arg48,
        void *arg49, void *arg50, void *arg51, void *arg52);
typedef void (*c53func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6, void *arg7, void *arg8,
        void *arg9, void *arg10, void *arg11, void *arg12, void *arg13, void *arg14, void *arg15, void *arg16,
        void *arg17, void *arg18, void *arg19, void *arg20, void *arg21, void *arg22, void *arg23, void *arg24,
        void *arg25, void *arg26, void *arg27, void *arg28, void *arg29, void *arg30, void *arg31, void *arg32,
        void *arg33, void *arg34, void *arg35, void *arg36, void *arg37, void *arg38, void *arg39, void *arg40,
        void *arg41, void *arg42, void *arg43, void *arg44, void *arg45, void *arg46, void *arg47, void *arg48,
        void *arg49, void *arg50, void *arg51, void *arg52, void *arg53);
typedef void (*c54func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6, void *arg7, void *arg8,
        void *arg9, void *arg10, void *arg11, void *arg12, void *arg13, void *arg14, void *arg15, void *arg16,
        void *arg17, void *arg18, void *arg19, void *arg20, void *arg21, void *arg22, void *arg23, void *arg24,
        void *arg25, void *arg26, void *arg27, void *arg28, void *arg29, void *arg30, void *arg31, void *arg32,
        void *arg33, void *arg34, void *arg35, void *arg36, void *arg37, void *arg38, void *arg39, void *arg40,
        void *arg41, void *arg42, void *arg43, void *arg44, void *arg45, void *arg46, void *arg47, void *arg48,
        void *arg49, void *arg50, void *arg51, void *arg52, void *arg53, void *arg54);
typedef void (*c55func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6, void *arg7, void *arg8,
        void *arg9, void *arg10, void *arg11, void *arg12, void *arg13, void *arg14, void *arg15, void *arg16,
        void *arg17, void *arg18, void *arg19, void *arg20, void *arg21, void *arg22, void *arg23, void *arg24,
        void *arg25, void *arg26, void *arg27, void *arg28, void *arg29, void *arg30, void *arg31, void *arg32,
        void *arg33, void *arg34, void *arg35, void *arg36, void *arg37, void *arg38, void *arg39, void *arg40,
        void *arg41, void *arg42, void *arg43, void *arg44, void *arg45, void *arg46, void *arg47, void *arg48,
        void *arg49, void *arg50, void *arg51, void *arg52, void *arg53, void *arg54, void *arg55);
typedef void (*c56func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6, void *arg7, void *arg8,
        void *arg9, void *arg10, void *arg11, void *arg12, void *arg13, void *arg14, void *arg15, void *arg16,
        void *arg17, void *arg18, void *arg19, void *arg20, void *arg21, void *arg22, void *arg23, void *arg24,
        void *arg25, void *arg26, void *arg27, void *arg28, void *arg29, void *arg30, void *arg31, void *arg32,
        void *arg33, void *arg34, void *arg35, void *arg36, void *arg37, void *arg38, void *arg39, void *arg40,
        void *arg41, void *arg42, void *arg43, void *arg44, void *arg45, void *arg46, void *arg47, void *arg48,
        void *arg49, void *arg50, void *arg51, void *arg52, void *arg53, void *arg54, void *arg55, void *arg56);
typedef void (*c57func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6, void *arg7, void *arg8,
        void *arg9, void *arg10, void *arg11, void *arg12, void *arg13, void *arg14, void *arg15, void *arg16,
        void *arg17, void *arg18, void *arg19, void *arg20, void *arg21, void *arg22, void *arg23, void *arg24,
        void *arg25, void *arg26, void *arg27, void *arg28, void *arg29, void *arg30, void *arg31, void *arg32,
        void *arg33, void *arg34, void *arg35, void *arg36, void *arg37, void *arg38, void *arg39, void *arg40,
        void *arg41, void *arg42, void *arg43, void *arg44, void *arg45, void *arg46, void *arg47, void *arg48,
        void *arg49, void *arg50, void *arg51, void *arg52, void *arg53, void *arg54, void *arg55, void *arg56,
        void *arg57);
typedef void (*c58func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6, void *arg7, void *arg8,
        void *arg9, void *arg10, void *arg11, void *arg12, void *arg13, void *arg14, void *arg15, void *arg16,
        void *arg17, void *arg18, void *arg19, void *arg20, void *arg21, void *arg22, void *arg23, void *arg24,
        void *arg25, void *arg26, void *arg27, void *arg28, void *arg29, void *arg30, void *arg31, void *arg32,
        void *arg33, void *arg34, void *arg35, void *arg36, void *arg37, void *arg38, void *arg39, void *arg40,
        void *arg41, void *arg42, void *arg43, void *arg44, void *arg45, void *arg46, void *arg47, void *arg48,
        void *arg49, void *arg50, void *arg51, void *arg52, void *arg53, void *arg54, void *arg55, void *arg56,
        void *arg57, void *arg58);
typedef void (*c59func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6, void *arg7, void *arg8,
        void *arg9, void *arg10, void *arg11, void *arg12, void *arg13, void *arg14, void *arg15, void *arg16,
        void *arg17, void *arg18, void *arg19, void *arg20, void *arg21, void *arg22, void *arg23, void *arg24,
        void *arg25, void *arg26, void *arg27, void *arg28, void *arg29, void *arg30, void *arg31, void *arg32,
        void *arg33, void *arg34, void *arg35, void *arg36, void *arg37, void *arg38, void *arg39, void *arg40,
        void *arg41, void *arg42, void *arg43, void *arg44, void *arg45, void *arg46, void *arg47, void *arg48,
        void *arg49, void *arg50, void *arg51, void *arg52, void *arg53, void *arg54, void *arg55, void *arg56,
        void *arg57, void *arg58, void *arg59);
typedef void (*c60func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6, void *arg7, void *arg8,
        void *arg9, void *arg10, void *arg11, void *arg12, void *arg13, void *arg14, void *arg15, void *arg16,
        void *arg17, void *arg18, void *arg19, void *arg20, void *arg21, void *arg22, void *arg23, void *arg24,
        void *arg25, void *arg26, void *arg27, void *arg28, void *arg29, void *arg30, void *arg31, void *arg32,
        void *arg33, void *arg34, void *arg35, void *arg36, void *arg37, void *arg38, void *arg39, void *arg40,
        void *arg41, void *arg42, void *arg43, void *arg44, void *arg45, void *arg46, void *arg47, void *arg48,
        void *arg49, void *arg50, void *arg51, void *arg52, void *arg53, void *arg54, void *arg55, void *arg56,
        void *arg57, void *arg58, void *arg59, void *arg60);
typedef void (*c61func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6, void *arg7, void *arg8,
        void *arg9, void *arg10, void *arg11, void *arg12, void *arg13, void *arg14, void *arg15, void *arg16,
        void *arg17, void *arg18, void *arg19, void *arg20, void *arg21, void *arg22, void *arg23, void *arg24,
        void *arg25, void *arg26, void *arg27, void *arg28, void *arg29, void *arg30, void *arg31, void *arg32,
        void *arg33, void *arg34, void *arg35, void *arg36, void *arg37, void *arg38, void *arg39, void *arg40,
        void *arg41, void *arg42, void *arg43, void *arg44, void *arg45, void *arg46, void *arg47, void *arg48,
        void *arg49, void *arg50, void *arg51, void *arg52, void *arg53, void *arg54, void *arg55, void *arg56,
        void *arg57, void *arg58, void *arg59, void *arg60, void *arg61);
typedef void (*c62func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6, void *arg7, void *arg8,
        void *arg9, void *arg10, void *arg11, void *arg12, void *arg13, void *arg14, void *arg15, void *arg16,
        void *arg17, void *arg18, void *arg19, void *arg20, void *arg21, void *arg22, void *arg23, void *arg24,
        void *arg25, void *arg26, void *arg27, void *arg28, void *arg29, void *arg30, void *arg31, void *arg32,
        void *arg33, void *arg34, void *arg35, void *arg36, void *arg37, void *arg38, void *arg39, void *arg40,
        void *arg41, void *arg42, void *arg43, void *arg44, void *arg45, void *arg46, void *arg47, void *arg48,
        void *arg49, void *arg50, void *arg51, void *arg52, void *arg53, void *arg54, void *arg55, void *arg56,
        void *arg57, void *arg58, void *arg59, void *arg60, void *arg61, void *arg62);
typedef void (*c63func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6, void *arg7, void *arg8,
        void *arg9, void *arg10, void *arg11, void *arg12, void *arg13, void *arg14, void *arg15, void *arg16,
        void *arg17, void *arg18, void *arg19, void *arg20, void *arg21, void *arg22, void *arg23, void *arg24,
        void *arg25, void *arg26, void *arg27, void *arg28, void *arg29, void *arg30, void *arg31, void *arg32,
        void *arg33, void *arg34, void *arg35, void *arg36, void *arg37, void *arg38, void *arg39, void *arg40,
        void *arg41, void *arg42, void *arg43, void *arg44, void *arg45, void *arg46, void *arg47, void *arg48,
        void *arg49, void *arg50, void *arg51, void *arg52, void *arg53, void *arg54, void *arg55, void *arg56,
        void *arg57, void *arg58, void *arg59, void *arg60, void *arg61, void *arg62, void *arg63);
typedef void (*c64func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6, void *arg7, void *arg8,
        void *arg9, void *arg10, void *arg11, void *arg12, void *arg13, void *arg14, void *arg15, void *arg16,
        void *arg17, void *arg18, void *arg19, void *arg20, void *arg21, void *arg22, void *arg23, void *arg24,
        void *arg25, void *arg26, void *arg27, void *arg28, void *arg29, void *arg30, void *arg31, void *arg32,
        void *arg33, void *arg34, void *arg35, void *arg36, void *arg37, void *arg38, void *arg39, void *arg40,
        void *arg41, void *arg42, void *arg43, void *arg44, void *arg45, void *arg46, void *arg47, void *arg48,
        void *arg49, void *arg50, void *arg51, void *arg52, void *arg53, void *arg54, void *arg55, void *arg56,
        void *arg57, void *arg58, void *arg59, void *arg60, void *arg61, void *arg62, void *arg63, void *arg64);
typedef void (*c65func)(void *arg1, void *arg2, void *arg3, void *arg4, void *arg5, void *arg6, void *arg7, void *arg8,
        void *arg9, void *arg10, void *arg11, void *arg12, void *arg13, void *arg14, void *arg15, void *arg16,
        void *arg17, void *arg18, void *arg19, void *arg20, void *arg21, void *arg22, void *arg23, void *arg24,
        void *arg25, void *arg26, void *arg27, void *arg28, void *arg29, void *arg30, void *arg31, void *arg32,
        void *arg33, void *arg34, void *arg35, void *arg36, void *arg37, void *arg38, void *arg39, void *arg40,
        void *arg41, void *arg42, void *arg43, void *arg44, void *arg45, void *arg46, void *arg47, void *arg48,
        void *arg49, void *arg50, void *arg51, void *arg52, void *arg53, void *arg54, void *arg55, void *arg56,
        void *arg57, void *arg58, void *arg59, void *arg60, void *arg61, void *arg62, void *arg63, void *arg64,
        void *arg65);

static void doCall(jlong address, int len, void** cargs) {
  switch (len) {
    case 0: {
        c0func c0 = (c0func) address;
        (*c0)();
        break;
    }

    case 1: {
        c1func c1 = (c1func) address;
        (*c1)(cargs[0]);
        break;
    }

    case 2: {
        c2func c2 = (c2func) address;
        (*c2)(cargs[0], cargs[1]);
        break;
    }

    case 3: {
        c3func c3 = (c3func) address;
        (*c3)(cargs[0], cargs[1], cargs[2]);
        break;
    }

    case 4: {
        c4func c4 = (c4func) address;
        (*c4)(cargs[0], cargs[1], cargs[2], cargs[3]);
        break;
    }

    case 5: {
        c5func c5 = (c5func) address;
        (*c5)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4]);
        break;
    }

    case 6: {
        c6func c6 = (c6func) address;
        (*c6)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5]);
        break;
    }

    case 7: {
        c7func c7 = (c7func) address;
        (*c7)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5], cargs[6]);
        break;
    }

    case 8: {
        c8func c8 = (c8func) address;
        (*c8)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5], cargs[6], cargs[7]);
        break;
    }

    case 9: {
        c9func c9 = (c9func) address;
        (*c9)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5], cargs[6], cargs[7],
        cargs[8]);
        break;
    }

    case 10: {
        c10func c10 = (c10func) address;
        (*c10)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5], cargs[6], cargs[7],
        cargs[8], cargs[9]);
        break;
    }

    case 11: {
        c11func c11 = (c11func) address;
        (*c11)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5], cargs[6], cargs[7],
        cargs[8], cargs[9], cargs[10]);
        break;
    }

    case 12: {
        c12func c12 = (c12func) address;
        (*c12)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5], cargs[6], cargs[7],
        cargs[8], cargs[9], cargs[10], cargs[11]);
        break;
    }

    case 13: {
        c13func c13 = (c13func) address;
        (*c13)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5], cargs[6], cargs[7],
        cargs[8], cargs[9], cargs[10], cargs[11], cargs[12]);
        break;
    }

    case 14: {
        c14func c14 = (c14func) address;
        (*c14)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5], cargs[6], cargs[7],
        cargs[8], cargs[9], cargs[10], cargs[11], cargs[12], cargs[13]);
        break;
    }

    case 15: {
        c15func c15 = (c15func) address;
        (*c15)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5], cargs[6], cargs[7],
        cargs[8], cargs[9], cargs[10], cargs[11], cargs[12], cargs[13], cargs[14]);
        break;
    }

    case 16: {
        c16func c16 = (c16func) address;
        (*c16)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5], cargs[6], cargs[7],
        cargs[8], cargs[9], cargs[10], cargs[11], cargs[12], cargs[13], cargs[14], cargs[15]);
        break;
    }

    case 17: {
        c17func c17 = (c17func) address;
        (*c17)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5], cargs[6], cargs[7],
        cargs[8], cargs[9], cargs[10], cargs[11], cargs[12], cargs[13], cargs[14], cargs[15],
        cargs[16]);
        break;
    }

    case 18: {
        c18func c18 = (c18func) address;
        (*c18)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5], cargs[6], cargs[7],
        cargs[8], cargs[9], cargs[10], cargs[11], cargs[12], cargs[13], cargs[14], cargs[15],
        cargs[16], cargs[17]);
        break;
    }

    case 19: {
        c19func c19 = (c19func) address;
        (*c19)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5], cargs[6], cargs[7],
        cargs[8], cargs[9], cargs[10], cargs[11], cargs[12], cargs[13], cargs[14], cargs[15],
        cargs[16], cargs[17], cargs[18]);
        break;
    }

    case 20: {
        c20func c20 = (c20func) address;
        (*c20)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5], cargs[6], cargs[7],
        cargs[8], cargs[9], cargs[10], cargs[11], cargs[12], cargs[13], cargs[14], cargs[15],
        cargs[16], cargs[17], cargs[18], cargs[19]);
        break;
    }

    case 21: {
        c21func c21 = (c21func) address;
        (*c21)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5], cargs[6], cargs[7],
        cargs[8], cargs[9], cargs[10], cargs[11], cargs[12], cargs[13], cargs[14], cargs[15],
        cargs[16], cargs[17], cargs[18], cargs[19], cargs[20]);
        break;
    }

    case 22: {
        c22func c22 = (c22func) address;
        (*c22)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5], cargs[6], cargs[7],
        cargs[8], cargs[9], cargs[10], cargs[11], cargs[12], cargs[13], cargs[14], cargs[15],
        cargs[16], cargs[17], cargs[18], cargs[19], cargs[20], cargs[21]);
        break;
    }

    case 23: {
        c23func c23 = (c23func) address;
        (*c23)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5], cargs[6], cargs[7],
        cargs[8], cargs[9], cargs[10], cargs[11], cargs[12], cargs[13], cargs[14], cargs[15],
        cargs[16], cargs[17], cargs[18], cargs[19], cargs[20], cargs[21], cargs[22]);
        break;
    }

    case 24: {
        c24func c24 = (c24func) address;
        (*c24)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5], cargs[6], cargs[7],
        cargs[8], cargs[9], cargs[10], cargs[11], cargs[12], cargs[13], cargs[14], cargs[15],
        cargs[16], cargs[17], cargs[18], cargs[19], cargs[20], cargs[21], cargs[22], cargs[23]);
        break;
    }

    case 25: {
        c25func c25 = (c25func) address;
        (*c25)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5], cargs[6], cargs[7],
        cargs[8], cargs[9], cargs[10], cargs[11], cargs[12], cargs[13], cargs[14], cargs[15],
        cargs[16], cargs[17], cargs[18], cargs[19], cargs[20], cargs[21], cargs[22], cargs[23],
        cargs[24]);
        break;
    }

    case 26: {
        c26func c26 = (c26func) address;
        (*c26)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5], cargs[6], cargs[7],
        cargs[8], cargs[9], cargs[10], cargs[11], cargs[12], cargs[13], cargs[14], cargs[15],
        cargs[16], cargs[17], cargs[18], cargs[19], cargs[20], cargs[21], cargs[22], cargs[23],
        cargs[24], cargs[25]);
        break;
    }

    case 27: {
        c27func c27 = (c27func) address;
        (*c27)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5], cargs[6], cargs[7],
        cargs[8], cargs[9], cargs[10], cargs[11], cargs[12], cargs[13], cargs[14], cargs[15],
        cargs[16], cargs[17], cargs[18], cargs[19], cargs[20], cargs[21], cargs[22], cargs[23],
        cargs[24], cargs[25], cargs[26]);
        break;
    }

    case 28: {
        c28func c28 = (c28func) address;
        (*c28)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5], cargs[6], cargs[7],
        cargs[8], cargs[9], cargs[10], cargs[11], cargs[12], cargs[13], cargs[14], cargs[15],
        cargs[16], cargs[17], cargs[18], cargs[19], cargs[20], cargs[21], cargs[22], cargs[23],
        cargs[24], cargs[25], cargs[26], cargs[27]);
        break;
    }

    case 29: {
        c29func c29 = (c29func) address;
        (*c29)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5], cargs[6], cargs[7],
        cargs[8], cargs[9], cargs[10], cargs[11], cargs[12], cargs[13], cargs[14], cargs[15],
        cargs[16], cargs[17], cargs[18], cargs[19], cargs[20], cargs[21], cargs[22], cargs[23],
        cargs[24], cargs[25], cargs[26], cargs[27], cargs[28]);
        break;
    }

    case 30: {
        c30func c30 = (c30func) address;
        (*c30)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5], cargs[6], cargs[7],
        cargs[8], cargs[9], cargs[10], cargs[11], cargs[12], cargs[13], cargs[14], cargs[15],
        cargs[16], cargs[17], cargs[18], cargs[19], cargs[20], cargs[21], cargs[22], cargs[23],
        cargs[24], cargs[25], cargs[26], cargs[27], cargs[28], cargs[29]);
        break;
    }

    case 31: {
        c31func c31 = (c31func) address;
        (*c31)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5], cargs[6], cargs[7],
        cargs[8], cargs[9], cargs[10], cargs[11], cargs[12], cargs[13], cargs[14], cargs[15],
        cargs[16], cargs[17], cargs[18], cargs[19], cargs[20], cargs[21], cargs[22], cargs[23],
        cargs[24], cargs[25], cargs[26], cargs[27], cargs[28], cargs[29], cargs[30]);
        break;
    }

    case 32: {
        c32func c32 = (c32func) address;
        (*c32)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5], cargs[6], cargs[7],
        cargs[8], cargs[9], cargs[10], cargs[11], cargs[12], cargs[13], cargs[14], cargs[15],
        cargs[16], cargs[17], cargs[18], cargs[19], cargs[20], cargs[21], cargs[22], cargs[23],
        cargs[24], cargs[25], cargs[26], cargs[27], cargs[28], cargs[29], cargs[30], cargs[31]);
        break;
    }

    case 33: {
        c33func c33 = (c33func) address;
        (*c33)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5], cargs[6], cargs[7],
        cargs[8], cargs[9], cargs[10], cargs[11], cargs[12], cargs[13], cargs[14], cargs[15],
        cargs[16], cargs[17], cargs[18], cargs[19], cargs[20], cargs[21], cargs[22], cargs[23],
        cargs[24], cargs[25], cargs[26], cargs[27], cargs[28], cargs[29], cargs[30], cargs[31],
        cargs[32]);
        break;
    }

    case 34: {
        c34func c34 = (c34func) address;
        (*c34)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5], cargs[6], cargs[7],
        cargs[8], cargs[9], cargs[10], cargs[11], cargs[12], cargs[13], cargs[14], cargs[15],
        cargs[16], cargs[17], cargs[18], cargs[19], cargs[20], cargs[21], cargs[22], cargs[23],
        cargs[24], cargs[25], cargs[26], cargs[27], cargs[28], cargs[29], cargs[30], cargs[31],
        cargs[32], cargs[33]);
        break;
    }

    case 35: {
        c35func c35 = (c35func) address;
        (*c35)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5], cargs[6], cargs[7],
        cargs[8], cargs[9], cargs[10], cargs[11], cargs[12], cargs[13], cargs[14], cargs[15],
        cargs[16], cargs[17], cargs[18], cargs[19], cargs[20], cargs[21], cargs[22], cargs[23],
        cargs[24], cargs[25], cargs[26], cargs[27], cargs[28], cargs[29], cargs[30], cargs[31],
        cargs[32], cargs[33], cargs[34]);
        break;
    }

    case 36: {
        c36func c36 = (c36func) address;
        (*c36)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5], cargs[6], cargs[7],
        cargs[8], cargs[9], cargs[10], cargs[11], cargs[12], cargs[13], cargs[14], cargs[15],
        cargs[16], cargs[17], cargs[18], cargs[19], cargs[20], cargs[21], cargs[22], cargs[23],
        cargs[24], cargs[25], cargs[26], cargs[27], cargs[28], cargs[29], cargs[30], cargs[31],
        cargs[32], cargs[33], cargs[34], cargs[35]);
        break;
    }

    case 37: {
        c37func c37 = (c37func) address;
        (*c37)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5], cargs[6], cargs[7],
        cargs[8], cargs[9], cargs[10], cargs[11], cargs[12], cargs[13], cargs[14], cargs[15],
        cargs[16], cargs[17], cargs[18], cargs[19], cargs[20], cargs[21], cargs[22], cargs[23],
        cargs[24], cargs[25], cargs[26], cargs[27], cargs[28], cargs[29], cargs[30], cargs[31],
        cargs[32], cargs[33], cargs[34], cargs[35], cargs[36]);
        break;
    }

    case 38: {
        c38func c38 = (c38func) address;
        (*c38)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5], cargs[6], cargs[7],
        cargs[8], cargs[9], cargs[10], cargs[11], cargs[12], cargs[13], cargs[14], cargs[15],
        cargs[16], cargs[17], cargs[18], cargs[19], cargs[20], cargs[21], cargs[22], cargs[23],
        cargs[24], cargs[25], cargs[26], cargs[27], cargs[28], cargs[29], cargs[30], cargs[31],
        cargs[32], cargs[33], cargs[34], cargs[35], cargs[36], cargs[37]);
        break;
    }

    case 39: {
        c39func c39 = (c39func) address;
        (*c39)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5], cargs[6], cargs[7],
        cargs[8], cargs[9], cargs[10], cargs[11], cargs[12], cargs[13], cargs[14], cargs[15],
        cargs[16], cargs[17], cargs[18], cargs[19], cargs[20], cargs[21], cargs[22], cargs[23],
        cargs[24], cargs[25], cargs[26], cargs[27], cargs[28], cargs[29], cargs[30], cargs[31],
        cargs[32], cargs[33], cargs[34], cargs[35], cargs[36], cargs[37], cargs[38]);
        break;
    }

    case 40: {
        c40func c40 = (c40func) address;
        (*c40)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5], cargs[6], cargs[7],
        cargs[8], cargs[9], cargs[10], cargs[11], cargs[12], cargs[13], cargs[14], cargs[15],
        cargs[16], cargs[17], cargs[18], cargs[19], cargs[20], cargs[21], cargs[22], cargs[23],
        cargs[24], cargs[25], cargs[26], cargs[27], cargs[28], cargs[29], cargs[30], cargs[31],
        cargs[32], cargs[33], cargs[34], cargs[35], cargs[36], cargs[37], cargs[38], cargs[39]);
        break;
    }

    case 41: {
        c41func c41 = (c41func) address;
        (*c41)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5], cargs[6], cargs[7],
        cargs[8], cargs[9], cargs[10], cargs[11], cargs[12], cargs[13], cargs[14], cargs[15],
        cargs[16], cargs[17], cargs[18], cargs[19], cargs[20], cargs[21], cargs[22], cargs[23],
        cargs[24], cargs[25], cargs[26], cargs[27], cargs[28], cargs[29], cargs[30], cargs[31],
        cargs[32], cargs[33], cargs[34], cargs[35], cargs[36], cargs[37], cargs[38], cargs[39],
        cargs[40]);
        break;
    }

    case 42: {
        c42func c42 = (c42func) address;
        (*c42)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5], cargs[6], cargs[7],
        cargs[8], cargs[9], cargs[10], cargs[11], cargs[12], cargs[13], cargs[14], cargs[15],
        cargs[16], cargs[17], cargs[18], cargs[19], cargs[20], cargs[21], cargs[22], cargs[23],
        cargs[24], cargs[25], cargs[26], cargs[27], cargs[28], cargs[29], cargs[30], cargs[31],
        cargs[32], cargs[33], cargs[34], cargs[35], cargs[36], cargs[37], cargs[38], cargs[39],
        cargs[40], cargs[41]);
        break;
    }

    case 43: {
        c43func c43 = (c43func) address;
        (*c43)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5], cargs[6], cargs[7],
        cargs[8], cargs[9], cargs[10], cargs[11], cargs[12], cargs[13], cargs[14], cargs[15],
        cargs[16], cargs[17], cargs[18], cargs[19], cargs[20], cargs[21], cargs[22], cargs[23],
        cargs[24], cargs[25], cargs[26], cargs[27], cargs[28], cargs[29], cargs[30], cargs[31],
        cargs[32], cargs[33], cargs[34], cargs[35], cargs[36], cargs[37], cargs[38], cargs[39],
        cargs[40], cargs[41], cargs[42]);
        break;
    }

    case 44: {
        c44func c44 = (c44func) address;
        (*c44)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5], cargs[6], cargs[7],
        cargs[8], cargs[9], cargs[10], cargs[11], cargs[12], cargs[13], cargs[14], cargs[15],
        cargs[16], cargs[17], cargs[18], cargs[19], cargs[20], cargs[21], cargs[22], cargs[23],
        cargs[24], cargs[25], cargs[26], cargs[27], cargs[28], cargs[29], cargs[30], cargs[31],
        cargs[32], cargs[33], cargs[34], cargs[35], cargs[36], cargs[37], cargs[38], cargs[39],
        cargs[40], cargs[41], cargs[42], cargs[43]);
        break;
    }

    case 45: {
        c45func c45 = (c45func) address;
        (*c45)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5], cargs[6], cargs[7],
        cargs[8], cargs[9], cargs[10], cargs[11], cargs[12], cargs[13], cargs[14], cargs[15],
        cargs[16], cargs[17], cargs[18], cargs[19], cargs[20], cargs[21], cargs[22], cargs[23],
        cargs[24], cargs[25], cargs[26], cargs[27], cargs[28], cargs[29], cargs[30], cargs[31],
        cargs[32], cargs[33], cargs[34], cargs[35], cargs[36], cargs[37], cargs[38], cargs[39],
        cargs[40], cargs[41], cargs[42], cargs[43], cargs[44]);
        break;
    }

    case 46: {
        c46func c46 = (c46func) address;
        (*c46)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5], cargs[6], cargs[7],
        cargs[8], cargs[9], cargs[10], cargs[11], cargs[12], cargs[13], cargs[14], cargs[15],
        cargs[16], cargs[17], cargs[18], cargs[19], cargs[20], cargs[21], cargs[22], cargs[23],
        cargs[24], cargs[25], cargs[26], cargs[27], cargs[28], cargs[29], cargs[30], cargs[31],
        cargs[32], cargs[33], cargs[34], cargs[35], cargs[36], cargs[37], cargs[38], cargs[39],
        cargs[40], cargs[41], cargs[42], cargs[43], cargs[44], cargs[45]);
        break;
    }

    case 47: {
        c47func c47 = (c47func) address;
        (*c47)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5], cargs[6], cargs[7],
        cargs[8], cargs[9], cargs[10], cargs[11], cargs[12], cargs[13], cargs[14], cargs[15],
        cargs[16], cargs[17], cargs[18], cargs[19], cargs[20], cargs[21], cargs[22], cargs[23],
        cargs[24], cargs[25], cargs[26], cargs[27], cargs[28], cargs[29], cargs[30], cargs[31],
        cargs[32], cargs[33], cargs[34], cargs[35], cargs[36], cargs[37], cargs[38], cargs[39],
        cargs[40], cargs[41], cargs[42], cargs[43], cargs[44], cargs[45], cargs[46]);
        break;
    }

    case 48: {
        c48func c48 = (c48func) address;
        (*c48)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5], cargs[6], cargs[7],
        cargs[8], cargs[9], cargs[10], cargs[11], cargs[12], cargs[13], cargs[14], cargs[15],
        cargs[16], cargs[17], cargs[18], cargs[19], cargs[20], cargs[21], cargs[22], cargs[23],
        cargs[24], cargs[25], cargs[26], cargs[27], cargs[28], cargs[29], cargs[30], cargs[31],
        cargs[32], cargs[33], cargs[34], cargs[35], cargs[36], cargs[37], cargs[38], cargs[39],
        cargs[40], cargs[41], cargs[42], cargs[43], cargs[44], cargs[45], cargs[46], cargs[47]);
        break;
    }

    case 49: {
        c49func c49 = (c49func) address;
        (*c49)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5], cargs[6], cargs[7],
        cargs[8], cargs[9], cargs[10], cargs[11], cargs[12], cargs[13], cargs[14], cargs[15],
        cargs[16], cargs[17], cargs[18], cargs[19], cargs[20], cargs[21], cargs[22], cargs[23],
        cargs[24], cargs[25], cargs[26], cargs[27], cargs[28], cargs[29], cargs[30], cargs[31],
        cargs[32], cargs[33], cargs[34], cargs[35], cargs[36], cargs[37], cargs[38], cargs[39],
        cargs[40], cargs[41], cargs[42], cargs[43], cargs[44], cargs[45], cargs[46], cargs[47],
        cargs[48]);
        break;
    }

    case 50: {
        c50func c50 = (c50func) address;
        (*c50)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5], cargs[6], cargs[7],
        cargs[8], cargs[9], cargs[10], cargs[11], cargs[12], cargs[13], cargs[14], cargs[15],
        cargs[16], cargs[17], cargs[18], cargs[19], cargs[20], cargs[21], cargs[22], cargs[23],
        cargs[24], cargs[25], cargs[26], cargs[27], cargs[28], cargs[29], cargs[30], cargs[31],
        cargs[32], cargs[33], cargs[34], cargs[35], cargs[36], cargs[37], cargs[38], cargs[39],
        cargs[40], cargs[41], cargs[42], cargs[43], cargs[44], cargs[45], cargs[46], cargs[47],
        cargs[48], cargs[49]);
        break;
    }

    case 51: {
        c51func c51 = (c51func) address;
        (*c51)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5], cargs[6], cargs[7],
        cargs[8], cargs[9], cargs[10], cargs[11], cargs[12], cargs[13], cargs[14], cargs[15],
        cargs[16], cargs[17], cargs[18], cargs[19], cargs[20], cargs[21], cargs[22], cargs[23],
        cargs[24], cargs[25], cargs[26], cargs[27], cargs[28], cargs[29], cargs[30], cargs[31],
        cargs[32], cargs[33], cargs[34], cargs[35], cargs[36], cargs[37], cargs[38], cargs[39],
        cargs[40], cargs[41], cargs[42], cargs[43], cargs[44], cargs[45], cargs[46], cargs[47],
        cargs[48], cargs[49], cargs[50]);
        break;
    }

    case 52: {
        c52func c52 = (c52func) address;
        (*c52)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5], cargs[6], cargs[7],
        cargs[8], cargs[9], cargs[10], cargs[11], cargs[12], cargs[13], cargs[14], cargs[15],
        cargs[16], cargs[17], cargs[18], cargs[19], cargs[20], cargs[21], cargs[22], cargs[23],
        cargs[24], cargs[25], cargs[26], cargs[27], cargs[28], cargs[29], cargs[30], cargs[31],
        cargs[32], cargs[33], cargs[34], cargs[35], cargs[36], cargs[37], cargs[38], cargs[39],
        cargs[40], cargs[41], cargs[42], cargs[43], cargs[44], cargs[45], cargs[46], cargs[47],
        cargs[48], cargs[49], cargs[50], cargs[51]);
        break;
    }

    case 53: {
        c53func c53 = (c53func) address;
        (*c53)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5], cargs[6], cargs[7],
        cargs[8], cargs[9], cargs[10], cargs[11], cargs[12], cargs[13], cargs[14], cargs[15],
        cargs[16], cargs[17], cargs[18], cargs[19], cargs[20], cargs[21], cargs[22], cargs[23],
        cargs[24], cargs[25], cargs[26], cargs[27], cargs[28], cargs[29], cargs[30], cargs[31],
        cargs[32], cargs[33], cargs[34], cargs[35], cargs[36], cargs[37], cargs[38], cargs[39],
        cargs[40], cargs[41], cargs[42], cargs[43], cargs[44], cargs[45], cargs[46], cargs[47],
        cargs[48], cargs[49], cargs[50], cargs[51], cargs[52]);
        break;
    }

    case 54: {
        c54func c54 = (c54func) address;
        (*c54)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5], cargs[6], cargs[7],
        cargs[8], cargs[9], cargs[10], cargs[11], cargs[12], cargs[13], cargs[14], cargs[15],
        cargs[16], cargs[17], cargs[18], cargs[19], cargs[20], cargs[21], cargs[22], cargs[23],
        cargs[24], cargs[25], cargs[26], cargs[27], cargs[28], cargs[29], cargs[30], cargs[31],
        cargs[32], cargs[33], cargs[34], cargs[35], cargs[36], cargs[37], cargs[38], cargs[39],
        cargs[40], cargs[41], cargs[42], cargs[43], cargs[44], cargs[45], cargs[46], cargs[47],
        cargs[48], cargs[49], cargs[50], cargs[51], cargs[52], cargs[53]);
        break;
    }

    case 55: {
        c55func c55 = (c55func) address;
        (*c55)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5], cargs[6], cargs[7],
        cargs[8], cargs[9], cargs[10], cargs[11], cargs[12], cargs[13], cargs[14], cargs[15],
        cargs[16], cargs[17], cargs[18], cargs[19], cargs[20], cargs[21], cargs[22], cargs[23],
        cargs[24], cargs[25], cargs[26], cargs[27], cargs[28], cargs[29], cargs[30], cargs[31],
        cargs[32], cargs[33], cargs[34], cargs[35], cargs[36], cargs[37], cargs[38], cargs[39],
        cargs[40], cargs[41], cargs[42], cargs[43], cargs[44], cargs[45], cargs[46], cargs[47],
        cargs[48], cargs[49], cargs[50], cargs[51], cargs[52], cargs[53], cargs[54]);
        break;
    }

    case 56: {
        c56func c56 = (c56func) address;
        (*c56)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5], cargs[6], cargs[7],
        cargs[8], cargs[9], cargs[10], cargs[11], cargs[12], cargs[13], cargs[14], cargs[15],
        cargs[16], cargs[17], cargs[18], cargs[19], cargs[20], cargs[21], cargs[22], cargs[23],
        cargs[24], cargs[25], cargs[26], cargs[27], cargs[28], cargs[29], cargs[30], cargs[31],
        cargs[32], cargs[33], cargs[34], cargs[35], cargs[36], cargs[37], cargs[38], cargs[39],
        cargs[40], cargs[41], cargs[42], cargs[43], cargs[44], cargs[45], cargs[46], cargs[47],
        cargs[48], cargs[49], cargs[50], cargs[51], cargs[52], cargs[53], cargs[54], cargs[55]);
        break;
    }

    case 57: {
        c57func c57 = (c57func) address;
        (*c57)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5], cargs[6], cargs[7],
        cargs[8], cargs[9], cargs[10], cargs[11], cargs[12], cargs[13], cargs[14], cargs[15],
        cargs[16], cargs[17], cargs[18], cargs[19], cargs[20], cargs[21], cargs[22], cargs[23],
        cargs[24], cargs[25], cargs[26], cargs[27], cargs[28], cargs[29], cargs[30], cargs[31],
        cargs[32], cargs[33], cargs[34], cargs[35], cargs[36], cargs[37], cargs[38], cargs[39],
        cargs[40], cargs[41], cargs[42], cargs[43], cargs[44], cargs[45], cargs[46], cargs[47],
        cargs[48], cargs[49], cargs[50], cargs[51], cargs[52], cargs[53], cargs[54], cargs[55],
        cargs[56]);
        break;
    }

    case 58: {
        c58func c58 = (c58func) address;
        (*c58)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5], cargs[6], cargs[7],
        cargs[8], cargs[9], cargs[10], cargs[11], cargs[12], cargs[13], cargs[14], cargs[15],
        cargs[16], cargs[17], cargs[18], cargs[19], cargs[20], cargs[21], cargs[22], cargs[23],
        cargs[24], cargs[25], cargs[26], cargs[27], cargs[28], cargs[29], cargs[30], cargs[31],
        cargs[32], cargs[33], cargs[34], cargs[35], cargs[36], cargs[37], cargs[38], cargs[39],
        cargs[40], cargs[41], cargs[42], cargs[43], cargs[44], cargs[45], cargs[46], cargs[47],
        cargs[48], cargs[49], cargs[50], cargs[51], cargs[52], cargs[53], cargs[54], cargs[55],
        cargs[56], cargs[57]);
        break;
    }

    case 59: {
        c59func c59 = (c59func) address;
        (*c59)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5], cargs[6], cargs[7],
        cargs[8], cargs[9], cargs[10], cargs[11], cargs[12], cargs[13], cargs[14], cargs[15],
        cargs[16], cargs[17], cargs[18], cargs[19], cargs[20], cargs[21], cargs[22], cargs[23],
        cargs[24], cargs[25], cargs[26], cargs[27], cargs[28], cargs[29], cargs[30], cargs[31],
        cargs[32], cargs[33], cargs[34], cargs[35], cargs[36], cargs[37], cargs[38], cargs[39],
        cargs[40], cargs[41], cargs[42], cargs[43], cargs[44], cargs[45], cargs[46], cargs[47],
        cargs[48], cargs[49], cargs[50], cargs[51], cargs[52], cargs[53], cargs[54], cargs[55],
        cargs[56], cargs[57], cargs[58]);
        break;
    }

    case 60: {
        c60func c60 = (c60func) address;
        (*c60)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5], cargs[6], cargs[7],
        cargs[8], cargs[9], cargs[10], cargs[11], cargs[12], cargs[13], cargs[14], cargs[15],
        cargs[16], cargs[17], cargs[18], cargs[19], cargs[20], cargs[21], cargs[22], cargs[23],
        cargs[24], cargs[25], cargs[26], cargs[27], cargs[28], cargs[29], cargs[30], cargs[31],
        cargs[32], cargs[33], cargs[34], cargs[35], cargs[36], cargs[37], cargs[38], cargs[39],
        cargs[40], cargs[41], cargs[42], cargs[43], cargs[44], cargs[45], cargs[46], cargs[47],
        cargs[48], cargs[49], cargs[50], cargs[51], cargs[52], cargs[53], cargs[54], cargs[55],
        cargs[56], cargs[57], cargs[58], cargs[59]);
        break;
    }

    case 61: {
        c61func c61 = (c61func) address;
        (*c61)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5], cargs[6], cargs[7],
        cargs[8], cargs[9], cargs[10], cargs[11], cargs[12], cargs[13], cargs[14], cargs[15],
        cargs[16], cargs[17], cargs[18], cargs[19], cargs[20], cargs[21], cargs[22], cargs[23],
        cargs[24], cargs[25], cargs[26], cargs[27], cargs[28], cargs[29], cargs[30], cargs[31],
        cargs[32], cargs[33], cargs[34], cargs[35], cargs[36], cargs[37], cargs[38], cargs[39],
        cargs[40], cargs[41], cargs[42], cargs[43], cargs[44], cargs[45], cargs[46], cargs[47],
        cargs[48], cargs[49], cargs[50], cargs[51], cargs[52], cargs[53], cargs[54], cargs[55],
        cargs[56], cargs[57], cargs[58], cargs[59], cargs[60]);
        break;
    }

    case 62: {
        c62func c62 = (c62func) address;
        (*c62)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5], cargs[6], cargs[7],
        cargs[8], cargs[9], cargs[10], cargs[11], cargs[12], cargs[13], cargs[14], cargs[15],
        cargs[16], cargs[17], cargs[18], cargs[19], cargs[20], cargs[21], cargs[22], cargs[23],
        cargs[24], cargs[25], cargs[26], cargs[27], cargs[28], cargs[29], cargs[30], cargs[31],
        cargs[32], cargs[33], cargs[34], cargs[35], cargs[36], cargs[37], cargs[38], cargs[39],
        cargs[40], cargs[41], cargs[42], cargs[43], cargs[44], cargs[45], cargs[46], cargs[47],
        cargs[48], cargs[49], cargs[50], cargs[51], cargs[52], cargs[53], cargs[54], cargs[55],
        cargs[56], cargs[57], cargs[58], cargs[59], cargs[60], cargs[61]);
        break;
    }

    case 63: {
        c63func c63 = (c63func) address;
        (*c63)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5], cargs[6], cargs[7],
        cargs[8], cargs[9], cargs[10], cargs[11], cargs[12], cargs[13], cargs[14], cargs[15],
        cargs[16], cargs[17], cargs[18], cargs[19], cargs[20], cargs[21], cargs[22], cargs[23],
        cargs[24], cargs[25], cargs[26], cargs[27], cargs[28], cargs[29], cargs[30], cargs[31],
        cargs[32], cargs[33], cargs[34], cargs[35], cargs[36], cargs[37], cargs[38], cargs[39],
        cargs[40], cargs[41], cargs[42], cargs[43], cargs[44], cargs[45], cargs[46], cargs[47],
        cargs[48], cargs[49], cargs[50], cargs[51], cargs[52], cargs[53], cargs[54], cargs[55],
        cargs[56], cargs[57], cargs[58], cargs[59], cargs[60], cargs[61], cargs[62]);
        break;
    }

    case 64: {
        c64func c64 = (c64func) address;
        (*c64)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5], cargs[6], cargs[7],
        cargs[8], cargs[9], cargs[10], cargs[11], cargs[12], cargs[13], cargs[14], cargs[15],
        cargs[16], cargs[17], cargs[18], cargs[19], cargs[20], cargs[21], cargs[22], cargs[23],
        cargs[24], cargs[25], cargs[26], cargs[27], cargs[28], cargs[29], cargs[30], cargs[31],
        cargs[32], cargs[33], cargs[34], cargs[35], cargs[36], cargs[37], cargs[38], cargs[39],
        cargs[40], cargs[41], cargs[42], cargs[43], cargs[44], cargs[45], cargs[46], cargs[47],
        cargs[48], cargs[49], cargs[50], cargs[51], cargs[52], cargs[53], cargs[54], cargs[55],
        cargs[56], cargs[57], cargs[58], cargs[59], cargs[60], cargs[61], cargs[62], cargs[63]);
        break;
    }

    case 65: {
        c65func c65 = (c65func) address;
        (*c65)(cargs[0], cargs[1], cargs[2], cargs[3], cargs[4], cargs[5], cargs[6], cargs[7],
        cargs[8], cargs[9], cargs[10], cargs[11], cargs[12], cargs[13], cargs[14], cargs[15],
        cargs[16], cargs[17], cargs[18], cargs[19], cargs[20], cargs[21], cargs[22], cargs[23],
        cargs[24], cargs[25], cargs[26], cargs[27], cargs[28], cargs[29], cargs[30], cargs[31],
        cargs[32], cargs[33], cargs[34], cargs[35], cargs[36], cargs[37], cargs[38], cargs[39],
        cargs[40], cargs[41], cargs[42], cargs[43], cargs[44], cargs[45], cargs[46], cargs[47],
        cargs[48], cargs[49], cargs[50], cargs[51], cargs[52], cargs[53], cargs[54], cargs[55],
        cargs[56], cargs[57], cargs[58], cargs[59], cargs[60], cargs[61], cargs[62], cargs[63],
        cargs[64]);
        break;
    }
	}
}

// Note: hasStrings indicates that the args array may contain 2 dimensional byte arrays, which represent string vectors.
JNIEXPORT void JNICALL
Java_com_oracle_truffle_r_ffi_impl_jni_JNI_1C_c(JNIEnv *env, jclass c, jlong address, jobjectArray args, jboolean hasStrings) {
  int len = (*env)->GetArrayLength(env, args);
  void *cargs[len];     // pointers to primitive arrays suitable for the actual c call
  jobject jarrays[len]; // jarray instances corresponding to cargs native counterparts
  jobject *dim2[len];   // if corresponding jarray[i] is 2-dimensional, this holds the array of jarrays, otherwise NULL
  jclass byteArrayClass = NULL;
  if (hasStrings) {
      byteArrayClass = (*env)->FindClass(env, "[[B");
  }

  for (int i = 0; i < len; i++) {
    jarrays[i] = (*env)->GetObjectArrayElement(env, args, i);
    bool isString = hasStrings && (*env)->IsInstanceOf(env, jarrays[i], byteArrayClass);
    if (isString) {
      int len2 = (*env)->GetArrayLength(env, jarrays[i]);
      dim2[i] = calloc(sizeof(jobject), len2);
      const char **strArgs = calloc(sizeof(const char*), len2);
      cargs[i] = strArgs;
      for (int j = 0; j < len2; j++) {
        dim2[i][j] = (*env)->GetObjectArrayElement(env, jarrays[i], j);
        strArgs[j] = (*env)->GetPrimitiveArrayCritical(env, dim2[i][j], NULL);
      }
    } else {
      dim2[i] = NULL;
      cargs[i] = (*env)->GetPrimitiveArrayCritical(env, jarrays[i], NULL);
    }
  }

  doCall(address, len, cargs);

  for (int i = 0; i < len; i++) {
    if (dim2[i] != NULL) {
      int len2 = (*env)->GetArrayLength(env, jarrays[i]);
      const char **strArgs = (const char**) cargs[i];
      for (int j = 0; j < len2; j++) {
        (*env)->ReleasePrimitiveArrayCritical(env, dim2[i][j], (void*) strArgs[j], 0);
      }
      free(dim2[i]);
      free(cargs[i]);
    } else {
      (*env)->ReleasePrimitiveArrayCritical(env, jarrays[i], cargs[i], 0);
    }
  }
}
