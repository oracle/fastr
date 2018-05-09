/*
 * Copyright (c) 1995-2015, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, a copy is available at
 * https://www.R-project.org/Licenses/
 */
#include <Defn.h>
#include "rffiutils.h"
#include <stdlib.h>
#include <string.h>
#include <rffi_upcalls.h>

// R_alloc should allocate memory that is auto-reclaimed across FFI calls.
// In FastR this memory is managed by RContext to avoid race conditions.
// FastR frees this memory in Java at the end of every FFI call (down-call).
char *R_alloc(size_t n, int size) {
    return (char *) ((call_R_alloc) callbacks[R_alloc_x])(n, size);
}

// This is S compatible version of R_alloc
char *S_alloc(long n, int size) {
    char *p = R_alloc(n, size);
    memset(p, 0, n);
    return p;
}

char *S_realloc(char *p, long a, long b, int size) {
    return unimplemented("S_realloc");
}

void *R_chk_calloc(size_t nelem, size_t elsize) {
    void *p;
#ifndef HAVE_WORKING_CALLOC
    if (nelem == 0)
        return (NULL);
#endif
    p = calloc(nelem, elsize);
    if (!p) /* problem here is that we don't have a format for size_t. */
        error("'Calloc' could not allocate memory (%.0f of %u bytes)",
              (double) nelem, elsize);
    return (p);
}

void *R_chk_realloc(void *ptr, size_t size) {
    void *p;
    /* Protect against broken realloc */
    if (ptr) p = realloc(ptr, size); else p = malloc(size);
    if (!p)
        error("'Realloc' could not re-allocate memory (%.0f bytes)",
              (double) size);
    return (p);
}

void R_chk_free(void *ptr) {
    if (ptr) {
        free(ptr);
    }
}

int VMAX_MAGIC = 1234;

void *vmaxget(void) {
    // ignored
    return &VMAX_MAGIC;
}

void vmaxset(const void *x) {
    if (x != &VMAX_MAGIC) {
        unimplemented("vmaxset with different value");
    }
}

void R_gc(void) {
    unimplemented("R_gc");
}

int R_gc_running() {
    unimplemented("R_gc_running");
    return 0;
}

SEXP Rf_allocS4Object() {
    unimplemented("Rf_allocS4Object unimplemented");
    return NULL;
}

SEXP do_address(SEXP call, SEXP op, SEXP args, SEXP rho)
{
    checkArity(op, args);
    return R_MakeExternalPtr((void *) CAR(args), R_NilValue, R_NilValue);
}

