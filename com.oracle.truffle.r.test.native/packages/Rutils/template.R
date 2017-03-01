#
# Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
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
#

expandTemplate <- function(template, ...) {
	resultLength <- 1
	params <- list(...)
	for (i in seq_along(params)) {
		resultLength <- resultLength * length(params[[i]])
	}
	index <- 1
	result <- character(resultLength)
	positions <- rep(1, length(params))
	while (index <= resultLength) {
		currentString <- template
		for (i in seq_along(params)) {
			currentPos <- positions[[i]]
			currentString <- sub(paste0("%", i - 1), params[[i]][[currentPos]], currentString)
		}
		result[[index]] <- currentString
		index <- index + 1

		for (i in seq_along(params)) {
			positions[[i]] <- positions[[i]] + 1
			if (positions[[i]] >= length(params[[i]])) {
				positions[[i]] <- 1
			} else {
				break
			}
		}
	}
	result
}

initialTest <- function(libname, template, ...) {
	tests <- expandTemplate(template, ...)
	Rfile <- sub("\\.Rin$", ".R", commandArgs(T))
	sink(Rfile)
	requireLibrary(libname)
	outputTests(tests)
}

extraTest <- function(template, ...) {
	tests <- expandTemplate(template, ...)
	outputTests(tests)
}

outputTests <- function(tests) {
	# errors must not cause halt, so wrap in try
	for (i in seq_along(tests)) {
		cat(paste0("try(", tests[[i]], ")\n"))
	}
}

requireLibrary <- function(libname) {
	cat(paste0("stopifnot(require(", libname, "))\n"))
}
