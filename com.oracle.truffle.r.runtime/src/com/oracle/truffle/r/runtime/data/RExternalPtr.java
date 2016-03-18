/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data;

import com.oracle.truffle.r.runtime.RType;

/**
 * The rarely seen {@code externalptr} type.
 */
public class RExternalPtr extends RAttributeStorage implements RTypedValue {
    private long addr;
    private Object tag;
    private Object prot;

    RExternalPtr(long addr, Object tag, Object prot) {
        this.addr = addr;
        this.tag = tag;
        this.prot = prot;
    }

    public RExternalPtr copy() {
        return RDataFactory.createExternalPtr(addr, tag, prot);
    }

    public long getAddr() {
        return addr;
    }

    public Object getTag() {
        return tag;
    }

    public Object getProt() {
        return prot;
    }

    public void setAddr(long value) {
        this.addr = value;
    }

    public void setTag(Object tag) {
        this.tag = tag;
    }

    public void setProt(Object prot) {
        this.prot = prot;
    }

    @Override
    public RType getRType() {
        return RType.ExternalPtr;
    }

    private static final RStringVector implicitClass = RDataFactory.createStringVector(RType.ExternalPtr.getName());

    @Override
    public final RStringVector getImplicitClass() {
        return implicitClass;
    }
}
