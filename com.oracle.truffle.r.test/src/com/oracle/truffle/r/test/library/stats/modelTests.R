# Copyright (c) 2016, 2017 Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
# or visit www.oracle.com if you need additional information or have any
# questions.

# These tests invoke external C functions and compare their results to
# the corresponding FastR functions implemented in R.
#
# This file is intended to be run only in GnuR, because running this in FastR
# would mean that we would be comparing the results of the same functions.
#
# N.B.: results of these tests are more precise than the Java unit tests, because
# we use 'identical' that checks types and attributes recursively, whereas just
# comparing the output may not discover some differences.
#
# IMPORTANT: some test cases from this file are used in Java unit tests in TestFormulae
# class, please update them accordingly when updating this file.

gsubVec <- function(pattern, replace, text) {
    for (i in seq_along(pattern)) {
        text <- gsub(pattern[[i]], replace[[i]], text);
    }
    text
}

saved <- list()
saveArgs <- function(...) {
    saved <<- list(...)
}

replaceExternalWithSaveArgs <- function(func, externalType='.External2') {
    body <- deparse(func)
    idx <- which(grepl(externalType, body))
    body[[idx]] <- gsubVec(c('C_[^,]*,', externalType), c("", "saveArgs"), body[[idx]])
    eval(parse(text=body))
}

# stubs that save original arguments to .External calls so that we can call them with these args by hand
# note: there is no logic in terms.formula executed before the .External call
saveArgs.model.frame.default <- replaceExternalWithSaveArgs(model.frame.default)
saveArgs.model.matrix.default <- replaceExternalWithSaveArgs(model.matrix.default)

# check function compares the results
failedTests <- 0
successTests <- 0
check <- function(expected, actual, name) {
    if (try(identical(expected, actual)) != TRUE) {
        failedTests <<- failedTests + 1
        cat(name, ": FAIL  expected:\n\n")
        print(expected)
        cat("\n>>>>>>>>>actual:\n\n")
        print(actual)
        cat("\n-------------\n")
        if (failedTests > 10) {
            stop("Too many failed tests...")
        }
    } else {
        successTests <<- successTests + 1
        cat(".")
    }
}

# tests data: formulae
tests <- c(y~z, y~1+z, y~0+z, y~-1+z, y~z*k, y~z*k+w*m, u~z*k+w*m, y~z:k)
tests <- c(tests, y~z^2, y~(z+k)^2, y~z*((m+w)^3), y~(z+k)*(w+u))
tests <- c(tests, y~w%in%v, y~w/k, y~(1 + w/k), ~k+y+z)
ignoremm <- c(y~log(z), y~z+I(k+4), y~z+I(k^2), y~z+offset(log(z)))
ignoremf <- NULL
tests <- c(tests, ignoremm)

run.tests <- function() {
    for (t in tests) {
        print(t)
        check(.External(stats:::C_termsform, t, NULL, NULL, FALSE, FALSE), termsform(t, NULL, NULL, FALSE, FALSE), "termsform")

        # modelframe
        if (!(c(t) %in% ignoremf)) {
            saveArgs.model.frame.default(t)
            their <- do.call(.External2, c(list(stats:::C_modelframe), saved))
            ours <- do.call(modelframe, saved)
            check(their, ours, "model.frame.default")
        } else {
            next
        }

        # modelmatrix
        if (!(c(t) %in% ignoremm)) {
            mf <- model.frame.default(t)
            saveArgs.model.matrix.default(mf)
            their <- do.call(.External2, c(list(stats:::C_modelmatrix), saved))
            ours <- do.call(modelmatrix, saved)
            mode(ours) <- "double" # GnuR has always double results, even when not necessary
            check(their, ours, "model.matrix.default")
        }
    }
}

# test data: variables for the tests:
idx <- 1
for (var in c("y", "z", "k", "w", "m", "u", "v")) {
    assign(var, idx:(idx+10))
    idx <- idx+1
}

run.tests()

cat("now some variables will be factors\n")
ignoremf <- ignoremm # log(x) and I(x+4) etc is not supported with factors in R
k <- factor(c(rep(c("m", "f"), 5), "f"))
z <- factor(c(rep(c("a", "b", "c"), 3), "c", "c"))
run.tests()

# check subsetting
print(y~z)
saveArgs.model.frame.default(y~z, subset=3:7)
their <- do.call(.External2, c(list(stats:::C_modelframe), saved))
ours <- do.call(modelframe, saved)
check(their, ours, "model.frame.default with subset")

# check specials
t <- y~myfun(z)+x
print(t)
check(.External(stats:::C_termsform, t, c('myfun'), NULL, FALSE, FALSE), termsform(t, c('myfun'), NULL, FALSE, FALSE), "termsform with specials")

# check expand dots
t <- cyl~hp*mpg+.
print(t)
check(.External(stats:::C_termsform, t, NULL, mtcars, FALSE, FALSE), termsform(t, NULL, mtcars, FALSE, FALSE), "termsform with expandDots")

# check specials and expand dots
t <- cyl~mufun(mpg)+.
print(t)
check(.External(stats:::C_termsform, t, c('myfun'), mtcars, FALSE, FALSE), termsform(t, c('myfun'), mtcars, FALSE, FALSE), "termsform with specials and expandDots")


# ------------------------------------
# tests for update formula

body <- deparse(update.formula)
idx <- which(grepl(".Call", body))
body[[idx]] <- gsub("C_updateform,", "", gsub(".Call", "updateform", body[[idx]]))
updateformula <- eval(parse(text=body))

test.update.formula <- function(old, new) {
    print(old);
    print(new);
    check(update.formula(old, new), updateformula(old, new), "update.formula test")
}

test.update.formula(y ~ x, ~ . + x2)
test.update.formula(y ~ x, log(.) ~ . )
test.update.formula(. ~ u+v, res  ~ . )
test.update.formula(~ u+v, res  ~ . )
test.update.formula(~ u+v, ~ . )
test.update.formula(~ u+v, . ~ . )
test.update.formula(~ u+v, ~ x*. )
test.update.formula(~ u+v, ~ x:. )


cat("\n\nFinished\nsuccessful:", successTests, "\nfailed:", failedTests)

