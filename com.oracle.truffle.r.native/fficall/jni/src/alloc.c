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

void init_alloc(JNIEnv *env) {

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
