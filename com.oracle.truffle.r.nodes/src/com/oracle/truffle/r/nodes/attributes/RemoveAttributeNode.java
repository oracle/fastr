/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RAttributes;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * Simple attribute access node that specializes on the position at which the attribute was found
 * last time.
 */
public abstract class RemoveAttributeNode extends RBaseNode {

    protected final String name;

    protected RemoveAttributeNode(String name) {
        this.name = name.intern();
    }

    public static RemoveAttributeNode create(String name) {
        return RemoveAttributeNodeGen.create(name);
    }

    public static RemoveAttributeNode createDim() {
        return RemoveAttributeNodeGen.create(RRuntime.DIM_ATTR_KEY);
    }

    public static RemoveAttributeNode createDimNames() {
        return RemoveAttributeNodeGen.create(RRuntime.DIMNAMES_ATTR_KEY);
    }

    public abstract void execute(RAttributes attr);

    protected boolean nameMatches(RAttributes attr, int index) {
        /*
         * The length check is against names.length instead of size, so that the check folds into
         * the array bounds check.
         */
        return index != -1 && attr.getNames().length > index && attr.getNames()[index] == name;
    }

    @Specialization(limit = "1", guards = "nameMatches(attr, index)")
    @ExplodeLoop
    protected void accessCached(RAttributes attr, //
                    @Cached("attr.find(name)") int index, //
                    @Cached("attr.size()") int cachedSize) {
        attr.setSize(cachedSize - 1);
        String[] names = attr.getNames();
        Object[] values = attr.getValues();
        for (int i = index + 1; i < cachedSize; i++) {
            names[i - 1] = names[i];
            values[i - 1] = values[i];
        }
        values[cachedSize - 1] = null;
    }

    @Specialization(limit = "1", guards = "cachedSize == attr.size()")
    @ExplodeLoop
    protected void accessCachedSize(RAttributes attr, //
                    @Cached("attr.size()") int cachedSize, //
                    @Cached("create()") BranchProfile foundProfile, //
                    @Cached("create()") BranchProfile notFoundProfile) {
        String[] names = attr.getNames();
        for (int i = 0; i < cachedSize; i++) {
            if (names[i] == name) {
                foundProfile.enter();
                removeAt(attr, i, cachedSize);
                return;
            }
        }
        notFoundProfile.enter();
    }

    private static void removeAt(RAttributes attr, int index, int cachedSize) {
        String[] names = attr.getNames();
        Object[] values = attr.getValues();
        for (int i = index + 1; i < cachedSize; i++) {
            names[i - 1] = names[i];
            values[i - 1] = values[i];
        }
        values[cachedSize - 1] = null;
    }

    @Specialization(contains = {"accessCached", "accessCachedSize"})
    protected void access(RAttributes attr) {
        attr.remove(name);
    }
}
