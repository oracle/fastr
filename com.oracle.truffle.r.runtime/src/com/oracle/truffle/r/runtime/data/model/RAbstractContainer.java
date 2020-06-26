/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data.model;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.library.ExportMessage.Ignore;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.AbstractContainerLibrary;
import com.oracle.truffle.r.runtime.data.InternalDeprecation;
import com.oracle.truffle.r.runtime.data.RAttributesLayout;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RSharingAttributeStorage;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;

import java.util.concurrent.atomic.AtomicReference;

@ExportLibrary(AbstractContainerLibrary.class)
public abstract class RAbstractContainer extends RSharingAttributeStorage {

    public static final String DATA_LIB_LIMIT = "getDataLibCacheSize()";

    public static int getDataLibCacheSize() {
        // this has to be a method, because DSLConfig gets initialized lazily
        return DSLConfig.getCacheSize(3);
    }

    protected Object data;

    public RAbstractContainer() {
        this.data = this;
    }

    public final Object getData() {
        return data;
    }

    @Ignore
    public abstract boolean isComplete();

    @Ignore
    public abstract int getLength();

    public abstract void setLength(int l);

    public abstract int getTrueLength();

    public abstract void setTrueLength(int l);

    public abstract RAbstractContainer resize(int size);

    public abstract boolean hasDimensions();

    public abstract int[] getDimensions();

    public abstract void setDimensions(int[] newDimensions);

    @Ignore
    public abstract RAbstractContainer materialize();

    public abstract Object getDataAtAsObject(int index);

    /**
     * Copies attributes from this container to the given one. It is a verbatim copy without any
     * checks and it preserves the order of the attributes.
     */
    @InternalDeprecation("This should be rewritten to a node. Unlike with UnaryCopyAttributesNode, the semantics of this method is that it preserves the shape, which means that it preserves the order of attributes.")
    public final void setAttributes(RAbstractContainer result) {
        if (this.attributes != null) {
            result.initAttributes(RAttributesLayout.copy(this.attributes));
        }
    }

    @Ignore
    @Override
    public abstract RAbstractContainer copy();

    /**
     * Note: elements inside lists may be in inconsistent state reference counting wise. You may
     * need to put them into consistent state depending on what you use them for, consult the
     * documentation of {@code ExtractListElement}.
     */
    public Object getDataAtAsObject(@SuppressWarnings("unused") Object store, int index) {
        return getDataAtAsObject(index);
    }

    /**
     * Returns an object that could be passed to {@link #getDataAtAsObject(Object, int)} or type
     * specialized versions in concrete vector types. The {@code store} object should contain data
     * necessary for the vector to perform {@link #getDataAtAsObject(Object, int)} and similar
     * methods without any field loads. If {@code store} is saved into a local variable, then the
     * {@code getDataAsObject} overloads with {@code store} parameter do not have to load the
     * vector's fields, but instead read the necessary data from a local variable, which could be
     * beneficial when in loop.
     */
    public abstract Object getInternalStore();

    public RStringVector getNames() {
        CompilerAsserts.neverPartOfCompilation();
        return (RStringVector) getAttr(RRuntime.NAMES_ATTR_KEY);
    }

    /**
     * Sets names for the container, or removes them in case that <code>newNames</code> is
     * <code>null</code>.
     *
     * @param newNames
     */
    public void setNames(RStringVector newNames) {
        CompilerAsserts.neverPartOfCompilation();
        setAttr(RRuntime.NAMES_ATTR_KEY, newNames);
    }

    public RList getDimNames() {
        CompilerAsserts.neverPartOfCompilation();
        return (RList) getAttr(RRuntime.DIMNAMES_ATTR_KEY);
    }

    public void setDimNames(RList newDimNames) {
        CompilerAsserts.neverPartOfCompilation();
        setAttr(RRuntime.DIMNAMES_ATTR_KEY, newDimNames);
    }

    public Object getRowNames() {
        CompilerAsserts.neverPartOfCompilation();
        return getAttr(RRuntime.ROWNAMES_ATTR_KEY);
    }

    public void setRowNames(RAbstractVector rowNames) {
        CompilerAsserts.neverPartOfCompilation();
        setAttr(RRuntime.ROWNAMES_ATTR_KEY, rowNames);
    }

    public abstract VectorAccess access();

    public abstract VectorAccess slowPathAccess();

    // ------------------------------
    // AbstractContainerLibrary

    @ExportMessage(name = "getType", library = AbstractContainerLibrary.class)
    public RType containerLibGetType(@Shared("rtypeProfile") @Cached("createIdentityProfile()") ValueProfile typeProfile) {
        return typeProfile.profile(getRType());
    }

    @ExportMessage(name = "createEmptySameType", library = AbstractContainerLibrary.class)
    public RAbstractVector containerLibCreateEmptySameType(int newLength, boolean fillWithNA,
                    @Cached("createEqualityProfile()") ValueProfile fillWithNAProfile,
                    @Shared("rtypeProfile") @Cached("createIdentityProfile()") ValueProfile typeProfile) {
        assert this instanceof RAbstractVector;
        return typeProfile.profile(getRType()).create(newLength, fillWithNAProfile.profile(fillWithNA));
    }

    @ExportMessage(name = "isMaterialized", library = AbstractContainerLibrary.class)
    public boolean containerLibIsMaterialized(@CachedLibrary(limit = DATA_LIB_LIMIT) VectorDataLibrary dataLib) {
        return dataLib.isWriteable(data);
    }

    @ExportMessage(name = "getLength", library = AbstractContainerLibrary.class)
    public int containerLibGetLength(@CachedLibrary(limit = DATA_LIB_LIMIT) VectorDataLibrary dataLib) {
        return dataLib.getLength(data);
    }

    @ExportMessage(name = "isComplete", library = AbstractContainerLibrary.class)
    public boolean containerLibIsComplete(@CachedLibrary(limit = DATA_LIB_LIMIT) VectorDataLibrary dataLib) {
        return dataLib.isComplete(data);
    }

    private final AtomicReference<RAbstractContainer> materialized = new AtomicReference<>();

    @ExportMessage(name = "cachedMaterialize", library = AbstractContainerLibrary.class)
    public RAbstractContainer containerLibCachedMaterialize(@CachedLibrary("this") AbstractContainerLibrary containerLibrary) {
        if (materialized.get() == null) {
            materialized.compareAndSet(null, containerLibrary.materialize(this));
        }
        return materialized.get();
    }

    @ExportMessage(name = "materialize", library = AbstractContainerLibrary.class)
    public RAbstractContainer containerLibMaterialize() {
        return materialize();
    }

    @ExportMessage(name = "copy", library = AbstractContainerLibrary.class)
    public RAbstractContainer containerLibCopy() {
        return copy();
    }

    @ExportMessage(name = "toNative", library = AbstractContainerLibrary.class)
    public void containerLibToNativeDummyImpl() {
        throw RInternalError.shouldNotReachHere(this.getClass().getSimpleName());
    }
}
