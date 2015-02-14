/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.library.graphics.core;

import com.oracle.truffle.r.library.graphics.*;
import com.oracle.truffle.r.library.graphics.core.geometry.Coordinates;
import com.oracle.truffle.r.runtime.Utils;

import static com.oracle.truffle.r.library.graphics.core.GraphicsEvent.*;

// todo implement 'active' devices array from devices.c
public final class GraphicsEngineImpl implements GraphicsEngine {
    private static final int MAX_GRAPHICS_SYSTEMS_AMOUNT = 24;  // GNUR: GraphicsEngine.h
    private static final int MAX_GRAPHICS_DEVICES_AMOUNT = 64;
    private static final int NULL_GRAPHICS_DEVICE_INDEX = 0;
    private static final int LAST_GRAPHICS_DEVICE_INDEX = MAX_GRAPHICS_DEVICES_AMOUNT - 1;
    private static final int NOT_FOUND = -1;
    private static final GraphicsEngine instance = new GraphicsEngineImpl();

    /**
     * According to GNUR devices.c: 0 - null device, 63 - empty.
     */
    private final GraphicsDevice[] graphicsDevices = new GraphicsDevice[MAX_GRAPHICS_DEVICES_AMOUNT];
    private final GraphicsSystem[] graphicsSystems = new AbstractGraphicsSystem[MAX_GRAPHICS_SYSTEMS_AMOUNT];

    private int graphicsSystemsAmount = 0;
    private int devicesAmountWithoutNullDevice = 0;
    private GraphicsDevice currentGraphicsDevice = NullGraphicsDevice.getInstance();

    public static GraphicsEngine getInstance() {
        return instance;
    }

    private GraphicsEngineImpl() {
        initNullGraphicsDevice();
    }

    /**
     * According to GNUR 0 index is for the Null graphics device.
     */
    private void initNullGraphicsDevice() {
        graphicsDevices[NULL_GRAPHICS_DEVICE_INDEX] = NullGraphicsDevice.getInstance();
    }

    public int registerGraphicsSystem(GraphicsSystem newGraphicsSystem) throws Exception {
        if (newGraphicsSystem == null) {
            throw new NullPointerException("Graphics system to register is null");
        }
        int index = findElementIndexInArray(null, graphicsSystems); // find null in the
        // graphicsSystems
        if (NOT_FOUND == index) {
            throw handleErrorAndMakeException("too many graphics systems registered");
        }
        graphicsSystems[index] = newGraphicsSystem;
        callListenerForEachDevice(newGraphicsSystem.getGraphicsEventsListener(), GE_INIT_STATE);
        graphicsSystemsAmount++;
        return index;
    }

    private void callListenerForEachDevice(AbstractGraphicsSystem.GraphicsEventsListener listener, GraphicsEvent event) {
        if (noGraphicsDevices()) {
            return;
        }
        for (int i = NULL_GRAPHICS_DEVICE_INDEX + 1; i < LAST_GRAPHICS_DEVICE_INDEX; i++) {
            GraphicsDevice graphicsDevice = graphicsDevices[i];
            if (graphicsDevice != null) {
                listener.onEvent(event, graphicsDevice);
            }
        }
    }

    // todo transcribe error(_(msg)) from errors.c
    private static Exception handleErrorAndMakeException(String message) {
        return new Exception(message);
    }

    // todo implement in GNUR way
    private static void issueWarning(String warningMessage) {
        Utils.warn(warningMessage);
    }

    public void unRegisterGraphicsSystem(int graphicsSystemId) {
        checkGraphicsSystemIndex(graphicsSystemId);
        if (graphicsSystemsAmount == 0) {
            issueWarning("no graphics system to unregister");
            return;
        }
        GraphicsSystem graphicsSystem = graphicsSystems[graphicsSystemId];
        callListenerForEachDevice(graphicsSystem.getGraphicsEventsListener(), GE_FINAL_STATE);
        graphicsSystems[graphicsSystemId] = null;
        graphicsSystemsAmount--;
    }

    private void checkGraphicsSystemIndex(int graphicsSystemIndex) {
        if (graphicsSystemIndex < 0 || graphicsSystemIndex >= graphicsSystems.length) {
            throw new IllegalArgumentException("Wrong graphics system index: " + graphicsSystemIndex);
        }
    }

    // todo implement '.Devices' list related logic from GEaddDevices (devices.c)
    public void registerGraphicsDevice(GraphicsDevice newGraphicsDevice) throws Exception {
        if (newGraphicsDevice == null) {
            throw new NullPointerException("Graphics device to register is null");
        }
        if (!noGraphicsDevices()) {
            getCurrentGraphicsDevice().deactivate();
        }
        int index = findElementIndexInArray(NULL_GRAPHICS_DEVICE_INDEX + 1, LAST_GRAPHICS_DEVICE_INDEX, null, graphicsDevices);
        if (index == NOT_FOUND) {
            throw handleErrorAndMakeException("too many open devices");
        }
        graphicsDevices[index] = newGraphicsDevice;
        devicesAmountWithoutNullDevice++;
        currentGraphicsDevice = newGraphicsDevice;
        notifyEachGraphicsSystem(newGraphicsDevice, GE_INIT_STATE);
        newGraphicsDevice.activate();
    }

    private void notifyEachGraphicsSystem(GraphicsDevice graphicsDevice, GraphicsEvent event) {
        for (int i = 0; i < MAX_GRAPHICS_SYSTEMS_AMOUNT; i++) {
            GraphicsSystem graphicsSystem = graphicsSystems[i];
            if (graphicsSystem != null) {
                graphicsSystem.getGraphicsEventsListener().onEvent(event, graphicsDevice);
            }
        }
    }

