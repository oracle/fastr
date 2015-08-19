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
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.graphics.core;

import com.oracle.truffle.r.library.graphics.core.geometry.Coordinates;

public interface GraphicsEngine {
    void registerGraphicsSystem(GraphicsSystem newGraphicsSystem) throws Exception;

    void unRegisterGraphicsSystem(GraphicsSystem graphicsSystem);

    void registerGraphicsDevice(GraphicsDevice newGraphicsDevice) throws Exception;

    void unRegisterGraphicsDevice(GraphicsDevice deviceToUnregister);

    int getGraphicsDevicesAmount();

    /**
     * @return true if there is only Null graphics device registered
     */
    boolean noGraphicsDevices();

    /**
     * Tries to install one if there is no current device.
     *
     * @return current {@link GraphicsDevice}
     */
    GraphicsDevice getCurrentGraphicsDevice();

    /**
     * @return {@link com.oracle.truffle.r.library.grDevices.NullGraphicsDevice} if unable to find
     *         other
     */
    GraphicsDevice getGraphicsDeviceNextTo(GraphicsDevice graphicsDevice);

    /**
     * @return {@link com.oracle.truffle.r.library.grDevices.NullGraphicsDevice} if unable to find
     *         other
     */
    GraphicsDevice getGraphicsDevicePrevTo(GraphicsDevice graphicsDevice);

    void setCurrentGraphicsDeviceMode(GraphicsDevice.Mode mode);

    void setCurrentGraphicsDeviceClipRect(double x1, double y1, double x2, double y2);

    void drawPolyline(Coordinates coordinates, DrawingParameters drawingParameters);

    void killGraphicsDeviceByIndex(int graphicsDeviceIndex);

    int getCurrentGraphicsDeviceIndex();
}
