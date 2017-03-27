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

eval(expression({
    # This should be preffered way of starting the FastR java device.
    # For compatibility reasons, both X11 and awt end up calling C_X11.
    # In the future, this function may support extra parameters like a
    # reference to java 2D graphics object, which will be used for the drawing.
    awt <- function(...) {
        .External2(grDevices:::C_X11)
    }
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
}), asNamespace("grDevices"))