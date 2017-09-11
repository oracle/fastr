##
 # Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
##

refcmpEnv <- new.env(parent = emptyenv())
refcmpEnv$snapshot_dir <- 'snapshots'
refcmpEnv$snapshot_id <- 0L

snapshot.isFastR <- function() {
    length(grep('FastR', R.Version()$version.string)) != 0
}
refcmpEnv$isReferenceRun <- function() !snapshot.isFastR()

snapshot.init <- function(dir) {
    if(!missing(dir)) {
        refcmpEnv$snaphost_dir <- dir
    } 
    refcmpEnv$snapshot_id <- 0
}

snapshot <- function(...) {
    vars <- list(...)
    try({
        snapshot_id(refcmpEnv$snapshot_id, vars)
        refcmpEnv$snapshot_id <- refcmpEnv$snapshot_id + 1
    })
}

snapshot_id <- function(id, vars) {
    if(refcmpEnv$isReferenceRun()) {
        snapshot.record(id, vars)
    } else {
        snapshot.check(id, vars)
    }
}

snapshot.record <- function(id, vars) {
    dumpDir <- file.path(refcmpEnv$snapshot_dir)
    if(!dir.exists(dumpDir)) {
        dir.create(dumpDir)
    }
    fcon <- file(file.path(refcmpEnv$snapshot_dir, paste0("snapshot", id, ".obj")))
    saveRDS(vars, file=fcon)
    close(fcon)
}

snapshot.check <- function(id, vars) {
    fcon <- file(file.path(refcmpEnv$snapshot_dir, paste0("snapshot", id, ".obj")))
    restoredVars <- readRDS(file=fcon)
    close(fcon)

    if(length(restoredVars) < length(vars)) {
        stop(paste("recorded snapshot has", length(restoredVars), "recorded variables but expected", length(vars)))
    }

    var_names <- names(vars)
    restored_names <- names(restoredVars)
    for(i in seq_along(var_names)) {
        if(var_names[[i]] %in% restored_names) {
            actualVal <- vars[[var_names[[i]]]]
            expectedVal <- restoredVars[[var_names[[i]]]]
            if(actualVal != expectedVal) {
                stop(paste0("Value of variable '", var_names[[i]], "' differs. Expected ", expectedVal, " but was ", actualVal))
            }
        } else {
            stop(paste0("Missing variable '", var_names[[i]], "' in recorded variables"))
        }

    }
}

snapshot.show <- function(id = refcmpEnv$snapshot_id) {
    fcon <- file(file.path(refcmpEnv$snapshot_dir, paste0("snapshot", id, ".obj")))
    restoredVars <- readRDS(file=fcon)
    close(fcon)

    restoredVars
}

