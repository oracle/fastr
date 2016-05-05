/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.runtime.nodes;

import java.util.HashSet;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;

/**
 * Code sharing vehicle for the slight differences in behavior between {@code duplicated} and
 * {@code anyDuplicated} (both internal and native versions) and whether {@code fromLast} is
 * {@code TRUE/FALSE}.
 */
public class DuplicationHelper {
    private final RAbstractContainer x;
    private final HashSet<Object> vectorContents = new HashSet<>();
    private final HashSet<Object> incompContents;
    private final byte[] dupVec;
    private int index;

    public DuplicationHelper(RAbstractContainer x, RAbstractContainer incomparables, boolean justIndex, boolean fromLast) {
        this.x = x;
        vectorContents.add(x.getDataAtAsObject(fromLast ? x.getLength() - 1 : 0));

        if (incomparables != null) {
            incompContents = new HashSet<>();
            for (int i = 0; i < incomparables.getLength(); i++) {
                incompContents.add(incomparables.getDataAtAsObject(i));
            }
        } else {
            incompContents = null;
        }
        dupVec = justIndex ? null : new byte[x.getLength()];
    }

    public boolean doIt(int i) {
        if (incompContents == null || !incompContents.contains(x.getDataAtAsObject(i))) {
            if (vectorContents.contains(x.getDataAtAsObject(i))) {
                if (dupVec == null) {
                    index = i + 1;
                    return true;
                } else {
                    dupVec[i] = RRuntime.LOGICAL_TRUE;
                }
            } else {
                vectorContents.add(x.getDataAtAsObject(i));
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

}
