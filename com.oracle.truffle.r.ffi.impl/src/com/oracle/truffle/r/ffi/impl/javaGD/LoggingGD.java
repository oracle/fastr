/*
 * Copyright (c) 2020, 2023, Oracle and/or its affiliates. All rights reserved.
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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.function.Function;

import org.rosuda.javaGD.GDInterface;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.r.ffi.impl.javaGD.FileOutputContainer.SaveImageException;
import com.oracle.truffle.r.common.RVersionNumber;

public class LoggingGD extends GDInterface {
    private static final boolean logReturns = false;

    public enum Mode {
        off,
        wrap,
        headless;

        static Mode getMode() {
            String gdLogEnv = System.getenv("GDLOG");
            if (gdLogEnv == null) {
                return off;
            } else {
                return valueOf(gdLogEnv);
            }
        }
    }

    private final GDInterface delegate;
    private final String deviceName;
    private final String fileNameTemplate;
    @SuppressWarnings("unused") private final int gdId;

    private StringWriter buffer;
    private PrintWriter writer = new PrintWriter(new NullWriter());
    private int pageNumber;
    private double width;
    private double height;

    public LoggingGD(GDInterface delegate, String fileNameTemplate, int gdId, String deviceName) {
        this.delegate = delegate;
        this.fileNameTemplate = fileNameTemplate;
        this.gdId = gdId;
        this.deviceName = deviceName;
    }

    @TruffleBoundary
    void logGDCall(String format, Object... args) {
        writer.print("  grDevices:::");
        writer.printf(Locale.US, format, args);
    }

    @Override
    public boolean gdOpen(double w, double h) {
        this.width = w;
        this.height = h;
        return delegate.gdOpen(w, h);
    }

    @Override
    public void gdActivate() {
        // logGDCall("gdActivate()\n");
        delegate.gdActivate();
    }

    @Override
    public void gdCircle(double x, double y, double r) {
        logGDCall("gdCircle(%f, %f, %f)\n", x, y, r);
        delegate.gdCircle(x, y, r);
    }

    @Override
    public void gdClip(double x0, double x1, double y0, double y1) {
        logGDCall("gdClip(%f, %f, %f, %f)\n", x0, x1, y0, y1);
        delegate.gdClip(x0, x1, y0, y1);
    }

    @Override
    public void gdClose() {
        outro();
        try {
            save();
        } finally {
            delegate.gdClose();
            writer.close();
        }
    }

    @Override
    public void gdDeactivate() {
        // logGDCall("gdDeactivate()\n");
        delegate.gdDeactivate();
    }

    @Override
    public void gdHold() {
        logGDCall("gdHold()\n");
        delegate.gdHold();
    }

    private static String toRLogical(boolean b) {
        return b ? "TRUE" : "FALSE";
    }

    @Override
    public void gdFlush(boolean flush) {
        logGDCall("gdFlush(%s)\n", toRLogical(flush));
        writer.flush();
        delegate.gdFlush(flush);
    }

    @Override
    public double[] gdLocator() {
        // logGDCall("gdLocator()\n");
        return delegate.gdLocator();
    }

    @Override
    public void gdLine(double x1, double y1, double x2, double y2) {
        logGDCall("gdLine(%f, %f, %f, %f)\n", x1, y1, x2, y2);
        delegate.gdLine(x1, y1, x2, y2);
    }

    @Override
    public double[] gdMetricInfo(int ch) {
        double[] res = delegate.gdMetricInfo(ch);
        if (logReturns) {
            logGDCall("gdMetricInfo('%c') # res=%s\n", (char) ch, arrayToRVector(res));
        }
        return res;
    }

    @Override
    public void gdMode(int mode) {
        logGDCall("gdMode(%dL)\n", mode);
        delegate.gdMode(mode);
    }

    @Override
    public void gdNewPage() {
        delegate.gdNewPage();
        // logGDCall("gdNewPage()\n");
        outro();
        save();
        pageNumber++;
        intro();
    }

    @Override
    public void gdNewPage(int devNr, int explicitPageNumber) {
        delegate.gdNewPage(devNr, explicitPageNumber);
        // logGDCall("gdNewPage(%dL)\n", devNr);
        outro();
        save();
        if (explicitPageNumber >= 0) {
            pageNumber = explicitPageNumber;
        } else {
            pageNumber++;
        }
        intro();
    }

    @TruffleBoundary
    private static String toRVector(int length, Function<Integer, String> arrayFun) {
        StringBuilder sb = new StringBuilder("c(");
        for (int i = 0; i < length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(arrayFun.apply(i));
        }
        sb.append(')');
        return sb.toString();
    }

    private static String arrayToRVector(double[] x) {
        return toRVector(x.length, (i) -> String.format(Locale.US, "%f", x[i]));
    }

    private static String arrayToRVector(int[] x) {
        return toRVector(x.length, (i) -> Integer.toString(x[i]) + "L");
    }

    @Override
    public void gdPath(int npoly, int[] nper, double[] x, double[] y, boolean winding) {
        logGDCall("gdPath(%dL, %s, %s, %s, %s)\n", npoly, arrayToRVector(nper), arrayToRVector(x), arrayToRVector(y), toRLogical(winding));
        delegate.gdPath(npoly, nper, x, y, winding);
    }

    @Override
    public void gdPolygon(int n, double[] x, double[] y) {
        logGDCall("gdPolygon(%dL, %s, %s)\n", n, arrayToRVector(x), arrayToRVector(y));
        delegate.gdPolygon(n, x, y);
    }

    @Override
    public void gdPolyline(int n, double[] x, double[] y) {
        logGDCall("gdPolyline(%dL, %s, %s)\n", n, arrayToRVector(x), arrayToRVector(y));
        delegate.gdPolyline(n, x, y);
    }

    @Override
    public void gdRect(double x0, double y0, double x1, double y1) {
        logGDCall("gdRect(%f, %f, %f, %f)\n", x0, y0, x1, y1);
        delegate.gdRect(x0, y0, x1, y1);
    }

    @Override
    public void gdRaster(byte[] img, int imgW, int imgH, double x, double y, double w, double h, double rot, boolean interpolate) {
        int[] intImgArray = byteArrayToIntArray(img);

        logGDCall("gdRaster(%s, %dL, %dL, %f, %f, %f, %f, %f, %s)\n", arrayToRVector(intImgArray), imgW, imgH, x, y, w, h, rot, toRLogical(interpolate));
        delegate.gdRaster(img, imgW, imgH, x, y, w, h, rot, interpolate);
    }

    private static int[] byteArrayToIntArray(byte[] img) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(img.length);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.put(img);
        byteBuffer.rewind();
        IntBuffer intBuffer = byteBuffer.asIntBuffer();
        int[] intArray = new int[img.length / 4];
        intBuffer.get(intArray);
        return intArray;
    }

    @Override
    public double[] gdSize() {
        double[] res = delegate.gdSize();
        if (logReturns) {
            logGDCall("gdSize() # res=%s\n", arrayToRVector(res));
        }
        return res;
    }

    @Override
    public double gdStrWidth(String str) {
        double res = delegate.gdStrWidth(str);
        if (logReturns) {
            logGDCall("getStrWidth('%s') # res=%f\n", str, res);
        }
        return res;
    }

    @Override
    public void gdText(double x, double y, String str, double rot, double hadj) {
        logGDCall("gdText(%f, %f, '%s', %f, %f)\n", x, y, str, rot, hadj);
        delegate.gdText(x, y, str, rot, hadj);
    }

    @Override
    public void gdcSetColor(int cc) {
        logGDCall("gdcSetColor(%dL)\n", cc);
        delegate.gdcSetColor(cc);
    }

    @Override
    public void gdcSetFill(int cc) {
        logGDCall("gdcSetFill(%dL)\n", cc);
        delegate.gdcSetFill(cc);
    }

    @Override
    public void gdcSetLine(double lwd, int lty) {
        logGDCall("gdcSetLine(%f, %dL)\n", lwd, lty);
        delegate.gdcSetLine(lwd, lty);
    }

    @Override
    public void gdcSetFont(double cex, double ps, double lineheight, int fontface, String fontfamily) {
        logGDCall("gdcSetFont(%f, %f, %f, %dL, '%s')\n", cex, ps, lineheight, fontface, fontfamily);
        delegate.gdcSetFont(cex, ps, lineheight, fontface, fontfamily);
    }

    private void intro() {
        buffer = new StringWriter();
        writer = new PrintWriter(buffer);
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        writer.printf("# Generated by DLOG device on %s, FastR version %s\n", dateFormat.format(date), RVersionNumber.FULL);
        writer.printf("replayLogWithJavaGD <- function() {\n");
        writer.printf("  warning('Resizing X11 (JavaGD) is currently not supported for replaying GDLOG')\n");
        writer.printf("  X11(width=%f, height=%f)\n", width, height);
        writer.printf("  replayLog(open=FALSE, close=FALSE)\n");
        writer.printf("}\n");
        writer.printf("\n");
        writer.printf("replayLog <- function(deviceName = '%s', open = TRUE, close = TRUE, pageNumber=%d) {\n", deviceName, pageNumber);
        writer.printf("  if (open) {\n");
        logGDCall("gdOpen(deviceName, %f,%f)\n", width, height);
        logGDCall("gdActivate()\n");
        writer.printf("  }\n");
        logGDCall("gdNewPage(0, pageNumber)\n");
    }

    private void outro() {
        writer.println("  if (close) {");
        logGDCall("gdDeactivate()\n");
        logGDCall("gdClose()\n");
        writer.println("  }");
        writer.println("}");
        writer.println("if (!interactive()) replayLog()");
    }

    private void save() {
        try {
            TruffleFile file = FileOutputContainer.nextFile(pageNumber, fileNameTemplate);
            if (file != null) {
                BufferedWriter fileWriter = file.newBufferedWriter();
                writer.flush();
                fileWriter.append(buffer.toString());
                fileWriter.close();
            }
        } catch (Exception e) {
            throw new SaveImageException(e);
        }
    }
}

class NullWriter extends Writer {

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
    }

    @Override
    public void flush() throws IOException {
    }

    @Override
    public void close() throws IOException {
    }

}
