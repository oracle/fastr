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
    graphicsWarning <- function(...) {
        warning("FastR does not support graphics package and most of its functions. Please use grid package or grid based packages like lattice or ggplot2 instead.")
        NULL
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

    abline <- graphicsWarning;
    arrows <- graphicsWarning;
    assocplot <- graphicsWarning;
    axis <- graphicsWarning;
    Axis <- graphicsWarning;
    axis.Date <- graphicsWarning;
    axis.POSIXct <- graphicsWarning;
    axTicks <- graphicsWarning;
    barplot.default <- graphicsWarning;
    box <- graphicsWarning;
    boxplot.default <- graphicsWarning;
    boxplot.matrix <- graphicsWarning;
    bxp <- graphicsWarning;
    cdplot <- graphicsWarning;
    clip <- graphicsWarning;
    close.screen <- graphicsWarning;
    co.intervals <- graphicsWarning;
    contour <- graphicsWarning;
    contour.default <- graphicsWarning;
    coplot <- graphicsWarning;
    curve <- graphicsWarning;
    dotchart <- graphicsWarning;
    erase.screen <- graphicsWarning;
    filled.contour <- graphicsWarning;
    fourfoldplot <- graphicsWarning;
    frame <- graphicsWarning;
    grconvertX <- graphicsWarning;
    grconvertY <- graphicsWarning;
    grid <- graphicsWarning;
    hist <- graphicsWarning;
    hist.default <- graphicsWarning;
    identify <- graphicsWarning;
    image <- graphicsWarning;
    image.default <- graphicsWarning;
    layout <- graphicsWarning;
    layout.show <- graphicsWarning;
    lcm <- graphicsWarning;
    legend <- graphicsWarning;
    lines <- graphicsWarning;
    lines.default <- graphicsWarning;
    locator <- graphicsWarning;
    matlines <- graphicsWarning;
    matplot <- graphicsWarning;
    matpoints <- graphicsWarning;
    mosaicplot <- graphicsWarning;
    mtext <- graphicsWarning;
    pairs <- graphicsWarning;
    pairs.default <- graphicsWarning;
    panel.smooth <- graphicsWarning;
    persp <- graphicsWarning;
    pie <- graphicsWarning;
    plot.design <- graphicsWarning;
    plot.function <- graphicsWarning;
    plot.new <- graphicsWarning;
    plot.window <- graphicsWarning;
    plot.xy <- graphicsWarning;
    points <- graphicsWarning;
    points.default <- graphicsWarning;
    polygon <- graphicsWarning;
    polypath <- graphicsWarning;
    rasterImage <- graphicsWarning;
    rect <- graphicsWarning;
    rug <- graphicsWarning;
    screen <- graphicsWarning;
    segments <- graphicsWarning;
    smoothScatter <- graphicsWarning;
    spineplot <- graphicsWarning;
    split.screen <- graphicsWarning;
    stars <- graphicsWarning;
    stem <- graphicsWarning;
    strheight <- graphicsWarning;
    stripchart <- graphicsWarning;
    strwidth <- graphicsWarning;
    sunflowerplot <- graphicsWarning;
    symbols <- graphicsWarning;
    text <- graphicsWarning;
    text.default <- graphicsWarning;
    title <- graphicsWarning;
    xinch <- graphicsWarning;
    xspline <- graphicsWarning;
    xyinch <- graphicsWarning;
    yinch <- graphicsWarning;
}), asNamespace("graphics"))