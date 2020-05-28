/*
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.runtime.nodes;

import java.util.HashSet;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractListBaseVector;

/**
 * Code sharing vehicle for the slight differences in behavior between {@code duplicated} and
 * {@code anyDuplicated} (both internal and native versions) and whether {@code fromLast} is
 * {@code TRUE/FALSE}.
 */
public class DuplicationHelper {
    private final RAbstractContainer x;
    private final HashSet<DupEntry> vectorContents = new HashSet<>();
    private final HashSet<DupEntry> incompContents;
    private final byte[] dupVec;
    private int index;

    public DuplicationHelper(RAbstractContainer x, RAbstractContainer incomparables, boolean justIndex, boolean fromLast) {
        this.x = x;
        vectorContents.add(new DupEntry(x.getDataAtAsObject(fromLast ? x.getLength() - 1 : 0)));

        if (incomparables != null) {
            incompContents = new HashSet<>();
            for (int i = 0; i < incomparables.getLength(); i++) {
                incompContents.add(new DupEntry(incomparables.getDataAtAsObject(i)));
            }
        } else {
            incompContents = null;
        }
        dupVec = justIndex ? null : new byte[x.getLength()];
    }

    public boolean doIt(int i) {
        DupEntry entry = new DupEntry(x.getDataAtAsObject(i));
        if (incompContents == null || !incompContents.contains(entry)) {
            if (!vectorContents.add(entry)) {
                if (dupVec == null) {
                    index = i + 1;
                    return true;
                } else {
                    dupVec[i] = RRuntime.LOGICAL_TRUE;
                }
            }
        } else {
            if (dupVec != null) {
                dupVec[i] = RRuntime.LOGICAL_FALSE;
            }
        }
        return false;
    }

    public int getIndex() {
        return index;
    }

    public byte[] getDupVec() {
        return dupVec;
    }

    @TruffleBoundary
    public static DuplicationHelper analyze(RAbstractContainer x, RAbstractContainer incomparables, boolean justIndex, boolean fromLast) {
        DuplicationHelper ds = new DuplicationHelper(x, incomparables, justIndex, fromLast);
        if (fromLast) {
            for (int i = x.getLength() - 2; i >= 0; i--) {
                if (ds.doIt(i)) {
                    break;
                }
            }
        } else {
            for (int i = 1; i < x.getLength(); i++) {
                if (ds.doIt(i)) {
                    break;
                }
            }
        }
        return ds;
    }

    private static final class DupEntry {

        private final Object element;
        private final VectorDataLibrary dataLib;
        private final int hashCode;

        DupEntry(Object elementIn) {
            Object elem = normalizeSingletonContainer(elementIn);
            // hashcode: first bit indicates whether it is abstract container or not
            if (elem instanceof RAbstractContainer) {
                RAbstractContainer container = (RAbstractContainer) elem;
                dataLib = VectorDataLibrary.getFactory().getUncached(container.getData());
                Object data = this.element = container.getData();
                int type = container.getRType().ordinal();
                int len = dataLib.getLength(data);
                int valsHash = 0;
                for (int i = 0; i < Math.min(len, 5); i++) {
                    final Object obj0 = dataLib.getDataAtAsObject(data, i);
                    valsHash ^= obj0.hashCode();
                }
                // 6 bits for the type only, 10 bits for the length only,
                // the rest is xor of the remaining bits from length and
                // the first few elements. We also lshift by 1 to make
                // the first bit 0
                hashCode = ((type | len << 6) ^ (valsHash << 16)) << 1;
            } else {
                this.element = elem;
                dataLib = null;
                hashCode = elem.hashCode() | 1;
            }
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public boolean equals(Object o) {
            Object other = o;
            VectorDataLibrary otherLib = null;
            if (other instanceof DupEntry) {
                otherLib = ((DupEntry) other).dataLib;
                other = ((DupEntry) other).element;
            }
            return equals(this.dataLib, this.element, otherLib, other);
        }

        static boolean equals(VectorDataLibrary data1Lib, Object o1, VectorDataLibrary data2Lib, Object o2) {
            if (data1Lib != null && data2Lib != null) {
                int len = data1Lib.getLength(o1);
                if (len != data2Lib.getLength(o2)) {
                    return false;
                }

                for (int i = 0; i < len; i++) {
                    Object el1 = data1Lib.getDataAtAsObject(o1, i);
                    Object el2 = data2Lib.getDataAtAsObject(o2, i);
                    if (!equals(el1, el2)) {
                        return false;
                    }
                }

                return true;
            } else {
                // if data lib field is set => length > 1 or it is a list
                return data1Lib == null && data2Lib == null && o1.equals(o2);
            }
        }

        private static boolean equals(Object o1In, Object o2In) {
            Object o1 = normalizeSingletonContainer(o1In);
            Object o2 = normalizeSingletonContainer(o2In);
            if ((o1 instanceof RAbstractContainer) != (o2 instanceof RAbstractContainer)) {
                return false;
            }
            VectorDataLibrary dataLib = null;
            if (o1 instanceof RAbstractContainer) {
                assert o2 instanceof RAbstractContainer;
                dataLib = VectorDataLibrary.getFactory().getUncached();
            }

            return equals(dataLib, o1, dataLib, o2);
        }

        private static Object normalizeSingletonContainer(Object element) {
            if (element instanceof RAbstractContainer && ((RAbstractContainer) element).getLength() == 1 && !(element instanceof RAbstractListBaseVector)) {
                return ((RAbstractContainer) element).getDataAtAsObject(0);
            }
            return element;
        }
    }
}
