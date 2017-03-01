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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.function.Supplier;

import com.oracle.truffle.r.library.graphics.FastRFrame;

public class JFrameDevice implements GridDevice {
    // Grid's coordinate system has origin in left bottom corner and y axis grows from bottom to
    // top. Moreover, the grid system uses inches as units. We use transformation to adjust the java
    // coordinate system to the grid one. However, in the case of text rendering, we cannot simply
    // turn upside down the y-axis, because the text would be upside down too, so for text rendering
    // only, we reset the transformation completely and transform the coordinates ourselves

    private FastRFrame currentFrame;
    private Graphics2D graphics;

    @Override
    public void openNewPage() {
        if (currentFrame == null) {
            currentFrame = new FastRFrame();
            currentFrame.setVisible(true);
            graphics = (Graphics2D) currentFrame.getGraphics();
            graphics.translate(0, currentFrame.getHeight());
            graphics.scale(72, -72); // doc: 72 points ~ 1 inch
            graphics.setStroke(new BasicStroke(1f / 72f));
        } else {
            noTranform(() -> {
                graphics.clearRect(0, 0, currentFrame.getWidth(), currentFrame.getHeight());
                return null;
            });
        }
    }

    @Override
    public void drawRect(DrawingContext ctx, double leftX, double topY, double heigh, double width) {
        setContext(ctx);
        graphics.draw(new Rectangle2D.Double(leftX, topY, heigh, width));
    }

    @Override
    public void drawPolyLines(DrawingContext ctx, double[] x, double[] y, int startIndex, int length) {
        assert startIndex >= 0 && startIndex < x.length && startIndex < y.length : "startIndex out of bounds";
        assert length > 0 && (startIndex + length) <= Math.min(x.length, y.length) : "length out of bounds";
        setContext(ctx);
        Path2D.Double path = new Path2D.Double();
        path.moveTo(x[startIndex], y[startIndex]);
        for (int i = startIndex + 1; i < length; i++) {
            path.lineTo(x[i], y[i]);
        }
        graphics.draw(path);
    }

    @Override
    public void drawString(DrawingContext ctx, double x, double y, double rotation, String text) {
        setContext(ctx);
        noTranform(() -> {
            graphics.rotate(rotation);
            graphics.drawString(text, (float) x * 72f, (float) (currentFrame.getContentPane().getHeight() - y * 72f));
            return null;
        });
    }

    @Override
    public double getWidth() {
        return currentFrame.getContentPane().getWidth() / 72.0;
    }

    @Override
    public double getHeight() {
        return currentFrame.getContentPane().getHeight() / 72.0;
    }

    @Override
    public double getStringWidth(DrawingContext ctx, String text) {
        setContext(ctx);
        return noTranform(() -> {
            int swingUnits = graphics.getFontMetrics(graphics.getFont()).stringWidth(text);
            return swingUnits / 72.;
        });
    }

    @Override
    public double getStringHeight(DrawingContext ctx, String text) {
        setContext(ctx);
        return noTranform(() -> {
            // int swingUnits = graphics.getFontMetrics(graphics.getFont()).getHeight();
            int swingUnits = graphics.getFont().getSize();
            return swingUnits / 72.;
        });
    }

    private void setContext(DrawingContext ctx) {
        graphics.setFont(graphics.getFont().deriveFont((float) ctx.getFontSize()));
        graphics.setColor(Color.decode(ctx.getColor()));
    }

    private <T> T noTranform(Supplier<T> action) {
        AffineTransform transform = graphics.getTransform();
        graphics.setTransform(new AffineTransform());
        T result = action.get();
        graphics.setTransform(transform);
        return result;
    }
}
