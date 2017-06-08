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

#include "../common/rffi_upcalls.h"

extern void init_memory();
extern void init_utils();

// use for an unimplemented API function
void *unimplemented(char *msg) __attribute__((noreturn));
// use for any fatal error
void fatalError(char *msg) __attribute__((noreturn));

// checks x against the list of global refs, returning the global version if x matches (IsSameObject)
SEXP checkRef(SEXP x);
// creates a global JNI global ref from x. If permanent is non-zero, calls to
// releaseGlobalRef are ignored and the global ref persists for the entire execution
// (used for the R global variables such as R_NilValue).
SEXP createGlobalRef(SEXP x, int permanent);
// release a previously created global ref
void releaseGlobalRef(SEXP x);

#endif
