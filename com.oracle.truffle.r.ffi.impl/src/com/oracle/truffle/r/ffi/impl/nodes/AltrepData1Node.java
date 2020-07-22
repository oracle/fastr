/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.ffi.impl.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RPairListLibrary;
import com.oracle.truffle.r.runtime.data.altrep.AltrepUtilities;
import com.oracle.truffle.r.runtime.data.model.RAbstractAtomicVector;

@GenerateUncached
@ImportStatic({AltrepUtilities.class, DSLConfig.class})
public abstract class AltrepData1Node extends FFIUpCallNode.Arg1 {
    public static AltrepData1Node create() {
        return AltrepData1NodeGen.create();
    }

    @Specialization(guards = "altrepVec == cachedAltrepVec", limit = "getGenericDataLibraryCacheSize()")
    public Object getData1FromAltrepCached(@SuppressWarnings("unused") RAbstractAtomicVector altrepVec,
                    @Cached("altrepVec") @SuppressWarnings("unused") RAbstractAtomicVector cachedAltrepVec,
                    @Cached("getPairListData(altrepVec)") RPairList pairListData,
                    @CachedLibrary("pairListData") RPairListLibrary pairListLibrary) {
        return pairListLibrary.car(pairListData);
    }

    @Specialization(replaces = "getData1FromAltrepCached")
    public Object getData1FromAltrepUncached(RAbstractAtomicVector altrepVec) {
        RPairList pairListData = AltrepUtilities.getPairListData(altrepVec);
        return pairListData.car();
    }

    @Fallback
    public Object fallback(Object object) {
        throw RInternalError.shouldNotReachHere("Unknown type " + object.getClass().getSimpleName() + " for R_altrep_data1");
    }
}
