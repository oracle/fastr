# Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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

eval(expression({

    # This should be preffered way of starting the FastR java device.
    # For compatibility reasons, both X11 and awt end up calling C_X11.
    awt <- function(width = NULL, height = NULL, graphicsObj = NULL)
        invisible(.External2(grDevices:::C_X11, ".FASTR.AWT", width, height, graphicsObj))

    # Allows to get the SVG code from SVG device, it also closes the device,
    # but the contents are not saved to the given file.
    svg.off <- function(which = dev.cur()) {
        if (which == 1) {
            stop("cannot shut down device 1 (the null device)")
        }
        .External(C_devoff, as.integer(-which))
    }

    # Allows to get the SVG code from SVG device without closing it
    # the symbol info should be constant
    svgStringSymbol <- list(name='svgstring')
    svg.string <- function() .External(svgStringSymbol)

    # GnuR version only works with "X11cairo" device. Our version of savePlot
    # works with "awt" device and "X11cairo", which is for us only alias for
    # "awt". Moreover, we only support formats that awt supports.
    savePlot <- function (filename = paste("Rplot", type, sep = "."), type = c("png", "jpeg", "bmp"), device = dev.cur()) {
        type <- match.arg(type)
        devlist <- dev.list()
        devcur <- match(device, devlist, NA)
        if (is.na(devcur)) {
            stop("no such device")
        }
        devname <- names(devlist)[devcur]
        if (devname != "X11cairo" && devname != "awt") {
            stop("can only copy from 'X11(type=\"*cairo\")' or 'awt' devices")
        }
        invisible(.External2(C_savePlot, filename, type, device))
    }
    
	# The plot recording/replaying in FastR uses the grid display list, instead of the device DL.
    dev.control <- function (displaylist = c("inhibit", "enable")) {
	    if (dev.cur() <= 1) 
    	    stop("dev.control() called without an open graphics device")
	    if (!missing(displaylist)) {
	        displaylist <- match.arg(displaylist)
        	grid::grid.display.list(displaylist == "enable")
    	}
	    else stop("argument is missing with no default")
    	invisible()
	}

	# The grid display list is extracted using private grid C functions. 
    recordPlot <- function() {
		dl <- grid:::grid.Call(grid:::C_getDisplayList)
		dl.idx <- grid:::grid.Call(grid:::C_getDLindex)
		# The dummy elements and the class 'recordedplot' make the display list look 
		# like the GNUR one, which enables its use in the 'evaluate' package
		# (used in knitr, for instance).
		pl <- list(list(list("dummyCallX",list(list("dummyCallY")))), dl = dl, dl.idx = dl.idx)
		class(pl) <- "recordedplot"
		pl
	}

    # When replaying, the argument DL must be one produced by the overridden function recordPlot.
    # The DL is restored using private grid C functions.
	replayPlot <- function(dl) {
		grid:::grid.Call(grid:::C_setDisplayList, dl$dl)
		grid:::grid.Call(grid:::C_setDLindex, dl$dl.idx)
		grid:::grid.refresh()	
	}

}), asNamespace("grDevices"))

# export new public functions
exports <- asNamespace("grDevices")[[".__NAMESPACE__."]][['exports']]
assign('svg.off', 'svg.off', envir = exports)
assign('svg.string', 'svg.string', envir = exports)
assign('awt', 'awt', envir = exports)

# add help files for the new public functions
.fastr.addHelpPath('/com/oracle/truffle/r/library/fastrGrid/grDevices/Rd')