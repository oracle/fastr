#
# This material is distributed under the GNU General Public License
# Version 2. You may review the terms of this license at
# http://www.gnu.org/licenses/gpl-2.0.html
#
# Copyright (c) 1995-2014, The R Core Team
# Copyright (c) 2016, Oracle and/or its affiliates
#
# All rights reserved.
#

## Derived from snow and parallel packages

eval(expression(
closeNode.SHAREDnode <- function(node) {
    .fastr.channel.close(node$channel)
}), asNamespace("parallel"))

eval(expression(
sendData.SHAREDnode <- function(node, data) {
    .fastr.channel.send(node$channel, data)
}), asNamespace("parallel"))

eval(expression(
recvData.SHAREDnode <- function(node) {
    .fastr.channel.receive(node$channel)
}), asNamespace("parallel"))

eval(expression(
recvOneData.SHAREDcluster <- function(cl) {
	channel_ids = lapply(cl, function(l) l[["channel"]])
    res <- .fastr.channel.select(channel_ids)
	selected_id = res[[1]]
	# TODO: I am sure there is a better way...
	indexes = lapply(cl, function(l, id) if (identical(l[["channel"]], id)) id else as.integer(NA), id=selected_id)
	node_ind = which(as.double(indexes)==as.double(selected_id))
	list(node = node_ind, value = res[[2]])
}), asNamespace("parallel"))

eval(expression(
fastr.newSHAREDnodes <- function(nnodes, debug, options = defaultClusterOptions)
{
	context_code <- vector("character", nnodes)
	contexts <- vector("integer", nnodes)
	channels <- vector("integer", nnodes)
	for (i in 1:nnodes) {
		# generate unique values for channel keys (addition factor is chosen based on how snow generates port numbers)
		port <- as.integer(parallel:::getClusterOption("port", options) + i * 1000)
		script <- file.path(R.home(), "com.oracle.truffle.r.native", "library", "parallel", "RSHAREDnode.R")

    	context_code[[i]] <- paste0("commandArgs<-function() c('--args', 'PORT=", port, "'); source('", script, "')")
		if (isTRUE(debug)) cat(sprintf("Starting context: %d with code %s\n", i, context_code[[i]]))

		## Need to return a list here, in the same form as the
		## "cluster" data structure.
    	channels[[i]] <- .fastr.channel.create(port)
		if (isTRUE(debug)) cat(sprintf("Context %d started!\n", i))
	}
    contexts <- .fastr.context.spawn(context_code, nnodes)
    cl <- vector("list", nnodes)
	for (i in 1:nnodes) {
		cl[[i]] <- structure(list(channel = channels[[i]], context=contexts[[i]], rank = i), class = "SHAREDnode")
	}
	cl
	
}), asNamespace("parallel"))

makeForkClusterExpr <- expression({
makeForkCluster <- function(nnodes = getOption("mc.cores", 2L), options = defaultClusterOptions, ...)
{
    nnodes <- as.integer(nnodes)
    if(is.na(nnodes) || nnodes < 1L) stop("'nnodes' must be >= 1")
    .check_ncores(nnodes)
	options <- addClusterOptions(options, list(...))

	# Add the "debug" option defaulted to FALSE, if the user didn't specify
	# If the user gives TRUE, print extra stuff during cluster setup
	debug <- FALSE
	tryCatch(
		debug <- parallel:::getClusterOption("debug", options),
		error = function(e) { }
	)
	options <- parallel:::addClusterOptions(options, list(debug = debug))
	
    cl <- fastr.newSHAREDnodes(nnodes, debug = debug, options=options)
	class(cl) <- c("SHAREDcluster", "cluster")
	cl
}; environment(makeForkCluster)<-asNamespace("parallel")})
eval(makeForkClusterExpr, asNamespace("parallel"))
# seems like we don't need these anymore, but let's make sure
#eval(makeForkClusterExpr, as.environment("package:parallel"))


eval(expression(
stopCluster.SHAREDcluster <- function(cl) {
    for (n in cl) {
        parallel:::postNode(n, "DONE")
        .fastr.context.join(n$context)
    }
}), asNamespace("parallel"))
