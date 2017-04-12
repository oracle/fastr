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

import static com.oracle.truffle.r.library.fastrGrid.device.DrawingContext.GRID_LINE_BLANK;
import static com.oracle.truffle.r.library.fastrGrid.device.DrawingContext.INCH_TO_POINTS_FACTOR;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.Collections;

import com.oracle.truffle.r.library.fastrGrid.device.DrawingContext.GridFontStyle;
import com.oracle.truffle.r.library.fastrGrid.device.DrawingContext.GridLineEnd;
import com.oracle.truffle.r.library.fastrGrid.device.DrawingContext.GridLineJoin;
import com.oracle.truffle.r.runtime.RInternalError;

public class SVGDevice implements GridDevice {
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.000");
    private final StringBuilder data = new StringBuilder(1024);
    private final String filename;
    private final double width;
    private final double height;

    private DrawingContext cachedCtx;

    public SVGDevice(String filename, double width, double height) {
        this.filename = filename;
        this.width = width;
        this.height = height;
    }

    public String getContents() {
        return data.toString();
    }

    @Override
    public void openNewPage() {
        // We stay compatible with GnuR: opening new page wipes out what has been drawn without
        // saving it anywhere.
        data.setLength(0);
        cachedCtx = null;
        append("<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">");
        // we could use real inches, but that makes the output different to GnuR and other formats
        // (jpg, ...), which use conversion 70px ~ 1in
        append("<svg xmlns='http://www.w3.org/2000/svg' xmlns:xlink='http://www.w3.org/1999/xlink' version='1.1' width='%.3fpx' height='%.3fpx' viewBox='0 0 %.3f %.3f'>", width * 70d, height * 70d,
                        width,
                        height);
    }

    @Override
    public void close() throws DeviceCloseException {
        if (cachedCtx != null) {
            // see #appendStyle
            append("</g>");
        }
        append("</svg>");
        try {
            Files.write(Paths.get(filename), Collections.singleton(data.toString()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DeviceCloseException(e);
        }
    }

    @Override
    public void drawRect(DrawingContext ctx, double leftX, double bottomY, double width, double height, double rotationAnticlockWise) {
        appendStyle(ctx);
        append("<rect vector-effect='non-scaling-stroke' x='%.3f' y='%.3f' width='%.3f' height='%.3f'", leftX, transY(bottomY + height), width, height);
        if (rotationAnticlockWise != 0) {
            append("transform='rotate(%.3f %.3f,%.3f)'", toDegrees(rotationAnticlockWise), (leftX + width / 2.), transY(bottomY + height / 2.));
        }
        data.append("/>"); // end of 'rect' tag
    }

    @Override
    public void drawPolyLines(DrawingContext ctx, double[] x, double[] y, int startIndex, int length) {
        drawPoly(ctx, x, y, startIndex, length, "style='fill:transparent'");
    }

    @Override
    public void drawPolygon(DrawingContext ctx, double[] x, double[] y, int startIndex, int length) {
        drawPoly(ctx, x, y, startIndex, length, "");
    }

    @Override
    public void drawCircle(DrawingContext ctx, double centerX, double centerY, double radius) {
        appendStyle(ctx);
        append("<circle vector-effect='non-scaling-stroke' cx='%.3f' cy='%.3f' r='%.3f'/>", centerX, transY(centerY), radius);
    }

    @Override
    public void drawString(DrawingContext ctx, double leftX, double bottomY, double rotationAnticlockWise, String text) {
        appendStyle(ctx);
        append("<text x='%.3f' y='%.3f' textLength='%.3f' lengthAdjust='spacingAndGlyphs' ", leftX, transY(bottomY), getStringWidth(ctx, text));
        // SVG interprets the "fill" as the color of the text
        data.append("style='").append(getStyleColor("fill", ctx.getColor())).append(";stroke:transparent'");
        if (rotationAnticlockWise != 0) {
            append(" transform='rotate(%.3f %.3f,%.3f)'", toDegrees(rotationAnticlockWise), leftX, transY(bottomY));
        }
        data.append(">").append(text).append("</text>");
    }

    @Override
    public double getWidth() {
        return width;
    }

    @Override
    public double getHeight() {
        return height;
    }

    @Override
    public double getStringWidth(DrawingContext ctx, String text) {
        // The architecture of the GridDevice and grid package requires the devices be able to
        // calculate the width of given string, this way one can e.g. create a box around text. SVG
        // supports this by means of "textLength" attribute, which allows us to force text to have
        // specified width. So we approximate the width of given text in the calculation below and
        // then force the text to actually have such width if it ever gets displayed by #drawString.
        double factor = 0.5;    // empirically chosen
        if (ctx.getFontStyle() == GridFontStyle.BOLD || ctx.getFontStyle() == GridFontStyle.BOLDITALIC) {
            factor = 0.62;
        }
        double letterWidth = (ctx.getFontSize() / INCH_TO_POINTS_FACTOR);
        double result = factor * (double) text.length() * letterWidth;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == 'w' || c == 'm') {
                result += letterWidth * 0.2;
            } else if (c == 'z') {
                result += letterWidth * 0.1;
            }
        }
        return result;
    }

    @Override
    public double getStringHeight(DrawingContext ctx, String text) {
        // we need height without ascent/descent of letters that are not in the string, this is
        // empirically tested calculation
        return 0.8 * (ctx.getFontSize() / INCH_TO_POINTS_FACTOR);
    }

    private void drawPoly(DrawingContext ctx, double[] x, double[] y, int startIndex, int length, String attributes) {
        appendStyle(ctx);
        data.append("<polyline vector-effect='non-scaling-stroke' points='");
        for (int i = 0; i < length; i++) {
            data.append(DECIMAL_FORMAT.format(x[i + startIndex]));
            data.append(',');
            data.append(DECIMAL_FORMAT.format(transY(y[i + startIndex])));
            data.append(' ');
        }
        data.append("' ").append(attributes).append(" />");
    }

    private void appendStyle(DrawingContext ctx) {
        if (cachedCtx == null || !DrawingContext.areSame(cachedCtx, ctx)) {
            if (cachedCtx != null) {
                append("</g>"); // close the previous style definition
            }
            append("<g style='");
            appendStyleUncached(ctx);
            append("'>");
        }
        cachedCtx = ctx;
    }

    private void appendStyleUncached(DrawingContext ctx) {
        byte[] lineType = ctx.getLineType();
        if (lineType == GRID_LINE_BLANK) {
            append("stroke:transparent");
        } else {
            append(getStyleColor("stroke", ctx.getColor()));
        }
        data.append(';').append(getStyleColor("fill", ctx.getFillColor()));
        data.append(";stroke-width:").append(ctx.getLineWidth());
        if (lineType != DrawingContext.GRID_LINE_SOLID && lineType != DrawingContext.GRID_LINE_BLANK) {
            data.append(";stroke-dasharray:");
            for (int i = 0; i < lineType.length; i++) {
                data.append(lineType[i]);
                if (i != lineType.length - 1) {
                    data.append(',');
                }
            }
        }
        data.append(";stroke-linejoin:").append(getSVGLineJoin(ctx.getLineJoin()));
        data.append(";stroke-linecap:").append(getSVGLineCap(ctx.getLineEnd()));
        if (ctx.getLineJoin() == GridLineJoin.MITRE) {
            data.append(";stroke-miterlimit:").append(ctx.getLineMitre());
        }
        data.append(";font-size:").append(ctx.getFontSize() / INCH_TO_POINTS_FACTOR).append("px");
        if (!ctx.getFontFamily().isEmpty()) {
            // Font-family strings 'mono', 'sans', and 'serif' are OK for us
            data.append(";font-family:").append(ctx.getFontFamily());
        }
        if (ctx.getFontStyle().isBold()) {
            data.append(";font-weight:bold");
        }
        if (ctx.getFontStyle().isItalic()) {
            data.append(";font-style:italic");
        }
    }

    private static String getSVGLineCap(GridLineEnd lineEnd) {
        switch (lineEnd) {
            case ROUND:
                return "round";
            case BUTT:
                return "butt";
            case SQUARE:
                return "square";
            default:
                throw RInternalError.shouldNotReachHere("Unexpected value of GridLineEnd enum.");
        }
    }

    private static String getSVGLineJoin(GridLineJoin lineJoin) {
        switch (lineJoin) {
            case ROUND:
                return "round";
            case MITRE:
                return "miter";
            case BEVEL:
                return "bevel";
            default:
                throw RInternalError.shouldNotReachHere("Unexpected value of GridLineJoin enum.");
        }
    }

    private static String getStyleColor(String prefix, GridColor color) {
        return String.format("%s:rgb(%d,%d,%d);%s-opacity:%.3f", prefix, color.getRed(), color.getGreen(), color.getBlue(), prefix, (double) color.getAlpha() / 255d);
    }

    private void append(String fmt, Object... args) {
        data.append(String.format(fmt + "\n", args));
    }

    private double transY(double y) {
        return (height - y);
    }

    private static double toDegrees(double rotationAnticlockWise) {
        return (180. / Math.PI) * -rotationAnticlockWise;
    }
}
