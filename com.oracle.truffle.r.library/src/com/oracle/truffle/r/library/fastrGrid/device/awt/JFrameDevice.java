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

    private final JFrame currentFrame;
    private final boolean disposeResources;
    private Runnable onResize;

    /**
     * @param frame The frame that should be used for drawing.
     * @param graphics The graphics object that should be used for drawing. This constructor
     *            overload allows to initialize the graphics object.
     * @param disposeResources Should the frame and graphics objects be disposed at {@link #close()}
     *            .
     */
    private JFrameDevice(JFrame frame, Graphics2D graphics, boolean disposeResources) {
        super(graphics, frame.getContentPane().getWidth(), frame.getContentPane().getHeight(), true);
        currentFrame = frame;
        this.disposeResources = disposeResources;
        if (currentFrame instanceof FastRFrame) {
            ((FastRFrame) currentFrame).device = this;
        }
    }

    /**
     * Creates a standalone device that manages the window itself and closes it once
     * {@link #close()} gets called.
     */
    public static JFrameDevice create(int width, int height) {
        FastRFrame frame = new FastRFrame(width, height);
        frame.setVisible(true);
        frame.pack();
        Graphics2D graphics = (Graphics2D) frame.getGraphics();
        defaultInitGraphics(graphics);
        return new JFrameDevice(frame, graphics, true);
    }

    @Override
    public void close() throws DeviceCloseException {
        if (disposeResources) {
            getGraphics2D().dispose();
            currentFrame.dispose();
        }
    }

    @Override
    int getWidthAwt() {
        return currentFrame.getContentPane().getWidth();
    }

    @Override
    int getHeightAwt() {
        return currentFrame.getContentPane().getHeight();
    }

    public void setResizeListener(Runnable onResize) {
        this.onResize = onResize;
    }

    JFrame getCurrentFrame() {
        return currentFrame;
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
                    dispose();
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
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                oldWidth = getWidth();
                                oldHeight = getHeight();
                                resizeScheduled = false;
                                if (device.onResize != null) {
                                    device.onResize.run();
                                }
                            }
                        }, 1000);
                    }
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
