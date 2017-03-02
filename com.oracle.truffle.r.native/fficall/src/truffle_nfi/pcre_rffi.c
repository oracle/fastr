/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1997-2015,  The R Core Team
 * Copyright (c) 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
#include <rffiutils.h>

#define PCRE_INFO_CAPTURECOUNT       2
#define PCRE_INFO_NAMEENTRYSIZE      7
#define PCRE_INFO_NAMECOUNT          8
#define PCRE_INFO_NAMETABLE          9

//char *pcre_maketables();
void *pcre_compile(char *pattern, int options, char **errorMessage, int *errOffset, char *tables);
//int  pcre_exec(void *code, void *extra, char* subject, int subjectLength, int startOffset, int options, int *ovector, int ovecSize);
int pcre_fullinfo(void *code, void *extra, int what, void *where);
//void pcre_free(void *code);

void call_pcre_compile(void *closure, char *pattern, int options, long tables) {
	void (*makeresult)(long result, char *errMsg, int errOffset) = closure;
	char *errorMessage;
	int errOffset;
	void *pcre_result = pcre_compile(pattern, options, &errorMessage, &errOffset, (char*) tables);
	makeresult((long) pcre_result, errorMessage, errOffset);
}

int call_pcre_getcapturecount(long code, long extra) {
    int captureCount;
	int rc = pcre_fullinfo((void*) code, (void*) extra, PCRE_INFO_CAPTURECOUNT, &captureCount);
    return rc < 0 ? rc : captureCount;
}

int call_pcre_getcapturenames(void *closure, long code, long extra) {
	void (*setcapturename)(int i, char *name) = closure;
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
	    setcapturename(captureNum, entry + 2);
    }
    return res;
}
