/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.library.fastrGrid.device;

import java.awt.image.BufferStrategy;
import java.util.ArrayList;

/**
 * Decorator for {@link JFrameDevice} that implements {@link #hold()} and {@link #flush()}. Those
 * methods open/draw a 2D graphics buffer, while the buffer is open, any drawing is done in the
 * buffer not on the screen and we also record any drawing code to be able to replay it if the
 * buffer happens to loose contents, which is a possibility mentioned in the documentation. Note: we
 * rely on the fact that {@link DrawingContext} is immutable.
 */
public class BufferedJFrameDevice implements GridDevice {
    private final JFrameDevice inner;
    private BufferStrategy buffer;
    private ArrayList<Runnable> drawActions;

    public BufferedJFrameDevice(JFrameDevice inner) {
        this.inner = inner;
    }

    @Override
    public void openNewPage() {
        inner.openNewPage();
    }

    @Override
    public void hold() {
        if (buffer != null) {
            return; // already buffering
        }
        buffer = inner.getCurrentFrame().getBufferStrategy();
        if (buffer == null) {
            inner.getCurrentFrame().createBufferStrategy(2);
            buffer = inner.getCurrentFrame().getBufferStrategy();
        }
        if (drawActions == null) {
            drawActions = new ArrayList<>();
        } else {
            drawActions.clear();
        }
        inner.initGraphics(buffer.getDrawGraphics());
    }

    @Override
    public void flush() {
        if (buffer == null) {
            return;
        }

        buffer.show();
        // re-draw the buffer if the contents were lost
        while (buffer.contentsLost()) {
            inner.initGraphics(buffer.getDrawGraphics());
            for (Runnable drawAction : drawActions) {
                drawAction.run();
            }
            buffer.show();
        }

        inner.initGraphics(inner.getCurrentFrame().getGraphics());
        buffer.dispose();
        buffer = null;
    }

    @Override
    public void drawRect(DrawingContext ctx, double leftX, double topY, double width, double height) {
        inner.drawRect(ctx, leftX, topY, width, height);
        if (buffer != null) {
            drawActions.add(() -> inner.drawRect(ctx, leftX, topY, width, height));
        }
    }

    @Override
    public void drawPolyLines(DrawingContext ctx, double[] x, double[] y, int startIndex, int length) {
        inner.drawPolyLines(ctx, x, y, startIndex, length);
        if (buffer != null) {
            drawActions.add(() -> inner.drawPolyLines(ctx, x, y, startIndex, length));
        }
    }

    @Override
    public void drawCircle(DrawingContext ctx, double centerX, double centerY, double radius) {
        inner.drawCircle(ctx, centerX, centerY, radius);
        if (buffer != null) {
            drawActions.add(() -> inner.drawCircle(ctx, centerX, centerY, radius));
        }
    }

    @Override
    public void drawString(DrawingContext ctx, double leftX, double bottomY, double rotationAnticlockWise, String text) {
        inner.drawString(ctx, leftX, bottomY, rotationAnticlockWise, text);
        if (buffer != null) {
            drawActions.add(() -> inner.drawString(ctx, leftX, bottomY, rotationAnticlockWise, text));
        }
    }

    @Override
    public double getWidth() {
        return inner.getWidth();
    }

    @Override
    public double getHeight() {
        return inner.getHeight();
    }

    @Override
    public double getStringWidth(DrawingContext ctx, String text) {
        return inner.getStringWidth(ctx, text);
    }

    @Override
    public double getStringHeight(DrawingContext ctx, String text) {
        return inner.getStringHeight(ctx, text);
    }
}
