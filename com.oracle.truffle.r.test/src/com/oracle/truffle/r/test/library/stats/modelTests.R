# Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

# create patched versions of R stubs for externals:
# we replace call to .External with call to our implementation
body <- deparse(model.frame.default)
idx <- which(grepl(".External2", body))
body[[idx]] <- gsub("C_modelframe,", "", gsub(".External2", "modelframe", body[[idx]]))
modelframedefault <- eval(parse(text=body))

body <- deparse(model.matrix.default)
idx <- which(grepl(".External2", body))
body[[idx]] <- gsub("C_modelmatrix,", "", gsub(".External2", "modelmatrix", body[[idx]]))
modelmatrixdefault <- eval(parse(text=body))

# check function compares the results
check <- function(expected, actual, name) {
	if (try(identical(expected, actual)) != TRUE) {
		cat(name, ": FAIL  expected:\n\n")
		print(expected)
		cat("\n>>>>>>>>>actual:\n\n")
		print(actual)
		cat("\n-------------\n")
	}
	else { cat(name, ": OK\n") }
}

# tests data: formulae
tests <- c(y~z, y~1+z, y~0+z, y~-1+z, y~z*k, y~z*k+w*m, u~z*k+w*m, y~z:k)
tests <- c(tests, y~z^2, y~(z+k)^2, y~z*((m+w)^3), y~(z+k)*(w+u))
tests <- c(tests, y~w%in%v, y~w/k, y~(1 + w/k))
ignoremm <- c(y~log(z), y~z+I(k+4), y~z+I(k^2), y~z+offset(log(z)))
ignoremf <- NULL
tests <- c(tests, ignoremm)

run.tests <- function() {
    for (t in tests) {
    	print(t)
    	check(terms.formula(t), termsform(t, NULL, NULL, FALSE, FALSE), "termsform")
    	
    	# modelframe
    	if (!(c(t) %in% ignoremf)) {
    	    mf <- model.frame.default(t)
    	    check(mf, modelframedefault(t), "model.frame.default")
    	} else {
    	    next
    	}

    	# modelmatrix
    	if (!(c(t) %in% ignoremm)) {
        	our <- modelmatrixdefault(mf)
        	mode(our) <- "double" # GnuR has always double results, even when not necessary
        	check(model.matrix.default(mf), our, "model.matrix.default")
        	# for one off testing: modelmatrixdefault(model.frame.default(t))
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
mf <- model.frame.default(y~z, subset=3:7)
check(mf, modelframedefault(y~z, subset=3:7), "model.frame.default with subset")

# check specials
t <- y~myfun(z)+x
print(t)
check(terms.formula(t, c('myfun')), termsform(t, c('myfun'), NULL, FALSE, FALSE), "termsform with specials")

# check expand dots
t <- cyl~hp*mpg+.
print(t)
check(terms.formula(t, data=mtcars), termsform(t, NULL, mtcars, FALSE, FALSE), "termsform with expandDots")

# check specials and expand dots
t <- cyl~mufun(mpg)+.
print(t)
check(terms.formula(t, specials=c('myfun'), data=mtcars), termsform(t, c('myfun'), mtcars, FALSE, FALSE), "termsform with specials and expandDots")


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
