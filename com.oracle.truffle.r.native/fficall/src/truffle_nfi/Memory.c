/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2015, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
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

