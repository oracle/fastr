/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2015, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
#include "rffiutils.h"
#include <stdlib.h>

#define T_MEM_TABLE_INITIAL_SIZE 0
// The table of transient objects that have been allocated dur the current FFI call
static void **tMemTable;
// hwm of tMemTable
static int tMemTableIndex;
static int tMemTableLength;
void init_alloc(JNIEnv *env) {
	tMemTable = malloc(sizeof(void*) * T_MEM_TABLE_INITIAL_SIZE);
    tMemTableLength = T_MEM_TABLE_INITIAL_SIZE;
    tMemTableIndex = 0;
}


// Memory that is auto-reclaimed across FFI calls
char *R_alloc(size_t n, int size) {
	void *p = R_chk_alloc(n, size);
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

void allocExit() {
	int i;
	for (i = 0; i < tMemTableIndex; i++) {
		free(tMemTable[i]);
	}
}

void *R_chk_calloc(size_t nelem, size_t elsize) {
	    void *p;
	#ifndef HAVE_WORKING_CALLOC
	    if(nelem == 0)
		return(NULL);
	#endif
	    p = calloc(nelem, elsize);
	    if(!p) /* problem here is that we don't have a format for size_t. */
		error(_("'Calloc' could not allocate memory (%.0f of %u bytes)"),
		      (double) nelem, elsize);
	    return(p);
}

void *R_chk_realloc(void *p, size_t size) {
	unimplemented("R_chk_realloc");
}

void R_chk_free(void *p) {
	unimplemented("R_chk_free");
}

void* vmaxget(void) {
    unimplemented("vmaxget");
}

void vmaxset(const void * x) {
    unimplemented("vmaxget");
}


void R_gc(void) {
    unimplemented("R_gc");
}

int	R_gc_running() {
    unimplemented("R_gc_running");
}
