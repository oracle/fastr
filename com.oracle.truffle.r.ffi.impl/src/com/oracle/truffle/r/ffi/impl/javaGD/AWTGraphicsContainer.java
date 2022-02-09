/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

import org.rosuda.javaGD.GDContainer;
import org.rosuda.javaGD.GDObject;
import org.rosuda.javaGD.GDState;
import org.rosuda.javaGD.LocatorSync;

import java.awt.*;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Implementation of {@link GDContainer} that wraps {@link Graphics}. Used in our java-interop
 * example ({@code java-interop.R}).
 */
public class AWTGraphicsContainer implements GDContainer {
    private final GDState gdState;
    private final List<GDObject> objects;
    private final Dimension size;
    private Graphics graphics;
    private int deviceNumber;

    public AWTGraphicsContainer(double width, double height) {
        this.gdState = new GDState();
        this.objects = new LinkedList<>();
        this.size = new Dimension((int) width, (int) height);
        this.deviceNumber = -1;
    }

    public void setGraphics(Graphics graphics) {
        assert this.graphics == null : "graphics field should be set just once";
        assert this.objects.size() == 0;
        this.graphics = graphics;
        // clearRect is necessary so that objects created with, e.g., `grid.rect` are visible
        // on the graphics object.
        this.graphics.clearRect(0, 0, size.width, size.height);
    }

    @Override
    public void add(GDObject o) {
        objects.add(o);
        o.paint(null, gdState, graphics);
    }

    @Override
    public Collection<GDObject> getGDObjects() {
        return objects;
    }

    @Override
    public void reset(int pageNumber) {
        objects.clear();
        graphics.clearRect(0, 0, size.width, size.height);
    }

    @Override
    public GDState getGState() {
        return gdState;
    }

    @Override
    public Graphics getGraphics() {
        return graphics;
    }

    @Override
    public boolean prepareLocator(LocatorSync ls) {
        return false;
    }

    @Override
    public void syncDisplay(boolean finish) {
        // nop
    }

    @Override
    public void setDeviceNumber(int dn) {
        this.deviceNumber = dn;
    }

    @Override
    public void closeDisplay() {
        objects.clear();
    }

    @Override
    public int getDeviceNumber() {
        return deviceNumber;
    }

    @Override
    public Dimension getSize() {
        return size;
    }
}
