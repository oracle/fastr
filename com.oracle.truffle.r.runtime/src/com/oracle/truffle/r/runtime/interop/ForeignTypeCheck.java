/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.interop;

import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RNull;

public class ForeignTypeCheck {
    private RType type = null;
    private final boolean byteToRaw;

    public ForeignTypeCheck() {
        this(false);
    }

    ForeignTypeCheck(boolean byteToRaw) {
        this.byteToRaw = byteToRaw;
    }

    public RType check(Object value) {
        if (value instanceof Byte) {
            setType(byteToRaw ? RType.Raw : RType.Logical);
        } else if (value instanceof Integer) {
            setType(RType.Integer);
        } else if (value instanceof Double) {
            setType(RType.Double);
        } else if (value instanceof String) {
            setType(RType.Character);
        } else if (value == RNull.instance) {
            setType(RType.Null);
        } else {
            this.type = RType.List;
        }
        return this.type;
    }

    private void setType(RType check) {
        if (this.type != RType.List) {
            if (this.type == null || type == RType.Null) {
                this.type = check;
            } else if (this.type == RType.Integer && check == RType.Double || check == RType.Integer && this.type == RType.Double) {
                this.type = RType.Double;
            } else if (this.type != check && check != RType.Null) {
                this.type = RType.List;
            }
        }
    }

    public RType getType() {
        return type != null ? type : RType.List;
    }
}
