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
#include <rffiutils.h>
#include <stdlib.h>
#include <string.h>

// TODO: this file can likely be removed, using the NFI version instead

#define T_MEM_TABLE_INITIAL_SIZE 0
// The table of transient objects that have been allocated dur the current FFI call
static void **tMemTable;
// hwm of tMemTable
static int tMemTableIndex;
static int tMemTableLength;

void *R_chk_calloc(size_t nelem, size_t elsize);

// Memory that is auto-reclaimed across FFI calls
char *R_alloc(size_t n, int size) {
    void *p = R_chk_calloc(n, size);
    if (tMemTableIndex >= tMemTableLength) {
	int newLength = 2 * tMemTableLength;
	void *newtMemTable = malloc(sizeof(void*) * newLength);
	if (newtMemTable == NULL) {
	    fatalError("malloc failure");
	}
	memcpy(newtMemTable, tMemTable, tMemTableLength * sizeof(void*));
	free(tMemTable);
	tMemTable = newtMemTable;
	tMemTableLength = newLength;
    }
    tMemTable[tMemTableIndex] = p;
    return (char*) p;
}

char* S_alloc(long n, int size) {
	char *p = R_alloc(n, size);
	memset(p, 0, n);
	return p;
}

char* S_realloc(char *p, long a, long b, int size) {
	return unimplemented("S_realloc");
}

void allocExit() {
    int i;
    for (i = 0; i < tMemTableIndex; i++) {
	free(tMemTable[i]);
    }
}

void *R_chk_calloc(size_t nelem, size_t elsize) {
    void *p;
#ifndef HAVE_WORKING_CALLOC
    if (nelem == 0)
	return (NULL);
#endif
    p = calloc(nelem, elsize);
    if (!p) /* problem here is that we don't have a format for size_t. */
	error(_("'Calloc' could not allocate memory (%.0f of %u bytes)"),
		(double) nelem, elsize);
    return (p);
}

void *R_chk_realloc(void *ptr, size_t size) {
    void *p;
    /* Protect against broken realloc */
    if(ptr) p = realloc(ptr, size); else p = malloc(size);
    if(!p)
	error(_("'Realloc' could not re-allocate memory (%.0f bytes)"),
	      (double) size);
    return(p);
}

void R_chk_free(void *ptr) {
    if(ptr) {
	    free(ptr);
    }
}

int VMAX_MAGIC = 1234;

void* vmaxget(void) {
//    unimplemented("vmaxget");
    // ignored
    return &VMAX_MAGIC;
}

void vmaxset(const void * x) {
//    unimplemented("vmaxget");
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

