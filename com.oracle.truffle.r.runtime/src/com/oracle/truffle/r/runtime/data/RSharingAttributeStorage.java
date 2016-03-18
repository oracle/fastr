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
package com.oracle.truffle.r.runtime.data;

import com.oracle.truffle.r.runtime.FastROptions;

/**
 * An adaptor class for the several R types that are both attributable and shareable.
 */
public abstract class RSharingAttributeStorage extends RAttributeStorage implements RShareable {

    private int refCount;

    private static final int TEMPORARY = 0x1;
    private static final int SHARED = 0x2;

    protected RSharingAttributeStorage() {
        if (!FastROptions.NewStateTransition.getBooleanValue()) {
            refCount = TEMPORARY;
        }
    }

    @Override
    public final void markNonTemporary() {
        assert !FastROptions.NewStateTransition.getBooleanValue();
        refCount &= ~TEMPORARY;
    }

    @Override
    public final boolean isTemporary() {
        if (FastROptions.NewStateTransition.getBooleanValue()) {
            return refCount == 0;
        } else {
            return (refCount & TEMPORARY) != 0;
        }
    }

    @Override
    public final boolean isShared() {
        if (FastROptions.NewStateTransition.getBooleanValue()) {
            return refCount > 1;
        } else {
            return (refCount & SHARED) != 0;
        }
    }

    @Override
    public final RSharingAttributeStorage makeShared() {
        assert !FastROptions.NewStateTransition.getBooleanValue();
        refCount = SHARED;
        return this;
    }

    @Override
    public final void incRefCount() {
        refCount++;
    }

    @Override
    public final void decRefCount() {
        assert refCount > 0;
        refCount--;
    }

    @Override
    public boolean isSharedPermanent() {
        return refCount == SHARED_PERMANENT_VAL;
    }

    @Override
    public void makeSharedPermanent() {
        if (FastROptions.NewStateTransition.getBooleanValue()) {
            refCount = SHARED_PERMANENT_VAL;
        } else {
            // old scheme never reverts states
            makeShared();
        }
    }

    @Override
    public RShareable getNonShared() {
        if (this.isShared()) {
            RShareable res = this.copy();
            if (FastROptions.NewStateTransition.getBooleanValue()) {
                assert res.isTemporary();
                res.incRefCount();
            } else {
                res.markNonTemporary();
            }
            return res;
        }
        if (this.isTemporary()) {
            // this is needed for primitive value coercion - they need to be marked as
            // non-temp, otherwise the following code will not work:
            // x<-1; attributes(x) <- list(my = 1); y<-x; attributes(y)<-list(his = 2); x
            if (FastROptions.NewStateTransition.getBooleanValue()) {
                this.incRefCount();
            } else {
                this.markNonTemporary();
            }
        }
        return this;
    }
}
