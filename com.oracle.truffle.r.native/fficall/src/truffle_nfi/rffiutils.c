/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
#include <trufflenfi.h>

void* unimplemented(char *f) {
	printf("unimplemented %s\n", f);
	exit(1);
}

typedef struct globalRefTable_struct {
    int permanent;
    SEXP gref;         // The jobject (SEXP) global ref
} GlobalRefElem;

#define CACHED_GLOBALREFS_INITIAL_SIZE 64
static GlobalRefElem *cachedGlobalRefs = NULL;
static int cachedGlobalRefsHwm;
static int cachedGlobalRefsLength;

void init_utils() {
	if (cachedGlobalRefs == NULL) {
		cachedGlobalRefs = calloc(CACHED_GLOBALREFS_INITIAL_SIZE, sizeof(GlobalRefElem));
		cachedGlobalRefsLength = CACHED_GLOBALREFS_INITIAL_SIZE;
		cachedGlobalRefsHwm = 0;
	}
}
static SEXP findCachedGlobalRef(SEXP obj) {
    for (int i = 0; i < cachedGlobalRefsHwm; i++) {
        GlobalRefElem elem = cachedGlobalRefs[i];
        if (elem.gref == NULL) {
            continue;
        }
        if (isSameObject(elem.gref, obj)) {
            return elem.gref;
        }
    }
    return NULL;
}

SEXP addGlobalRef(SEXP obj, int permanent) {
    SEXP gref;
    if (cachedGlobalRefsHwm >= cachedGlobalRefsLength) {
        int newLength = cachedGlobalRefsLength * 2;
        SEXP newCachedGlobalRefs = calloc(newLength, sizeof(GlobalRefElem));
        if (newCachedGlobalRefs == NULL) {
            fatalError("FFI global refs table expansion failure");
        }
        memcpy(newCachedGlobalRefs, cachedGlobalRefs, cachedGlobalRefsLength * sizeof(GlobalRefElem));
        free(cachedGlobalRefs);
        cachedGlobalRefs = newCachedGlobalRefs;
        cachedGlobalRefsLength = newLength;
    }
    gref = newObjectRef(obj);
    cachedGlobalRefs[cachedGlobalRefsHwm].gref = gref;
    cachedGlobalRefs[cachedGlobalRefsHwm].permanent = permanent;
    cachedGlobalRefsHwm++;
    return gref;
}

SEXP checkRef(SEXP obj) {
    SEXP gref = findCachedGlobalRef(obj);
    if (gref == NULL) {
        return obj;
    } else {
        return gref;
    }
}

SEXP createGlobalRef(SEXP obj, int permanent) {
    SEXP gref = findCachedGlobalRef(obj);
    if (gref == NULL) {
        gref = addGlobalRef(obj, permanent);
    }
    return gref;
}

void releaseGlobalRef(SEXP obj) {
    for (int i = 0; i < cachedGlobalRefsHwm; i++) {
        GlobalRefElem elem = cachedGlobalRefs[i];
        if (elem.gref == NULL || elem.permanent) {
            continue;
        }
        if (isSameObject(elem.gref, obj)) {
        	releaseObjectRef(elem.gref);
            cachedGlobalRefs[i].gref = NULL;
        }
    }
}
