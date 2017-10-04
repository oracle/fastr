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
refcmpEnv$equals <- all.equal

snapshot.isFastR <- function() {
    length(grep('FastR', R.Version()$version.string)) != 0
}

#' Initialize package
#'
#' @param dir The directory where to store snapshot to (default: 'snapshots')
#' @param is_reference_run A function returning TRUE if snapshots should be taken and FALSE if values should be compared (default: snapshot dir does not exist)
#' @param equalityFunction The function to use for comparing actual values to snapshotted values (default: 'all.equal')
#' @examples
#' # Only comparing using snapshots in directory "my/snapshot/dir" and using function 'identical' to compare values.
#' snapshot.init(dir = "my/snapshot/dir", referenceRunPredicate = function() FALSE, equalityFunction = identical)
#' 
#' # This should do the job in most cases
#' snapshot.init()
#' @export
snapshot.init <- function (dir, is_reference_run, equalityFunction) {
    if (!missing(dir)) {
        refcmpEnv$snaphost_dir <- dir
    }
    if (!missing(is_reference_run)) {
        refcmpEnv$is_reference_run <- is_reference_run
    } else {
        refcmpEnv$is_reference_run <- !file.exists(refcmpEnv$snapshot_dir)
    }
    if (!missing(equalityFunction)) {
        refcmpEnv$equals <- equalityFunction
    }
    refcmpEnv$snapshot_id <- 0L
}

#' Take a snapshot of some variable's values or compare the values to a previously taken snapshot.
#' 
#' This function has two modes, (1) record, and (2) check.
#' Depending on the result of function 'refcmpEnv$is_reference_run', the mode is (1) if TRUE is returned or (2) otherwise.
#' 
#' When in the first mode 'record', function 'snapshot' serializes the values of the specified variables into a file together with the names of the variables. It also increases the ID of the snapshot automatically.
#' 
#' When in the second mode 'check', function 'snapshot' deserializes values of a previously taken snapshot and compares the values of the provided variables to the deserialized values by matching names.
#'
#' @examples
#' x <- 10
#' y <- 20
#' z <- function() print("hello")
#' 
#' snapshot(x, y, z)
#' @export
snapshot <- function(...) {
    # the actual parameter expessions
    actParExprs <- as.list(match.call()[-1])
    valueList <- actParsToList(actParExprs, parent.frame())
    try({
        snapshot.id(refcmpEnv$snapshot_id, valueList)
        refcmpEnv$snapshot_id <- refcmpEnv$snapshot_id + 1
    })
}

#' Take a snapshot of provided values and name the values as specified in the arguments.
#' 
#' This function has two modes, (1) record, and (2) check.
#' Depending on the result of function 'refcmpEnv$is_reference_run', the mode is (1) if TRUE is returned or (2) otherwise.
#' 
#' When in the first mode 'record', function 'snapshot.named' serializes the values of the specified variables into a file together with the names of the variables. It also increases the ID of the snapshot automatically.
#' 
#' When in the second mode 'check', function 'snapshot.named' deserializes values of a previously taken snapshot and compares the values of the provided variables to the deserialized values by matching names.
#' 
#' I contrast to function 'snapshot', this function does not try to automatically determine the name of a value.
#' It uses the names as provided in the arguments.
#'
#' @examples
#' snapshot.named(a = 10 + 20, b = 30, c = function() print("hello"))
#' @export
snapshot.named <- function (...) {
    args <- list(...)
    valueList <- args[names(args) != ""] 
    try({
        snapshot.id(refcmpEnv$snapshot_id, valueList)
        refcmpEnv$snapshot_id <- refcmpEnv$snapshot_id + 1 
    })  
}

snapshot.id <- function(id, valueList) {
    if(refcmpEnv$is_reference_run) {
        snapshot.record(id, valueList)
    } else {
        snapshot.check(id, valueList)
    }
}

snapshot.record <- function(id, valueList) {
    dumpDir <- file.path(refcmpEnv$snapshot_dir)
    if(!dir.exists(dumpDir)) {
        dir.create(dumpDir)
    }
    fcon <- file(file.path(refcmpEnv$snapshot_dir, paste0("snapshot", id, ".obj")))
    saveRDS(valueList, file=fcon)
    close(fcon)
}

snapshot.check <- function(id, valueList) {
    fcon <- file(file.path(refcmpEnv$snapshot_dir, paste0("snapshot", id, ".obj")))
    restoredVars <- readRDS(file=fcon)
    close(fcon)

    if(length(restoredVars) < length(valueList)) {
        stop(paste("recorded snapshot has", length(restoredVars), "recorded variables but expected", length(valueList)))
    }

    var_names <- names(valueList)
    restored_names <- names(restoredVars)
    for(i in seq_along(var_names)) {
        if(var_names[[i]] %in% restored_names) {
            actualVal <- valueList[[var_names[[i]]]]
            expectedVal <- restoredVars[[var_names[[i]]]]
            equalsResult <- refcmpEnv$equals(expectedVal, actualVal)
            if(!is.logical(equalsResult) || !equalsResult) {
                stop(paste0("Value of variable '", var_names[[i]], "' differs. Expected ", expectedVal, " but was ", actualVal))
            }
        } else {
            stop(paste0("Missing variable '", var_names[[i]], "' in recorded variables"))
        }
    }
}

#' Shows the contents of a snapshot.
#' 
#' @param id The ID of the snapshot to show (default: latest)
#' 
#' @examples
#' a <- 1
#' snapshot(a)
#' snapshot.show()
#' 
#' a <- 2
#' snapshot(a)
#' 
#' a <- 3
#' snapshot(a)
#' 
#' a <- 4
#' snapshot(a)
#' snapshot.show(0)
#' snapshot.show(1)
#' snapshot.show(2)
#' snapshot.show(3)
#' @export
snapshot.show <- function(id = refcmpEnv$snapshot_id) {
    snapshot_filename <- file.path(refcmpEnv$snapshot_dir, paste0("snapshot", id, ".obj"))
    if(!file.exists(snapshot_filename)) {
        stop(paste0("Snapshot with ID=", id, " does not exist"))
    }
    fcon <- file(snapshot_filename)
    restoredVars <- readRDS(file=fcon)
    close(fcon)

    return (restoredVars)
}

actParsToList <- function(pars, env) { 
    l <- list() 
    for(i in seq_along(pars)) { 
        strrep <- as.character(pars[[i]])
        if (is.symbol(pars[[i]])) {
            value <- eval(pars[[i]], envir=env)
            l[[strrep]] <- value
        } else {
            warning(paste0("Skipping '", strrep, "' because only symbols are allowed"))
        }
    }
    l
}


