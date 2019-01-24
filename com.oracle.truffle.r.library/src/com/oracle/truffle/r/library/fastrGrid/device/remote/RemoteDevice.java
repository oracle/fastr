/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.library.fastrGrid.device.remote;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.WeakHashMap;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.library.fastrGrid.device.DrawingContext;
import com.oracle.truffle.r.library.fastrGrid.device.GridDevice;
import com.oracle.truffle.r.library.fastrGrid.device.NotSupportedImageFormatException;
import com.oracle.truffle.r.runtime.REnvVars;
import com.oracle.truffle.r.runtime.RInternalError;

public final class RemoteDevice implements GridDevice {

    private static final Logger log = Logger.getLogger(RemoteDevice.class.getName());

    /** Marker bit for commands required to return a result from server. */
    static final byte RESULT_MASK = 64;

    /**
     * Create new image with filename, fileType, width and height params. Server returns integer
     * imageId of the new image. Other requests encode command-type byte followed by imagedId int
     * followed by command-specific parameters.
     */
    public static final byte CREATE_IMAGE = RESULT_MASK | 1;
    public static final byte OPEN_NEW_PAGE = 2;
    public static final byte HOLD = 3;
    public static final byte FLUSH = 4;
    public static final byte CLOSE = RESULT_MASK | 5; // Result is exception msg or ""
    public static final byte DRAW_RECT = 6;
    public static final byte DRAW_POLY_LINES = 7;
    public static final byte DRAW_POLYGON = 8;
    public static final byte DRAW_CIRCLE = 9;
    public static final byte DRAW_RASTER = 10;
    public static final byte DRAW_STRING = 11;
    public static final byte GET_WIDTH = RESULT_MASK | 12;
    public static final byte GET_HEIGHT = RESULT_MASK | 13;
    public static final byte GET_NATIVE_WIDTH = RESULT_MASK | 14;
    public static final byte GET_NATIVE_HEIGHT = RESULT_MASK | 15;
    public static final byte GET_STRING_WIDTH = RESULT_MASK | 16;
    public static final byte GET_STRING_HEIGHT = RESULT_MASK | 17;
    public static final byte CREATE_DRAWING_CONTEXT = RESULT_MASK | 18;
    public static final byte RELEASE_DRAWING_CONTEXT = 19;

    /** Status is sent back from server as first byte of the response stream. */
    public static final byte STATUS_OK = 0;
    public static final byte STATUS_SERVER_ERROR = 1;

    public static final int SERVER_PORT = 8011;

    public static final String COMMAND_HANDLER = "/command";

    private static final int SERVER_CONNECT_RETRIES = 3;
    private static final int SERVER_CONNECT_TIMEOUT = 2000; // in ms
    private static final int SERVER_CONNECT_RETRY_DELAY = 1000; // in ms
    private static final int SERVER_AFTER_RUN_DELAY = 500; // in ms

    private static final String SERVER_JAR_NAME = "grid-device-remote-server.jar";

    private static LinkedBlockingDeque<RemoteRequest> queue = new LinkedBlockingDeque<>();

    private static Thread queueWorker;

    private static ReferenceQueue<DrawingContext> drawingContextRefQueue = new ReferenceQueue<>();

    private static Process serverProcess;

    private static Path javaCmd;

    private final int remoteDeviceId;

    private boolean closed;

    private final RemoteDeviceDataExchange paramsEncoder = new RemoteDeviceDataExchange();

    private final Map<DrawingContext, DrawingContextWeakRef> drawingContext2Ref = new WeakHashMap<>();

    public static RemoteDevice open(String filename, String fileType, int width, int height) throws NotSupportedImageFormatException {
        return new RemoteDevice(DeviceType.BUFFERED_IMAGE, filename, fileType, width, height);
    }

    public static RemoteDevice createWindowDevice(int width, int height) {
        try {
            return new RemoteDevice(DeviceType.WINDOW, null, null, width, height);
        } catch (NotSupportedImageFormatException ex) { // Should never happen for this device type
            throw new AssertionError();
        }
    }

    private static Path javaCmd() {
        if (javaCmd == null) {
            String javaHome = System.getenv("JAVA_HOME");
            if (javaHome == null) {
                throw new RInternalError("JAVA_HOME is null");
            }
            javaCmd = Paths.get(javaHome, "bin", "java");
            if (!Files.exists(javaCmd)) {
                throw new RInternalError("Non-existent path '" + javaCmd + "'.");
            }
        }
        return javaCmd;
    }

