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

import static com.oracle.truffle.r.library.fastrGrid.device.DrawingContext.INCH_TO_POINTS_FACTOR;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.HeadlessException;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.function.Supplier;

import javax.swing.JFrame;
import javax.swing.JPanel;

import com.oracle.truffle.r.library.fastrGrid.device.DrawingContext.GridFontStyle;
import com.oracle.truffle.r.library.fastrGrid.device.DrawingContext.GridLineEnd;
import com.oracle.truffle.r.library.fastrGrid.device.DrawingContext.GridLineJoin;
import com.oracle.truffle.r.runtime.RInternalError;

public class JFrameDevice implements GridDevice {
    // Grid's coordinate system has origin in left bottom corner and y axis grows from bottom to
    // top. Moreover, the grid system uses inches as units. We use transformation to adjust the java
    // coordinate system to the grid one. However, in the case of text rendering, we cannot simply
    // turn upside down the y-axis, because the text would be upside down too, so for text rendering
    // only, we reset the transformation completely and transform the coordinates ourselves
    private static final double POINTS_IN_INCH = 72.;

    private static BasicStroke solidStroke;
    private static BasicStroke blankStroke;

    private FastRFrame currentFrame;
    private Graphics2D graphics;

    @Override
    public void openNewPage() {
        initStrokes();
        if (currentFrame == null) {
            currentFrame = new FastRFrame();
            currentFrame.setVisible(true);
            initGraphics(currentFrame.getGraphics());
        } else {
            noTranform(() -> {
                graphics.clearRect(0, 0, currentFrame.getWidth(), currentFrame.getHeight());
                return null;
            });
        }
    }

