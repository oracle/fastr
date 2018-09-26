/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.library.fastrGrid.server;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.LocalTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.oracle.truffle.r.library.fastrGrid.device.DrawingContext;
import com.oracle.truffle.r.library.fastrGrid.device.GridColor;
import com.oracle.truffle.r.library.fastrGrid.device.GridDevice;
import com.oracle.truffle.r.library.fastrGrid.device.GridDevice.DeviceCloseException;
import com.oracle.truffle.r.library.fastrGrid.device.GridDevice.ImageInterpolation;
import com.oracle.truffle.r.library.fastrGrid.device.NotSupportedImageFormatException;
import com.oracle.truffle.r.library.fastrGrid.device.remote.RemoteDevice;
import com.oracle.truffle.r.library.fastrGrid.device.remote.RemoteDevice.DeviceType;
import com.oracle.truffle.r.library.fastrGrid.device.remote.RemoteDeviceDataExchange;
import com.oracle.truffle.r.library.fastrGrid.GridContext;
import com.oracle.truffle.r.library.fastrGrid.WindowDevice;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class RemoteDeviceServer {

    private static final Logger log = Logger.getLogger(RemoteDevice.class.getName());

    private static final String STATUS_HANDLER = "/status";
    private static final String QUIT_HANDLER = "/quit";

    private static HttpServer server;

    private static int lastDeviceId = 0;

    private static Map<Integer, GridDevice> id2Device = new ConcurrentHashMap<>();

    private static int lastDrawingContextId = 0;

    private static Map<Integer, ServerDrawingContext> id2DrawingContext = new ConcurrentHashMap<>();

    private static Map<DrawingContext, Integer> drawingContext2id = new ConcurrentHashMap<>();

    private static long totalRequestsServiced;

    private static long totalBytesRead;

    private static long totalBytesWritten;

    private static synchronized ServerDrawingContext getDrawingContext(Integer ctxId) {
        return getDrawingContextImpl(ctxId);
    }

    private static ServerDrawingContext getDrawingContextImpl(Integer ctxId) {
        ServerDrawingContext ctx = id2DrawingContext.get(ctxId);
        assert (ctx != null) : "Unknown or GCed drawing context id=" + ctxId + ", lastDrawingContextId=" + lastDrawingContextId;
        return ctx;
    }

    private static void releaseDrawingContextImpl(Integer ctxId) {
        ServerDrawingContext ctx = getDrawingContextImpl(ctxId);
        if (ctx.decRefCount()) {
            id2DrawingContext.remove(ctxId);
            drawingContext2id.remove(ctx);
        }
    }

    public static void main(String[] args) throws IOException {
        server = HttpServer.create(new InetSocketAddress(RemoteDevice.SERVER_PORT), 0);
        server.createContext(RemoteDevice.COMMAND_HANDLER, new CommandHandler());
        server.createContext(STATUS_HANDLER, new StatusAndQuitHandler(false));
        server.createContext(QUIT_HANDLER, new StatusAndQuitHandler(true));
        server.setExecutor(null); // creates a default executor
        server.start();
    }

    private static class CommandHandler implements HttpHandler {

        private RemoteDeviceDataExchange resultEncoder = new RemoteDeviceDataExchange();

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                handleImpl(exchange);
            } catch (Throwable t) {
                t.printStackTrace();
                System.exit(1);
            }
        }

        private void handleImpl(HttpExchange exchange) throws IOException {
            totalRequestsServiced++;
            InputStream is = exchange.getRequestBody();
            byte[] isBuf = new byte[is.available() + 1];
            int off = 0;
            int len;
            try {
                while ((len = is.read(isBuf, off, isBuf.length - off)) != -1) {
                    off += len;
                    if (off == isBuf.length) {
                        byte[] newBuf = new byte[isBuf.length + Math.max(isBuf.length, is.available() + 1)];
                        System.arraycopy(isBuf, 0, newBuf, 0, off);
                        isBuf = newBuf;
                    }
                }
            } finally {
                is.close();
            }
            if (log.isLoggable(Level.FINER)) {
                log.finer(RemoteDeviceDataExchange.bytesToString("Server Input: ", isBuf, off));
            }

            if (off == 0) {
                throw new IOException("Empty request to grid server");
            }
            totalBytesRead += off;
            RemoteDeviceDataExchange paramsDecoder = new RemoteDeviceDataExchange(isBuf, off);
            byte commandId = paramsDecoder.readByte();
            // Optimistically write ok status for all ops (revert if necessary)
            resultEncoder.writeByte(RemoteDevice.STATUS_OK);
            boolean checkServerClose = false;
            RuntimeException rExc = null;
            try {
                if (commandId == RemoteDevice.CREATE_IMAGE) {
                    DeviceType type = DeviceType.values()[paramsDecoder.readInt()];
                    String filename = paramsDecoder.readString();
                    String fileType = paramsDecoder.readString();
                    int width = paramsDecoder.readInt();
                    int height = paramsDecoder.readInt();
                    int deviceId;
                    synchronized (RemoteDeviceServer.class) {
                        deviceId = ++lastDeviceId;
                    }
                    GridDevice device;
                    switch (type) {
                        case BUFFERED_IMAGE:
                            try {
                                device = GridContext.openLocalOrRemoteDevice(filename, fileType, width, height);
                            } catch (NotSupportedImageFormatException ex) {
                                deviceId = -1;
                                device = null;
                            }
                            break;
                        case WINDOW:
                            device = WindowDevice.createWindowDevice(true, width, height);
                            break;
                        default:
                            throw new AssertionError();
                    }
                    if (deviceId != -1) {
                        id2Device.put(deviceId, device);
                    }
                    resultEncoder.writeInt(deviceId);
                } else if (commandId == RemoteDevice.CREATE_DRAWING_CONTEXT) {
                    ServerDrawingContext ctx = new ServerDrawingContext(paramsDecoder);
                    Integer ctxId = drawingContext2id.get(ctx);
                    if (ctxId == null) {
                        synchronized (RemoteDeviceServer.class) {
                            ctxId = ++lastDrawingContextId;
                            id2DrawingContext.put(ctxId, ctx);
                            drawingContext2id.put(ctx, ctxId);
                        }
                    } else {
                        synchronized (RemoteDeviceServer.class) {
                            ctx = getDrawingContextImpl(ctxId);
                            ctx.incRefCount();
                        }
                    }
                    resultEncoder.writeInt(ctxId);
                } else if (commandId == RemoteDevice.RELEASE_DRAWING_CONTEXT) {
                    int ctxId = paramsDecoder.readInt();
                    synchronized (RemoteDeviceServer.class) {
                        releaseDrawingContextImpl(ctxId);
                    }
                } else {
                    Integer deviceId = paramsDecoder.readInt();
                    GridDevice device = id2Device.get(deviceId);
                    if (device == null) {
                        throw new IllegalStateException("Grid device for id=" + deviceId + " does not exist on server.");
                    }
                    switch (commandId) {
                        case RemoteDevice.OPEN_NEW_PAGE: {
                            device.openNewPage();
                            break;
                        }
                        case RemoteDevice.HOLD: {
                            device.hold();
                            break;
                        }
                        case RemoteDevice.FLUSH: {
                            device.flush();
                            break;
                        }
                        case RemoteDevice.CLOSE: {
                            int[] releaseDrawingContextIds = paramsDecoder.readIntArray();
                            synchronized (RemoteDeviceServer.class) {
                                for (int i = 0; i < releaseDrawingContextIds.length; i++) {
                                    int ctxId = releaseDrawingContextIds[i];
                                    if (ctxId != 0) {
                                        releaseDrawingContextImpl(ctxId);
                                    }
                                }
                            }
                            id2Device.remove(deviceId);
                            checkServerClose = true;
                            String exMsg = null;
                            try {
                                device.close();
                            } catch (DeviceCloseException ex) {
                                exMsg = ex.getMessage();
                            }
                            resultEncoder.writeString(exMsg);
                            break;
                        }
                        case RemoteDevice.DRAW_RECT: {
                            DrawingContext ctx = getDrawingContext(paramsDecoder.readInt());
                            double leftX = paramsDecoder.readDouble();
                            double bottomY = paramsDecoder.readDouble();
                            double width = paramsDecoder.readDouble();
                            double height = paramsDecoder.readDouble();
                            double rotationAnticlockWise = paramsDecoder.readDouble();
                            device.drawRect(ctx, leftX, bottomY, width, height, rotationAnticlockWise);
                            break;
                        }
                        case RemoteDevice.DRAW_POLY_LINES: {
                            DrawingContext ctx = getDrawingContext(paramsDecoder.readInt());
                            double[] x = paramsDecoder.readDoubleArray();
                            double[] y = paramsDecoder.readDoubleArray();
                            int startIndex = paramsDecoder.readInt();
                            int length = paramsDecoder.readInt();
                            device.drawPolyLines(ctx, x, y, startIndex, length);
                            break;
                        }
                        case RemoteDevice.DRAW_POLYGON: {
                            DrawingContext ctx = getDrawingContext(paramsDecoder.readInt());
                            double[] x = paramsDecoder.readDoubleArray();
                            double[] y = paramsDecoder.readDoubleArray();
                            int startIndex = paramsDecoder.readInt();
                            int length = paramsDecoder.readInt();
                            device.drawPolygon(ctx, x, y, startIndex, length);
                            break;
                        }
                        case RemoteDevice.DRAW_CIRCLE: {
                            DrawingContext ctx = getDrawingContext(paramsDecoder.readInt());
                            double centerX = paramsDecoder.readDouble();
                            double centerY = paramsDecoder.readDouble();
                            double radius = paramsDecoder.readDouble();
                            device.drawCircle(ctx, centerX, centerY, radius);
                            break;
                        }
                        case RemoteDevice.DRAW_RASTER: {
                            double leftX = paramsDecoder.readDouble();
                            double bottomY = paramsDecoder.readDouble();
                            double width = paramsDecoder.readDouble();
                            double height = paramsDecoder.readDouble();
                            int[] pixels = paramsDecoder.readIntArray();
                            int pixelsColumnsCount = paramsDecoder.readInt();
                            ImageInterpolation interpolation = ImageInterpolation.values()[paramsDecoder.readInt()];
                            device.drawRaster(leftX, bottomY, width, height, pixels, pixelsColumnsCount, interpolation);
                            break;
                        }
                        case RemoteDevice.DRAW_STRING: {
                            DrawingContext ctx = getDrawingContext(paramsDecoder.readInt());
                            double leftX = paramsDecoder.readDouble();
                            double bottomY = paramsDecoder.readDouble();
                            double rotationAnticlockWise = paramsDecoder.readDouble();
                            String text = paramsDecoder.readString();
                            device.drawString(ctx, leftX, bottomY, rotationAnticlockWise, text);
                            break;
                        }
                        case RemoteDevice.GET_WIDTH: {
                            resultEncoder.writeDouble(device.getWidth());
                            break;
                        }
                        case RemoteDevice.GET_HEIGHT: {
                            resultEncoder.writeDouble(device.getHeight());
                            break;
                        }
                        case RemoteDevice.GET_NATIVE_WIDTH: {
                            resultEncoder.writeDouble(device.getNativeWidth());
                            break;
                        }
                        case RemoteDevice.GET_NATIVE_HEIGHT: {
                            resultEncoder.writeDouble(device.getNativeHeight());
                            break;
                        }
                        case RemoteDevice.GET_STRING_WIDTH: {
                            DrawingContext ctx = getDrawingContext(paramsDecoder.readInt());
                            String text = paramsDecoder.readString();
                            resultEncoder.writeDouble(device.getStringWidth(ctx, text));
                            break;
                        }
                        case RemoteDevice.GET_STRING_HEIGHT: {
                            DrawingContext ctx = getDrawingContext(paramsDecoder.readInt());
                            String text = paramsDecoder.readString();
                            resultEncoder.writeDouble(device.getStringHeight(ctx, text));
                            break;
                        }
                        default:
                            throw new IllegalStateException("Invalid requestId=" + commandId);
                    }
                }
            } catch (RuntimeException ex) {
                rExc = ex;
            }
            byte[] osBuf = resultEncoder.resetWrite();
            if (rExc != null) {
                resultEncoder.writeByte(RemoteDevice.STATUS_SERVER_ERROR);
                osBuf = resultEncoder.resetWrite();
            }
            if (log.isLoggable(Level.FINER)) {
                log.finer(RemoteDeviceDataExchange.bytesToString("Server Output: ", osBuf, osBuf.length));
            }
            exchange.sendResponseHeaders(200, osBuf.length);
            OutputStream os = exchange.getResponseBody();
            try {
                os.write(osBuf);
            } finally {
                os.close();
            }
            totalBytesWritten += osBuf.length;
            exchange.close();
            if (checkServerClose && rExc == null && id2Device.isEmpty()) {
                log.fine("Server closing automatically after last device was closed.");
                server.stop(0);
                System.exit(0);
            }
            if (rExc != null) {
                throw rExc;
            }
        }

    }

    private static final class ServerDrawingContext implements DrawingContext {

        private final byte[] lineType;
        private final double lineWidth;
        private final GridLineJoin lineJoin;
        private final GridLineEnd lineEnd;
        private final double lineMitre;
        private final GridColor color;
        private final double fontSize;
        private final GridFontStyle fontStyle;
        private final String fontFamily;
        private final double lineHeight;
        private final GridColor fillColor;

        private int hash;
        private int refCount = 1;

        ServerDrawingContext(RemoteDeviceDataExchange paramsDecoder) {
            byte[] lineTypeRead = paramsDecoder.readByteArray();
            if (lineTypeRead == null) {
                lineType = DrawingContext.GRID_LINE_BLANK;
            } else if (lineTypeRead.length == 0) {
                lineType = DrawingContext.GRID_LINE_SOLID;
            } else {
                lineType = lineTypeRead;
            }
            lineWidth = paramsDecoder.readDouble();
            lineJoin = GridLineJoin.values()[paramsDecoder.readInt()];
            lineEnd = GridLineEnd.values()[paramsDecoder.readInt()];
            lineMitre = paramsDecoder.readDouble();
            color = GridColor.fromRawValue(paramsDecoder.readInt());
            fontSize = paramsDecoder.readDouble();
            fontStyle = GridFontStyle.values()[paramsDecoder.readInt()];
            fontFamily = paramsDecoder.readString();
            lineHeight = paramsDecoder.readDouble();
            fillColor = GridColor.fromRawValue(paramsDecoder.readInt());
        }

        @Override
        public byte[] getLineType() {
            return lineType;
        }

        @Override
        public double getLineWidth() {
            return lineWidth;
        }

        @Override
        public GridLineJoin getLineJoin() {
            return lineJoin;
        }

        @Override
        public GridLineEnd getLineEnd() {
            return lineEnd;
        }

        @Override
        public double getLineMitre() {
            return lineMitre;
        }

        @Override
        public GridColor getColor() {
            return color;
        }

        @Override
        public double getFontSize() {
            return fontSize;
        }

        @Override
        public GridFontStyle getFontStyle() {
            return fontStyle;
        }

        @Override
        public String getFontFamily() {
            return fontFamily;
        }

        @Override
        public double getLineHeight() {
            return lineHeight;
        }

        @Override
        public GridColor getFillColor() {
            return fillColor;
        }

        @Override
        public int hashCode() {
            int h = hash;
            if (h == 0) {
                h = lineType.length;
                for (int i = 0; i < lineType.length; i++) {
                    h = (h << 8) ^ lineType[i];
                }
                h ^= Double.hashCode(lineWidth);
                h ^= lineJoin.ordinal();
                h ^= lineEnd.ordinal();
                h ^= Double.hashCode(lineMitre);
                h ^= color.getRawValue();
                h ^= Double.hashCode(fontSize);
                h ^= fontStyle.ordinal();
                h ^= (fontFamily != null) ? fontFamily.hashCode() : 0;
                h ^= Double.hashCode(lineHeight);
                h ^= fillColor.getRawValue();
                hash = h;
            }
            return h;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ServerDrawingContext) {
                ServerDrawingContext ctx = (ServerDrawingContext) obj;
                byte[] ctxLT = ctx.lineType;
                if (lineType != ctxLT) {
                    if (lineType != null && ctxLT != null && lineType.length == ctxLT.length) {
                        for (int i = ctxLT.length - 1; i >= 0; i--) {
                            if (lineType[i] != ctxLT[i]) {
                                return false;
                            }
                        }
                    } else {
                        return false;
                    }
                }
                if (lineWidth != ctx.lineWidth || lineJoin != ctx.lineJoin ||
                                lineEnd != ctx.lineEnd || lineMitre != ctx.lineMitre ||
                                !color.equals(ctx.color) || fontSize != ctx.fontSize ||
                                fontStyle != ctx.fontStyle || lineHeight != ctx.lineHeight ||
                                !fillColor.equals(ctx.fillColor)) {
                    return false;
                }
                if (fontFamily != ctx.fontFamily) {
                    return (fontFamily != null && fontFamily.equals(ctx.fontFamily));
                }
                return true;
            }
            return false;
        }

        void incRefCount() {
            refCount++;
        }

        boolean decRefCount() {
            return (--refCount == 0);
        }

    }

    private static class StatusAndQuitHandler implements HttpHandler {

        private final boolean handleQuit;

        StatusAndQuitHandler(boolean handleQuitArg) {
            this.handleQuit = handleQuitArg;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            OutputStream os = exchange.getResponseBody();
            String response = (handleQuit ? LocalTime.now().toString() + ": Grid server stopped. Exit.\n" : "") +
                            "Total devices created: " + lastDeviceId + ", active: " + id2Device.size() +
                            "\nTotal DrawingContexts created: " + lastDrawingContextId + ", active: " + id2DrawingContext.size() +
                            "\nTotal requests serviced: " + totalRequestsServiced +
                            "\nTotal bytes read: " + totalBytesRead + ", written: " + totalBytesWritten;
            byte[] responseBytes = response.getBytes();
            exchange.sendResponseHeaders(200, responseBytes.length);
            os.write(responseBytes);
            os.close();
            if (handleQuit) {
                server.stop(0);
                System.exit(0);
            }
        }

    }

}
