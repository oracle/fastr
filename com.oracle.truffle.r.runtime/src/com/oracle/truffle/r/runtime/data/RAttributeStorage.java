/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

/**
 * An adaptor class for the several R types that are attributable. Only useful for classes that
 * don't already inherit from another class, otherwise just cut and paste this code.
 */
public abstract class RAttributeStorage extends RBaseObject implements RAttributable {

    protected RAttributes attributes;

    @Override
    public final RAttributes getAttributes() {
        return attributes;
    }

    @Override
    public final RAttributes initAttributes() {
        if (attributes == null) {
            attributes = RAttributes.create();
        }
        return attributes;
    }

    @Override
    public final void initAttributes(RAttributes newAttributes) {
        this.attributes = newAttributes;
    }

    @Override
    public abstract RStringVector getImplicitClass();
}
