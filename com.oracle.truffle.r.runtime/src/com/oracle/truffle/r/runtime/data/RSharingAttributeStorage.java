/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data;

/**
 * An adaptor class for the several R types that are both attributable and shareable.
 * 
 * <pre>
 * Reference counting on vectors:
 * A vector can have three states: temporary, non-shared, shared
 * Operations with copy plus escape semantics (e.g., local variable assignment):
 * - temporary => non-shared
 * - non-shared => temporary copy
 * Operations with copy and non-escaping semantics (e.g., argument)
 * - temporary => temporary
 * - non-shared => shared
 * </pre>
 */
public abstract class RSharingAttributeStorage extends RAttributable {

    // SHARED_PERMANENT_VAL describes both overflow value and a value that can be set to prevent
    // further updates to ref count (for sharing between different threads) - can potentially be
    // made smaller
    // TODO: a better placement for this constant?
    public static final int SHARED_PERMANENT_VAL = Integer.MAX_VALUE;

    /**
     * Tagging interface. <br/>
     * TODO for the time being: RScalarVector, RSequence and RForeignWrapper types are now instances
     * of RSharingAttributeStorage, so shareable handling could be managed w/o materialization, but
     * has to be checked first; for now lets stay with the assumption that non-materialized vectors
     * aren't shareable.
     */
    public interface Shareable {

    }

    public final boolean isShareable() {
        return this instanceof Shareable;
    }

    public RSharingAttributeStorage deepCopy() {
        return copy();
    }

    private int refCount;

    public final boolean isTemporary() {
        return refCount == 0;
    }

    public final boolean isShared() {
        return refCount > 1;
    }

    /**
     * It is invalid to invoke this method on 'shared permanent' shareables.
     */
    public final void incRefCount() {
        assert refCount != SHARED_PERMANENT_VAL : "cannot incRefCount of shared permanent value";
        refCount++;
    }

    /**
     * It is invalid to invoke this method on 'temporary' shareables.
     */
    public final void decRefCount() {
        assert refCount != SHARED_PERMANENT_VAL : "cannot decRefCount of shared permanent value";
        assert refCount > 0 : "cannot decRefCount when refCount <= 0";
        refCount--;
    }

    public final boolean isSharedPermanent() {
        return refCount == SHARED_PERMANENT_VAL;
    }

    /**
     * Turns off reference counting for this object.
     * 
     * @return {@code this}
     */
    public final RSharingAttributeStorage makeSharedPermanent() {
        refCount = SHARED_PERMANENT_VAL;
        return this;
    }

    /**
     * In order to support some hacks of some packages done via C API.
     * 
     * @return {@code this}
     */
    public RSharingAttributeStorage makeTemporary() {
        refCount = 0;
        return this;
    }

    public RBaseObject getNonShared() {
        if (isShared()) {
            RSharingAttributeStorage res = copy();
            assert res.isTemporary();
            res.incRefCount();
            return res;
        }
        if (isTemporary()) {
            incRefCount();
        }
        return this;
    }

    public abstract RSharingAttributeStorage copy();

    public static boolean isShareable(Object o) {
        return o instanceof RSharingAttributeStorage && ((RSharingAttributeStorage) o).isShareable();
    }
}
