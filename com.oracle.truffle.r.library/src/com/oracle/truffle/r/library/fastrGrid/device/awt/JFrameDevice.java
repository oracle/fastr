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
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;

public final class JFrameDevice extends Graphics2DDevice {
    private final JFrame currentFrame;
    private final boolean disposeResources;

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
    }

    /**
     * Creates a standalone device that manages the window itself and closes it once
     * {@link #close()} gets called.
     */
    public static JFrameDevice create() {
        FastRFrame frame = new FastRFrame();
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

    JFrame getCurrentFrame() {
        return currentFrame;
    }

    static void defaultInitGraphics(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
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
