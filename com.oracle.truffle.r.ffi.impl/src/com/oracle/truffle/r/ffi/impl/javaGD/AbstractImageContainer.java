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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.rosuda.javaGD.GDObject;
import org.rosuda.javaGD.GDState;
import org.rosuda.javaGD.LocatorSync;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleFile;

public abstract class AbstractImageContainer extends FileOutputContainer {

    private final List<GDObject> objects = new LinkedList<>();
    protected final GDState gs;
    protected final Dimension size;

    private int deviceNumber;

    protected AbstractImageContainer(int width, int height, String fileNameTemplate) {
        super(fileNameTemplate);
        this.size = new Dimension(width, height);
        this.gs = new GDState();
        this.gs.f = new Font(null, 0, 12);
    }

    @Override
    public void add(GDObject o) {
        objects.add(o);
    }

    @Override
    public Collection<GDObject> getGDObjects() {
        return objects;
    }

    protected abstract void resetGraphics();

    @Override
    public void reset(int pageNumber) {
        super.reset(pageNumber);
        objects.clear();
    }

    @Override
    public GDState getGState() {
        return gs;
    }

    @Override
    public boolean prepareLocator(LocatorSync ls) {
        return false;
    }

    @TruffleBoundary
    public void repaint() {
        Graphics graphics = getGraphics();
        graphics.clearRect(0, 0, size.width, size.height);
        for (GDObject o : objects) {
            o.paint(null, gs, graphics);
        }
    }

    @Override
    public final void saveImage(TruffleFile file) throws IOException {
        repaint();
        dumpImage(file);
    }

    protected abstract void dumpImage(TruffleFile file) throws IOException;

    @Override
    public void syncDisplay(boolean finish) {
    }

    @Override
    public void setDeviceNumber(int dn) {
        this.deviceNumber = dn;
    }

    @Override
    public void closeDisplay() {
        super.closeDisplay();
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

    protected static void defaultInitGraphics(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        graphics.setBackground(Color.WHITE);
    }
}
