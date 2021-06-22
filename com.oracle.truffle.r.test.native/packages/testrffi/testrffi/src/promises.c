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

#include "promises.h"

static void simple_promise_test();
static void reset_promise_environment_test();
static void reset_promise_code_test();

/**
 * Creates a promise from given `expr` and `env`.
 */
SEXP promises_create_promise(SEXP expr, SEXP env) {
    SEXP promise = PROTECT(allocSExp(PROMSXP));
    SET_PRENV(promise, env);
    SET_PRCODE(promise, expr);
    SET_PRVALUE(promise, R_UnboundValue);
    UNPROTECT(1);
    return promise;
}

/**
 * The rest of the native tests.
 */
SEXP promises_tests() {
    simple_promise_test();
    reset_promise_environment_test();
    reset_promise_code_test();
    return R_NilValue;
}

/**
 * Creates a simple promise from native and evaluates it.
 */
static void simple_promise_test() {
    SEXP promise = PROTECT(allocSExp(PROMSXP));
    SEXP expr = ScalarInteger(42);
    SET_PRCODE(promise, expr);
    SET_PRENV(promise, R_GlobalEnv);
    SET_PRVALUE(promise, R_UnboundValue);
    SEXP res = PROTECT(eval(promise, R_GlobalEnv));
    if (TYPEOF(res) != INTSXP || LENGTH(res) != 1 || INTEGER_ELT(res, 0) != 42) {
        error("simple_promise_test: Expected integer result 42L");
    }
    UNPROTECT(2);
}

/**
 * Creates a promise with some initial environment and then changes the environment.
 */
static void reset_promise_environment_test() {
    // env1 and env2 are created in R with:
    // env1 <- new.env()
    // env1$func <- function() 1L
    // env2 <- new.env()
    // env2$func <- function() 2L
    SEXP env1 = PROTECT(eval(lang1(install("new.env")), R_GlobalEnv));
    SEXP env2 = PROTECT(eval(lang1(install("new.env")), R_GlobalEnv));
    SEXP const1 = PROTECT(ScalarInteger(1));
    SEXP const2 = PROTECT(ScalarInteger(2));
    SEXP const_symbol = install("const");
    defineVar(const_symbol, const1, env1);
    defineVar(const_symbol, const2, env2);

    // Create a promise that calls `func()`
    SEXP prom = PROTECT(allocSExp(PROMSXP));
    SET_PRCODE(prom, const_symbol);
    SET_PRENV(prom, env1);
    SET_PRVALUE(prom, R_UnboundValue);
    // Reset its environment to env2
    SET_PRENV(prom, env2);

    // The result should be 2, because the promise should call const2 from env2.
    SEXP res = PROTECT(eval(prom, R_GlobalEnv));
    if (TYPEOF(res) != INTSXP || LENGTH(res) != 1 || INTEGER_ELT(res, 0) != 2) {
        error("reset_promise_environment_test: Evaluation of promise failed");
    }
    UNPROTECT(6);
}

/**
 * Creates a promise with some code and then changes the code before the promise is evaluated.
 */
static void reset_promise_code_test() {
    SEXP prom = PROTECT(allocSExp(PROMSXP));
    SET_PRCODE(prom, install("non_existing_symbol"));
    SET_PRENV(prom, R_GlobalEnv);
    SET_PRVALUE(prom, R_UnboundValue);
    // Reset the code and evaluate it
    SET_PRCODE(prom, ScalarInteger(42));
    SEXP res = PROTECT(eval(prom, R_GlobalEnv));
    if (TYPEOF(res) != INTSXP || LENGTH(res) != 1 || INTEGER_ELT(res, 0) != 42) {
        error("reset_promise_code_test: Evaluation of promise failed");
    }
    UNPROTECT(2);
}

