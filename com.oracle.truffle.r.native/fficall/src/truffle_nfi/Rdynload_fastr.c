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
#include <rffiutils.h>

static void **dynload_callbacks;

void *ensure_fun(void *fun) {
	return fun;
}

#include "../truffle_common/Rdynload_fastr.h"

void Rdynload_init(TruffleEnv* env, int index, void* closure) {
	(*env)->newClosureRef(env, closure);
	if (dynload_callbacks == NULL) {
		dynload_callbacks = malloc(CALLBACK_TABLE_SIZE * sizeof(void*));
	}
	dynload_callbacks[index] = closure;
}

