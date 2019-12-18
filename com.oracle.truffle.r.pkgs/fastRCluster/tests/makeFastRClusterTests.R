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

# --------------
# set-up
makePSOCKCluster_mock_args <- NULL
makePSOCKCluster_mock <- function(...) {
  makePSOCKCluster_mock_args <<- list(...)
  res <- 42L
  class(res) <- 'myClass'
  res
}
fastRCluster:::mockServices(makePSOCKCluster_mock, validateGraalVMInstallation = F)
checkArgs <- function(...) stopifnot(identical(makePSOCKCluster_mock_args, list(...)))

# --------------
# tests

cl <- makeFastRCluster()
stopifnot(cl == 42L)
stopifnot(identical(c('fastRCluster', 'myClass'), class(cl)))
checkArgs('localhost', rscript=file.path(getGraalVMHome(), 'bin', 'Rscript'), rscript_args = c('--jvm'))


cl <- makeFastRCluster(3L, graalVMHome='mygvm', polyglot = T, fastROptions = c('--vm.Xmx1g'))
stopifnot(cl == 42L)
stopifnot(identical(c('fastRCluster', 'myClass'), class(cl)))
checkArgs(c('localhost', 'localhost', 'localhost'), rscript=file.path('mygvm', 'bin', 'Rscript'), rscript_args = c('--jvm', '--polyglot', '--vm.Xmx1g'))