    private static void checkQueueInited() {
        if (queueWorker == null) {
            Runnable queueWorkerRun = new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        RemoteRequest request;
                        try {
                            request = queue.take();
                        } catch (InterruptedException ex) {
                            break;
                        }

                        do {
                            try {
                                String url = "http://localhost:" + SERVER_PORT + COMMAND_HANDLER;
                                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                                sendRequest(request, conn);
                            } catch (IOException ex) {
                                if (!checkServerConnectable()) {
                                    destroyServer();
                                    request.finish(new byte[]{STATUS_SERVER_ERROR}, ex);
                                }
                            }
                        } while (!request.isFinished());
                    }
                }
            };
            queueWorker = new Thread(queueWorkerRun, "Grid-Remote-Device-Queue-Worker");
            // queueWorker.setDaemon(true);
            queueWorker.setPriority(Thread.NORM_PRIORITY + 2);
            queueWorker.start();

            Runnable dcRefGC = new Runnable() {
                RemoteDeviceDataExchange paramsEncoder = new RemoteDeviceDataExchange();

                @Override
                public void run() {
                    while (true) {
                        try {
                            DrawingContextWeakRef ref = (DrawingContextWeakRef) drawingContextRefQueue.remove();
                            assert (paramsEncoder.isEmpty());
                            paramsEncoder.writeByte(RELEASE_DRAWING_CONTEXT);
                            paramsEncoder.writeInt(ref.getContextId());
                            addRequestImpl(paramsEncoder.resetWrite());
                            if (log.isLoggable(Level.FINE)) {
                                log.fine("Drawing context with contextRefIHC=" + System.identityHashCode(ref) + " and id=" + ref.getContextId() + " released.");
                            }
                        } catch (InterruptedException ex) {
                        }
                    }
                }
            };
            Thread dcRefQueueWorker = new Thread(dcRefGC, "Grid-Drawing-Context-GC");
            dcRefQueueWorker.setDaemon(true);
            dcRefQueueWorker.start();
        }
    }

    private static boolean checkServerConnectable() {
        for (int i = SERVER_CONNECT_RETRIES - 1; i >= 0; i--) {
            if (serverProcess != null && !serverProcess.isAlive()) {
                serverProcess.destroy();
                serverProcess = null;
            }
            if (serverProcess == null) {
                String rHome = REnvVars.rHome();
                Path serverJar = Paths.get(rHome, SERVER_JAR_NAME);
                if (!Files.exists(serverJar)) {
                    Path buildServerJar = Paths.get(rHome, "mxbuild", "dists", SERVER_JAR_NAME);
                    if (!Files.exists(buildServerJar)) {
                        RInternalError.shouldNotReachHere(
                                        "Remote grid server jar " + serverJar + " nor " + buildServerJar + " not found.");
                    }
                    serverJar = buildServerJar;
                }
                ProcessBuilder pb = new ProcessBuilder(
                                javaCmd().toAbsolutePath().toString(),
                                "-Dsun.net.httpserver.nodelay=true",
                                "-jar",
                                serverJar.toAbsolutePath().toString());
                pb.inheritIO();
                try {
                    serverProcess = pb.start();
                } catch (IOException ex) {
                    throw new RInternalError(ex, "Cannot start remote grid server process.");
                }
                try {
                    // Wait for an estimate of how long it takes to the server process
                    // to start listening on server port.
                    Thread.sleep(SERVER_AFTER_RUN_DELAY);
                } catch (InterruptedException ex) {
                }
            }

            // Check that server is connectable
            Socket socket = new Socket();
            try {
                socket.connect(new InetSocketAddress(SERVER_PORT), SERVER_CONNECT_TIMEOUT);
                return true;
            } catch (SocketTimeoutException ex) {
            } catch (IOException ex) {
            } finally {
                try {
                    socket.close();
                } catch (IOException ex) {
                }
            }

            if (serverProcess != null) {
                // In case the server start delay timeout was
                // insufficient this (repetitive) timeout should
                // ensure that the client will finally connect.
                try {
                    Thread.sleep(SERVER_CONNECT_RETRY_DELAY);
                } catch (InterruptedException ex) {
                }
            }
        }
        return false;
    }

    private static void destroyServer() {
        if (serverProcess != null) {
            serverProcess.destroy();
            serverProcess = null;
        }
    }

    private static RInternalError serverError() {
        return new RInternalError("Grid Server communication error.");
    }

    private RemoteDevice(DeviceType type, String filename, String fileType, int width, int height) throws NotSupportedImageFormatException {
        checkQueueInited();
        paramsEncoder.writeByte(CREATE_IMAGE);
        paramsEncoder.writeInt(type.ordinal());
        paramsEncoder.writeString(filename);
        paramsEncoder.writeString(fileType);
        paramsEncoder.writeInt(width);
        paramsEncoder.writeInt(height);
        RemoteDeviceDataExchange resultDecoder = addResultRequest(true);
        remoteDeviceId = resultDecoder.readInt();
        if (remoteDeviceId == -1) {
            throw new NotSupportedImageFormatException();
        }
        if (log.isLoggable(Level.FINE)) {
            log.fine("New remote device id=" + remoteDeviceId + " created.");
        }
    }

    private void encodeOp(byte opId) {
        if (closed) {
            throw new RInternalError("Operation opId=" + opId + " with closed remote grid device (id=" + remoteDeviceId + ") prohibited.");
        }
        assert (paramsEncoder.isEmpty());
        paramsEncoder.writeByte(opId);
        assert (remoteDeviceId != 0) : "Remote device not obtained yet.";
        paramsEncoder.writeInt(remoteDeviceId);
    }

    private boolean encodeOpAndDrawingContext(byte opId, DrawingContext ctx) {
        DrawingContextWeakRef ctxRef;
        synchronized (drawingContext2Ref) {
            ctxRef = drawingContext2Ref.get(ctx);
        }
        if (ctxRef == null) { // Precede op with a request for drawing context creation
            assert (paramsEncoder.isEmpty());
            paramsEncoder.writeByte(CREATE_DRAWING_CONTEXT);
            paramsEncoder.writeByteArray(ctx.getLineType());
            paramsEncoder.writeDouble(ctx.getLineWidth());
            paramsEncoder.writeInt(ctx.getLineJoin().ordinal());
            paramsEncoder.writeInt(ctx.getLineEnd().ordinal());
            paramsEncoder.writeDouble(ctx.getLineMitre());
            paramsEncoder.writeInt(ctx.getColor().getRawValue());
            paramsEncoder.writeDouble(ctx.getFontSize());
            paramsEncoder.writeInt(ctx.getFontStyle().ordinal());
            paramsEncoder.writeString(ctx.getFontFamily());
            paramsEncoder.writeDouble(ctx.getLineHeight());
            paramsEncoder.writeInt(ctx.getFillColor().getRawValue());
            RemoteDeviceDataExchange resultDecoder = addResultRequest(false);
            if (resultDecoder.readByte() == STATUS_OK) {
                ctxRef = new DrawingContextWeakRef(ctx, resultDecoder.readInt());
                if (log.isLoggable(Level.FINE)) {
                    log.fine("Drawing context id=" + ctxRef.getContextId() + " for contextRefIHC=" + System.identityHashCode(ctxRef));
                }
                assert resultDecoder.isReadFinished();
                synchronized (drawingContext2Ref) {
                    drawingContext2Ref.put(ctx, ctxRef);
                }
            } else {
                return false;
            }
        }
        encodeOp(opId);
        paramsEncoder.writeInt(ctxRef.getContextId());
        return true;
    }

    @TruffleBoundary
    RemoteDeviceDataExchange addResultRequest(boolean decodeReturnStatus) {
        RemoteRequest request = addRequestImpl(paramsEncoder.resetWrite());
        assert (request.params[0] & RESULT_MASK) == RESULT_MASK : "Unexpected no-result command-id " + request.params[0];
        while (true) {
            synchronized (request) {
                if (request.isFinished()) {
                    RemoteDeviceDataExchange resultDecoder = request.resultDecoder();
                    if (decodeReturnStatus) {
                        if (resultDecoder.readByte() != STATUS_OK) {
                            throw serverError();
                        }
                    }
                    return resultDecoder;
                }
                try {
                    request.wait();
                    if (request.errorCause != null) {
                        throw new RInternalError(request.errorCause, "Grid Server communication error");
                    }
                } catch (InterruptedException ex) {
                    throw new RInternalError("Waiting for result interrupted");
                }
            }
        }
    }

    @TruffleBoundary
    void addNoResultRequest() {
        RemoteRequest request = addRequestImpl(paramsEncoder.resetWrite());
        assert (request.params[0] & RESULT_MASK) == 0 : "Unexpected result command-id " + request.params[0];
    }

    private static RemoteRequest addRequestImpl(byte[] params) {
        RemoteRequest request = new RemoteRequest(params);
        queue.add(request);
        return request;
    }

    private static void sendRequest(RemoteRequest request, HttpURLConnection conn) throws IOException {
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "R-Grid-Remote-Client");
        conn.setDoOutput(true);
        OutputStream os = conn.getOutputStream();
        byte[] osBuf = request.params;
        if (log.isLoggable(Level.FINER)) {
            log.finer(RemoteDeviceDataExchange.bytesToString("Data to server:", osBuf, osBuf.length));
        }
        try {
            os.write(osBuf);
        } finally {
            os.close();
        }
        int responseCode = conn.getResponseCode();
        if (responseCode == 200) { // Status OK
            InputStream is = conn.getInputStream();
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
                log.finer(RemoteDeviceDataExchange.bytesToString("Response from server:", isBuf, off));
            }
            byte[] result;
            if (off != isBuf.length) {
                result = new byte[off];
                System.arraycopy(isBuf, 0, result, 0, off);
            } else {
                result = isBuf;
            }
            request.finish(result, null);
        }
    }

    @Override
    public void openNewPage() {
        encodeOp(OPEN_NEW_PAGE);
        addNoResultRequest();
    }

    @Override
    public void close() throws DeviceCloseException {
        encodeOp(CLOSE);
        int[] releaseDrawingContextIds;
        synchronized (RemoteDevice.class) {
            releaseDrawingContextIds = new int[drawingContext2Ref.size()];
            int i = 0;
            for (DrawingContextWeakRef ref : drawingContext2Ref.values()) {
                int ctxId = ref.invalidateContextId();
                assert (ctxId != -1);
                releaseDrawingContextIds[i++] = ctxId;
            }
            drawingContext2Ref.clear();
        }
        paramsEncoder.writeIntArray(releaseDrawingContextIds); // Possible zero ids skipped on
                                                               // server
        RemoteDeviceDataExchange resultDecoder = addResultRequest(true);
        String excMsg = resultDecoder.readString();
        closed = true;
        if (excMsg != null) {
            throw new DeviceCloseException(new IOException(excMsg)); // wrap into IOException for
                                                                     // now
        }
        if (log.isLoggable(Level.FINE)) {
            log.fine("Remote device id=" + remoteDeviceId + " closed successfully.");
        }
    }

    @Override
    public void drawRect(DrawingContext ctx, double leftX, double bottomY, double width, double height, double rotationAnticlockWise) {
        if (encodeOpAndDrawingContext(DRAW_RECT, ctx)) {
            paramsEncoder.writeDouble(leftX);
            paramsEncoder.writeDouble(bottomY);
            paramsEncoder.writeDouble(width);
            paramsEncoder.writeDouble(height);
            paramsEncoder.writeDouble(rotationAnticlockWise);
            addNoResultRequest();
        } else {
            throw serverError();
        }
    }

    @Override
    public void drawPolyLines(DrawingContext ctx, double[] x, double[] y, int startIndex, int length) {
        if (encodeOpAndDrawingContext(DRAW_POLY_LINES, ctx)) {
            paramsEncoder.writeDoubleArray(x);
            paramsEncoder.writeDoubleArray(y);
            paramsEncoder.writeInt(startIndex);
            paramsEncoder.writeInt(length);
            addNoResultRequest();
        } else {
            throw serverError();
        }
    }

    @Override
    public void drawPolygon(DrawingContext ctx, double[] x, double[] y, int startIndex, int length) {
        if (encodeOpAndDrawingContext(DRAW_POLYGON, ctx)) {
            paramsEncoder.writeDoubleArray(x);
            paramsEncoder.writeDoubleArray(y);
            paramsEncoder.writeInt(startIndex);
            paramsEncoder.writeInt(length);
            addNoResultRequest();
        } else {
            throw serverError();
        }
    }

    @Override
    public void drawCircle(DrawingContext ctx, double centerX, double centerY, double radius) {
        if (encodeOpAndDrawingContext(DRAW_CIRCLE, ctx)) {
            paramsEncoder.writeDouble(centerX);
            paramsEncoder.writeDouble(centerY);
            paramsEncoder.writeDouble(radius);
            addNoResultRequest();
        } else {
            throw serverError();
        }
    }

    @Override
    public void drawRaster(double leftX, double bottomY, double width, double height, int[] pixels, int pixelsColumnsCount, ImageInterpolation interpolation) {
        encodeOp(DRAW_RASTER);
        paramsEncoder.writeDouble(leftX);
        paramsEncoder.writeDouble(bottomY);
        paramsEncoder.writeDouble(width);
        paramsEncoder.writeDouble(height);
        paramsEncoder.writeIntArray(pixels);
        paramsEncoder.writeInt(pixelsColumnsCount);
        paramsEncoder.writeInt(interpolation.ordinal());
        addNoResultRequest();
    }

    @Override
    public void drawString(DrawingContext ctx, double leftX, double bottomY, double rotationAnticlockWise, String text) {
        if (encodeOpAndDrawingContext(DRAW_STRING, ctx)) {
            paramsEncoder.writeDouble(leftX);
            paramsEncoder.writeDouble(bottomY);
            paramsEncoder.writeDouble(rotationAnticlockWise);
            paramsEncoder.writeString(text);
            addNoResultRequest();
        } else {
            throw serverError();
        }
    }

    @Override
    public double getWidth() {
        encodeOp(GET_WIDTH);
        RemoteDeviceDataExchange resultDecoder = addResultRequest(true);
        return resultDecoder.readDouble();
    }

    @Override
    public double getHeight() {
        encodeOp(GET_HEIGHT);
        RemoteDeviceDataExchange resultDecoder = addResultRequest(true);
        return resultDecoder.readDouble();
    }

    @Override
    public int getNativeWidth() {
        encodeOp(GET_NATIVE_WIDTH);
        RemoteDeviceDataExchange resultDecoder = addResultRequest(true);
        return resultDecoder.readInt();
    }

    @Override
    public int getNativeHeight() {
        encodeOp(GET_NATIVE_HEIGHT);
        RemoteDeviceDataExchange resultDecoder = addResultRequest(true);
        return resultDecoder.readInt();
    }

    @Override
    public double getStringWidth(DrawingContext ctx, String text) {
        if (encodeOpAndDrawingContext(GET_STRING_WIDTH, ctx)) {
            paramsEncoder.writeString(text);
            RemoteDeviceDataExchange resultDecoder = addResultRequest(true);
            return resultDecoder.readDouble();
        } else {
            throw serverError();
        }
    }

    @Override
    public double getStringHeight(DrawingContext ctx, String text) {
        if (encodeOpAndDrawingContext(GET_STRING_HEIGHT, ctx)) {
            paramsEncoder.writeString(text);
            RemoteDeviceDataExchange resultDecoder = addResultRequest(true);
            return resultDecoder.readDouble();
        } else {
            throw serverError();
        }
    }

    static final class RemoteRequest {

        static final byte[] EMPTY_RESULT = new byte[0];

        final byte[] params;

        byte[] result;

        Exception errorCause;

        RemoteRequest(byte[] params) {
            this.params = params;
        }

        RemoteDeviceDataExchange resultDecoder() {
            return (result != null) ? new RemoteDeviceDataExchange(result, result.length) : null;
        }

        void finish(byte[] resultArg, Exception errorCauseArg) {
            assert (this.result == null) : "Result already assigned";
            byte[] resultArg2 = resultArg;
            if (resultArg2 != null) {
                assert (params[0] | RESULT_MASK) != 0 : "Attempt to assign result to non-result request";
            } else {
                resultArg2 = EMPTY_RESULT;
            }
            synchronized (this) {
                this.result = resultArg2;
                this.errorCause = errorCauseArg;
                notifyAll();
            }
        }

        boolean isFinished() {
            return (result != null);
        }

    }

    public enum DeviceType {
        WINDOW,
        BUFFERED_IMAGE;
    }

    private static final class DrawingContextWeakRef extends WeakReference<DrawingContext> {

        private int contextId;

        DrawingContextWeakRef(DrawingContext drawingContext, int contextIdArg) {
            super(drawingContext, drawingContextRefQueue);
            this.contextId = contextIdArg;
        }

        int getContextId() {
            return contextId;
        }

        int invalidateContextId() {
            int id = contextId;
            contextId = -1;
            return id;
        }

    }

}
