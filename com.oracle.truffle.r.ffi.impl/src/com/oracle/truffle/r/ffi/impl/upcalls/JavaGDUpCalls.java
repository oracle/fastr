/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.ffi.impl.upcalls;

import com.oracle.truffle.r.ffi.processor.RFFICarray;
import com.oracle.truffle.r.ffi.processor.RFFICarray.Type;
import com.oracle.truffle.r.ffi.processor.RFFICstring;
import com.oracle.truffle.r.ffi.processor.RFFIInject;
import com.oracle.truffle.r.runtime.context.RContext;

public interface JavaGDUpCalls {

    boolean gdOpen(int gdId, @RFFICstring String deviceName, double w, double h, @RFFIInject RContext ctx);

    void gdClose(int gdId, @RFFIInject RContext ctx);

    void gdActivate(int gdId, @RFFIInject RContext ctx);

    void gdcSetColor(int gdId, int cc, @RFFIInject RContext ctx);

    void gdcSetFill(int gdId, int cc, @RFFIInject RContext ctx);

    void gdcSetLine(int gdId, double lwd, int lty, @RFFIInject RContext ctx);

    void gdcSetFont(int gdId, double cex, double ps, double lineheight, int fontface, @RFFICstring String fontfamily, @RFFIInject RContext ctx);

    void gdNewPage(int gdId, int devId, int pageNumber, @RFFIInject RContext ctx);

    void gdCircle(int gdId, double x, double y, double r, @RFFIInject RContext ctx);

    void gdClip(int gdId, double x0, double x1, double y0, double y1, @RFFIInject RContext ctx);

    void gdDeactivate(int gdId, @RFFIInject RContext ctx);

    void gdHold(int gdId, @RFFIInject RContext ctx);

    void gdFlush(int gdId, int flush, @RFFIInject RContext ctx);

    @RFFICarray
    Object gdLocator(int gdId, @RFFIInject RContext ctx);

    void gdLine(int gdId, double x1, double y1, double x2, double y2, @RFFIInject RContext ctx);

    void gdMode(int gdId, int mode, @RFFIInject RContext ctx);

    void gdPath(int gdId, int npoly, @RFFICarray(length = "{1}", element = Type.Int) Object nper, int n, @RFFICarray(length = "{3}", element = Type.Double) Object x,
                    @RFFICarray(length = "{3}", element = Type.Double) Object y,
                    int winding, @RFFIInject RContext ctx);

    void gdPolygon(int gdId, int n, @RFFICarray(length = "{1}", element = Type.Double) Object x, @RFFICarray(length = "{1}", element = Type.Double) Object y, @RFFIInject RContext ctx);

    void gdPolyline(int gdId, int n, @RFFICarray(length = "{1}", element = Type.Double) Object x, @RFFICarray(length = "{1}", element = Type.Double) Object y, @RFFIInject RContext ctx);

    void gdRect(int gdId, double x0, double y0, double x1, double y1, @RFFIInject RContext ctx);

    @RFFICarray
    Object gdSize(int gdId, @RFFIInject RContext ctx);

    double getStrWidth(int gdId, @RFFICstring String str, @RFFIInject RContext ctx);

    void gdText(int gdId, double x, double y, @RFFICstring String str, double rot, double hadj, @RFFIInject RContext ctx);

    void gdRaster(int gdId, int imgW, int imgH, @RFFICarray(length = "4 * {1} * {2}", element = Type.Byte) Object img, double x, double y, double w, double h,
                    double rot, int interpolate, @RFFIInject RContext ctx);

    @RFFICarray
    Object gdMetricInfo(int gdId, int ch, @RFFIInject RContext ctx);

}
