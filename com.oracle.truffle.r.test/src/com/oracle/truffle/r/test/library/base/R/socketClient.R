#
# Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

# Client side of a simple socket test program
# Usage:
# > source("socketClient.R")
# > run(verbose, runs)

writeLength <- function(con, len) {
	writeBin(len, con, endian="big")
}

getSessionData <- function(size) {
	r <- raw(size)
	for (i in (0:size-1)) {
		r[i] <- as.raw(i %% 256)
	}
	r
}

writeData <- function(con, data) {
	writeLength(con, length(data))
	writeBin(data, con)
}

readAck <- function(con) {
	readBin(con, what="integer", endian="big")
}

run <- function(verbose = FALSE, runs) {
	con <- socketConnection(port=10000, open="r+b", blocking=TRUE)
	data <- getSessionData(1000)
	totalOps <- 0
	for (i in (1:runs)) {
		writeData(con, data);
		if (verbose) {
			cat("wrote ", 1000, "bytes, totalOps", totalOps, "\n")
		}
		ack<- readAck(con)
		if (ack != totalOps) {
			cat("ACK mismatch, expected", totalOps, "got", ack, "\n")
		}
		if (verbose) {
			print("readAck")
		}
		totalOps <- totalOps + 1
	}
	if (verbose) {
		print("closing")
	}
	close(con)
}
