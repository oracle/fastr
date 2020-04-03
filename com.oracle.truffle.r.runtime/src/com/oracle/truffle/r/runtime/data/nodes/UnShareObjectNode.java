/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.data.RSharingAttributeStorage;

import static com.oracle.truffle.api.nodes.NodeCost.NONE;

/**
 * Internal node that should be used whenever you need to decrement reference count of some object.
 * If the object is not instance of {@link RSharingAttributeStorage} or if it is shared permanent,
 * then does nothing.
 *
 * This class relies (and asserts) that all RShareable objects are subclasses of
 * RSharingAttributeStorage.
 */
@NodeInfo(cost = NONE)
public abstract class UnShareObjectNode extends Node {

    public abstract Object execute(Object obj);

    public static UnShareObjectNode create() {
        return UnShareObjectNodeGen.create();
    }

    @Specialization(guards = "obj.isShareable()")
    protected Object doUnshareable(RSharingAttributeStorage obj,
                    @Cached("createBinaryProfile()") ConditionProfile shareTemporary) {
        if (shareTemporary.profile(!(obj.isTemporary()) && !(obj.isSharedPermanent()))) {
            obj.decRefCount();
        }
        return obj;
    }

    @Fallback
    protected Object doNonUnshareable(Object obj) {
        return obj;
    }

    public static void unshare(Object value) {
        if (RSharingAttributeStorage.isShareable(value)) {
            RSharingAttributeStorage shareable = (RSharingAttributeStorage) value;
            if (!shareable.isTemporary() && !shareable.isSharedPermanent()) {
                shareable.decRefCount();
            }
        }
    }

}
