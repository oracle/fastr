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
package com.oracle.truffle.r.library.fastrGrid;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.library.fastrGrid.DisplayListFactory.LGetDisplayListElementNodeGen;
import com.oracle.truffle.r.library.fastrGrid.DisplayListFactory.LSetDisplayListOnNodeGen;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;

public class DisplayList {
    private static final int INITIAL_DL_SIZE = 100;

    static RList createInitialDisplayList() {
        Object[] data = new Object[INITIAL_DL_SIZE];
        Arrays.fill(data, 0, data.length, RNull.instance);
        RList list = RDataFactory.createList(data);
        list.makeSharedPermanent();
        return list;
    }

    static void initDisplayList(GridState gridState) {
        RList list = createInitialDisplayList();
        list.setDataAt(list.getInternalStore(), 0, gridState.getViewPort());
        gridState.setDisplayList(list);
        gridState.setDisplayListIndex(1);
    }

    public abstract static class LGetDisplayListElement extends RExternalBuiltinNode.Arg1 {
        static {
            Casts casts = new Casts(LGetDisplayListElement.class);
            casts.arg(0).asIntegerVector().findFirst();
        }

        public static LGetDisplayListElement create() {
            return LGetDisplayListElementNodeGen.create();
        }

        @Specialization
        @TruffleBoundary
        Object getDLElement(int index) {
            return GridContext.getContext().getGridState().getDisplayList().getDataAt(index);
        }
    }

    public abstract static class LSetDisplayListOn extends RExternalBuiltinNode.Arg1 {
        static {
            Casts casts = new Casts(LSetDisplayListOn.class);
            casts.arg(0).asLogicalVector().findFirst().map(toBoolean());
        }

        public static LSetDisplayListOn create() {
            return LSetDisplayListOnNodeGen.create();
        }

        @Specialization
        @TruffleBoundary
        byte setDLOn(boolean value) {
            GridState gridState = GridContext.getContext().getGridState();
            boolean result = gridState.isDisplayListOn();
            gridState.setIsDisplayListOn(value);
            return RRuntime.asLogical(result);
        }
    }

    public static final class LInitDisplayList extends RExternalBuiltinNode.Arg0 {
        static {
            Casts.noCasts(LInitDisplayList.class);
        }

        @Override
        @TruffleBoundary
        public Object execute() {
            GridState gridState = GridContext.getContext().getGridState();
            initDisplayList(gridState);
            return RNull.instance;
        }

    }
}
