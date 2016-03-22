/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.engine.interop;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.r.engine.TruffleRLanguage;
import com.oracle.truffle.r.nodes.access.vector.ElementAccessMode;
import com.oracle.truffle.r.nodes.access.vector.ReplaceVectorNode;
import com.oracle.truffle.r.nodes.builtin.base.InfixEmulationFunctions.UpdateFieldBuiltin;
import com.oracle.truffle.r.runtime.RRuntime;

abstract class ListWriteNode extends RootNode {
    @Child private UpdateFieldBuiltin builtin;
    @Child private ReplaceVectorNode extract = ReplaceVectorNode.create(ElementAccessMode.SUBSCRIPT, true);

    ListWriteNode() {
        super(TruffleRLanguage.class, null, null);
    }

    @Override
    public final Object execute(VirtualFrame frame) {
        Object label = ForeignAccess.getArguments(frame).get(0);
        Object value = ForeignAccess.getArguments(frame).get(1);
        Object receiver = ForeignAccess.getReceiver(frame);
        return execute(frame, receiver, label, value);
    }

    protected abstract Object execute(VirtualFrame frame, Object reciever, Object label, Object value);

    @Specialization
    protected Object writeField(VirtualFrame frame, Object receiver, String field, Object valueObj) {
        Object value = valueObj;
        if (value instanceof Short) {
            value = (int) ((Short) value).shortValue();
        } else if (value instanceof Float) {
            float floatValue = ((Float) value).floatValue();
            value = new Double(floatValue);
        } else if (value instanceof Boolean) {
            boolean booleanValue = ((Boolean) value).booleanValue();
            value = booleanValue ? RRuntime.LOGICAL_TRUE : RRuntime.LOGICAL_FALSE;
        } else if (value instanceof Character) {
            value = (int) ((Character) value).charValue();
        } else if (value instanceof Byte) {
            value = (int) ((Byte) value).byteValue();
        }
        Object x = extract.apply(frame, receiver, new Object[]{field}, value);
        return x;
    }
}
