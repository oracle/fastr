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
package com.oracle.truffle.r.library.fastrGrid;

import java.util.ArrayList;

import com.oracle.truffle.r.library.fastrGrid.GridState.GridDeviceState;
import com.oracle.truffle.r.library.fastrGrid.device.GridDevice;
import com.oracle.truffle.r.library.fastrGrid.device.GridDevice.DeviceCloseException;
import com.oracle.truffle.r.library.fastrGrid.device.awt.BufferedJFrameDevice;
import com.oracle.truffle.r.library.fastrGrid.device.awt.JFrameDevice;
import com.oracle.truffle.r.library.fastrGrid.graphics.RGridGraphicsAdapter;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;

/**
 * Encapsulated the access to the global grid state.
 */
public final class GridContext {
    private static final GridContext INSTANCE = new GridContext();
    private final GridState gridState = new GridState();
    /**
     * This list should correspond to the names inside {@code .Devices} variable in R.
     */
    private final ArrayList<DeviceAndState> devices = new ArrayList<>(2);
    private int currentDeviceIdx = 0;

    private GridContext() {
        devices.add(new DeviceAndState(null));
    }

    public static GridContext getContext() {
        return INSTANCE;
    }

    public GridState getGridState() {
        gridState.setDeviceState(devices.get(currentDeviceIdx).state);
        return gridState;
    }

    public int getCurrentDeviceIndex() {
        return currentDeviceIdx;
    }

    public int getDevicesSize() {
        return devices.size();
    }

    public GridDevice getCurrentDevice() {
        assert currentDeviceIdx >= 0 : "accessing devices before they were initialized";
        return devices.get(currentDeviceIdx).device;
    }

    public void setCurrentDevice(String name, GridDevice currentDevice) {
        RGridGraphicsAdapter.addDevice(name);
        RGridGraphicsAdapter.setCurrentDevice(name);
        currentDeviceIdx = this.devices.size();
        this.devices.add(new DeviceAndState(currentDevice));
        assert devices.size() == RGridGraphicsAdapter.getDevicesCount();
    }

    public void openDefaultDevice() {
        String defaultDev = RGridGraphicsAdapter.getDefaultDevice();
        if (defaultDev.equals("awt") || defaultDev.startsWith("X11")) {
            BufferedJFrameDevice result = new BufferedJFrameDevice(JFrameDevice.create());
            setCurrentDevice(defaultDev, result);
        } else {
            throw RError.error(RError.NO_CALLER, Message.GENERIC, "FastR does not support device '" + defaultDev + "'.");
        }
        assert devices.size() == RGridGraphicsAdapter.getDevicesCount();
    }

    public void closeDevice(int which) throws DeviceCloseException {
        assert which >= 0 && which < devices.size();
        devices.get(which).device.close();
        RGridGraphicsAdapter.removeDevice(which);
        devices.remove(which);
        if (currentDeviceIdx >= which) {
            currentDeviceIdx--;
        }
    }

    private static final class DeviceAndState {
        final GridDevice device;
        final GridDeviceState state;

        DeviceAndState(GridDevice device) {
            this.device = device;
            this.state = new GridDeviceState();
        }
    }
}
