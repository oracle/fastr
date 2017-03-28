/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (C) 2001-3 Paul Murrell
 * Copyright (c) 1998-2013, The R Core Team
 * Copyright (c) 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.fastrGrid;

import com.oracle.truffle.r.library.fastrGrid.device.GridDevice;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.env.REnvironment;

public final class GridState {
    private REnvironment gridEnv;
    private GridDeviceState devState;

    /**
     * Current grob being drawn (for determining the list of grobs to search when evaluating a
     * grobwidth/height unit via gPath). May be RNull or RList.
     */
    private Object currentGrob;

    GridState() {
    }

    void setDeviceState(GridDeviceState state) {
        devState = state;
    }

    public int getDevHoldCount() {
        return devState.devHoldCount;
    }

    public int setDevHoldCount(int devHoldCount) {
        devState.devHoldCount = devHoldCount;
        return devHoldCount;
    }

    public RList getDisplayList() {
        if (devState.displayList == null) {
            devState.displayList = DisplayList.createInitialDisplayList();
        }
        return devState.displayList;
    }

    public void setDisplayList(RList newList) {
        newList.makeSharedPermanent();
        devState.displayList = newList;
    }

    public void setDisplayListElement(Object element) {
        Object[] data = devState.displayList.getDataWithoutCopying();
        data[devState.displayListIndex] = element;
    }

    public boolean isDisplayListOn() {
        return devState.isDisplayListOn;
    }

    public void setIsDisplayListOn(boolean flag) {
        devState.isDisplayListOn = flag;
    }

    public int getDisplayListIndex() {
        return devState.displayListIndex;
    }

    public void setDisplayListIndex(int newValue) {
        devState.displayListIndex = newValue;
    }

    public void init(REnvironment gridEnv) {
        this.gridEnv = gridEnv;
        this.currentGrob = RNull.instance;
    }

    void initGPar(GridDevice currentDevice) {
        devState.gpar = GPar.createNew(currentDevice);
    }

    /**
     * Returns something like a canonical gpar, or top level gpar. This is used when we need a
     * context to do e.g. unit conversion, but we are in a situation that no context is available.
     */
    public static GPar getInitialGPar(GridDevice device) {
        return GPar.create(GPar.createNew(device));
    }

    public RList getGpar() {
        assert gridEnv != null : "GridState not initialized";
        return devState.gpar;
    }

    public void setGpar(RList gpar) {
        assert gridEnv != null : "GridState not initialized";
        devState.gpar = gpar;
    }

    /**
     * Has the current device been initialized for use by grid? Note: the null device should never
     * get initialized. The code initializing device should check if any device is open and if not,
     * it should open the default device and initialize it.
     */
    public boolean isDeviceInitialized() {
        return devState.isDeviceInitialized;
    }

    public void setDeviceInitialized() {
        devState.isDeviceInitialized = true;
    }

    public RList getViewPort() {
        return devState.viewPort;
    }

    public void setViewPort(RList viewPort) {
        devState.viewPort = viewPort;
    }

    public REnvironment getGridEnv() {
        return gridEnv;
    }

    public Object getCurrentGrob() {
        return currentGrob;
    }

    public void setCurrentGrob(Object currentGrob) {
        this.currentGrob = currentGrob;
    }

    public double getScale() {
        return devState.scale;
    }

    static final class GridDeviceState {
        private boolean isDeviceInitialized = false;
        private RList gpar;
        private RList viewPort;
        private double scale = 1;
        private int devHoldCount;
        private boolean isDisplayListOn = true;
        private RList displayList;
        private int displayListIndex = 0;
    }
}
