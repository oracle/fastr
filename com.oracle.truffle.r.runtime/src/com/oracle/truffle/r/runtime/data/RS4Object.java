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
package com.oracle.truffle.r.runtime.data;

import com.oracle.truffle.r.runtime.RType;

/**
 * This is a placeholder class for an S4 object (GnuR S4SXP). It has no functionality at present but
 * is needed as such objects are generated when unserializing the "methods" package.
 */
public final class RS4Object extends RSharingAttributeStorage {

    private static final RStringVector implicitClass = RDataFactory.createStringVectorFromScalar("S4");

    public RS4Object() {
        setS4();
    }

    @Override
    public RStringVector getImplicitClass() {
        return implicitClass;
    }

    @Override
    public RType getRType() {
        return RType.S4Object;
    }

    @Override
    public RS4Object copy() {
        RS4Object resultS4 = RDataFactory.createS4Object();
        if (getAttributes() != null) {
            resultS4.initAttributes(RAttributesLayout.copy(getAttributes()));
        }
        resultS4.setTypedValueInfo(getTypedValueInfo());
        return resultS4;
    }
}
