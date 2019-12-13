# Copyright (c) 2019, 2019, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 3 only, as
# published by the Free Software Foundation.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 3 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 3 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
# or visit www.oracle.com if you need additional information or have any
# questions.

# Simple framework for simple "specification" like tests that allows to mix C and R code in one file

state <- list2env(list(cnt = 1L, showCode = interactive()))
default_includes <- paste(
    '#include <stdio.h>',
    '#include <R.h>',
    '#include <Rdefines.h>',
    '#include <Rinterface.h>',
    '#include <Rinternals.h>',
    '#include <Rmath.h>',
    '#include <R_ext/Connections.h>',
    '#include <R_ext/Parse.h>\n\n',
    '#define assert_equal_i(A, B) { int ___a = (A); int ___b = (B); if (___a != ___b) { printf("ASSERTION ERROR: " #A " != " #B ", values: %d != %d\\n", ___a, ___b); } }\n\n',
    sep = '\n')

#' Compiles C code of a function, loads it, and returns an R function that
#' calls that C function via the '.Call' convention.
#' 
#' @param fun The function: formal arguments will be used as formal arguments
#'  for the C function and body must be a single string literal (without curly braces!).
#' @param includes Any additional include directives neccessary for the C code.
#' @return A closure that calls the C function.
#' @examples
#' fun <- load.Call(function(a,b) 'return ScalarInteger(TYPEOF(a) + TYPEOF(b));')
#' fun(1L, 2L) # gives 26
load.Call <- function(fun, includes = character()) {
  if (!all(sapply(formals(fun), is.symbol))) {
    stop("load.Call: default values for 'fun' arguments have no effect")
  }
  if (!is.character(body(fun)[[1L]])) {
    stop("load.Call: 'fun' must have a single statement, which must be a string literal")
  }
  name <- tempfun()
  formals <- paste(paste('SEXP', names(formals(fun))), collapse=', ')
  code <- paste0(
    paste(includes, collapse = '\n'),
    '\n\n',
    'SEXP ', name, '(', formals, ') {\n', body(fun)[[1L]], '\n}')
  load.Code(code)
  closurefor(name, '.Call')
}

#' Compiles C code of a function, loads it, and returns an R function that
#' calls that C function via the '.External' convention.
#' 
#' @param code the code of the C function, signature is always the same: SEXP (SEXP args).
#' @return A closure that calls the C function.
load.External <- function(code, includes = character()) {
  name <- tempfun()
  code <- paste0(
    paste(includes, collapse = '\n'),
    '\n\n',
    'SEXP ', name, '(SEXP args) {\n', code, '\n}')
  load.Code(code)
  closurefor(name, '.External')
}

#' Compiles C code of a function, loads it, and returns an R function that
#' calls that C function via the '.External2' convention.
#' 
#' @param code the code of the C function, signature is always the same: SEXP (SEXP call, SEXP op, SEXP args, SEXP rho).
#' @return A closure that calls the C function.
load.External2 <- function(code, includes = character()) {
  name <- tempfun()
  code <- paste0(
    paste(includes, collapse = '\n'),
    '\n\n',
    'SEXP ', name, '(SEXP call, SEXP op, SEXP args, SEXP rho) {\n', code, '\n}')
  load.Code(code)
  closurefor(name, '.External2')
}

#' Compiles C code of a function, loads it, and returns an R function that
#' calls that C function via the '.C' convention.
#' 
#' @param fun The function: formal arguments will be used as formal arguments
#'  for the C function, their default values must give the desired C type
#'  and body must be a single string literal (without curly braces!).
#' @param includes Any additional include directives neccessary for the C code.
#' @return A closure that calls the C function.
#' @examples
#' fun <- load.C(function(a = 'int*', b = 'double*') 'a[0] = 42; b[0] = 42;')
#' res <- fun(1L, 2)
#' print(res)
load.C <- function(fun, code, includes = character()) {
  if (!all(sapply(formals(fun), is.character))) {
    stop("load.C: all arguments of 'fun' must have default value that is a string that specifies the type")
  }
  if (!is.character(body(fun)[[1L]])) {
    stop("load.Call: 'fun' must have a single statement, which must be a string literal")
  }  
  name <- tempfun()
  # args: for the example in Roxygen: c("int* a", "double* b")
  args <- sapply(1:length(formals(fun)), function(i) paste(formals(fun)[[i]], names(formals(fun)))[[i]])
  formals <- paste(args, collapse = ', ')
  code <- paste0(
    paste(includes, collapse = '\n'),
    '\n\n',
    'void ', name, '(', formals, ') {\n', body(fun)[[1L]], '\n}')
  load.Code(code)
  closurefor(name, '.C')
}

# TODO: R CMD SHLIB is not working with the LLVM Toolchain

#' Allows to load arbitrary C code
load.Code <- function(code, filename = NULL) {
  src <- if (is.null(filename)) tempfile(fileext = '.c') else paste0(filename, '.c')
  cat(default_includes, code, file=src)
  if (state$showCode) {
    cat("\n\n\n----\n", default_includes, code, "\n----\n\n")
  }
  obj <- if (is.null(filename)) tempfile(fileext = '.so') else paste0(filename, '.so')
  default_out <- if (interactive()) "" else NULL
  res <- system2('R', c('CMD', 'SHLIB', '-o', obj, src), stdout = default_out)
  if (res != 0L) {
    stop("Error during the compilation")
  }
  dyn.load(obj)
}

showCode <- function(value = T) {
  state$showCode <- value
  invisible(NULL)
}

tempfun <- function() {
  id <- state$cnt
  state$cnt <- state$cnt + 1L
  name <- paste0('tempfun', id)
}

closurefor <- function(name, convention) {
  res <- function(...) {
    if (convention == '.Call') .Call(name, ...)
    else if (convention == '.C') .C(name, ...)
    else if (convention == '.External') .External(name, ...)
    else if (convention == '.External2') .External2(name, ...)
    else stop('Wrong convention')
  }
  attr(res, 'name') <- name
  res
}