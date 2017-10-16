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
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.fastrGrid.graphics;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.library.fastrGrid.FastRGridExternalLookup;
import com.oracle.truffle.r.runtime.FastRConfig;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.ROptions;
import com.oracle.truffle.r.runtime.ROptions.OptionsException;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.env.REnvironment;

/**
 * Initialization of graphics package emulation for the purposes of FastR grid package
 * implementation.
 *
 * FastR exposes two devices: the null device and 'awt' device, and adds function 'awt' to the
 * grDevices package. The 'awt' function ends up calling 'C_X11' (the same as the 'X11' function
 * from grDevices), we capture that call in {@link FastRGridExternalLookup} and replace it with our
 * own logic. This way we also "implement" 'X11' device with java awt should anyone try to activate
 * it.
 *
 * Moreover, we change the value of option "device" to our "awt" function so that when e.g. lattice
 * tries to open the default device it uses 'awt'. If the future this should be either 'awt' for
 * interactive sessions, or some image format device for batch sessions. We should also honor the
 * R_INTERACTIVE_DEVICE and R_DEFAULT_DEVICE environment variables.
 *
 * The responsibility of this class if to provide convenient access to those R-level variables. The
 * actual devices instances are maintained in
 * {@link com.oracle.truffle.r.library.fastrGrid.GridContext} since we only have grid devices and no
 * generic graphics devices.
 */
public final class RGridGraphicsAdapter {
    private static final String DEFAULT_DEVICE_OPTION = "device";
    private static final String NULL_DEVICE = "null device";
    /**
     * The graphics devices system maintains two variables .Device and .Devices in the base
     * environment both are always set: .Devices gives a list of character vectors of the names of
     * open devices, .Device is the element corresponding to the currently active device. The null
     * device will always be open.
     */
    private static final String DOT_DEVICE = ".Device";
    private static final String DOT_DEVICES = ".Devices";

    private RGridGraphicsAdapter() {
        // only static members
    }

    public static void initialize() {
        REnvironment.baseEnv().safePut(DOT_DEVICES, RDataFactory.createPairList(NULL_DEVICE));
        setCurrentDevice(NULL_DEVICE);
        RContext ctx = RContext.getInstance();
        ROptions.ContextStateImpl options = ctx.stateROptions;
        if (options.getValue(DEFAULT_DEVICE_OPTION) != RNull.instance) {
            return;
        }
        String defaultDevice = (ctx.isInteractive() && FastRConfig.InternalGridAwtSupport) ? "awt" : "svg";
        try {
            options.setValue(DEFAULT_DEVICE_OPTION, defaultDevice);
        } catch (OptionsException e) {
            RError.warning(RError.NO_CALLER, Message.GENERIC, "FastR could not set the 'device' options to awt.");
        }
    }

    /**
     * Fixup .Devices global variable to be a pair list of length at least 1. Non-empty lists are
     * converted to pair lists, anything else causes the variable to be reset back to the initial
     * value.
     */
    @TruffleBoundary
    public static RPairList fixupDevicesVariable() {
        REnvironment baseEnv = REnvironment.baseEnv();
        Object devices = baseEnv.get(DOT_DEVICES);
        if (devices instanceof RPairList) {
            return (RPairList) devices;
        }
        if (devices instanceof RList) {
            RList list = (RList) devices;
            if (list.getLength() > 0) {
                RPairList head = RDataFactory.createPairList(list.getDataAt(0));
                RPairList curr = head;
                for (int i = 1; i < list.getLength(); i++) {
                    RPairList next = RDataFactory.createPairList(list.getDataAt(i));
                    curr.setCdr(next);
                    curr = next;
                }
                baseEnv.safePut(DOT_DEVICES, head);
                return head;
            }
        }
        // reset the .Devices and .Device variables to initial values
        RPairList nullDevice = RDataFactory.createPairList(NULL_DEVICE);
        baseEnv.safePut(DOT_DEVICES, nullDevice);
        setCurrentDevice(NULL_DEVICE);
        return nullDevice;
    }

    public static void removeDevice(int index) {
        assert index > 0 : "cannot remove null device";
        RPairList devices = fixupDevicesVariable();
        assert index < devices.getLength() : "wrong index in removeDevice";
        RPairList prev = devices;
        for (int i = 0; i < index - 1; ++i) {
            prev = (RPairList) prev.cdr();
        }
        RPairList toRemove = (RPairList) prev.cdr();
        prev.setCdr(toRemove.cdr());
        setCurrentDevice((String) prev.car());
    }

    public static void setCurrentDevice(String name) {
        REnvironment baseEnv = REnvironment.baseEnv();
        assert contains(baseEnv.get(DOT_DEVICES), name) : "setCurrentDevice can be invoked only after the device is added with addDevice";
        baseEnv.safePut(DOT_DEVICE, name);
    }

    public static void addDevice(String name) {
        REnvironment baseEnv = REnvironment.baseEnv();
        baseEnv.safePut(DOT_DEVICE, name);
        RPairList dotDevices = fixupDevicesVariable();
        dotDevices.appendToEnd(RDataFactory.createPairList(name));
    }

    public static String getDefaultDevice() {
        ROptions.ContextStateImpl options = RContext.getInstance().stateROptions;
        String defaultDev = RRuntime.asString(options.getValue(DEFAULT_DEVICE_OPTION));
        if (RRuntime.isNA(defaultDev)) {
            throw RError.error(RError.NO_CALLER, Message.GENERIC, "FastR does only supports character value as the default 'device' option");
        }
        return defaultDev;
    }

    public static int getDevicesCount() {
        Object dotDevices = REnvironment.baseEnv().get(DOT_DEVICES);
        return dotDevices instanceof RPairList ? ((RPairList) dotDevices).getLength() : 0;
    }

    private static boolean contains(Object devices, String name) {
        if (!(devices instanceof RPairList)) {
            return false;
        }
        for (RPairList dev : (RPairList) devices) {
            if (dev.car().equals(name)) {
                return true;
            }
        }
        return false;
    }
}
