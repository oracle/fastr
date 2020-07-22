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
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractAtomicVector;
import com.oracle.truffle.r.runtime.ffi.RObjectDataPtr;

@GenerateUncached
public abstract class REALNode extends FFIUpCallNode.Arg1 {

    public static REALNode create() {
        return REALNodeGen.create();
    }

    @Specialization
    protected RObjectDataPtr doForNull(RNull rNull,
                    @Cached.Shared("getObjectDataPtrNode") @Cached RObjectDataPtr.GetObjectDataPtrNode getObjectDataPtrNode) {
        return getObjectDataPtrNode.execute(rNull);
    }

    @Specialization
    protected RObjectDataPtr doForStringVector(RStringVector stringVector,
                    @Cached ConditionProfile isNativized,
                    @Cached ConditionProfile needsWrapping,
                    @Cached.Shared("getObjectDataPtrNode") @Cached RObjectDataPtr.GetObjectDataPtrNode getObjectDataPtrNode) {
        stringVector.wrapStrings(isNativized, needsWrapping);
        return getObjectDataPtrNode.execute(stringVector);
    }

    @Specialization
    protected RObjectDataPtr doForAtomicVector(RAbstractAtomicVector atomicVector,
                    @Cached.Shared("getObjectDataPtrNode") @Cached RObjectDataPtr.GetObjectDataPtrNode getObjectDataPtrNode) {
        return getObjectDataPtrNode.execute(atomicVector);
    }
}
