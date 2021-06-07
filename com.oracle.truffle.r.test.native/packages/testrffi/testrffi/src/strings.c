/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

#include "strings.h"

/**
* Replaces an n-th string in place from given character vector with `replacement`.
* Is a wrapper for SET_STRING_ELT
* @param n Index of the string to replace.
* @param replacement Replacement for the string.
* @returns New string vector with the replacement.
*/
SEXP replace_nth_str(SEXP str, SEXP n, SEXP replacement) {
    if (TYPEOF(str) != STRSXP || LENGTH(str) == 0) {
        error("`str` expected STRSXP type with length greater than zero");
    }
    if (TYPEOF(n) != INTSXP || LENGTH(n) != 1) {
        error("`n` expected integer of length 1");
    }
    if (TYPEOF(replacement) != STRSXP || LENGTH(replacement) != 1) {
        error("`replacement` expected STRSXP of length 1");
    }
    const char *replacement_char = CHAR(STRING_ELT(replacement, 0));
    int idx = INTEGER_ELT(n, 0);
    if (LENGTH(str) < idx) {
        error("Trying to replace a string outside of bounds");
    }
    for (int i = 0; i < LENGTH(str); i++) {
        if (i == idx) {
            SET_STRING_ELT(str, i, mkChar(replacement_char));
        }
    }
    return str;
}

/**
* A wrapper for STRING_ELT.
*/
SEXP nth_str(SEXP str, SEXP n) {
    int idx = INTEGER_ELT(n, 0);
    return ScalarString(STRING_ELT(str, idx));
}

/**
* Creates a native empty character vector. For the purpose of demonstration that we
* can create a native character vector, and then modify it in R code.
*/
SEXP create_empty_str(SEXP n) {
    int n_int = INTEGER_ELT(n, 0);
    SEXP str = PROTECT(allocVector(STRSXP, n_int));
    for (int i = 0; i < n_int; i++) {
        SET_STRING_ELT(str, i, mkChar(""));
    }
    UNPROTECT(1);
    return str;
}

/**
* Runs all other native tests
*/
SEXP str_tests() {
    SEXP str = PROTECT(allocVector(STRSXP, 1));
    SEXP elem = mkChar("Hello");
    SET_STRING_ELT(str, 0, elem);
    SEXP elem_from_elt = STRING_ELT(str, 0);
    if (elem != elem_from_elt) {
        error("elem != elem_from_elt");
    }
    // TODO: Add some more tests?
    // ...

    UNPROTECT(1);
    return R_NilValue;
}
