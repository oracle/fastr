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
import static java.lang.Math.round;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.Base64;
import java.util.Collections;

import com.oracle.truffle.r.library.fastrGrid.GridColorUtils;
import com.oracle.truffle.r.library.fastrGrid.device.DrawingContext.GridFontStyle;
import com.oracle.truffle.r.library.fastrGrid.device.DrawingContext.GridLineEnd;
import com.oracle.truffle.r.library.fastrGrid.device.DrawingContext.GridLineJoin;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.Utils;

public class SVGDevice implements GridDevice, FileGridDevice {
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.000");
    private static final double COORD_FACTOR = INCH_TO_POINTS_FACTOR;

    private final StringBuilder data = new StringBuilder(1024);
    private String filename;
    private final double width;
    private final double height;

    private DrawingContext cachedCtx;

    public SVGDevice(String filename, double width, double height) {
        this.filename = filename;
        this.width = width;
        this.height = height;
    }

    public String closeAndGetContents() {
        closeSVGDocument();
        return data.toString();
    }

    @Override
    public void openNewPage() {
        // We stay compatible with GnuR: opening new page wipes out what has been drawn without
        // saving it anywhere.
        data.setLength(0);
        cachedCtx = null;
        data.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        data.append("<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">\n");
        append("<svg xmlns='http://www.w3.org/2000/svg' xmlns:xlink='http://www.w3.org/1999/xlink' version='1.1' viewBox='0 0 %d %d' style='fill:transparent'>\n",
                        trRound(width),
                        trRound(height));
    }

    @Override
    public void openNewPage(String newFilename) throws DeviceCloseException {
        saveFile();
        filename = newFilename;
        openNewPage();
    }

    @Override
    public void close() throws DeviceCloseException {
        saveFile();
    }

    @Override
    public void drawRect(DrawingContext ctx, double leftX, double bottomY, double newWidth, double newHeight, double rotationAnticlockWise) {
        appendStyle(ctx);
        data.append("<rect x='").append(trRound(leftX)).append("' y='").append(trRound(transY(bottomY + newHeight))).append("' width='").append(trRound(newWidth)).append("' height='").append(
                        trRound(newHeight)).append('\'');
        if (rotationAnticlockWise != 0) {
            appendTransform((int) round(toDegrees(rotationAnticlockWise)), trRound(leftX + newWidth / 2.), trRound(transY(bottomY + newHeight / 2.)));
        }
        appendColorStyle(ctx);
        data.append("/>\n"); // end of 'rect' tag
    }

    @Override
    public void drawPolyLines(DrawingContext ctx, double[] x, double[] y, int startIndex, int length) {
        drawPoly(ctx, x, y, startIndex, length, true);
    }

    @Override
    public void drawPolygon(DrawingContext ctx, double[] x, double[] y, int startIndex, int length) {
        drawPoly(ctx, x, y, startIndex, length, false);
    }

    @Override
    public void drawCircle(DrawingContext ctx, double centerX, double centerY, double radius) {
        appendStyle(ctx);
        data.append("<circle cx='").append(trRound(centerX)).append("' cy='").append(trRound(transY(centerY))).append("' r='").append(trRound(radius)).append('\'');
        appendColorStyle(ctx);
        data.append("/>\n");
    }

    @Override
    public void drawRaster(double leftX, double bottomY, double width, double height, int[] pixels, int pixelsColumnsCount, ImageInterpolation interpolation) {
        byte[] bitmap = Bitmap.create(pixels, pixelsColumnsCount);
        String base64 = Base64.getEncoder().encodeToString(bitmap);
        data.append("<image x='").append(round(leftX * COORD_FACTOR)).append("' y='").append(trRound(transY(bottomY + height)));
        data.append("' width='").append(round(width * COORD_FACTOR)).append("' height='").append(trRound(height));
        data.append("' preserveAspectRatio='none' xlink:href='data:image/bmp;base64,").append(base64).append("'/>\n");
    }

    @Override
    public void drawString(DrawingContext ctx, double leftX, double bottomY, double rotationAnticlockWise, String text) {
        closeStyle();
        data.append("<text x='").append(round(leftX * COORD_FACTOR)).append("' y='").append(trRound(transY(bottomY)));
        data.append("' lengthAdjust='spacingAndGlyphs' textLength='").append(round(getStringWidth(ctx, text) * COORD_FACTOR)).append("px'");
        appendFontStyle(ctx);
        if (rotationAnticlockWise != 0) {
            appendTransform((int) round(toDegrees(rotationAnticlockWise)), trRound(leftX), trRound(transY(bottomY)));
        }
        data.append(">").append(text).append("</text>\n");
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
        double factor = 0.6;    // empirically chosen
        if (ctx.getFontStyle() == GridFontStyle.BOLD || ctx.getFontStyle() == GridFontStyle.BOLDITALIC) {
            factor = 0.675;
        }
        double letterWidth = (ctx.getFontSize() / INCH_TO_POINTS_FACTOR);
        double result = text.length() * factor * letterWidth;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == 'w' || c == 'm') {
                result += letterWidth * 0.2;
            } else if (c == 'z' || c == 'v') {
                result += letterWidth * 0.1;
            }
        }
        return result;
    }

    @Override
    public double getStringHeight(DrawingContext ctx, String text) {
        // we need height without ascent/descent of letters that are not in the string, this is
        // empirically tested calculation
        return 0.7 * (ctx.getFontSize() / INCH_TO_POINTS_FACTOR);
    }

    private void drawPoly(DrawingContext ctx, double[] x, double[] y, int startIndex, int length, boolean noFill) {
        appendStyle(ctx);
        data.append("<polyline points='");
        for (int i = 0; i < length; i++) {
            data.append(trRound(x[i + startIndex]));
            data.append(',');
            data.append(trRound(transY(y[i + startIndex])));
            if (i != length - 1) {
                data.append(' ');
            }
        }
        data.append('\'');
        appendColorStyle(ctx, noFill);
        data.append("/>\n");
    }

    private void saveFile() throws DeviceCloseException {
        closeSVGDocument();
        try {
            Files.write(Paths.get(filename), Collections.singleton(data.toString()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DeviceCloseException(e);
        }
    }

    private void closeSVGDocument() {
        if (data.length() == 0) {
            return;
        }
        if (cachedCtx != null) {
            // see #appendStyle
            data.append("</g>");
        }
        data.append("</svg>");
    }

    // closes opened <g> tag if necessary
    private void closeStyle() {
        if (cachedCtx != null) {
            cachedCtx = null;
            data.append("</g>");
        }
    }

    private void appendStyle(DrawingContext ctx) {
        if (cachedCtx == null || !areSameGlobalStyles(cachedCtx, ctx)) {
            if (cachedCtx != null) {
                data.append("</g>"); // close the previous style definition
            }
            data.append("<g");
            appendStyleUncached(ctx);
            data.append('>').append('\n');
        }
        cachedCtx = ctx;
    }

    private void appendStyleUncached(DrawingContext ctx) {
        byte[] lineType = ctx.getLineType();
        data.append(" style='stroke-width:").append(ctx.getLineWidth());
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
        data.append('\'');
    }

    private void appendColorStyle(DrawingContext ctx) {
        appendColorStyle(ctx, false);
    }

    private void appendColorStyle(DrawingContext ctx, boolean noFill) {
        byte[] lineType = ctx.getLineType();
        if (lineType == GRID_LINE_BLANK) {
            data.append(" style='stroke:transparent");
        } else {
            data.append(" style='");
            appendStyleColorAttrs("stroke", ctx.getColor());
        }
        if (!noFill && !ctx.getFillColor().equals(GridColor.TRANSPARENT)) {
            data.append(';');
            appendStyleColorAttrs("fill", ctx.getFillColor());
            data.append('\'');
        }
        data.append('\'');
    }

    private void appendFontStyle(DrawingContext ctx) {
        // Note: SVG interprets the "fill" as the color of the text
        data.append(" style='font-size:").append(ctx.getFontSize()).append("px;");
        appendStyleColorAttrs("fill", ctx.getColor());
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
        data.append('\'');
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

    private void appendStyleColorAttrs(String prefix, GridColor color) {
        data.append(prefix).append(':');
        if (color.getAlpha() == GridColor.OPAQUE_ALPHA) {
            data.append('#');
            data.append(GridColorUtils.getHexDigit(color.getRed() >> 4));
            data.append(GridColorUtils.getHexDigit(color.getRed()));
            data.append(GridColorUtils.getHexDigit(color.getGreen() >> 4));
            data.append(GridColorUtils.getHexDigit(color.getGreen()));
            data.append(GridColorUtils.getHexDigit(color.getBlue() >> 4));
            data.append(GridColorUtils.getHexDigit(color.getBlue()));
        } else {
            data.append("rgb(").append(color.getRed()).append(',').append(color.getGreen()).append(',').append(color.getBlue()).append(')').append(';');
            data.append(prefix).append("-opacity:").append(DECIMAL_FORMAT.format(color.getAlpha() / 255d));
        }
    }

    private void append(String fmt, Object... args) {
        data.append(Utils.stringFormat(fmt, args));
    }

    private void appendTransform(int a, int b, int c) {
        data.append(" transform='rotate(").append(a).append(',').append(b).append(',').append(c).append(")'");
    }

    private double transY(double y) {
        return (height - y);
    }

    private static double toDegrees(double rotationAnticlockWise) {
        return (180. / Math.PI) * -rotationAnticlockWise;
    }

    private static int trRound(double value) {
        return (int) Math.round(value * COORD_FACTOR);
    }

    static boolean areSameGlobalStyles(DrawingContext ctx1, DrawingContext ctx2) {
        return ctx1 == ctx2 || (ctx1.getLineEnd() == ctx2.getLineEnd() &&
                        ctx1.getLineJoin() == ctx2.getLineJoin() &&
                        ctx1.getLineType() == ctx2.getLineType() &&
                        ctx1.getLineHeight() == ctx2.getLineHeight() &&
                        ctx1.getLineWidth() == ctx2.getLineWidth() &&
                        ctx1.getLineMitre() == ctx2.getLineMitre());
    }

    private static final class Bitmap {
        private static final int FILE_HEADER_SIZE = 14;
        private static final int IMAGE_HEADER_SIZE = 40;
        private static final int BITS_PER_PIXEL = 24;
        private static final int COMPRESSION_TYPE = 0;

        static byte[] create(int[] pixels, int width) {
            int height = pixels.length / width;
            int widthInBytes = width * 3;
            int widthPadding = widthInBytes % 2;
            widthInBytes += widthPadding;

            int len = FILE_HEADER_SIZE + IMAGE_HEADER_SIZE + height * widthInBytes;
            byte[] result = new byte[len];

            // file header
            result[0] = 0x42; // B
            result[1] = 0x4d; // M
            int offset = putInt(result, 2, len);
            offset += 4;    // unused 4B must be zero
            offset = putInt(result, offset, FILE_HEADER_SIZE + IMAGE_HEADER_SIZE);  // data offset

            // image header
            offset = putInt(result, offset, IMAGE_HEADER_SIZE);
            offset = putInt(result, offset, width);
            offset = putInt(result, offset, height);
            result[offset++] = 1;   // fixed value
            result[offset++] = 0;   // fixed value
            result[offset++] = BITS_PER_PIXEL;
            result[offset++] = 0;   // bits per pixel is 2B value
            offset = putInt(result, offset, COMPRESSION_TYPE);
            // followed by 5 unimportant values (each 4B) that we leave 0
            offset += 4 * 5;

            // image data
            for (int row = height - 1; row >= 0; row--) {
                for (int col = 0; col < width; col++) {
                    GridColor color = GridColor.fromRawValue(pixels[row * width + col]);
                    result[offset++] = (byte) (color.getBlue() & 0xff);
                    result[offset++] = (byte) (color.getGreen() & 0xff);
                    result[offset++] = (byte) (color.getRed() & 0xff);
                }
                offset += widthPadding;
            }
            return result;
        }

        private static int putInt(byte[] data, int offset, int value) {
            data[offset] = (byte) (value & 0xff);
            data[offset + 1] = (byte) (value >>> 8 & 0xff);
            data[offset + 2] = (byte) (value >>> 16 & 0xff);
            data[offset + 3] = (byte) (value >>> 24 & 0xff);
            return offset + 4;
        }
    }
}
