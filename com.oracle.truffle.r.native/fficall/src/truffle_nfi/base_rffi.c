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


void call_uname(void (*call_uname_setfields)(char *sysname, char *release, char *version, char *machine, char *nodename)) {
	struct utsname name;

	uname(&name);
	call_uname_setfields(name.sysname, name.release, name.version, name.machine, name.nodename);
}

void call_glob(char *pattern, void *closure) {
	void (*call_addpath)(char *path) = closure;

	glob_t globstruct;
	int rc = glob(pattern, 0, NULL, &globstruct);
	if (rc == 0) {
		int i;
		for (i = 0; i < globstruct.gl_pathc; i++) {
			char *path = globstruct.gl_pathv[i];
			call_addpath(path);
		}
	}
}

void call_readlink(void (*call_setresult)(char *link, int errno), char *path) {
	char *link = NULL;
	int cerrno = 0;
    char buf[4096];
    int len = readlink(path, buf, 4096);
    if (len == -1) {
    	cerrno = errno;
    } else {
    	buf[len] = 0;
    	link = buf;
    }
	call_setresult(link, cerrno);
}

void call_strtol(void (*call_setresult)(long result, int errno), char *s, int base) {
    long rc = strtol(s, NULL, base);
	call_setresult(rc, errno);
}
