/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.ffi.impl.javaGD;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;

import com.oracle.truffle.api.profiles.BranchProfile;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.rosuda.javaGD.GDInterface;
import org.rosuda.javaGD.JavaGD;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.Collections;
import com.oracle.truffle.r.runtime.FastRConfig;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.context.RContext;

public final class JavaGDContext {

    private final Collections.ArrayListObj<GDInterface> devices;
    private final Collections.ArrayListObj<Integer> deviceIndexes;
    private static final String awtDeviceName = ".FASTR.AWT";

    private JavaGDContext(JavaGDContext parentGDCtx) {
        this.devices = parentGDCtx != null ? parentGDCtx.devices : new Collections.ArrayListObj<>();
        this.deviceIndexes = new Collections.ArrayListObj<>();
        this.deviceIndexes.push(-1, BranchProfile.getUncached());
    }

    public static RError awtNotSupported() {
        throw RError.error(RError.NO_CALLER, Message.GENERIC, "AWT based grid devices are not supported.");
    }

    /**
     * Called from an up call, i.e. from native code `gdOpen`.
     * @param gdId fastr-specific ID of JavaGD device. See {@code jGDtalk.c:javaGDDeviceCounter}.
     * @param deviceName Template of the device name is {@code fileType::params:fileNameTemplate}.
     *                   For example "svg::family=sans,bg=white:myfile.svg"
     */
    @TruffleBoundary
    public GDInterface newGD(int gdId, String deviceName) {
        if (!FastRConfig.AwtSupport) {
            throw awtNotSupported();
        }

        assert gdId == devices.size();
        deviceIndexes.push(gdId, BranchProfile.getUncached());
        GDInterface gd = newGD(gdId, deviceName, LoggingGD.Mode.getMode());
        devices.add(gd);
        return gd;
    }

    private static GDInterface newGD(int gdId, String deviceName, LoggingGD.Mode gdLogMode) {
        GDInterface gd;
        String nm = deviceName;
        int colon = nm.indexOf("::");
        if (colon > 0) {
            // device specified
            String fileType = nm.substring(0, colon);

            nm = nm.substring(colon + 2);
            int colon2 = nm.indexOf(":");
            String params = null;
            if (colon2 > 0) {
                params = nm.substring(0, colon2);
                nm = nm.substring(colon2 + 1);
            }

            String fileNameTemplate = nm;
            switch (gdLogMode) {
                case wrap:
                    gd = new LoggingGD(newGD(gdId, deviceName, LoggingGD.Mode.off), fileNameTemplate + ".Rgd", gdId, deviceName, false);
                    break;
                case headless:
                    gd = new LoggingGD(new NullGD(), fileNameTemplate, gdId, deviceName, false);
                    break;
                case off:
                default:
                    if ("svg".equals(fileType)) {
                        gd = new SVGImageGD(fileNameTemplate, params);
                    } else {
                        gd = new BufferedImageGD(fileType, fileNameTemplate, params);
                    }
                    break;
            }
        } else {
            // no device specified, i.e. use the default device
            switch (gdLogMode) {
                case wrap:
                    gd = new LoggingGD(createDefaultDevice(deviceName), getDefaultFileTemplate(deviceName), gdId, deviceName, false);
                    break;
                case headless:
                    gd = new LoggingGD(new NullGD(), getDefaultFileTemplate(deviceName), gdId, deviceName, false);
                    break;
                case off:
                default:
                    gd = createDefaultDevice(deviceName);
            }
        }
        return gd;
    }

    private static GDInterface createDefaultDevice(String deviceName) {
        if (deviceName.equals(awtDeviceName)) {
            return new AWTGraphicsGD();
        } else {
            return new JavaGD(new Resizer(), new DevOffCall());
        }
    }

    private static String getDefaultFileTemplate(String deviceName) {
        String gdlogTemplate = "".equals(deviceName) ? "Rplot" : deviceName;
        return gdlogTemplate + "%03d";
    }

    @SuppressWarnings("unused")
    private static String constructDevNullName(String fileType, String params) {
        return params == null ? fileType + "::/dev/null" : fileType + "::" + params + ":/dev/null";
    }

