# Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

# The following test simulates the behavior of some tidyverse packages after updating MRAN snapshot
# to 1.2.2021. Glue package registers an onLoad hook for vctrs package that registers some S3
# methods, so that once vctrs package is loaded, the hook is called.
# In this test, we denote pkgA as glue package and pkgB as vctrs package.
# Inside the function that is called from "library(pkgB)" expression, we want to access the
# namespace of pkgA package via parent.frame().
#
# Note that this test is not in stack-intro-tests.R because I have not found a way how to convert
# it into the shape suitable for stack-intro-tests.R.

saved_hook <- NULL

# This function represents an .onLoad function of some package. In our case glue package.
onLoad <- function() {
    onLoad_loc_var <- 42
    on_pkgB_load({
        # The parent.frame of this promise should be an evaluation environment of the
        # outer function - onLoad.
        # Note that this promise is called once pkgB is loaded.
        after_pkgB_loaded()
    })
}

on_pkgB_load <- function(expr) {
    save_hook(function() expr)
}

# This function emulates "saveHook" function.
save_hook <- function(func) {
    saved_hook <<- func
}

# This function emulates simplified behavior of "library(pkgB)" expression.
load_pkgB <- function() {
    saved_hook()
}

# This function is called after pkgB was loaded, i.e. after "library(pkgB)" expression.
after_pkgB_loaded <- function() {
    # parent.frame() should return an evaluation environment of onLoad function, and the
    # enclosing environment of this evaluation environment is the namespace of current package.
    # This means that in parent.frame() environment, we can search for symbols that are defined
    # inside the namespace in this package - and this is exactly what glue package does.
    parent.frame()
}

onLoad()
pf <- load_pkgB()
# Ensure that parent.frame() in after_pkgB_loaded function indeed returns evaluation environment
# of onLoad function.
"onLoad_loc_var" %in% ls(pf)