    public void unRegisterGraphicsDevice(GraphicsDevice deviceToUnregister) {
        if (deviceToUnregister == null) {
            throw new NullPointerException("Graphics device to unregister is null");
        }
        doUnregisterGraphicsDevice(deviceToUnregister);
        makeItCurrent(getGraphicsDeviceNextTo(deviceToUnregister));
        // todo Interesting that in GNUR a GraphicsSystem is not notified when a GraphicsDevice is
        // killed
    }

    private void doUnregisterGraphicsDevice(GraphicsDevice deviceToUnregister) {
        int index = findElementIndexInArray(deviceToUnregister, graphicsDevices);
        if (index == NOT_FOUND) {
            issueWarning("no graphics device to unregister");
            return;
        }
        graphicsDevices[index] = null;
        devicesAmountWithoutNullDevice--;
        currentGraphicsDevice = getNullGraphicsDevice();
        deviceToUnregister.close();
    }

    private void makeItCurrent(GraphicsDevice newCurrentGraphicsDevice) {
        currentGraphicsDevice = newCurrentGraphicsDevice;
        currentGraphicsDevice.activate();
    }

    public int getGraphicsDevicesAmount() {
        return devicesAmountWithoutNullDevice;
    }

    public boolean noGraphicsDevices() {
        return devicesAmountWithoutNullDevice == 0;
    }

    public GraphicsDevice getCurrentGraphicsDevice() {
        if (isNullDeviceIsCurrent()) {
            try {
                // todo transcribe device installation from GNUR GEcurrentDevice (devices.c)
                installCurrentGraphicsDevice();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return currentGraphicsDevice;
    }

    private boolean isNullDeviceIsCurrent() {
        return currentGraphicsDevice == getNullGraphicsDevice();
    }

    private void installCurrentGraphicsDevice() throws Exception {
        registerGraphicsDevice(new MockGraphicsDevice());
    }

    public GraphicsDevice getGraphicsDeviceNextTo(GraphicsDevice graphicsDevice) {
        if (graphicsDevice == null) {
            throw new NullPointerException("Graphics device is null");
        }
        int startIndex = findElementIndexInArray(graphicsDevice, graphicsDevices);
        if (startIndex == NOT_FOUND) {
            return getNullGraphicsDevice();
        }
        GraphicsDevice foundDevice = findNotNullGraphicsDevice(startIndex + 1, graphicsDevices.length, SearchDirection.FORWARD);
        if (foundDevice == null) {
            foundDevice = findNotNullGraphicsDevice(startIndex - 1, NULL_GRAPHICS_DEVICE_INDEX, SearchDirection.BACKWARD);
        }
        return foundDevice == null ? getNullGraphicsDevice() : foundDevice;
    }

    @Override
    public void setCurrentGraphicsDeviceMode(GraphicsDevice.Mode newMode) {
        GraphicsDevice currentDevice = getCurrentGraphicsDevice();
        if (currentDevice.getMode() != newMode) {
            currentDevice.setMode(newMode);
        }
    }

    public GraphicsDevice getGraphicsDevicePrevTo(GraphicsDevice graphicsDevice) {
        if (graphicsDevice == null) {
            throw new NullPointerException("Graphics device is null");
        }
        int startIndex = findElementIndexInArray(graphicsDevice, graphicsDevices);
        if (startIndex == NOT_FOUND) {
            return getNullGraphicsDevice();
        }
        GraphicsDevice foundDevice = findNotNullGraphicsDevice(startIndex - 1, NULL_GRAPHICS_DEVICE_INDEX, SearchDirection.BACKWARD);
        if (foundDevice == null) {
            foundDevice = findNotNullGraphicsDevice(startIndex + 1, graphicsDevices.length, SearchDirection.FORWARD);
        }
        return foundDevice == null ? getNullGraphicsDevice() : foundDevice;
    }

    private static <T> int findElementIndexInArray(T element, T[] array) {
        return findElementIndexInArray(0, array.length, element, array);
    }

    private static <T> int findElementIndexInArray(int startIndexInclusive, int endIndexNotInclusive, T element, T[] array) {
        for (int i = startIndexInclusive; i < endIndexNotInclusive; i++) {
            if (array[i] == element) {
                return i;
            }
        }
        return NOT_FOUND;
    }

    private GraphicsDevice findNotNullGraphicsDevice(int startIndexInclusive, int endIndexNotInclusive, SearchDirection direction) {
        switch (direction) {
            case FORWARD:
                for (int i = startIndexInclusive; i < endIndexNotInclusive; i++) {
                    GraphicsDevice graphicsDevice = graphicsDevices[i];
                    if (graphicsDevice != null) {
                        return graphicsDevice;
                    }
                }
                break;
            case BACKWARD:
                for (int i = startIndexInclusive; i > endIndexNotInclusive; i--) {
                    GraphicsDevice graphicsDevice = graphicsDevices[i];
                    if (graphicsDevice != null) {
                        return graphicsDevice;
                    }
                }
        }
        return getNullGraphicsDevice();
    }

    private GraphicsDevice getNullGraphicsDevice() {
        return graphicsDevices[NULL_GRAPHICS_DEVICE_INDEX];
    }

    @Override
    public void setCurrentGraphicsDeviceClipRect(double x1, double y1, double x2, double y2) {
        // todo transcribe from GESetClip() (engine.c)
        getCurrentGraphicsDevice().setClipRect(0, 0, 0, 0);
    }

    @Override
    public void drawPolyline(Coordinates coordinates, DrawingParameters drawingParameters) {
        getCurrentGraphicsDevice().drawPolyline(coordinates, drawingParameters);
    }

    private enum SearchDirection {
        FORWARD,
        BACKWARD
    }
}
