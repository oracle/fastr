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
package com.oracle.truffle.r.library.graphics.core;

import com.oracle.truffle.r.library.graphics.core.geometry.Coordinates;

public interface GraphicsDevice {
    void deactivate();

    void activate();

    void close();

    DrawingParameters getDrawingParameters();

    void setMode(Mode newMode);

    Mode getMode();

    void setClipRect(double x1, double y1, double x2, double y2);

    void drawPolyline(Coordinates coordinates, DrawingParameters drawingParameters);

    enum Mode {
        GRAPHICS_ON,    // allow graphics output
        GRAPHICS_OFF    // disable graphics output
    }
}
