/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (C) 1995, 1996  Robert Gentleman and Ross Ihaka
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 1998--2014, The R Core Team
 * Copyright (c) 2002--2010, The R Foundation
 * Copyright (C) 2005--2006, Morten Welinder
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.graphics;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JComponent;

import com.oracle.truffle.r.library.graphics.core.drawables.DrawableObject;
import com.oracle.truffle.r.library.graphics.core.geometry.CoordinateSystem;

public class FastRComponent extends JComponent {

    private static final long serialVersionUID = 1L;

    private final List<DrawableObject> displayList = Collections.synchronizedList(new ArrayList<>());

    private boolean shouldDraw;
    private CoordinateSystem coordinateSystem;

    /**
     * Note! Called from ED thread.
     */
    @Override
    public void doLayout() {
        super.doLayout();
        Dimension size = getSize();
        coordinateSystem = new CoordinateSystem(0, size.getWidth(), 0, size.getHeight());
        shouldDraw = true;
        recalculateDisplayList();
    }

    private void recalculateDisplayList() {
        synchronized (displayList) {
            displayList.stream().forEach(drawableObject -> drawableObject.recalculateForDrawingIn(coordinateSystem));
        }
    }

    /**
     * Note! Called from ED thread.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        if (shouldDraw) {
            drawDisplayListOn(g2);
        }
    }

    private void drawDisplayListOn(Graphics2D g2) {
        synchronized (displayList) {
            displayList.stream().forEach(drawableObject -> drawableObject.drawOn(g2));
        }
    }

    public void addDrawableObject(DrawableObject drawableObject) {
        synchronized (displayList) {
            displayList.add(drawableObject);
        }
        shouldDraw = true;
        if (coordinateSystem != null) {
            drawableObject.recalculateForDrawingIn(coordinateSystem);
            repaint();
        }
    }
}
