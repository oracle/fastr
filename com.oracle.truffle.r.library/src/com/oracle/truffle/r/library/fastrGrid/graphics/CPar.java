/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.library.fastrGrid.graphics;

import static com.oracle.truffle.r.library.fastrGrid.device.DrawingContext.INCH_TO_POINTS_FACTOR;

import java.util.Map;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.library.fastrGrid.GPar;
import com.oracle.truffle.r.library.fastrGrid.GridContext;
import com.oracle.truffle.r.library.fastrGrid.GridState;
import com.oracle.truffle.r.library.fastrGrid.LGridDirty;
import com.oracle.truffle.r.library.fastrGrid.device.DrawingContext;
import com.oracle.truffle.r.library.fastrGrid.device.GridDevice;
import com.oracle.truffle.r.library.fastrGrid.grDevices.OpenDefaultDevice;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.nodes.function.call.RExplicitCallNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.env.REnvironment;

/**
 * Retrieves and optionally sets the graphical parameters. Some of them are read-only. This function
 * has issues with shared search path, see {@link GridContext#getContext()} for details, therefore
 * we do not have any unit tests for it for now.
 */
public final class CPar extends RExternalBuiltinNode {
    static {
        Casts.noCasts(CPar.class);
    }

    @Child private OpenDefaultDevice openDefaultDevice = new OpenDefaultDevice();
    @Child private LGridDirty gridDirty = new LGridDirty();
    @Child private ReadVariableNode readLibraryFun = ReadVariableNode.createFunctionLookup("library");
    @Child private RExplicitCallNode callNode = RExplicitCallNode.create();

    @Override
    public Object call(VirtualFrame frame, RArgsValuesAndNames args) {
        if (args.getSignature().getNonNullCount() > 0) {
            throw error(Message.GENERIC, "Using par for setting device parameters is not supported in FastR grid emulation mode.");
        }
        initGridDevice(frame);
        return call(args);
    }

    @Override
    protected Object call(RArgsValuesAndNames args) {
        openDefaultDevice.execute();
        return getPar(args);
    }

    @TruffleBoundary
    private Object getPar(RArgsValuesAndNames args) {
        GridContext gridCtx = GridContext.getContext();
        GridDevice device = gridCtx.getCurrentDevice();
        Map<String, Object> graphicsPars = gridCtx.getGridState().getGraphicsPars();
        String[] names = null;
        RAbstractListVector values = null;

        // unwrap list if it is the first argument
        if (args.getLength() == 1 && args.getSignature().getName(0) == null) {
            Object first = args.getArgument(0);
            if (first instanceof RList) {
                values = (RList) first;
                RStringVector namesVec = values.getNames();
                names = namesVec == null ? null : namesVec.getReadonlyStringData();
            }
        }
        if (values == null) {
            names = args.getSignature().getNames();
            values = RDataFactory.createList(args.getArguments());
        }

        Object[] result = new Object[values.getLength()];
        String[] resultNames = new String[values.getLength()];
        for (int i = 0; i < values.getLength(); i++) {
            String paramName = names == null ? null : names[i];
            Object newValue = null;
            if (paramName == null) {
                // the value of the parameter itself is the name and we are only getting the value
                // of the graphical par
                paramName = RRuntime.asString(values.getDataAt(i));
                if (paramName == null) {
                    // if the name is not String, GNU-R just puts NULL into the result
                    result[i] = RNull.instance;
                    resultNames[i] = "";
                    continue;
                }
            } else {
                newValue = values.getDataAt(i);
            }
            resultNames[i] = paramName;
            result[i] = handleParam(paramName, newValue, graphicsPars, device);
        }
        return RDataFactory.createList(result, RDataFactory.createStringVector(resultNames, RDataFactory.COMPLETE_VECTOR));
    }

    private Object handleParam(String name, Object newValue, Map<String, Object> graphicsPars, GridDevice device) {
        // Note: some parameters which are readonly in GNU-R are writeable in FastR
        Object result = getComputedParam(name, device);
        if (result != null && newValue != null) {
            throw error(Message.GRAPHICAL_PAR_CANNOT_BE_SET, name);
        }
        if (result == null) {
            result = graphicsPars.get(name);
            if (result == null) {
                throw error(Message.IS_NOT_GRAPHICAL_PAR, name);
            }
            // TODO: we are not validating and coercing the values, e.g. "oma" should be integer
            // vector of size 4. This can be maybe based on the previous value: the type and size
            // must match
            if (newValue != null) {
                graphicsPars.put(name, newValue);
            }
        }
        return result;
    }

    private static Object getComputedParam(String name, GridDevice device) {
        switch (name) {
            case "din":
                return RDataFactory.createDoubleVector(new double[]{device.getWidth(), device.getHeight()}, RDataFactory.COMPLETE_VECTOR);
            case "cin":
                /*
                 * character size ‘(width, height)’ in inches. These are the same measurements as
                 * ‘cra’, expressed in different units.
                 *
                 * Note: cin/cra is used in dev.size() to figure out the conversion ratio between
                 * pixels and inches. For the time being what is important is to choose the values
                 * to keep this ratio!
                 */
                double cin = getCurrentDrawingContext().getFontSize() / INCH_TO_POINTS_FACTOR;
                return RDataFactory.createDoubleVector(new double[]{cin, cin}, RDataFactory.COMPLETE_VECTOR);
            case "cra":
                /*
                 * size of default character ‘(width, height)’ in ‘rasters’ (pixels). Some devices
                 * have no concept of pixels and so assume an arbitrary pixel size, usually 1/72
                 * inch. These are the same measurements as ‘cin’, expressed in different units.
                 */
                double cra = getCurrentDrawingContext().getFontSize();
                return RDataFactory.createDoubleVector(new double[]{cra, cra}, RDataFactory.COMPLETE_VECTOR);
            default:
                return null;
        }
    }

    private static DrawingContext getCurrentDrawingContext() {
        return GPar.create(GridContext.getContext().getGridState().getGpar()).getDrawingContext(0);
    }

    private void initGridDevice(VirtualFrame frame) {
        RContext rCtx = getRContext();
        GridState gridState = GridContext.getContext(rCtx).getGridState();
        if (!gridState.isDeviceInitialized()) {
            CompilerDirectives.transferToInterpreter();
            gridDirty.call(getGridEnv(frame, rCtx).getFrame(), RArgsValuesAndNames.EMPTY);
        }
    }

    private REnvironment getGridEnv(VirtualFrame frame, RContext rCtx) {
        REnvironment gridEnv = REnvironment.getRegisteredNamespace(rCtx, "grid");
        if (gridEnv != null) {
            return gridEnv;
        }
        // evaluate "library(grid)"
        RFunction libFun = (RFunction) readLibraryFun.execute(frame);
        ArgumentsSignature libFunSig = ArgumentsSignature.get(null, "character.only");
        callNode.call(frame, libFun, new RArgsValuesAndNames(new Object[]{"grid", RRuntime.LOGICAL_TRUE}, libFunSig));
        gridEnv = REnvironment.getRegisteredNamespace(rCtx, "grid");
        assert gridEnv != null : "grid should have been just loaded";
        return gridEnv;
    }
}
