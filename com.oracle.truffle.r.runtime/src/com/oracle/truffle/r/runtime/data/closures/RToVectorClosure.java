/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data.closures;

import java.util.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

public abstract class RToVectorClosure implements RAbstractVector {

    private final RAbstractVector vector;

    public RToVectorClosure(RAbstractVector vector) {
        this.vector = vector;
    }

    public int getLength() {
        return vector.getLength();
    }

    public boolean isComplete() {
        return vector.isComplete();
    }

    public boolean hasDimensions() {
        return vector.hasDimensions();
    }

    public int[] getDimensions() {
        return vector.getDimensions();
    }

    @Override
    public final void verifyDimensions(int[] newDimensions, SourceSection sourceSection) {
        vector.verifyDimensions(newDimensions, sourceSection);
    }

    @Override
    public Object getNames() {
        return vector.getNames();
    }

    @Override
    public RList getDimNames() {
        return vector.getDimNames();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return vector.getAttributes();
    }

    public RAbstractVector copy() {
        return copyWithNewDimensions(getDimensions());
    }

    public boolean isMatrix() {
        return vector.isMatrix();
    }

    public boolean isArray() {
        return vector.isArray();
    }

    public List<String> getClassHierarchy() {
        return vector.getClassHierarchy();
    }

    public boolean isObject() {
        return vector.isObject();
    }
}