    /**
     * Returns the current device ID, as tracked by javagd package.
     * Note that this should be mapped to value in {@code jGDtalk.c:javaGDDeviceController}.
     * Also note that this is a different value than `dev.cur()` returns.
     * `dev.cur()` is mapped to {@code devices.c:R_CurrentDevice} and is tracked by grDevices.
     * @return Current ID of a JavaGD device. -1 if there is no active JavaGD device.
     */
    public int getCurrentGdId() {
        return (int) deviceIndexes.peek();
    }

    public GDInterface getGD(int gdId) {
        if (!FastRConfig.AwtSupport) {
            throw awtNotSupported();
        }
        return this.devices.get(gdId);
    }

    public GDInterface removeGD(int gdId) {
        if (!FastRConfig.AwtSupport) {
            throw awtNotSupported();
        }
        assert gdId < devices.size();
        int lastDevIdx = (int) deviceIndexes.pop();
        assert lastDevIdx == gdId;
        GDInterface gd = devices.get(gdId);
        assert gd != null;
        devices.set(gdId, null);
        return gd;
    }

    /**
     * Retrieves current {@link JavaGDContext} from given {@link RContext}, if necessary initializes
     * the {@link JavaGDContext}. If the {@link RContext} is a child of existing context, the parent
     * {@link JavaGDContext} is used to initialize this {@link JavaGDContext}.
     */
    @TruffleBoundary
    public static JavaGDContext getContext(RContext rCtx) {
        return getContext(rCtx, true);
    }

    private static JavaGDContext getContext(RContext rCtx, boolean initialize) {
        if (rCtx.gridContext == null) {
            RContext parentRCtx = rCtx.getParent();
            boolean doInitialize = initialize;
            if (parentRCtx != null) {
                assert parentRCtx != rCtx;  // would cause infinite recursion
                JavaGDContext parentGDCtx = getContext(parentRCtx, false);
                if (parentGDCtx != null) {
                    rCtx.gridContext = new JavaGDContext(parentGDCtx);
                    doInitialize = false;
                }
            }
            if (doInitialize) {
                rCtx.gridContext = new JavaGDContext(null);
            }
        }
        return (JavaGDContext) rCtx.gridContext;
    }

    private static Function<Function<Context, Object>, Future<Object>> getExecutor() {
        final Context ctx = Context.getCurrent();
        final RContext rCtx = RContext.getInstance();
        return (task) -> {
            Future<Object> submit = rCtx.submit(() -> {
                return task.apply(ctx);
            });
            return submit;
        };
    }

    private static final class DevOffCall implements Consumer<Integer> {
        private final Function<Function<Context, Object>, Future<Object>> executor = getExecutor();
        private final Value devOffFun = Context.getCurrent().eval("R", "function (devNum) try({ dev.set(devNum); dev.off()},silent=TRUE)");

        @Override
        public void accept(Integer devNumber) {
            executor.apply((ctx) -> {
                devOffFun.execute(devNumber);
                return null;
            });
        }
    }

    private static final class Resizer implements Consumer<Integer> {

        private final Function<Function<Context, Object>, Future<Object>> executor = getExecutor();
        private final ReentrantLock lock = new ReentrantLock();
        private final AtomicInteger resizeRequestQueue = new AtomicInteger();
        private final AtomicBoolean interruptResize = RContext.getInstance().interruptResize;
        private final Value resize = Context.getCurrent().eval("R", "graphics:::.javaGD.resize");

        /**
         * This method is assumed to run in the AWT thread.
         */
        @Override
        public void accept(Integer devNumber) {
            lock.lock();
            try {
                if (resizeRequestQueue.getAndIncrement() == 0) {
                    // empty queue => schedule resize

                    executor.apply((ctx) -> {
                        Resizer.this.lock.lock();
                        try {
                            int size = resizeRequestQueue.get();
                            try {
                                while (size > 0) {
                                    interruptResize.set(false);

                                    Resizer.this.lock.unlock();
                                    resize.execute(devNumber);
                                    Resizer.this.lock.lock();

                                    size = resizeRequestQueue.addAndGet(-size);
                                }

                                return null;
                            } catch (Throwable t) {
                                t.printStackTrace();
                                return null;
                            } finally {
                                resizeRequestQueue.set(0);
                            }
                        } finally {
                            Resizer.this.lock.unlock();
                        }
                    });
                } else {
                    interruptResize.set(true);
                }
            } finally {
                lock.unlock();
            }
        }
    }

}
