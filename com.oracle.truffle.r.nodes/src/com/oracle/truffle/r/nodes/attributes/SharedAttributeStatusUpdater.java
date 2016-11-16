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
package com.oracle.truffle.r.nodes.attributes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.runtime.data.RShareable;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

public abstract class SharedAttributeStatusUpdater extends RBaseNode {

    protected SharedAttributeStatusUpdater() {
    }

    public abstract void execute(Object owner, Object attrValue);

    public final <T> T updateState(Object owner, T item) {
        execute(owner, item);
        return item;
    }

    public static SharedAttributeStatusUpdater create() {
        return SharedAttributeStatusUpdaterNodeGen.create();
    }

    @Specialization
    protected void doShareableValues(RShareable owner, RShareable value,
                    @Cached("createClassProfile()") ValueProfile valueProfile,
                    @Cached("createBinaryProfile()") ConditionProfile sharedValue,
                    @Cached("createBinaryProfile()") ConditionProfile temporaryOwner) {
        RShareable profiledValue = valueProfile.profile(value);
        if (sharedValue.profile(profiledValue.isShared())) {
            // it is already shared, not need to do anything
            return;
        }

        if (temporaryOwner.profile(owner.isTemporary())) {
            return;
        }

        if (profiledValue.isTemporary()) {
            // make it at least non-shared (the owner must be also at least non-shared)
            profiledValue.incRefCount();
        }
        if (owner.isShared()) {
            // owner is shared, make the attribute value shared too
            profiledValue.incRefCount();
        }
    }

    @Specialization(guards = "isFallback(owner, value)")
    @SuppressWarnings("unused")
    protected void doFallback(Object owner, Object value) {
    }

    protected static boolean isFallback(Object owner, Object value) {
        return !(value instanceof RShareable) || !(owner instanceof RShareable);
    }

}
