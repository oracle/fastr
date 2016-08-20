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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.runtime.data.RAttributes;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * Simple attribute access node, for a specific attributes, that specializes on the position at
 * which the attribute was found last time.
 */
public abstract class AttributeAccess extends RBaseNode {

    public static final int MAX_SIZE_BOUND = 10;

    protected final String name;
    @CompilationFinal private int maximumSize = 2;

    protected AttributeAccess(String name) {
        this.name = name.intern();
    }

    public static AttributeAccess create(String name) {
        return AttributeAccessNodeGen.create(name);
    }

    public abstract Object execute(RAttributes attr);

    protected boolean nameMatches(RAttributes attr, int index) {
        /*
         * The length check is against names.length instead of size, so that the check folds into
         * the array bounds check.
         */
        return index != -1 && attr.size() > index && attr.getNameAtIndex(index) == name;
    }

    @Specialization(limit = "1", guards = "nameMatches(attr, index)")
    protected Object accessCached(RAttributes attr, //
                    @Cached("attr.find(name)") int index) {
        return attr.getValueAtIndex(index);
    }

    @Specialization(limit = "1", guards = "cachedSize == attr.size()", contains = "accessCached")
    @ExplodeLoop
    protected Object accessCachedSize(RAttributes attr, //
                    @Cached("attr.size()") int cachedSize, //
                    @Cached("create()") BranchProfile foundProfile, //
                    @Cached("create()") BranchProfile notFoundProfile) {
        for (int i = 0; i < cachedSize; i++) {
            if (attr.getNameAtIndex(i) == name) {
                foundProfile.enter();
                return attr.getValueAtIndex(i);
            }
        }
        notFoundProfile.enter();
        return null;
    }

    @Specialization(contains = {"accessCached", "accessCachedSize"}, guards = "attr.size() <= MAX_SIZE_BOUND")
    @ExplodeLoop
    protected Object accessCachedMaximumSize(RAttributes attr, //
                    @Cached("create()") BranchProfile foundProfile, //
                    @Cached("create()") BranchProfile notFoundProfile) {
        int size = attr.size();
        if (size > maximumSize) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            maximumSize = size;
        }
        for (int i = 0; i < maximumSize; i++) {
            if (i >= size) {
                break;
            }
            if (attr.getNameAtIndex(i) == name) {
                foundProfile.enter();
                return attr.getValueAtIndex(i);
            }
        }
        notFoundProfile.enter();
        return null;
    }

    @Specialization(contains = {"accessCached", "accessCachedSize", "accessCachedMaximumSize"})
    @TruffleBoundary
    protected Object access(RAttributes attr) {
        return attr.get(name);
    }
}
