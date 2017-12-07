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

# Emulates some graphics package functions with grid. The ideal implementation
# would emulate the lowest level R functions that call to graphics externals,
# e.g. plot.xy, title, ... So far we only provide higher level "plot" that also
# prints a warning message instructing the user to use grid/lattice/ggplot2 instead

eval(expression({
    graphicsWarning <- function(name) {
        # lookup original function and fetch signature
        fun <- tryCatch(get(name, environment()), error=function(x) NULL)
        if(!is.null(fun)) {
            sig <- formals(fun)
        } else {
            sig <- NULL
        }

        if (.fastr.option('IgnoreGraphicsCalls')) {
            # we evaluate the arguments to simulate the function effects
            replacementFun <- function(...) { if (is.null(sig)) list(...) else get(names(sig)); invisible(NULL); }
        } else {
            replacementFun <- function(...) {
                warning(paste0(name, " not supported.", " Note: FastR does not support graphics package and most of its functions. Please use grid package or grid based packages like lattice instead."))
                NULL
            }
        }

        if(!is.null(sig)) {
            formals(replacementFun) <- sig
        }
        return(replacementFun)
    }

    plot.default <- function (x, y = NULL, type = "p", xlim = NULL, ylim = NULL,
        log = "", main = NULL, sub = NULL, xlab = NULL, ylab = NULL,
        ann = par("ann"), axes = TRUE, frame.plot = axes, panel.first = NULL,
        panel.last = NULL, asp = NA, ...)
    {
        library(grid)
        xlabel <- if (!missing(x)) deparse(substitute(x))
        ylabel <- if (!missing(y)) deparse(substitute(y))
        xy <- xy.coords(x, y, xlabel, ylabel, log)
        xlab <- if (is.null(xlab)) xy$xlab else xlab
        ylab <- if (is.null(ylab)) xy$ylab else ylab
        xlim <- if (is.null(xlim)) range(xy$x[is.finite(xy$x)]) else xlim
        ylim <- if (is.null(ylim)) range(xy$y[is.finite(xy$y)]) else ylim
        grid.newpage()
        dev.hold()
        on.exit(dev.flush())

        xlim <- range(xy$x[is.finite(xy$x)])
        ylim <- range(xy$y[is.finite(xy$y)])
        xfactor <- 1 / (xlim[[2]] - xlim[[1]])
        yfactor <- 1 / (ylim[[2]] - ylim[[1]])

        pushViewport(viewport(width=.7, height=.7))
        grid.points((xy$x - xlim[[1]]) * xfactor, (xy$y - ylim[[1]]) * yfactor)

        pushViewport(viewport(width=1.1, height=1.1))
        grid.rect()
        popViewport()

        pushViewport(viewport(width=1, height=1.1))
        grid.xaxis(seq(0, 1, by=.2), label = round(xlim[[1]] + seq(0, 1, by=.2) / xfactor, 2))
        popViewport()

        pushViewport(viewport(width=1.1, height=1))
        grid.yaxis(seq(0, 1, by=.2), label = round(ylim[[1]] + seq(0, 1, by=.2) / yfactor, 2))
        popViewport()

        popViewport()
        if (!is.null(main)) {
            grid.text(main, 0.5, 0.91, gp=gpar(font=2))
        }

        grid.text("FastR does not support graphics package and most of its functions. \nThe 'plot' function is emulated to a small extent. \nPlease use grid package or grid based packages like lattice or ggplot2 instead.", gp=gpar(fontsize=10))
        graphicsWarning()
        invisible()
    }

    # Note: explicitly supported functions: din
    # Note: harmless functions that we do not override: co.intervals, hist.default
    # Note: S3 dispatch functions that may dispatch to lattice/ggplot2/etc. implementation: hist, contour, lines, pairs, points, text

    abline <- graphicsWarning("abline");
    arrows <- graphicsWarning("arrows");
    assocplot <- graphicsWarning("assocplot");
    axis <- graphicsWarning("axis");
    Axis <- graphicsWarning("Axis");
    axis.Date <- graphicsWarning("axis.Date");
    axis.POSIXct <- graphicsWarning("axis.POSIXct");
    axTicks <- graphicsWarning("axTicks");
    barplot.default <- graphicsWarning("barplot.default");
    box <- graphicsWarning("box");
    boxplot.default <- graphicsWarning("boxplot.default");
    boxplot.matrix <- graphicsWarning("boxplot.matrix");
    bxp <- graphicsWarning("bxp");
    cdplot <- graphicsWarning("cdplot");
    clip <- graphicsWarning("clip");
    close.screen <- graphicsWarning("close.screen");
    contour.default <- graphicsWarning("contour.default");
    coplot <- graphicsWarning("coplot");
    curve <- graphicsWarning("curve");
    dotchart <- graphicsWarning("dotchart");
    erase.screen <- graphicsWarning("erase.screen");
    filled.contour <- graphicsWarning("filled.contour");
    fourfoldplot <- graphicsWarning("fourfoldplot");
    frame <- graphicsWarning("frame");
    grconvertX <- graphicsWarning("grconvertX");
    grconvertY <- graphicsWarning("grconvertY");
    grid <- graphicsWarning("grid");
    identify <- graphicsWarning("identify");
    image <- graphicsWarning("image");
    image.default <- graphicsWarning("image.default");
    layout <- graphicsWarning("layout");
    layout.show <- graphicsWarning("layout.show");
    lcm <- graphicsWarning("lcm");
    legend <- graphicsWarning("legend");
    lines.default <- graphicsWarning("lines.default");
    locator <- graphicsWarning("locator");
    matlines <- graphicsWarning("matlines");
    matplot <- graphicsWarning("matplot");
    matpoints <- graphicsWarning("matpoints");
    mosaicplot <- graphicsWarning("mosaicplot");
    mtext <- graphicsWarning("mtext");
    pairs.default <- graphicsWarning("pairs.default");
    panel.smooth <- graphicsWarning("panel.smooth");
    persp <- graphicsWarning("persp");
    pie <- graphicsWarning("pie");
    plot.design <- graphicsWarning("plot.design");
    plot.function <- graphicsWarning("plot.function");
    plot.new <- graphicsWarning("plot.new");
    plot.window <- graphicsWarning("plot.window");
    plot.xy <- graphicsWarning("plot.xy");
    points.default <- graphicsWarning("points.default");
    polygon <- graphicsWarning("polygon");
    polypath <- graphicsWarning("polypath");
    rasterImage <- graphicsWarning("rasterImage");
    rect <- graphicsWarning("rect");
    rug <- graphicsWarning("rug");
    screen <- graphicsWarning("screen");
    segments <- graphicsWarning("segments");
    smoothScatter <- graphicsWarning("smoothScatter");
    spineplot <- graphicsWarning("spineplot");
    split.screen <- graphicsWarning("split.screen");
    stars <- graphicsWarning("stars");
    stem <- graphicsWarning("stem");
    strheight <- graphicsWarning("strheight");
    stripchart <- graphicsWarning("stripchart");
    strwidth <- graphicsWarning("strwidth");
    sunflowerplot <- graphicsWarning("sunflowerplot");
    symbols <- graphicsWarning("symbols");
    text.default <- graphicsWarning("text.default");
    title <- graphicsWarning("title");
    xinch <- graphicsWarning("xinch");
    xspline <- graphicsWarning("xspline");
    xyinch <- graphicsWarning("xyinch");
    yinch <- graphicsWarning("yinch");
}), asNamespace("graphics"))