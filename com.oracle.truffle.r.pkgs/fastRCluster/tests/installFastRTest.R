#
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
#

library(fastRCluster)

createDummyGraalVMDistribution <- function() {
  wd <- tempfile('create-graalvm-mock')
  dir.create(wd)
  origWd <- getwd()
  setwd(wd)
  on.exit(setwd(origWd))

  dir.create('graalvm')
  writeLines('so that the directory is not empty', file.path('graalvm', 'dummy.txt'))
  tar('graalvm-mock.tar.gz', files = 'graalvm', compression = 'gzip')
  return(file.path(wd, 'graalvm-mock.tar.gz'))
}

# --------------
# set-up
system2_mock_args <- NULL
system2_mock <- function(...) {
  system2_mock_args <<- list(...)
  0L
}

download.file_mock <- function(url, target) {
  stopifnot(file.copy(createDummyGraalVMDistribution(), target))
}

fastRCluster:::mockServices(parallel::makePSOCKcluster, system2_mock, download.file_mock)

target <- file.path(tempdir(), 'graalvm-test-target')
dir.create(target)

# --------------
# test
installFastR(target)

# --------------
# check

# the archive was "downloaded" and extracted to the desired location
stopifnot(identical('so that the directory is not empty', readLines(file.path(target, 'dummy.txt'))))
stopifnot(identical(c('dummy.txt'), list.files(target)))

# gu was "run" to install FastR
stopifnot(identical(system2_mock_args, list(file.path(target, 'bin', 'gu'), args=c('install', 'R'))))
