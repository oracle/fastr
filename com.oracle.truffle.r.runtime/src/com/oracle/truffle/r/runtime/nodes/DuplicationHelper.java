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
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;

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
            if (vectorContents.contains(entry)) {
                if (dupVec == null) {
                    index = i + 1;
                    return true;
                } else {
                    dupVec[i] = RRuntime.LOGICAL_TRUE;
                }
            } else {
                vectorContents.add(entry);
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

        DupEntry(Object element) {
            this.element = element;
        }

        @Override
        public int hashCode() {
            if (element instanceof RAbstractContainer && !(element instanceof RComplex)) {
                return ((RAbstractContainer) element).getRType().hashCode();
            } else {
                return element.hashCode();
            }
        }

        @Override
        public boolean equals(Object o) {
            Object other = o;
            if (other instanceof DupEntry) {
                other = ((DupEntry) other).element;
            }
            return equals(this.element, other);
        }

        static boolean equals(Object o1, Object o2) {
            if (o1 instanceof RAbstractContainer && o2 instanceof RAbstractContainer) {
                RAbstractContainer cont1 = (RAbstractContainer) o1;
                RAbstractContainer cont2 = (RAbstractContainer) o2;

                if (cont1.getLength() != cont2.getLength()) {
                    return false;
                }

                for (int i = 0; i < cont1.getLength(); i++) {
                    Object el1 = cont1.getDataAtAsObject(i);
                    Object el2 = cont2.getDataAtAsObject(i);
                    if (!equals(el1, el2)) {
                        return false;
                    }
                }

                return true;

            } else {
                return o1.equals(o2);
            }
        }
    }
}
