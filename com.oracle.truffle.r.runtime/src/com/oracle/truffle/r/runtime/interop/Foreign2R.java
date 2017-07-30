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
package com.oracle.truffle.r.runtime.interop;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

@ImportStatic(Message.class)
public abstract class Foreign2R extends RBaseNode {

    public static Foreign2R createForeign2R() {
        return Foreign2RNodeGen.create();
    }

    public abstract Object execute(Object obj);

    @Specialization
    public byte doBoolean(boolean obj) {
        return RRuntime.asLogical(obj);
    }

    @Specialization
    public int doByte(byte obj) {
        return ((Byte) obj).intValue();
    }

    @Specialization
    public int doShort(short obj) {
        return ((Short) obj).intValue();
    }

    @Specialization
    public double doLong(long obj) {
        return (((Long) obj).doubleValue());
    }

    @Specialization
    public double doFloat(float obj) {
        return (((Float) obj).doubleValue());
    }

    @Specialization
    public String doChar(char obj) {
        return ((Character) obj).toString();
    }

    @Specialization(guards = "isNull(obj)")
    public RNull doNull(@SuppressWarnings("unused") Object obj) {
        return RNull.instance;
    }

    @Fallback
    public Object doObject(Object obj) {
        return obj;
    }

    protected boolean isNull(Object o) {
        return o == null;
    }
}
