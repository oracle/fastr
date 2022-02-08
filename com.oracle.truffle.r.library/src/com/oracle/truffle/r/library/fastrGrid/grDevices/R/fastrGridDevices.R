# Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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

if (.fastr.option("UseInternalGridGraphics")) {
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
	        	if (displaylist == "enable") {
		        	.Call(grid:::C_initDisplayList)
	        	}
	    	}
		    else stop("argument is missing with no default")
	    	invisible()
		}
	
		# The grid display list is extracted using private grid C functions. 
	    recordPlot <- function() {
			dl <- grid:::grid.Call(grid:::C_getDisplayList)
			dl.idx <- grid:::grid.Call(grid:::C_getDLindex)
			if (all(sapply(dl[-1], function (x) is.null(x)))) {
				pl <- list(NULL)
			} else {
				# The dummy elements and the class 'recordedplot' make the display list look 
				# like the GNUR one, which enables its use in the 'evaluate' package
				# (used in knitr, for instance).
				pl <- list(list(list("dummyCallX",list(list("dummyCallY")))), dl = dl, dl.idx = dl.idx)
			}
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
} else { # 'UseInternalGridGraphics' is false
	eval(expression({

		awt <- function(width = NULL, height = NULL, graphicsObj = NULL) {
			# We have to call grDevices:::C_X11, because we have to transitively call `gdOpen`.
			invisible(.External2(grDevices:::C_X11, ".FASTR.AWT", width, height))
			# We do not have a way how to pass `graphicsObj` to `grDevices:::C_X11`, so we have to
			# pass it to another fastr-specific builtin.
			.Internal(.fastr.awtSetGraphics(graphicsObj))
		}

		svg <- function (filename = if (onefile) "Rplots.svg" else "Rplot%03d.svg", width = 7, height = 7, pointsize = 12, onefile = FALSE, family = "sans", bg = "white", antialias = c("default", "none", "gray", "subpixel")) {
			invisible(.External2(C_X11, paste0("svg::onefile=", onefile, ",family=", family, ",bg=", bg, ",antialias=", antialias, ":", filename), 72 * width, 72 * height, pointsize))
		}

		# Allows to get the SVG code from SVG device, it also closes the device,
		# but the contents are not saved to the given file.
		svg.off <- function(which = dev.cur()) {
			if (which == 1) {
				stop("cannot shut down device 1 (the null device)")
			}
			if (which != dev.cur()) {
				stop("svg.off for a different device than dev.cur() is not supported yet")
			}
			svg_contents <- .Internal(.fastr.svg.get.content())
			svg_fname <- .Internal(.fastr.svg.filename())
			# We explicitly close the device here, as it would be difficult to close it in the
			# FastR builtin function.
			dev.off(which)
			# dev.off creates the SVG file, as a workaround, we delete the file.
			# Note that in current gnur's graphics engine (4.0.3), we cannot easily kill the
			# device without invoking any shutdown hooks. Therefore, we do this workaround.
			stopifnot(file.exists(svg_fname))
			unlink(svg_fname)
			return(svg_contents)
		}

		gdlog <- function (logfilename, width, height, pointsize) .External2(C_X11, paste0("log::", logfilename), width, height, pointsize)
		
		gdOpen <- function (name, w, h, compare = FALSE) if (!compare) .Call(C_api_gdOpen, name, w, h) else gdOpenCmpArg(name, w, h)
		gdClose <- function (compare = FALSE) if (!compare) .Call(C_api_gdClose)
		gdActivate <- function (compare = FALSE) if (!compare) .Call(C_api_gdActivate)
		gdDeactivate <- function (compare = FALSE) if (!compare) .Call(C_api_gdDeactivate)
		gdPolygon <- function (n, x, y, compare = FALSE) if (!compare) .Call(C_api_gdPolygon, as.integer(n), as.double(x), as.double(y)) else gdPolygonCmpArg(n, x, y)
		gdRaster <- function(img, img_w, img_h, x, y, w, h, rot, interpolate, compare = FALSE) if (!compare) .Call(C_api_gdRaster, as.integer(img), as.integer(img_w), as.integer(img_h), as.double(x), as.double(y), as.double(w), as.double(h), as.double(rot), as.integer(interpolate)) else gdRasterCmpArg(img, img_w, img_h, x, y, w, h, rot, interpolate)
		gdcSetColor <- function(cc, compare = FALSE) if (!compare) .Call(C_api_gdcSetColor, as.integer(cc)) else gdcSetColorCmpArg(cc)
		gdcSetFill <- function(cc, compare = FALSE) if (!compare) .Call(C_api_gdcSetFill, as.integer(cc)) else gdcSetFillCmpArg(cc)
		gdcSetLine <- function(lwd, lty, compare = FALSE) if (!compare) .Call(C_api_gdcSetLine, as.double(lwd), as.integer(lty)) else gdcSetLineCmpArg(lwd, lty)
		gdcSetFont <- function(cex, ps, lineheight, fontface, fontfamily, compare = FALSE) if (!compare) .Call(C_api_gdcSetFont, as.double(cex), as.double(ps), as.double(lineheight), as.integer(fontface), as.character(fontfamily)) else gdcSetFontCmpArg(cex, ps, lineheight, fontface, fontfamily)
		gdNewPage <- function(deviceNumber, pageNumber = -1L, compare = FALSE) if (!compare) .Call(C_api_gdNewPage, as.integer(deviceNumber), as.integer(pageNumber))
		gdCircle <- function(x, y, r, compare = FALSE) if (!compare) .Call(C_api_gdCircle, as.double(x), as.double(y), as.double(r)) else gdCircleCmpArg(x, y, r)
		gdClip <- function(x0, x1, y0, y1, compare = FALSE) if (!compare) .Call(C_api_gdClip, as.double(x0), as.double(x1), as.double(y0), as.double(y1)) else gdClipCmpArg(x0, x1, y0, y1)
		gdHold <- function(compare = FALSE) if (!compare) .Call(C_api_gdHold)
		gdFlush <- function(flush, compare = FALSE) if (!compare) .Call(C_api_gdFlush, as.integer(flush)) else gdFlushCmpArg(flush)
		gdLocator <- function(compare = FALSE) if (!compare) .Call(C_api_gdLocator)
		gdLine <- function(x1, y1, x2, y2, compare = FALSE) if (!compare) .Call(C_api_gdLine, as.double(x1), as.double(y1), as.double(x2), as.double(y2)) else gdLineCmpArg(x1, y1, x2, y2)
		gdMode <- function(mode, compare = FALSE) if (!compare) .Call(C_api_gdMode, as.integer(mode)) else gdModeCmpArg(mode)
		gdPath <- function(npoly, nper, n, x, y, winding, compare = FALSE) if (!compare) .Call(C_api_gdPath, as.integer(npoly), as.integer(nper), as.integer(n), as.double(x), as.double(y), as.integer(winding)) else gdPathCmpArg(npoly, nper, n, x, y, winding)
		gdPolyline <- function(n, x, y, compare = FALSE) if (!compare) .Call(C_api_gdPolyline, as.integer(n), as.double(x), as.double(y)) else gdPolylineCmpArg(n, x, y)
		gdRect <- function(x0, y0, x1, y1, compare = FALSE) if (!compare) .Call(C_api_gdRect, as.double(x0), as.double(y0), as.double(x1), as.double(y1)) else gdRectCmpArg(x0, y0, x1, y1)
		gdSize <- function(compare = FALSE) if (!compare) .Call(C_api_gdSize)
		getStrWidth <- function(str, compare = FALSE) if (!compare) .Call(C_api_getStrWidth, as.character(str)) else getStrWidthCmpArg(str)
		gdText <- function(x, y, str, rot, hadj, compare = FALSE) if (!compare) .Call(C_api_gdText, as.double(x), as.double(y), as.character(str), as.double(rot), as.double(hadj)) else gdTextCmpArg(x, y, str, rot, hadj)
		gdMetricInfo <- function(ch, compare = FALSE) if (!compare) .Call(C_api_gdMetricInfo, as.integer(ch)) else gdMetricInfoCmpArg(ch)
		
		assertTrue <- function(x, msg) {
			if (!x) stop(msg)
		}
		assertEquals <- function(arg) {
			assertTrue(arg[[1]] == arg[[2]], paste(deparse(substitute(arg)), "= (", deparse(arg[[1]]), ",", deparse(arg[[2]]), ")"))
		}  
		assertMetric <- function(arg, tolerance = 20) {
			delta = abs(eval(arg[[1]]) - eval(arg[[2]]))
			assertTrue(all(delta < tolerance), paste("delta", deparse(substitute(arg)), "=", delta))
		}
		assertBytes <- function(arg) {
			assertEquals(arg)
		}
		
		gdOpenCmpArg <- function (name, w, h) {
			assertEquals(name)
			assertEquals(w)
			assertEquals(h)
		}
		gdPolygonCmpArg <- function (n, x, y) {
			assertEquals(n)
			assertMetric(x)
			assertMetric(y)
		}
		gdRasterCmpArg <- function(img, img_w, img_h, x, y, w, h, rot, interpolate) {
			assertBytes(img)
			assertEquals(img_w)
			assertEquals(img_w)
			assertMetric(x)
			assertMetric(y)
			assertMetric(w)
			assertMetric(h)
			assertMetric(rot, 2)
		}
		gdcSetColorCmpArg <- function(cc) {
			assertEquals(cc)
		}
		gdcSetFillCmpArg <- function(cc) {
			assertEquals(cc)
		}
		gdcSetLineCmpArg <- function(lwd, lty) {
			assertMetric(lwd)
			assertEquals(lty)
		}
		gdcSetFontCmpArg <- function(cex, ps, lineheight, fontface, fontfamily) {
			assertEquals(cex)
			assertEquals(ps)
			assertEquals(lineheight)
			assertEquals(fontface)
			assertEquals(fontfamily)
		}
		gdCircleCmpArg <- function(x, y, r) {
			assertMetric(x)
			assertMetric(y)
			assertMetric(r)
		}
		gdClipCmpArg <- function(x0, x1, y0, y1) {
			assertMetric(x0)
			assertMetric(x1)
			assertMetric(y0)
			assertMetric(y1)
		}
		gdFlushCmpArg <- function(flush) {
			assertEquals(flush)
		}
		gdLineCmpArg <- function(x1, y1, x2, y2) {
			assertMetric(x1)
			assertMetric(y1)
			assertMetric(x2)
			assertMetric(y2)
		}
		gdModeCmpArg <- function(mode) {
			assertEquals(mode)
		}
		gdPathCmpArg <- function(npoly, nper, n, x, y, winding) {
			assertEquals(npoly)
			assertEquals(nper)
			assertEquals(n)
			assertMetric(x)
			assertMetric(y)
			assertEquals(winding)
		}
		gdPolylineCmpArg <- function(n, x, y) {
			assertEquals(n)
			assertMetric(x)
			assertMetric(y)
		}
		gdRectCmpArg <- function(x0, y0, x1, y1) {
			assertMetric(x0)
			assertMetric(y0)
			assertMetric(x1)
			assertMetric(y1)
		}
		getStrWidthCmpArg <- function(str) {
			assertEquals(str)
		}
		gdTextCmpArg <- function(x, y, str, rot, hadj) {
			assertMetric(x)
			assertMetric(y)
			assertEquals(str)
			assertMetric(rot, 2)
			assertEquals(hadj)
		}
		gdMetricInfoCmpArg <- function(ch) {
			assertEquals(ch)
		}

		compareGDLogs <- function (gdLogPath1, gdLogPath2, debug = FALSE) {
			gdLog1 <- parse(gdLogPath1)
			gdLog2 <- parse(gdLogPath2)
			stms1 <- gdLog1[[3]][[3]][[3]]
			stms2 <- gdLog2[[3]][[3]][[3]]
		
			len1 <- length(stms1)
			len2 <- length(stms2)
			if (len1 != len2) {
				if (debug) warning(paste("Log lengths differ:", len1, "!=", len2))
				return(FALSE)
			}
			compareStatements <- function (gdLogPath1, gdLogPath2) {
				for (i in seq_along(stms1)) {
					stm1 <- stms1[[i]]
					stm2 <- stms2[[i]]
					if (is.call(stm1)) {
						fn <- stm1[[1]]
						fnName <- deparse(fn)
						if (startsWith(fnName, "grDevices:::")) {
							argList1 <- stm1[-1]
							argList2 <- stm2[-1]
							args1 <- as.list(argList1)
							args2 <- as.list(argList2)
							combinedArgs <- mapply(c, args1, args2, SIMPLIFY=FALSE)
							# append the compare arg
							combinedArgs <- c(combinedArgs, compare = TRUE)
							combinedCall <- append(fn, as.pairlist(combinedArgs))
							combinedCall <- as.call(combinedCall)
							if (debug) print(combinedCall)
							eval(combinedCall)
						}
					}
				}
				TRUE
			}

			tryCatch(compareStatements(gdLogPath1, gdLogPath2), error = function(e) {
				if (debug) print(e)
				FALSE
			})
		}
		

		replayGDLogs <- function(logdir, outputDir = "regenerated", pattern="\\.(svg|png|bmp|jpeg2|jpg)") {
			dir.create(outputDir, showWarnings = FALSE)
			curWD <- getwd();
			cat(paste("Output directory:", outputDir, "\n"))
			for (f in dir(logdir, pattern=pattern, full.names = TRUE, recursive = TRUE)) { 
				cat(paste("Regenerating ", f, "...\n")); 
				source(f);
				setwd(outputDir) 
				tryCatch(replayLog(), finally = setwd(curWD)) 
			}
		}

		savePlot <- function (filename = paste("Rplot", type, sep = "."), type = c("png", "jpeg", "bmp"), device = dev.cur()) {
			if (device != dev.cur()) {
				error("Only dev.cur() device is supported in savePlot")
			}
			.Internal(.fastr.savePlot(filename, type, device))
		}

		# export new public functions
		exports <- asNamespace("grDevices")[[".__NAMESPACE__."]][['exports']]
		assign('svg', 'svg', envir = exports)
		assign('svg.off', 'svg.off', envir = exports)
		assign('gdlog', 'gdlog', envir = exports)
		assign('replayGDLogs', 'replayGDLogs', envir = exports)
		assign('compareGDLogs', 'compareGDLogs', envir = exports)

	}), asNamespace("grDevices"))	
}
