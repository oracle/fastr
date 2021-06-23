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

#include "charsxps.h"
#include <string.h>
#include <math.h>

static void assert_same_str(const char *actual, const char *expected);
static void charsxp_same_ptrs_test();
static void reorder_via_stringelt_test();
static void reorder_via_dataptr_test();
static void set_via_dataptr_test();
static void get_via_dataptr_test();

/**
* Replaces an n-th string in place from given character vector with `replacement`.
* Is a wrapper for SET_STRING_ELT
* @param n Index of the string to replace.
* @param replacement Replacement for the string.
* @returns New string vector with the replacement.
*/
SEXP charsxp_replace_nth_str(SEXP str, SEXP n, SEXP replacement) {
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
SEXP charsxp_nth_str(SEXP str, SEXP n) {
    int idx = INTEGER_ELT(n, 0);
    return ScalarString(STRING_ELT(str, idx));
}

/**
* Creates a native empty character vector. For the purpose of demonstration that we
* can create a native character vector, and then modify it in R code.
*/
SEXP charsxp_create_empty_str(SEXP n) {
    int n_int = INTEGER_ELT(n, 0);
    SEXP str = PROTECT(allocVector(STRSXP, n_int));
    for (int i = 0; i < n_int; i++) {
        SET_STRING_ELT(str, i, mkChar(""));
    }
    UNPROTECT(1);
    return str;
}

/**
 * Reverts a character vector in place via STRING_ELT API.
 */
SEXP charsxp_revert_via_elt(SEXP str) {
    int len = LENGTH(str);
    int half = (int) ceil(len / 2);
    for (int first_idx = 0; first_idx < half; first_idx++) {
        int second_idx = len - first_idx - 1;
        SEXP first_elem = STRING_ELT(str, first_idx);
        SET_STRING_ELT(str, first_idx, STRING_ELT(str, second_idx));
        SET_STRING_ELT(str, second_idx, first_elem);
    }
    return str;
}

/**
 * Reverts a character vector in place via DATAPTR.
 */
SEXP charsxp_revert_via_dataptr(SEXP str) {
    int len = LENGTH(str);
    int half = (int) ceil(len / 2);
    SEXP *dataptr = (SEXP *) DATAPTR(str);
    for (int first_idx = 0; first_idx < half; first_idx++) {
        int second_idx = len - first_idx - 1;
        SEXP first_elem = dataptr[first_idx];
        dataptr[first_idx] = dataptr[second_idx];
        dataptr[second_idx] = first_elem;
    }
    return str;
}

/**
* Runs all other native tests
*/
SEXP charsxp_tests() {
    charsxp_same_ptrs_test();
    reorder_via_stringelt_test();
    reorder_via_dataptr_test();
    set_via_dataptr_test();
    get_via_dataptr_test();
    return R_NilValue;
}

static void assert_same_str(const char *actual, const char *expected) {
    if (strcmp(actual, expected) != 0) {
        error("Strings are different: actual:'%s', expected:'%s'", actual, expected);
    }
}

/**
 * CHARSXP SEXP types are compared with equality operator.
 */
static void charsxp_same_ptrs_test() {
    SEXP str = PROTECT(allocVector(STRSXP, 1));
    SEXP elem = mkChar("Hello");
    SET_STRING_ELT(str, 0, elem);
    SEXP elem_from_elt = STRING_ELT(str, 0);
    if (elem != elem_from_elt) {
        error("elem != elem_from_elt");
    }
    UNPROTECT(1);
}

/**
 * Reorder the elements of the character vector via STRING_ELT API.
 */
static void reorder_via_stringelt_test() {
    // Reorder the elements of the character vector via STRING_ELT API.
    SEXP str = PROTECT(allocVector(STRSXP, 3));
    // We do not protect CHARSXP elements on purpose.
    SEXP first_elem = mkChar("One");
    SET_STRING_ELT(str, 0, first_elem);
    SEXP second_elem = mkChar("Two");
    SET_STRING_ELT(str, 1, second_elem);
    SEXP third_elem = mkChar("Three");
    SET_STRING_ELT(str, 2, third_elem);

    // Check that the character vector is correctly initialized.
    assert_same_str(CHAR(STRING_ELT(str, 0)), CHAR(first_elem));
    assert_same_str(CHAR(STRING_ELT(str, 1)), CHAR(second_elem));
    assert_same_str(CHAR(STRING_ELT(str, 2)), CHAR(third_elem));

    // Reorder
    SET_STRING_ELT(str, 0, third_elem);
    SET_STRING_ELT(str, 2, first_elem);

    // Check that the character vector was correctly reordered.
    assert_same_str(CHAR(STRING_ELT(str, 0)), CHAR(third_elem));
    assert_same_str(CHAR(STRING_ELT(str, 1)), CHAR(second_elem));
    assert_same_str(CHAR(STRING_ELT(str, 2)), CHAR(first_elem));

    UNPROTECT(1);
}

/**
 * Reorder the elements of the character vector via DATAPTR.
 * Currently, we know only data.table package that does this.
 */
static void reorder_via_dataptr_test() {
    SEXP str = PROTECT(allocVector(STRSXP, 3));
    SEXP first_elem = mkChar("One");
    SET_STRING_ELT(str, 0, first_elem);
    SEXP second_elem = mkChar("Two");
    SET_STRING_ELT(str, 1, second_elem);
    SEXP third_elem = mkChar("Three");
    SET_STRING_ELT(str, 2, third_elem);

    // Check that the character vector is correctly initialized.
    assert_same_str(CHAR(STRING_ELT(str, 0)), "One");
    assert_same_str(CHAR(STRING_ELT(str, 1)), "Two");
    assert_same_str(CHAR(STRING_ELT(str, 2)), "Three");

    // Reorder via DATAPTR.
    SEXP *dataptr = (SEXP *) DATAPTR(str);
    dataptr[0] = third_elem;
    dataptr[2] = first_elem;

    // Check (via STRING_ELT) that the character vector was correctly reordered.
    assert_same_str(CHAR(STRING_ELT(str, 0)), "Three");
    assert_same_str(CHAR(STRING_ELT(str, 1)), "Two");
    assert_same_str(CHAR(STRING_ELT(str, 2)), "One");

    UNPROTECT(1);
}

/**
 * Run with gctorture
 */
static void set_via_dataptr_test() {
    SEXP str = PROTECT(allocVector(STRSXP, 2));
    SEXP *dataptr = (SEXP *) DATAPTR(str);
    // Not protected on purpose.
    dataptr[0] = mkChar("One");
    // `dataptr[0]` must not be collected here, as it is referenced by `str`.
    dataptr[1] = mkChar("Two");
    assert_same_str(CHAR(STRING_ELT(str, 0)), "One");
    assert_same_str(CHAR(STRING_ELT(str, 1)), "Two");
    UNPROTECT(1);
}

static void get_via_dataptr_test() {
    SEXP str = PROTECT(allocVector(STRSXP, 1));
    // Get the dataptr before we set the values.
    SEXP *dataptr = (SEXP *) DATAPTR(str);
    SET_STRING_ELT(str, 0, mkChar("foo"));
    assert_same_str(CHAR(dataptr[0]), "foo");
    UNPROTECT(1);
}

