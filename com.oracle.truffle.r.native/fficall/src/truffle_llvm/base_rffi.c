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
#include <rffiutils.h>

#include <sys/types.h>
#include <unistd.h>
#include <sys/stat.h>
#include <glob.h>
#include <sys/utsname.h>
#include <errno.h>

#define IMPORT_BASE_HELPER() void *base = truffle_import_cached("_fastr_rffi_base")

int call_base_getpid() {
	return getpid();
}

int call_base_getwd(char *buf, int len) {
	char *r = getcwd(buf, len);
	if (r == NULL) {
		return 0;
	} else {
		return 1;
	}
}

int call_base_setwd(char *dir) {
	return chdir(dir);
}

int call_base_mkdir(char *dir, int mode) {
	return mkdir(dir, mode);
}

int call_base_mkdtemp(char *template) {
	char *r = mkdtemp(template);
	if (r == NULL) {
		return 0;
	} else {
		return 1;
	}
}

void call_base_readlink(void *callback, char* path) {
	char *link = NULL;
    char buf[4096];
	int cerrno = 0;
    int len = readlink(path, buf, 4096);
    IMPORT_BASE_HELPER();
    if (len == -1) {
    	cerrno = errno;
    } else {
    	buf[len] = 0;
    	link = ensure_truffle_chararray(buf);
    }
    truffle_invoke(base,"setReadlinkResult", callback, link, cerrno);
}

void call_base_strtol(void *callback, char *s, int nbase) {
	long rc = strtol(s, NULL, nbase);
	IMPORT_BASE_HELPER();
	truffle_invoke(base, "setStrtolResult", callback, rc, errno);
}

void call_base_uname(void *callback) {
	struct utsname name;

	uname(&name);
	IMPORT_BASE_HELPER();
	truffle_invoke(base, "setUnameResult",
			callback,
			ensure_truffle_chararray(name.sysname),
			ensure_truffle_chararray(name.release),
			ensure_truffle_chararray(name.version),
			ensure_truffle_chararray(name.machine),
			ensure_truffle_chararray(name.nodename));
}

int call_base_chmod(char *path, int mode) {
	int rc = chmod(path, mode);
	return rc;
}

int errfunc(const char* path, int error) {
	return 0;
}

void call_base_glob(void *callback, char *pattern) {
	glob_t globstruct;

	int rc = glob(pattern, 0, errfunc, &globstruct);
	if (rc == 0) {
		IMPORT_BASE_HELPER();

		int i;
		for (i = 0; i < globstruct.gl_pathc; i++) {
			char *path = globstruct.gl_pathv[i];
			truffle_invoke(base, "setGlobResult", callback, ensure_truffle_chararray(path));
		}
	}

}
