/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.fastr;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.singleElement;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.RVisibility.ON;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.nodes.access.UpdateSlotNode;
import com.oracle.truffle.r.nodes.access.UpdateSlotNodeGen;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.UpdateAttr.InternStringNode;
import com.oracle.truffle.r.nodes.builtin.base.UpdateAttrNodeGen.InternStringNodeGen;
import com.oracle.truffle.r.nodes.builtin.base.UpdateSlot.CheckSlotAssignNode;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RMissing;

@RBuiltin(name = ".fastr.methods.slotassign", visibility = ON, kind = PRIMITIVE, parameterNames = {"object", "name", "check", "value"}, behavior = COMPLEX)
public abstract class FastRSlotAssign extends RBuiltinNode {

    static {
        Casts casts = new Casts(FastRSlotAssign.class);
        casts.arg("object").mustNotBeNull();
        casts.arg("name").defaultError(Message.SLOT_INVALID_TYPE_OR_LEN).mustNotBeMissing().mustBe(stringValue()).asStringVector().mustBe(singleElement()).findFirst();
        casts.arg("check").mustNotBeMissing().asLogicalVector().findFirst().mustNotBeNA(Message.NA_UNEXP).map(toBoolean());
        casts.arg("value").mustNotBeMissing();
    }

    @Child private InternStringNode intern = InternStringNodeGen.create();
    @Child private UpdateSlotNode updateSlotNode = UpdateSlotNodeGen.create();

    @Override
    public Object[] getDefaultParameterValues() {
        return new Object[]{RMissing.instance, RMissing.instance, RRuntime.LOGICAL_TRUE, RMissing.instance};
    }

    @Specialization(guards = "check")
    public Object assign(VirtualFrame frame, Object object, String name, @SuppressWarnings("unused") boolean check, Object value,
                    @Cached("new()") CheckSlotAssignNode checkNode) {
        String interned = intern.execute(name);
        checkNode.execute(frame, object, interned, value);
        return updateSlotNode.executeUpdate(object, interned, value);
    }

    @Specialization(guards = "!check")
    public Object assign(Object object, String name, @SuppressWarnings("unused") boolean check, Object value) {
        String interned = intern.execute(name);
        return updateSlotNode.executeUpdate(object, interned, value);
    }
}
