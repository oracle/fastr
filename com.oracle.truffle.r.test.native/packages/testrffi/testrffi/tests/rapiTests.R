# Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

# Contains unit-tests of the individual R API functions

printCallPreffix <- function() {
    width <- 80L
    name <- substr(deparse(sys.call(-1), width)[[1L]], 1, width)
    cat(name, paste(rep('.', width + 3L - nchar(name)), collapse=''))
}

assertEquals <- function(expected, actual) {
    printCallPreffix()
    cat(if (identical(expected, actual)) 'pass' else 'fail', '\n')
}

assertTrue <- function(value) {
    printCallPreffix()
    cat(if (value) 'pass' else 'fail', '\n')
}

ignore <- function(...) {}

library(testrffi)

# ---------------------------------------------------------------------------------------
# SET_ATTRIB

x <- c(1,3,10)
assertEquals(NULL, api.SET_ATTRIB(x, pairlist(names=c('a','b','q'))))
assertEquals(c('a','b','q'), names(x))

# there is no validation
x <- c(1,3,10)
assertEquals(NULL, api.SET_ATTRIB(x, as.pairlist(list(names=c('a','b')))))
assertEquals(c('a','b'), names(x))
# note: printing x on GNU-R causes segfault

# ---------------------------------------------------------------------------------------
# Rf_mkCharLenCE, note: last arg is encoding and 0 ~ native encoding

assertEquals("hello world", api.Rf_mkCharLenCE("hello world", 11, 0))
ignore("FastR bug", assertEquals("hello", api.Rf_mkCharLenCE("hello this will be cut away", 5, 0)))


# ----------------------------------------------------------------------------------------
# Rf_eval

bar <- function(x) typeof(x)
lang <- quote(bar(x))
lang[[2L]] <- as.pairlist(1)
# lang will be call to "bar" with a pairlist as the argument -- the pairlist is interpreted as the value
assertEquals("pairlist", api.Rf_eval(lang, new.env()))

lang[[2L]] <- quote(1)
# lang will be call to "bar" with "language(1)" as the argument -- the language object is "executed"
assertEquals("double", api.Rf_eval(lang, new.env()))

# ----------------------------------------------------------------------------------------
# Rf_asS4
v <- c(1,2,3)
assertEquals(0L, api.IS_S4_OBJECT(v))
v <- api.Rf_asS4(v, TRUE, 0)
# GnuR returns 16, FastR returns 1
assertTrue(api.IS_S4_OBJECT(v) > 0)