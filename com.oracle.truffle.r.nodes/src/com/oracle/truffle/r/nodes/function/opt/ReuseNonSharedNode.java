/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.function.opt;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.data.RShareable;
import com.oracle.truffle.r.runtime.data.RSharingAttributeStorage;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

/**
 * Internal node that should be used whenever you want to alter some data: if the data is shared,
 * then it creates a copy, otherwise it returns the data. It does not increment the reference count
 * of the result in either case, but that is typically handled by write variable node, put container
 * element node or by put attribute node.
 *
 * This node also makes sure that the returned value is an actual RVector in which individual
 * elements can be modified (as opposed to sequences, etc.) by calling materialize.
 */
public abstract class ReuseNonSharedNode extends Node {

    protected static final int LIMIT = 5;

    public static ReuseNonSharedNode create() {
        return ReuseNonSharedNodeGen.create();
    }

    public abstract RVector<?> execute(RAbstractVector value);

    @Specialization(limit = "LIMIT", guards = "value.getClass() == valueClass")
    protected RVector<?> reuseNonShareable(RAbstractVector value,
                    @Cached("value.getClass()") Class<? extends RAbstractVector> valueClass,
                    @Cached("createBinaryProfile()") ConditionProfile isSharedProfile) {
        RAbstractVector profiledValue = valueClass.cast(value);
        if (RShareable.class.isAssignableFrom(valueClass)) {
            RShareable shareable = (RShareable) profiledValue;
            if (isSharedProfile.profile(shareable.isShared())) {
                RShareable res = shareable.copy();
                assert res.isTemporary();
                return (RVector<?>) res;
            } else {
                return (RVector<?>) profiledValue;
            }
        } else {
            RVector<?> res = profiledValue.materialize();
            assert res.isTemporary();
            return res;
        }
    }

    @Specialization
    @TruffleBoundary
    public static RVector<?> reuseSlow(RAbstractVector value) {
        RSharingAttributeStorage.verify(value);
        if (value instanceof RSharingAttributeStorage) {
            RShareable shareable = (RShareable) value;
            if (shareable.isShared()) {
                RShareable res = shareable.copy();
                assert res.isTemporary();
                return (RVector<?>) res;
            } else {
                return (RVector<?>) value;
            }
        } else {
            RVector<?> res = value.materialize();
            assert res.isTemporary();
            return res;
        }
    }
}
