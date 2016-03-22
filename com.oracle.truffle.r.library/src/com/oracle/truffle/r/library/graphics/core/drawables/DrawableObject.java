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
package com.oracle.truffle.r.library.graphics.core.drawables;

import java.awt.Graphics2D;

import com.oracle.truffle.r.library.graphics.core.geometry.CoordinateSystem;

/**
 * Denotes an object defined in <code>srcCoordinateSystem</code> that can be drawn in
 * <code>dstCoordinateSystem</code> on {@link Graphics2D}.
 */
public abstract class DrawableObject {
    private final CoordinateSystem srcCoordinateSystem;

    protected DrawableObject(CoordinateSystem srcCoordinateSystem) {
        this.srcCoordinateSystem = srcCoordinateSystem;
    }

    public abstract void drawOn(Graphics2D g2);

    /**
     * Override to prepare coordinates given in <code>srcCoordinateSystem</code> to be drawn in
     * <code>srcCoordinateSystem</code>.
     */
    public abstract void recalculateForDrawingIn(CoordinateSystem dstCoordinateSystem);

    protected final CoordinateSystem getSrcCoordinateSystem() {
        return srcCoordinateSystem;
    }
}
