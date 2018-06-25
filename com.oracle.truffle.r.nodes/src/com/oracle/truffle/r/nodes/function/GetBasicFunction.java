/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.function;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.nodes.helpers.AccessListField;
import com.oracle.truffle.r.nodes.helpers.GetFromEnvironment;
import com.oracle.truffle.r.nodes.helpers.GetFromEnvironmentNodeGen;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

public final class GetBasicFunction extends RBaseNode {
    @Child private AccessListField accessListField = AccessListField.create();
    @Child private GetFromEnvironment getMethods = GetFromEnvironmentNodeGen.create();
    @Child private GetFromEnvironment getBasicFunsList = GetFromEnvironmentNodeGen.create();

    public Object execute(VirtualFrame frame, String functionName) {
        REnvironment namespaceRegistry = REnvironment.getNamespaceRegistry();
        Object methods = getMethods.execute(frame, namespaceRegistry, "methods");
        if (!(methods instanceof REnvironment)) {
            throw error(RError.Message.GENERIC, "methods namespace not found on search list");
        }
        Object basicFunsList = getBasicFunsList.execute(frame, (REnvironment) methods, ".BasicFunsList");
        if (!(basicFunsList instanceof RList)) {
            throw error(RError.Message.GENERIC, ".BasicFunsList not found in methods namespace");
        }
        return accessListField.execute((RList) basicFunsList, functionName);
    }
}
