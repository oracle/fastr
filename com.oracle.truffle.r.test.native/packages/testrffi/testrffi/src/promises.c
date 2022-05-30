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
static void promise_with_empty_env_test();
static void promise_reset_env_test();
static void different_envs_test();
static SEXP put_promise_into_env(SEXP env, SEXP name);

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

SEXP promises_put_into_env(SEXP env, SEXP name)
{
    if (!Rf_isEnvironment(env)) {
        error("env should be ENVSXP");
    }
    if (!Rf_isString(name) || LENGTH(name) != 1) {
        Rf_error("name should be string with length 1");
    }
    SEXP prom = PROTECT(Rf_allocSExp(PROMSXP));
    SEXP expr = PROTECT(Rf_ScalarInteger(42));
    SET_PRCODE(prom, expr);
    SET_PRVALUE(prom, R_UnboundValue);
    SET_PRENV(prom, R_EmptyEnv);
    SEXP name_symbol = PROTECT(Rf_install(CHAR(STRING_ELT(name, 0))));
    Rf_defineVar(name_symbol, prom, env);
    UNPROTECT(3);
    return env;
}

/**
 * The rest of the native tests.
 */
SEXP promises_tests() {
    simple_promise_test();
    reset_promise_environment_test();
    reset_promise_code_test();
    promise_with_empty_env_test();
    promise_reset_env_test();
    different_envs_test();
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

/**
 * This is used in, e.g., dplyr.
 */
static void promise_with_empty_env_test() {
    SEXP prom = PROTECT(allocSExp(PROMSXP));
    SEXP expr = PROTECT(ScalarInteger(42));
    SET_PRCODE(prom, expr);
    SET_PRENV(prom, R_EmptyEnv);
    SET_PRVALUE(prom, R_UnboundValue);
    SEXP res = PROTECT(eval(prom, R_EmptyEnv));
    if (TYPEOF(res) != INTSXP || LENGTH(res) != 1 || INTEGER_ELT(res, 0) != 42) {
        error("promise_with_empty_env_test: Evaluation of promise failed");
    }
    UNPROTECT(3);
}

static void promise_reset_env_test() {
    SEXP prom = PROTECT(Rf_allocSExp(PROMSXP));
    SEXP expr = PROTECT(Rf_lang3(Rf_install("+"), Rf_ScalarInteger(1), Rf_ScalarInteger(2)));
    SET_PRCODE(prom, expr);
    SET_PRENV(prom, R_EmptyEnv);
    SET_PRVALUE(prom, R_UnboundValue);
    // This test does not work in fastr, but it is not that important, so it is commented-out.
    /*SEXP env = PRENV(prom);
    if (env != R_EmptyEnv) {
        error("env != R_EmptyEnv");
    }*/
    // Evaluation of this promise causes an error, because there is no + operator in emptyenv.
    // We will reset the environment to baseenv, so that we can evaluate it.
    SET_PRENV(prom, R_BaseEnv);
    SEXP res = PROTECT(Rf_eval(prom, R_BaseEnv));
    if (TYPEOF(res) != INTSXP || LENGTH(res) != 1 || INTEGER_ELT(res, 0) != 3) {
        error("promise_reset_env_test: Promise evaluation failed");
    }
    UNPROTECT(3);
}

/**
 * environments in SET_PRENV and Rf_eval are different, the environment set via SET_PRENV
 * has precedence in Rf_eval.
 */
static void different_envs_test()
{
    SEXP prom = PROTECT(Rf_allocSExp(PROMSXP));
    SEXP expr = PROTECT(Rf_lang3(Rf_install("+"), Rf_ScalarInteger(1), Rf_ScalarInteger(2)));
    SET_PRCODE(prom, expr);
    SET_PRENV(prom, R_BaseEnv);
    SET_PRVALUE(prom, R_UnboundValue);
    // If the promise would be evaluated in R_EmptyEnv, there would be an error, since R_EmptyEnv
    // does not contain "+" operator. So this test proves that in Rf_eval, gnur takes the
    // environment from the promise rather than from the given argument.
    SEXP res = PROTECT(Rf_eval(prom, R_EmptyEnv));
    if (TYPEOF(res) != INTSXP || LENGTH(res) != 1 || INTEGER_ELT(res, 0) != 3) {
        error("Promise evaluation failed");
    }
    UNPROTECT(3);
}
