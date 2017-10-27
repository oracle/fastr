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

import static com.oracle.truffle.r.library.fastrGrid.device.awt.Graphics2DDevice.defaultInitGraphics;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.HeadlessException;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

import com.oracle.truffle.r.library.fastrGrid.device.DrawingContext;
import com.oracle.truffle.r.library.fastrGrid.device.GridDevice;
import com.oracle.truffle.r.library.fastrGrid.device.ImageSaver;

/**
 * This device paints everything into an image, which is painted into a AWT component in its
 * {@code paint} method.
 */
public final class JFrameDevice implements GridDevice, ImageSaver {

    // This will be drawn on the component
    private BufferedImage componentImage;
    // Grid draings will be made into this image, may be == componentImage if isOnHold is false
    private BufferedImage image;
    // If have we created a new image for buffering while on hold, we keep it to reuse it
    private BufferedImage cachedImage;

    private Graphics2D graphics;
    private Graphics2DDevice inner;
    private boolean isOnHold = false;

    private FastRFrame currentFrame;
    private Runnable onResize;
    private Runnable onClose;

    public JFrameDevice(int width, int height) {
        currentFrame = new FastRFrame(new FastRPanel(width, height));
        openGraphics2DDevice(currentFrame.fastRComponent.getWidth(), currentFrame.fastRComponent.getHeight());
        componentImage = image;
    }

    @Override
    public synchronized void openNewPage() {
        inner.openNewPage();
        repaint();
    }

    @Override
    public void hold() {
        isOnHold = true;
        if (cachedImage == null) {
            cachedImage = new BufferedImage(inner.getWidthAwt(), inner.getHeightAwt(), TYPE_INT_RGB);
        }
        // we keep the original image so that we have the original screen state to paint while on
        // hold and drawing new stuff into the "image" buffer
        componentImage = image;
        image = cachedImage;
        graphics.dispose();
        graphics = (Graphics2D) image.getGraphics();
        defaultInitGraphics(graphics);
        // new drawings should be built on top of the old screen, not whatever was in the buffer
        graphics.drawImage(componentImage, 0, 0, null);
        inner.setGraphics2D(graphics);
    }

    @Override
    public void flush() {
        isOnHold = false;
        cachedImage = componentImage;
        componentImage = image;
        repaint();
    }

    @Override
    public void close() throws DeviceCloseException {
        disposeImageDevice();
        currentFrame.dispose();
        componentImage = null;
    }

    @Override
    public synchronized void drawRect(DrawingContext ctx, double leftX, double bottomY, double width, double height, double rotationAnticlockWise) {
        inner.drawRect(ctx, leftX, bottomY, width, height, rotationAnticlockWise);
        repaint();
    }

    @Override
    public synchronized void drawPolyLines(DrawingContext ctx, double[] x, double[] y, int startIndex, int length) {
        inner.drawPolyLines(ctx, x, y, startIndex, length);
        repaint();
    }

    @Override
    public synchronized void drawPolygon(DrawingContext ctx, double[] x, double[] y, int startIndex, int length) {
        inner.drawPolygon(ctx, x, y, startIndex, length);
        repaint();
    }

    @Override
    public synchronized void drawCircle(DrawingContext ctx, double centerX, double centerY, double radius) {
        inner.drawCircle(ctx, centerX, centerY, radius);
        repaint();
    }

    @Override
    public synchronized void drawRaster(double leftX, double bottomY, double width, double height, int[] pixels, int pixelsColumnsCount, ImageInterpolation interpolation) {
        inner.drawRaster(leftX, bottomY, width, height, pixels, pixelsColumnsCount, interpolation);
        repaint();
    }

    @Override
    public synchronized void drawString(DrawingContext ctx, double leftX, double bottomY, double rotationAnticlockWise, String text) {
        inner.drawString(ctx, leftX, bottomY, rotationAnticlockWise, text);
        repaint();
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
    public int getNativeWidth() {
        return inner.getWidthAwt();
    }

    @Override
    public int getNativeHeight() {
        return inner.getHeightAwt();
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
        ImageIO.write(image, fileType, new File(path));
    }

    public void setResizeListener(Runnable onResize) {
        this.onResize = onResize;
    }

    public void setCloseListener(Runnable onClose) {
        this.onClose = onClose;
    }

    private synchronized void openGraphics2DDevice(int width, int height) {
        image = new BufferedImage(width, height, TYPE_INT_RGB);
        graphics = (Graphics2D) image.getGraphics();
        defaultInitGraphics(graphics);
        graphics.clearRect(0, 0, width, height);
        inner = new Graphics2DDevice(graphics, width, height, true);
    }

    private void disposeImageDevice() {
        try {
            inner.close();
        } catch (DeviceCloseException e) {
            throw new RuntimeException(e);
        }
        if (graphics != null) {
            graphics.dispose();
        }
        image = null;
        cachedImage = null;
    }

    private void resize(int newWidth, int newHeight) {
        disposeImageDevice();
        openGraphics2DDevice(newWidth, newHeight);
        if (onResize != null) {
            onResize.run();
        }
    }

    private void ensureOpen() {
        if (!currentFrame.isVisible()) {
            int width = inner.getWidthAwt();
            int height = inner.getHeightAwt();
            currentFrame = new FastRFrame(new FastRPanel(width, height));
            disposeImageDevice();
            openGraphics2DDevice(width, height);
        }
    }

    private void repaint() {
        if (!isOnHold) {
            ensureOpen();
            currentFrame.repaint();
        }
    }

    /**
     * The component where the drawing will be shown.
     */
    class FastRPanel extends JPanel {
        private static final long serialVersionUID = 1234321L;

        // To avoid resizing too quickly while the user is still changing the size, we throttle the
        // resizing using this timer
        volatile boolean resizeScheduled = false;
        private final Timer timer = new Timer();

        // To avoid running the resizing code if the size actually did not change
        private int oldWidth;
        private int oldHeight;

        FastRPanel(int width, int height) {
            setPreferredSize(new Dimension(width, height));
            oldWidth = width;
            oldHeight = height;
        }

        @Override
        public void paint(Graphics g) {
            BufferedImage toDraw = JFrameDevice.this.componentImage;
            if (toDraw == null) {
                super.paint(g);
                return;
            }
            synchronized (JFrameDevice.this) {
                Graphics2D graphics = (Graphics2D) g;
                graphics.drawImage(toDraw, 0, 0, null);
            }
        }

        void resized() {
            if (oldHeight == getHeight() && oldWidth == getWidth()) {
                return;
            }
            if (!resizeScheduled) {
                resizeScheduled = true;
                scheduleResize();
            }
        }

        private void scheduleResize() {
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    oldWidth = getWidth();
                    oldHeight = getHeight();
                    resizeScheduled = false;
                    JFrameDevice.this.resize(oldWidth, oldHeight);
                }
            }, 1000);
        }
    }

    /**
     * The window that wraps the {@link FastRPanel}.
     */
    class FastRFrame extends JFrame {
        private static final long serialVersionUID = 187211L;
        private final FastRPanel fastRComponent;

        FastRFrame(FastRPanel fastRComponent) throws HeadlessException {
            super("FastR");
            this.fastRComponent = fastRComponent;
            addListeners();
            getContentPane().add(fastRComponent);
            pack();
            setLocationRelativeTo(null); // centers the window
            setVisible(true);
        }

        private void addListeners() {
            addWindowFocusListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    if (onClose != null) {
                        onClose.run();
                    }
                }
            });
            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    fastRComponent.resized();
                }
            });
        }
    }
}
