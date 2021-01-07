/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data.nodes;

import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqIterator;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * Resizes a string vector to new length filling the extra space with the empty string value.
 */
@GenerateUncached
public abstract class CopyResizedNamesWithEmpty extends RBaseNode {

    public static CopyResizedNamesWithEmpty create() {
        return CopyResizedNamesWithEmptyNodeGen.create();
    }

    public static CopyResizedNamesWithEmpty getUncached() {
        return CopyResizedNamesWithEmptyNodeGen.getUncached();
    }

    public static RStringVector executeSlowPath(RStringVector container, int newSize) {
        return CopyResizedNamesWithEmptyNodeGen.getUncached().execute(container, newSize);
    }

    public abstract RStringVector execute(RStringVector container, int newSize);

    @Specialization(limit = "getGenericVectorAccessCacheSize()")
    RStringVector doIt(RStringVector x, int newSize,
                    @CachedLibrary("x.getData()") VectorDataLibrary xDataLib) {
        Object xData = x.getData();
        String[] result = new String[newSize];
        SeqIterator it = xDataLib.iterator(xData);
        while (xDataLib.next(xData, it) && it.getIndex() < result.length) {
            result[it.getIndex()] = xDataLib.getNextString(xData, it);
        }
        final int xLen = xDataLib.getLength(xData);
        for (int i = xLen; i < result.length; i++) {
            result[i] = RRuntime.NAMES_ATTR_EMPTY_VALUE;
        }
        return RDataFactory.createStringVector(result, xDataLib.isComplete(xData));
    }
}
