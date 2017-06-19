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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JFrame;
import javax.swing.JPanel;

public final class JFrameDevice extends Graphics2DDevice {

    private final FastRFrame currentFrame;
    private Runnable onResize;
    private Runnable onClose;

    /**
     * @param frame The frame that should be used for drawing.
     * @param graphics The graphics object that should be used for drawing. This constructor
     *            overload allows to initialize the graphics object. .
     */
    private JFrameDevice(FastRFrame frame, Graphics2D graphics) {
        super(graphics, frame.getContentPane().getWidth(), frame.getContentPane().getHeight(), true);
        currentFrame = frame;
        currentFrame.device = this;
    }

    /**
     * Creates a standalone device that manages the window itself and closes it once
     * {@link #close()} gets called.
     */
    public static JFrameDevice create(int width, int height) {
        FastRFrame frame = new FastRFrame(width, height);
        Graphics2D graphics = initFrame(frame);
        graphics.clearRect(0, 0, width, height);
        return new JFrameDevice(frame, graphics);
    }

    @Override
    public void close() throws DeviceCloseException {
        getGraphics2D().dispose();
        currentFrame.dispose();
    }

    @Override
    int getWidthAwt() {
        return currentFrame.getContentPane().getWidth();
    }

    @Override
    int getHeightAwt() {
        return currentFrame.getContentPane().getHeight();
    }

    @Override
    void ensureOpen() {
        if (!currentFrame.isVisible()) {
            getGraphics2D().dispose();
            setGraphics2D(initFrame(currentFrame));
            // TODO: for some reason this does not always clear the whole page.
            getGraphics2D().clearRect(0, 0, currentFrame.getWidth(), currentFrame.getHeight());
        }
    }

    public void setResizeListener(Runnable onResize) {
        this.onResize = onResize;
    }

    public void setCloseListener(Runnable onClose) {
        this.onClose = onClose;
    }

    JFrame getCurrentFrame() {
        return currentFrame;
    }

    private static Graphics2D initFrame(FastRFrame frame) {
        frame.setVisible(true);
        frame.pack();
        Graphics2D graphics = (Graphics2D) frame.getGraphics();
        defaultInitGraphics(graphics);
        return graphics;
    }

    static class FastRFrame extends JFrame {
        private static final long serialVersionUID = 1L;
        private final JPanel fastRComponent = new JPanel();
        private JFrameDevice device;

        volatile boolean resizeScheduled = false;
        private int oldWidth;
        private int oldHeight;
        private final Timer timer = new Timer();

        FastRFrame(int width, int height) throws HeadlessException {
            super("FastR");
            addListeners();
            createUI(width, height);
            center();
            oldWidth = width;
            oldHeight = height;
        }

        private void createUI(int width, int height) {
            setLayout(new BorderLayout());
            setSize(new Dimension(width, height));
            add(fastRComponent, BorderLayout.CENTER);
            fastRComponent.setPreferredSize(getSize());
        }

        private void addListeners() {
            addWindowFocusListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    if (device.onClose != null) {
                        device.onClose.run();
                    }
                }
            });
            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    if (oldHeight == getHeight() && oldWidth == getWidth()) {
                        return;
                    }
                    if (!resizeScheduled) {
                        resizeScheduled = true;
                        scheduleResize();
                    }
                }
            });
        }

        private void scheduleResize() {
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (device == null) {
                        scheduleResize();
                        return;
                    }
                    oldWidth = getWidth();
                    oldHeight = getHeight();
                    resizeScheduled = false;
                    if (device.onResize != null) {
                        device.onResize.run();
                    }
                }
            }, 1000);
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
