/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
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

/*
 * This is a wrapper to the native dlopen/dlclose calls. The wrapper allows it to be called through the LLVM
 * RFFI machinery, and that takes care of the actual native call.
 *
 * In an LLVM build, this is only used in very special circumstances, namely when a native library
 * for which no LLVM is available must be loaded, e.g., by the R dyn.load call. It must be separate
 * because the DLLRFFI implementation in an LLVM build, expects, by default, that dlopen will open
 * a library containing LLVM.
 */

#include <rffiutils.h>
#include <stdio.h>
#include <dlfcn.h>
#include <errno.h>

long call_dlopen(void *callback, char *path, int local, int now) {
	int flags = (local ? RTLD_LOCAL : RTLD_GLOBAL) | (now ? RTLD_NOW : RTLD_LAZY);
	void *handle = dlopen(path, flags);
	if (handle == NULL) {
		int cerrno = errno;
		char *error = dlerror();
	    truffle_invoke(truffle_import_cached("_fastr_dllnative_helper"), "setDlopenResult", callback, error);
	}
	return (long) handle;
}

int call_dlclose(void *handle) {
	return dlclose(handle);
}
