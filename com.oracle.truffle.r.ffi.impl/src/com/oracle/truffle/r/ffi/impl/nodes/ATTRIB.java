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
package com.oracle.truffle.r.ffi.impl.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.attributes.GetAttributesNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RNull;

public abstract class ATTRIB extends FFIUpCallNode.Arg1 {
    @Child private GetAttributesNode getAttributesNode = GetAttributesNode.create();

    @Specialization
    public Object doAttributable(RAttributable obj) {
        return getAttributesNode.execute(obj);
    }

    @Fallback
    public RNull doOthers(Object obj) {
        if (obj == RNull.instance || RRuntime.isForeignObject(obj)) {
            return RNull.instance;
        } else {
            CompilerDirectives.transferToInterpreter();
            String type = obj == null ? "null" : obj.getClass().getSimpleName();
            throw RError.error(RError.NO_CALLER, Message.GENERIC, "object of type '" + type + "' cannot be attributed");
        }
    }
}
