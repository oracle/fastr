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

# Server side of a simple socket test program
# Usage:
# > source("socketServer.R")
# > run(verbose)

readLength <- function(con) {
	readBin(con, what="integer", endian="big")
}

readData <- function(con, n) {
	readBin(con, what="raw", n=n)
}

writeAck <- function(con, n) {
	writeBin(n, con, endian="big")
}

run <- function(verbose=FALSE, oneRun=FALSE) {
	while (TRUE) {
		if (verbose) {
			print("waiting for connection")
		}
		con <- socketConnection(port=10000, server=TRUE, open="r+b", blocking=TRUE)
		totalOps <- 0
		while (TRUE) {
			bytesToRead <- readLength(con)
			if (length(bytesToRead) == 0) {
				print("length(bytesToRead) == 0")
				break
			}
			totalRead <- 0
			while (totalRead < bytesToRead) {
				data <- readData(con, bytesToRead - totalRead)
				nr <- length(data)
				if (nr == 0) {
					print("nr==0")
					break
				}
				totalRead <- totalRead + nr
			}
			if (verbose) {
				cat("read ", totalRead, "bytes\n")
			}
			writeAck(con, as.integer(totalOps))
			if (verbose) {
				print("wrote ack")
			}
			if (totalRead == 0) {
				print("totalRead == 0")
				break
			}
			totalOps <- totalOps + 1
		}
		close(con)
		if (verbose) {
			cat("OPS", totalOps, "\n")
		}
		if (oneRun) break
	}
}
