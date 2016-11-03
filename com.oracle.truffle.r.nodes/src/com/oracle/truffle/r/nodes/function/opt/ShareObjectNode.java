/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.api.nodes.NodeCost.NONE;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.EmptyTypeSystemFlatLayout;
import com.oracle.truffle.r.runtime.data.RShareable;

/**
 * Internal node that should be used whenever you need to increment reference count of some object.
 * If the object is not instance of {@link RShareable} or if it is shared permanent, then does
 * nothing.
 */
@TypeSystemReference(EmptyTypeSystemFlatLayout.class)
@NodeInfo(cost = NONE)
public abstract class ShareObjectNode extends Node {
    public abstract Object execute(Object obj);

    public static ShareObjectNode create() {
        return ShareObjectNodeGen.create();
    }

    @Specialization
    protected Object doShareable(RShareable obj,
                    @Cached("createBinaryProfile()") ConditionProfile sharedPermanent) {
        if (sharedPermanent.profile(!obj.isSharedPermanent())) {
            obj.incRefCount();
        }
        return obj;
    }

    @Specialization(guards = "!isRShareable(obj)")
    protected Object doNonShareable(Object obj) {
        return obj;
    }

    protected static boolean isRShareable(Object value) {
        return value instanceof RShareable;
    }

    public static <T> T share(T value) {
        if (value instanceof RShareable) {
            RShareable shareable = (RShareable) value;
            if (!shareable.isSharedPermanent()) {
                shareable.incRefCount();
            }
        }
        return value;
    }

    public static <T> T sharePermanent(T value) {
        if (value instanceof RShareable) {
            ((RShareable) value).makeSharedPermanent();
        }
        return value;
    }

    public static void unshare(Object value) {
        if (value instanceof RShareable) {
            RShareable shareable = (RShareable) value;
            if (!shareable.isSharedPermanent()) {
                shareable.decRefCount();
            }
        }
    }
}
