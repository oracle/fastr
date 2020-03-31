/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.ffi.impl.javaGD;

import org.rosuda.javaGD.GDInterface;

public class NullGD extends GDInterface {
    private double width;
    private double height;

    @Override
    public void gdOpen(double w, double h) {
        this.width = w;
        this.height = h;
    }

    @Override
    public void gdActivate() {
    }

    @Override
    public void gdCircle(double x, double y, double r) {
    }

    @Override
    public void gdClip(double x0, double x1, double y0, double y1) {
    }

    @Override
    public void gdClose() {
    }

    @Override
    public void gdDeactivate() {
    }

    @Override
    public void gdHold() {
    }

    @Override
    public void gdFlush(boolean flush) {
    }

    @Override
    public double[] gdLocator() {
        return null;
    }

    @Override
    public void gdLine(double x1, double y1, double x2, double y2) {
    }

    @Override
    public void gdMode(int mode) {
    }

    @Override
    public void gdNewPage() {
    }

    @Override
    public void gdPath(int npoly, int[] nper, double[] x, double[] y, boolean winding) {
    }

    @Override
    public void gdPolygon(int n, double[] x, double[] y) {
    }

    @Override
    public void gdPolyline(int n, double[] x, double[] y) {
    }

    @Override
    public void gdRect(double x0, double y0, double x1, double y1) {
    }

    @Override
    public void gdRaster(byte[] img, int imgW, int imgH, double x, double y, double w, double h, double rot, boolean interpolate) {
    }

    @Override
    public double[] gdSize() {
        double[] res = new double[4];
        res[0] = 0d;
        res[1] = width;
        res[2] = height;
        res[3] = 0;
        return res;
    }

    @Override
    public void gdText(double x, double y, String str, double rot, double hadj) {
    }

    @Override
    public void gdcSetColor(int cc) {
    }

    @Override
    public void gdcSetFill(int cc) {
    }

    @Override
    public void gdcSetLine(double lwd, int lty) {
    }

    @Override
    public void gdcSetFont(double cex, double ps, double lineheight, int fontface, String fontfamily) {
    }

}
