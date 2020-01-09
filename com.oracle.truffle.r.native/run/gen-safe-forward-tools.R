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

# Script used during FastR build, creates safe-forward-xyz for all the tools used in the config files
# 1. argument: target directory where to write the result

args <- commandArgs(trailingOnly=TRUE)
target <- args[[1L]]
file <- 'safe-forward-tool'
template <- readChar(file, file.info(file)$size)

tools <- list(
  Renviron = list(
    c('R_PRINTCMD', 'lpr'),
    c('R_GZIPCMD', 'gzip'),
    c('R_UNZIPCMD', 'unzip'),
    c('R_ZIPCMD', 'zip'),
    c('R_BZIPCMD', 'bzip2'),
    c('R_BROWSER', 'xdg-open'),
    c('R_BROWSER', 'open'),
    c('EDITOR', 'vi'),
    c('PAGER', 'less'),
    c('R_PDFVIEWER', 'xdg-open'),
    c('MAKE', 'make'),
    c('SED', 'sed'),
    c('TAR', 'tar'),
    c('TEXI2DVI', 'texi2dvi')
  ),
  Makeconf = list(
    c('AR', 'ar'),
    c('YACC', 'yacc'),
    c('CC', 'gcc'),
    c('CXX', 'g++')
  )
)

for(config_file_name in names(tools)) {
  for(pair in tools[[config_file_name]]) {
    config_file <- paste0('$R_HOME/etc/', config_file_name)
    var_name <- pair[[1L]]
    tool_name <- pair[[2L]]
    code <- gsub('%%tool_name%%', tool_name, template)
    code <- gsub('%%var_name%%', var_name, code)
    code <- gsub('%%config_file%%', config_file, code)
    script_name <- paste0('safe-forward-', basename(tool_name))
    cat("Generating tool:", script_name, "\n")
    target_file <- file.path(target, script_name)
    cat(code, file = target_file)
    Sys.chmod(target_file, '0755')
  }
}
