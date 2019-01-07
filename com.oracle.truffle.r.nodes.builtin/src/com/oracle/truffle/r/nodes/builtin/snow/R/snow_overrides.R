#
# Copyright (c) 1995-2014, The R Core Team
# Copyright (c) 2016, 2018, Oracle and/or its affiliates
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#

## Derived from snow and parallel packages
## Note: the same code is used in forkcluster_overrides.R, reflect any updates in that file
## TODO: get rid of this redundancy

eval(expression({

# overwritten functions:

makeCluster <- function (spec, type = getClusterOption("type"), ...) {
    switch(type,
        SOCK = snow::makeSOCKcluster(spec, ...),
        MPI = snow::makeMPIcluster(spec, ...),
        NWS = snow::makeNWScluster(spec, ...),
        SHARED = makeSHAREDcluster(spec, ...), # this line was added
        stop("unknown cluster type"))
}

# added functions:

closeNode.SHAREDnode <- function(node) {
    .fastr.channel.close(node$channel)
}

sendData.SHAREDnode <- function(node, data) {
    .fastr.channel.send(node$channel, data)
}

recvData.SHAREDnode <- function(node) {
    .fastr.channel.receive(node$channel)
}

recvOneData.SHAREDcluster <- function(cl) {
	channel_ids = lapply(cl, function(l) l[["channel"]])
    res <- .fastr.channel.select(channel_ids)
	selected_id = res[[1]]
	# TODO: I am sure there is a better way...
	indexes = lapply(cl, function(l, id) if (identical(l[["channel"]], id)) id else as.integer(NA), id=selected_id)
	node_ind = which(as.double(indexes)==as.double(selected_id))
	list(node = node_ind, value = res[[2]])
}

newSHAREDnodes <- function(nnodes, debug, options = defaultClusterOptions) {
	context_code <- vector("character", nnodes)
	contexts <- vector("integer", nnodes)
	channels <- vector("integer", nnodes)
	outfile <- getClusterOption("outfile", options)
	
	for (i in 1:nnodes) {
            channel <- .fastr.channel.createForkChannel(snow:::getClusterOption("port", options))

            startup <- substitute(local({
                makeSHAREDmaster <- function(key) {
                    channel <- .fastr.channel.get(as.integer(key))
                    structure(list(channel=channel), class = "SHAREDnode")
                }
                snow:::sinkWorkerOutput(OUTFILE)
                snow:::slaveLoop(makeSHAREDmaster(PORT))
            }), list(OUTFILE=outfile, PORT=channel$port))
		
            context_code[[i]] <- paste0(deparse(startup), collapse="\n")
            if (isTRUE(debug)) cat(sprintf("Starting context: %d with code %s\n", i, context_code[[i]]))

            ## Need to return a list here, in the same form as the
            ## "cluster" data structure.
            channels[[i]] <- channel$channelId
            if (isTRUE(debug)) cat(sprintf("Context %d started!\n", i))
	}
        contexts <- .fastr.context.spawn(context_code)
        cl <- vector("list", nnodes)
	for (i in 1:nnodes) {
		cl[[i]] <- structure(list(channel = channels[[i]], context=contexts[[i]], rank = i), class = "SHAREDnode")
	}
	cl
}

makeSHAREDcluster <- function(nnodes = getOption("mc.cores", 2L), options = defaultClusterOptions, ...) {
    nnodes <- as.integer(nnodes)
    if(is.na(nnodes) || nnodes < 1L) stop("'nnodes' must be >= 1")
	options <- addClusterOptions(options, list(...))

	# Add the "debug" option defaulted to FALSE, if the user didn't specify
	# If the user gives TRUE, print extra stuff during cluster setup
	debug <- FALSE
	if (exists("debug", envir=options, inherits=FALSE)) {
		debug <- snow:::getClusterOption("debug", options)
	} else {
		options <- snow:::addClusterOptions(options, list(debug = debug))
	}
	
    cl <- newSHAREDnodes(nnodes, debug = debug, options=options)
	class(cl) <- c("SHAREDcluster", "cluster")
	cl
}

stopCluster.SHAREDcluster <- function(cl) {
    for (n in cl) {
        snow:::postNode(n, "DONE")
        .fastr.context.join(n$context)
    }
}

## manually register S3 generic methods
registerS3method("closeNode", "SHAREDnode", closeNode.SHAREDnode) 
registerS3method("sendData", "SHAREDnode", sendData.SHAREDnode) 
registerS3method("recvData", "SHAREDnode", recvData.SHAREDnode) 
registerS3method("recvOneData", "SHAREDcluster", recvOneData.SHAREDcluster) 
registerS3method("stopCluster", "SHAREDcluster", stopCluster.SHAREDcluster) 
}), asNamespace("snow"))
