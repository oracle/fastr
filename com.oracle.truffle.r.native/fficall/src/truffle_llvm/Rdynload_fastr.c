/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

// Registering routines from loaded shared libraries - LLVM variant

#include <truffle.h>
#include <rffiutils.h>

static void **dynload_callbacks = NULL;

void *ensure_fun(void *fun) {
	void *r = truffle_address_to_function(fun);
	return r;
}

#include "../truffle_common/Rdynload_fastr.h"

void Rdynload_addCallback(int index, void* callback) {
	if (dynload_callbacks == NULL) {
		dynload_callbacks = truffle_managed_malloc(CALLBACK_TABLE_SIZE * sizeof(void*));
	}
	dynload_callbacks[index] = callback;
}


