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

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

class GraphicsSystemParameters {
    private final Map<GraphicsDevice, HashMap<String, Object>> parametersByDevices = new IdentityHashMap<>();

    void addParameterForDevice(GraphicsDevice graphicsDevice, String parameterName, Object parameterValue) {
        HashMap<String, Object> parameters = parametersByDevices.get(graphicsDevice);
        if (parameters == null) {
            parameters = new HashMap<>();
            parametersByDevices.put(graphicsDevice, parameters);
        }
        parameters.put(parameterName, parameterValue);
    }

    Object getParameterForDevice(GraphicsDevice graphicsDevice, int parameterName) {
        HashMap<String, Object> parameters = parametersByDevices.get(graphicsDevice);
        return parameters == null ? null : parameters.get(parameterName);
    }

    void removeAllParametersForDevice(GraphicsDevice graphicsDevice) {
        parametersByDevices.remove(graphicsDevice);
    }
}
