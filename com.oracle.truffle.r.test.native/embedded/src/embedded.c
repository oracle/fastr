/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

// Simple program testing FastR embedded mode where R is initialized and then evaluation is controlled by the embedder
// See also main.c for example where R is initialized and then the R's REPL is run.

// Note: some of the examples were taken from GNU R tests/Embedded directory and slightly adapted

#include <stdlib.h>
#include <stdio.h>
#include <dlfcn.h>
#include <sys/utsname.h>
#include <string.h>
#define R_INTERFACE_PTRS 1
#include <Rinterface.h>
#include <Rembedded.h>
#include <R_ext/RStartup.h>
#include <R_ext/Rdynload.h>

#define CALLDEF(name, n)  {#name, (DL_FUNC) &name, n}

SEXP twice(SEXP x) {
    int *xi = INTEGER(x);
    int len = LENGTH(x);
    SEXP res;
    PROTECT(res = allocVector(INTSXP, len));
    int *resi = INTEGER(res);
    for (int i = 0; i < len; ++i) {
        resi[i] = xi[i] * 2;
    }
    UNPROTECT(1);
    return res;
}

static void checkError(int errorOccurred, const char* context) {
    if (errorOccurred) {
        printf("Unexpected error occurred when %s.\n", context);
        exit(1);
    }
}

static void source(const char* file) {
    FILE *f;
    if (f = fopen(file, "r")){
        fclose(f);
    } else {
        printf("File '%s' is not accessible. Are you running the program from within a directory that contains this file, e.g. 'obj'?\n", file);
        exit(1);
    }

    SEXP e;
    PROTECT(e = lang2(install("source"), mkString(file)));
    printf("Sourcing '%s'...\n", file);
    int errorOccurred;
    R_tryEval(e, R_GlobalEnv, &errorOccurred);
    UNPROTECT(1);
    checkError(errorOccurred, "sourcing a file");
}

/*
  Call the function foo() with 3 arguments, 2 of which
  are named.
   foo(pch="+", id = 123, c(T,F))

  Note that PrintValue() of the expression seg-faults.
  We have to set the print name correctly.
*/

static void bar1() {
    SEXP fun, pch;
    SEXP e;

    PROTECT(e = allocVector(LANGSXP, 4));
    fun = findFun(install("foo"), R_GlobalEnv);
    if(fun == R_NilValue) {
        printf("No definition for function foo. Source foo.R and save the session.\n");
        UNPROTECT(1);
        exit(1);
    }
    SETCAR(e, fun);

    SETCADR(e, mkString("+"));
    SET_TAG(CDR(e), install("pch"));

    SETCADDR(e, ScalarInteger(123));
    SET_TAG(CDR(CDR(e)), install("id"));

    pch = allocVector(LGLSXP, 2);
    LOGICAL(pch)[0] = TRUE;
    LOGICAL(pch)[1] = FALSE;
    SETCADDDR(e, pch);

    printf("Printing the expression to be eval'ed...\n");
    PrintValue(e);
    printf("Eval'ing the expression...\n");
    eval(e, R_GlobalEnv);

    SETCAR(e, install("foo"));
    printf("Printing the expression to be tryEval'ed...\n");
    PrintValue(e);
    printf("TryEval'ing the expression...\n");
    R_tryEval(e, R_GlobalEnv, NULL);

    UNPROTECT(1);
}

int main(int argc, char **argv) {
    setbuf(stdout, NULL);
    char *r_home = getenv("R_HOME");
    if (r_home == NULL) {
        printf("R_HOME must be set\n");
        exit(1);
    }
    printf("Initializing R with Rf_initEmbeddedR...\n");
    Rf_initEmbeddedR(argc, argv);

    // ------------------------------
    // tests/Embedded/Rerror.c

    /*
      Evaluates the two expressions:
      source("error.R")
      and then calls foo()  twice
      where foo is defined in the file error.R
    */
    SEXP e;
    int errorOccurred;
    source("error.R");

    PROTECT(e = lang1(install("foo")));
    printf("Invoking foo() via tryEval...");
    R_tryEval(e, R_GlobalEnv, &errorOccurred);
    printf("errorOccurred=%d\n", errorOccurred);
    printf("Invoking foo() via tryEval once more...");
    R_tryEval(e, R_GlobalEnv, &errorOccurred);
    printf("errorOccurred=%d\n", errorOccurred);
    UNPROTECT(1);

    // ------------------------------
    // tests/Embedded/tryEval.c

    printf("Trying sqrt with wrong and then correct argument...\n");
    PROTECT(e = lang2(install("sqrt"), mkString("")));
    SEXP val = R_tryEval(e, NULL, &errorOccurred);
    // Note: even the official example is not PROTECTing the val
    if(errorOccurred) {
        printf("Caught an error calling sqrt(). Try again with a different argument.\n");
    }
    SETCAR(CDR(e), ScalarInteger(9));
    val = R_tryEval(e, NULL, &errorOccurred);
    if(errorOccurred) {
        printf("Caught another error calling sqrt()\n");
    } else {
        Rf_PrintValue(val);
    }
    UNPROTECT(1);

    // ------------------------------
    // tests/Embedded/RNamedCall.c

    source("foo.R");
    printf("Calling foo with named arguments...\n");
    bar1();

    // ------------------------------
    // Register custom native symbols and invoke them

    printf("Calling R_getEmbeddingDllInfo...\n");
    DllInfo *eDllInfo = R_getEmbeddingDllInfo();
    R_CallMethodDef CallEntries[] = {
            CALLDEF(twice, 1),
            {NULL, NULL, 0}
    };
    R_registerRoutines(eDllInfo, NULL, CallEntries, NULL, NULL);
    source("embedding.R");
    PROTECT(e = lang1(install("runTwice")));
    SEXP twiceRes = R_tryEval(e, R_GlobalEnv, &errorOccurred);
    checkError(errorOccurred, "evaluating runTwice");
    UNPROTECT(1);
    Rf_PrintValue(twiceRes);


    Rf_endEmbeddedR(0);
    printf("DONE\n");
    return 0;
}

