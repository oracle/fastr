/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

#define PCRE_INFO_CAPTURECOUNT       2
#define PCRE_INFO_NAMEENTRYSIZE      7
#define PCRE_INFO_NAMECOUNT          8
#define PCRE_INFO_NAMETABLE          9

extern char *pcre_maketables();
extern void *pcre_compile(char *pattern, int options, char **errorMessage, int *errOffset, char *tables);
extern int  pcre_exec(void *code, void *extra, char* subject, int subjectLength, int startOffset, int options, int *ovector, int ovecSize);
int pcre_fullinfo(void *code, void *extra, int what, void *where);
extern void pcre_free(void *code);

void call_pcre_compile(void (*makeresult)(long result, char *errMsg, int errOffset), char *pattern, int options, long tables) {
	char *errorMessage;
	int errOffset;
	void *pcre_result = pcre_compile(pattern, options, &errorMessage, &errOffset, (char*) tables);
	void *msg = NULL;
	if (pcre_result == NULL) {
		msg = ensure_string(errorMessage);
	}	makeresult((long) pcre_result, msg, errOffset);
}

int call_pcre_getcapturecount(long code, long extra) {
    int captureCount;
	int rc = pcre_fullinfo((void*) code, (void*) extra, PCRE_INFO_CAPTURECOUNT, &captureCount);
    return rc < 0 ? rc : captureCount;
}

int call_pcre_getcapturenames(void (*setcapturename)(int i, char *name), long code, long extra) {
    int nameCount;
    int nameEntrySize;
    char* nameTable;
    int res;
	res = pcre_fullinfo((void*) code, (void*) extra, PCRE_INFO_NAMECOUNT, &nameCount);
    if (res < 0) {
        return res;
    }
    res = pcre_fullinfo((void*) code, (void*) extra, PCRE_INFO_NAMEENTRYSIZE, &nameEntrySize);
    if (res < 0) {
        return res;
    }
	res = pcre_fullinfo((void*) code, (void*) extra, PCRE_INFO_NAMETABLE, &nameTable);
    if (res < 0) {
        return res;
    }
    // from GNU R's grep.c
	for(int i = 0; i < nameCount; i++) {
	    char* entry = nameTable + nameEntrySize * i;
	    int captureNum = (entry[0] << 8) + entry[1] - 1;
	    setcapturename(captureNum, ensure_string(entry + 2));
    }
    return res;
}
