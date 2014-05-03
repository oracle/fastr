/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base;

import java.util.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin("row.names<-")
public abstract class UpdateRowNames extends RInvisibleBuiltinNode {

    @Child UpdateDimNames updateDimNames;

    private RAbstractVector setDimNames(VirtualFrame frame, RAbstractVector vector, Object dimNames) {
        if (updateDimNames == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            updateDimNames = insert(UpdateDimNamesFactory.create(new RNode[1], getBuiltin()));
        }
        return updateDimNames.executeList(frame, vector, dimNames);
    }

    private RAbstractVector setFreshDimNames(VirtualFrame frame, RAbstractVector vector, Object names) {
        Object[] data = new Object[vector.getDimensions().length];
        Arrays.fill(data, RNull.instance);
        data[0] = names;
        return setDimNames(frame, vector, RDataFactory.createList(data));
    }

    private RAbstractVector updateDimNames(VirtualFrame frame, RAbstractVector vector, Object names) {
        RList dimNames = (RList) vector.getDimNames().copy();
        dimNames.updateDataAt(0, names, null);
        return setDimNames(frame, vector, dimNames);
    }

    @Specialization(order = 1, guards = {"hasDims", "hasDimNames"})
    public RAbstractVector updateRowNamesDimNames(VirtualFrame frame, RAbstractVector vector, RNull names) {
        controlVisibility();
        return updateDimNames(frame, vector, names);
    }

    @Specialization(order = 2, guards = {"hasDims", "!hasDimNames"})
    public RAbstractVector updateRowNamesDims(RAbstractVector vector, @SuppressWarnings("unused") RNull names) {
        controlVisibility();
        // nothing to do
        return vector;
    }

    @Specialization(order = 3, guards = "!hasDims")
    public RAbstractVector updateRowNames(RAbstractVector vector, @SuppressWarnings("unused") RNull names) {
        controlVisibility();
        // nothing to do
        return vector;
    }

    @Specialization(order = 10, guards = {"emptyNames", "hasDims", "hasDimNames"})
    public RAbstractVector updateRowNamesEmptyNamesDimNames(VirtualFrame frame, RAbstractVector vector, @SuppressWarnings("unused") RAbstractVector names) {
        controlVisibility();
        return updateDimNames(frame, vector, RNull.instance);
    }

    @Specialization(order = 11, guards = {"emptyNames", "hasDims", "!hasDimNames"})
    public RAbstractVector updateRowNamesEmptyNamesDims(VirtualFrame frame, RAbstractVector vector, @SuppressWarnings("unused") RAbstractVector names) {
        controlVisibility();
        return setFreshDimNames(frame, vector, RNull.instance);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 12, guards = {"emptyNames", "!hasDims"})
    public RAbstractVector updateRowNamesEmptyNames(RAbstractVector vector, RAbstractVector names) {
        controlVisibility();
        throw RError.getRowNamesNoDims(getEncapsulatingSourceSection());
    }

    @Specialization(order = 20, guards = {"!emptyNames", "hasDims", "hasDimNames"})
    public RAbstractVector updateRowNamesDimNames(VirtualFrame frame, RAbstractVector vector, RAbstractVector names) {
        controlVisibility();
        return updateDimNames(frame, vector, names);
    }

    @Specialization(order = 21, guards = {"!emptyNames", "hasDims", "!hasDimNames"})
    public RAbstractVector updateRowNamesDims(VirtualFrame frame, RAbstractVector vector, RAbstractVector names) {
        controlVisibility();
        return setFreshDimNames(frame, vector, names);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 22, guards = {"!emptyNames", "!hasDims"})
    public RAbstractVector updateRowNames(RAbstractVector vector, RAbstractVector names) {
        controlVisibility();
        throw RError.getRowNamesNoDims(getEncapsulatingSourceSection());
    }

    @Specialization(order = 50, guards = "areExistingNamesNull")
    public RDataFrame updateRowNamesNoNames(RDataFrame dataFrame, @SuppressWarnings("unused") RNull names) {
        controlVisibility();
        dataFrame.getVector().setRowNames(RDataFactory.createEmptyIntVector());
        return dataFrame;
    }

    @Specialization(order = 51, guards = "!areExistingNamesNull")
    public RDataFrame updateRowNames(RDataFrame dataFrame, @SuppressWarnings("unused") RNull names) {
        controlVisibility();

        int[] data = new int[((RAbstractVector) dataFrame.getRowNames()).getLength()];
        for (int i = 0; i < data.length; i++) {
            data[i] = i + 1;
        }
        dataFrame.getVector().setRowNames(RDataFactory.createIntVector(data, RDataFactory.COMPLETE_VECTOR));
        return dataFrame;
    }

    @Specialization(order = 60, guards = "areNamesStringOrInt")
    public RDataFrame updateRowNamesNoNamesStringOrInt(RDataFrame dataFrame, RAbstractVector names) {
        controlVisibility();
        dataFrame.getVector().setRowNames(names);
        return dataFrame;
    }

    @Specialization(order = 61, guards = "!areNamesStringOrInt")
    public RDataFrame updateRowNamesNoNames(@SuppressWarnings("unused") RDataFrame dataFrame, RAbstractVector names) {
        controlVisibility();
        throw RError.getRowNamesStringOrInt(getEncapsulatingSourceSection(), RRuntime.classToString(names.getElementClass(), false));
    }

    protected boolean areExistingNamesNull(RDataFrame arg) {
        return arg.getRowNames() == RNull.instance;
    }

    protected boolean areNamesStringOrInt(@SuppressWarnings("unused") Object arg, RAbstractVector rowNames) {
        return rowNames.getElementClass() == RString.class || rowNames.getElementClass() == RInt.class;
    }

    protected boolean hasDims(RAbstractVector vector) {
        return vector.hasDimensions();
    }

    protected boolean hasDimNames(RAbstractVector vector) {
        return vector.getDimNames() != null;
    }

    protected boolean emptyNames(@SuppressWarnings("unused") RAbstractVector vector, RAbstractVector names) {
        return names.getLength() == 0;
    }
}
