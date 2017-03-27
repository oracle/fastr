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
package com.oracle.truffle.r.library.fastrGrid.device.awt;

import static java.awt.image.BufferedImage.TYPE_INT_RGB;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import com.oracle.truffle.r.library.fastrGrid.device.DrawingContext;
import com.oracle.truffle.r.library.fastrGrid.device.GridDevice;
import com.oracle.truffle.r.library.fastrGrid.device.ImageSaver;

/**
 * Decorator for {@link JFrameDevice} that implements {@link #hold()} and {@link #flush()}
 * functionality and implements the {@link ImageSaver} device.
 *
 * Methods {@link #hold()} and {@link #flush()} open/draw a 2D graphics buffer, while the buffer is
 * open, any drawing is done in the buffer not on the screen and the buffer is dumped to the screen
 * once {@link #flush()} is called.
 *
 * We also record any drawing code to be able to replay it if the buffer happens to loose its
 * contents, which is a possibility mentioned in the documentation. Moreover, this record of drawing
 * can be used in {@link #save(String, String)} to replay the drawing in a {@code BufferedImage}.
 * Note: here we rely on the fact that {@link DrawingContext} is immutable.
 */
public final class BufferedJFrameDevice implements GridDevice, ImageSaver {
    private final JFrameDevice inner;
    private final ArrayList<Runnable> drawActions = new ArrayList<>(200);
    private BufferStrategy buffer;

    public BufferedJFrameDevice(JFrameDevice inner) {
        this.inner = inner;
    }

    @Override
    public void openNewPage() {
        inner.openNewPage();
        drawActions.clear();
        if (buffer != null) {
            // if new page is opened while we are on hold, we should throw away current buffer. In
            // other words that is like starting new hold without previous flush.
            buffer.dispose();
            buffer = null;
            hold();
        }
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
        drawActions.clear();
        setGraphics(buffer.getDrawGraphics());
    }

    @Override
    public void flush() {
        if (buffer == null) {
            return;
        }

        buffer.show();
        // re-draw the buffer if the contents were lost
        while (buffer.contentsLost()) {
            setGraphics(buffer.getDrawGraphics());
            for (Runnable drawAction : drawActions) {
                drawAction.run();
            }
            buffer.show();
        }

        setGraphics(inner.getCurrentFrame().getGraphics());
        buffer.dispose();
        buffer = null;
    }

    @Override
    public void drawRect(DrawingContext ctx, double leftX, double bottomY, double width, double height, double rotationAnticlockWise) {
        inner.drawRect(ctx, leftX, bottomY, width, height, rotationAnticlockWise);
        drawActions.add(() -> inner.drawRect(ctx, leftX, bottomY, width, height, rotationAnticlockWise));
    }

    @Override
    public void drawPolyLines(DrawingContext ctx, double[] x, double[] y, int startIndex, int length) {
        inner.drawPolyLines(ctx, x, y, startIndex, length);
        double[] xCopy = new double[x.length];
        double[] yCopy = new double[y.length];
        System.arraycopy(x, 0, xCopy, 0, x.length);
        System.arraycopy(y, 0, yCopy, 0, y.length);
        drawActions.add(() -> inner.drawPolyLines(ctx, xCopy, yCopy, startIndex, length));
    }

    @Override
    public void drawPolygon(DrawingContext ctx, double[] x, double[] y, int startIndex, int length) {
        inner.drawPolygon(ctx, x, y, startIndex, length);
        double[] xCopy = new double[x.length];
        double[] yCopy = new double[y.length];
        System.arraycopy(x, 0, xCopy, 0, x.length);
        System.arraycopy(y, 0, yCopy, 0, y.length);
        drawActions.add(() -> inner.drawPolygon(ctx, xCopy, yCopy, startIndex, length));
    }

    @Override
    public void drawCircle(DrawingContext ctx, double centerX, double centerY, double radius) {
        inner.drawCircle(ctx, centerX, centerY, radius);
        drawActions.add(() -> inner.drawCircle(ctx, centerX, centerY, radius));
    }

    @Override
    public void drawString(DrawingContext ctx, double leftX, double bottomY, double rotationAnticlockWise, String text) {
        inner.drawString(ctx, leftX, bottomY, rotationAnticlockWise, text);
        drawActions.add(() -> inner.drawString(ctx, leftX, bottomY, rotationAnticlockWise, text));
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

    @Override
    public void save(String path, String fileType) throws IOException {
        int realWidth = (int) (getWidth() * JFrameDevice.AWT_POINTS_IN_INCH);
        int readHeight = (int) (getHeight() * JFrameDevice.AWT_POINTS_IN_INCH);
        BufferedImage image = new BufferedImage(realWidth, readHeight, TYPE_INT_RGB);
        Graphics2D imageGraphics = (Graphics2D) image.getGraphics();
        imageGraphics.setBackground(new Color(255, 255, 255));
        imageGraphics.clearRect(0, 0, realWidth, readHeight);
        setGraphics(imageGraphics);
        for (Runnable drawAction : drawActions) {
            drawAction.run();
        }
        ImageIO.write(image, fileType, new File(path));
        setGraphics(inner.getCurrentFrame().getGraphics());
    }

    private void setGraphics(Graphics graphics) {
        JFrameDevice.defaultInitGraphics((Graphics2D) graphics);
        inner.getGraphics2D().dispose();
        inner.setGraphics2D((Graphics2D) graphics);
    }
}
