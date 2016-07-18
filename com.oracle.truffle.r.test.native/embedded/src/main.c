/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
#include <stdlib.h>
#include <stdio.h>
#include <Rembedded.h>
#include <R_ext/RStartup.h>
/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
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

// A simple test program for FastR embedded mode.
// compile with "gcc -I include main.c -ldl

#include <stdio.h>
#include <dlfcn.h>
#include <sys/utsname.h>
#include <string.h>
#define R_INTERFACE_PTRS 1
#include <Rinterface.h>
#include <Rembedded.h>
#include <R_ext/Rdynload.h>


void (*ptr_stdR_CleanUp)(SA_TYPE, int, int);
void (*ptr_stdR_Suicide)(const char *);

void testR_CleanUp(SA_TYPE x, int y, int z) {
	printf("test Cleanup\n");
	(ptr_stdR_CleanUp)(x, y, z);
}

void testR_Suicide(const char *msg) {
	printf("testR_Suicide: %s\n",msg);
	(ptr_stdR_Suicide(msg));
}

int  testR_ReadConsole(const char *prompt, unsigned char *buf, int len, int h) {
	fputs(prompt, stdout);
	fflush(stdout); /* make sure prompt is output */
	if (fgets((char *)buf, len, stdin) == NULL) {
		return 0;
	} else {
	    return 1;
	}
}

void testR_WriteConsole(const char *buf, int len) {
    printf("%s", buf);
    fflush(stdout);
}

int main(int argc, char **argv) {
	char *r_home = getenv("R_HOME");
	if (r_home == NULL) {
		printf("R_HOME must be set\n");
		exit(1);
	}
	Rf_initialize_R(argc, argv);
	structRstart rp;
	Rstart Rp = &rp;
	R_DefParams(Rp);
	Rp->SaveAction = SA_SAVEASK;
	R_SetParams(Rp);
	ptr_stdR_CleanUp = ptr_R_CleanUp;
	ptr_R_CleanUp = &testR_CleanUp;
	ptr_stdR_Suicide = ptr_R_Suicide;
	ptr_R_Suicide = &testR_Suicide;
	ptr_R_ReadConsole = &testR_ReadConsole;
	ptr_R_WriteConsole = &testR_WriteConsole;
	DllInfo *eDllInfo = R_getEmbeddingDllInfo();
	Rf_mainloop();
	Rf_endEmbeddedR(0);
}