    @Override
    public void drawRect(DrawingContext ctx, double leftX, double topY, double width, double height) {
        setContext(ctx);
        drawShape(ctx, new Rectangle2D.Double(leftX, topY, width, height));
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
    public void drawCircle(DrawingContext ctx, double centerX, double centerY, double radius) {
        setContext(ctx);
        drawShape(ctx, new Ellipse2D.Double(centerX - radius, centerY - radius, radius * 2d, radius * 2d));
    }

    @Override
    public void drawString(DrawingContext ctx, double leftX, double bottomY, double rotationAnticlockWise, String text) {
        setContext(ctx);
        noTranform(() -> {
            AffineTransform tr = graphics.getTransform();
            tr.translate((float) (leftX * POINTS_IN_INCH), (float) (currentFrame.getContentPane().getHeight() - bottomY * POINTS_IN_INCH));
            tr.rotate(-rotationAnticlockWise);
            graphics.setTransform(tr);
            setFont(ctx);
            graphics.drawString(text, 0, 0);
            return null;
        });
    }

    @Override
    public double getWidth() {
        return currentFrame.getContentPane().getWidth() / POINTS_IN_INCH;
    }

    @Override
    public double getHeight() {
        return currentFrame.getContentPane().getHeight() / POINTS_IN_INCH;
    }

    @Override
    public double getStringWidth(DrawingContext ctx, String text) {
        setContext(ctx);
        return noTranform(() -> {
            setFont(ctx);
            int swingUnits = graphics.getFontMetrics(graphics.getFont()).stringWidth(text);
            return swingUnits / POINTS_IN_INCH;
        });
    }

    @Override
    public double getStringHeight(DrawingContext ctx, String text) {
        setContext(ctx);
        return noTranform(() -> {
            setFont(ctx);
            int swingUnits = graphics.getFont().getSize();
            return swingUnits / POINTS_IN_INCH;
        });
    }

    FastRFrame getCurrentFrame() {
        return currentFrame;
    }

    void initGraphics(Graphics newGraphics) {
        if (graphics != null) {
            graphics.dispose();
        }
        graphics = (Graphics2D) newGraphics;
        graphics.translate(0, currentFrame.getHeight());
        graphics.scale(POINTS_IN_INCH, -POINTS_IN_INCH);
        graphics.setStroke(new BasicStroke((float) (1d / POINTS_IN_INCH)));
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    private void drawShape(DrawingContext drawingCtx, Shape shape) {
        Paint paint = graphics.getPaint();
        graphics.setPaint(fromGridColor(drawingCtx.getFillColor()));
        graphics.fill(shape);
        graphics.setPaint(paint);
        graphics.draw(shape);
    }

    private void setContext(DrawingContext ctx) {
        graphics.setColor(fromGridColor(ctx.getColor()));
        graphics.setStroke(getStrokeFromCtx(ctx));
    }

    private void setFont(DrawingContext ctx) {
        float fontSize = (float) ((ctx.getFontSize() / INCH_TO_POINTS_FACTOR) * POINTS_IN_INCH);
        Font font = new Font(getFontName(ctx.getFontFamily()), getAwtFontStyle(ctx.getFontStyle()), 1).deriveFont(fontSize);
        graphics.setFont(font);
    }

    private String getFontName(String gridFontFamily) {
        if (gridFontFamily == null) {
            return null;
        }
        switch (gridFontFamily) {
            case DrawingContext.FONT_FAMILY_MONO:
                return Font.MONOSPACED;
            case DrawingContext.FONT_FAMILY_SANS:
                return Font.SANS_SERIF;
            case DrawingContext.FONT_FAMILY_SERIF:
                return Font.SERIF;
            case "":
                return null;
        }
        return gridFontFamily;
    }

    private int getAwtFontStyle(GridFontStyle fontStyle) {
        switch (fontStyle) {
            case PLAIN:
                return Font.PLAIN;
            case BOLD:
                return Font.BOLD;
            case ITALIC:
                return Font.ITALIC;
            case BOLDITALIC:
                return Font.BOLD | Font.ITALIC;
            default:
                throw RInternalError.shouldNotReachHere("unexpected value of GridFontStyle enum");
        }
    }

    private <T> T noTranform(Supplier<T> action) {
        AffineTransform transform = graphics.getTransform();
        graphics.setTransform(new AffineTransform());
        T result = action.get();
        graphics.setTransform(transform);
        return result;
    }

    private static Color fromGridColor(GridColor color) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    }

    private static BasicStroke getStrokeFromCtx(DrawingContext ctx) {
        byte[] type = ctx.getLineType();
        double width = ctx.getLineWidth();
        int lineJoin = fromGridLineJoin(ctx.getLineJoin());
        float lineMitre = (float) ctx.getLineMitre();
        int endCap = fromGridLineEnd(ctx.getLineEnd());
        if (type == DrawingContext.GRID_LINE_BLANK) {
            return blankStroke;
        } else if (type == DrawingContext.GRID_LINE_SOLID) {
            if (width == 1. && solidStroke.getLineJoin() == lineJoin && solidStroke.getMiterLimit() == lineMitre && solidStroke.getEndCap() == endCap) {
                return solidStroke;
            }
            return new BasicStroke((float) (width / POINTS_IN_INCH), endCap, lineJoin, lineMitre);
        }
        float[] pattern = new float[type.length];
        for (int i = 0; i < pattern.length; i++) {
            pattern[i] = (float) (type[i] / POINTS_IN_INCH);
        }
        return new BasicStroke((float) (width / POINTS_IN_INCH), endCap, lineJoin, lineMitre, pattern, 0f);
    }

    private static int fromGridLineEnd(GridLineEnd lineEnd) {
        switch (lineEnd) {
            case ROUND:
                return BasicStroke.CAP_ROUND;
            case BUTT:
                return BasicStroke.CAP_BUTT;
            case SQUARE:
                return BasicStroke.CAP_SQUARE;
            default:
                throw RInternalError.shouldNotReachHere("unexpected value of GridLineEnd enum");
        }
    }

    private static int fromGridLineJoin(GridLineJoin lineJoin) {
        switch (lineJoin) {
            case BEVEL:
                return BasicStroke.JOIN_BEVEL;
            case MITRE:
                return BasicStroke.JOIN_MITER;
            case ROUND:
                return BasicStroke.JOIN_ROUND;
            default:
                throw RInternalError.shouldNotReachHere("unexpected value of GridLineJoin enum");
        }
    }

    private static void initStrokes() {
        if (solidStroke != null) {
            return;
        }
        solidStroke = new BasicStroke((float) (1f / POINTS_IN_INCH));
        blankStroke = new BasicStroke(0f);
    }

    static class FastRFrame extends JFrame {
        private static final long serialVersionUID = 1L;
        private final Dimension framePreferredSize = new Dimension(720, 720);
        private final JPanel fastRComponent = new JPanel();

        FastRFrame() throws HeadlessException {
            super("FastR");
            addCloseListener();
            createUI();
            center();
        }

        private void createUI() {
            setLayout(new BorderLayout());
            setSize(framePreferredSize);
            add(fastRComponent, BorderLayout.CENTER);
            fastRComponent.setPreferredSize(getSize());
        }

        private void addCloseListener() {
            addWindowFocusListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    dispose();
                }
            });
        }

        private void center() {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            Dimension frameSize = getSize();
            int x = (screenSize.width - frameSize.width) / 2;
            int y = (screenSize.height - frameSize.height) / 2;
            setLocation(x, y);
        }
    }
}
