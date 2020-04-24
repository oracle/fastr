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
package com.oracle.truffle.r.runtime.data;

import com.oracle.truffle.api.library.GenerateLibrary;
import com.oracle.truffle.api.library.Library;
import com.oracle.truffle.api.library.LibraryFactory;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

@GenerateLibrary
public abstract class AbstractContainerLibrary extends Library {

    static final LibraryFactory<AbstractContainerLibrary> FACTORY = LibraryFactory.resolve(AbstractContainerLibrary.class);

    public static LibraryFactory<AbstractContainerLibrary> getFactory() {
        return FACTORY;
    }

    public abstract RType getType(Object container);

    public int getLength(Object container) {
        throw RInternalError.shouldNotReachHere(container.getClass().getSimpleName());
    }

    /**
     * If this method returns {@code true}, then it is guaranteed that this data does not contain
     * any {@code NA} value. If this method returns {@code false}, then this data may or may not
     * contain {@code NA} values.
     */
    public boolean isComplete(Object container) {
        throw RInternalError.shouldNotReachHere(container.getClass().getSimpleName());
    }

    /**
     * Creates an empty vector of the same type and of the given length. Defined only for
     * {@link com.oracle.truffle.r.runtime.data.model.RAbstractVector} subtypes.
     *
     * The completeness flag is guaranteed to be set to {@code false} if the {@code fillWithNA} is
     * {@code true} and the new length is not zero.
     */
    public RAbstractVector createEmptySameType(Object container, @SuppressWarnings("unused") int newLength, @SuppressWarnings("unused") boolean fillWithNA) {
        throw RInternalError.unimplemented("TODO:" + container.getClass().getSimpleName());
    }

    /**
     * Transforms this vector into another that is writeable (both data and attributes). This is
     * deprecated legacy method, use {@link #materializeData(Object)} instead.
     */
    public abstract RAbstractContainer materialize(Object container);

    /**
     * @see #materialize(Object)
     */
    public abstract boolean isMaterialized(Object container);

    /**
     * After this operation is performed the vector must be able to handle operations that write
     * into the vector data.
     */
    public void materializeData(@SuppressWarnings("unused") Object container) {
        throw RInternalError.unimplemented("TODO:" + container.getClass().getSimpleName());
    }

    /**
     * Creates a copy of the container. Note that attributes are not copied, use dedicated node
     * {@link com.oracle.truffle.r.runtime.data.nodes.CopyWithAttributes} in such case.
     */
    public abstract RAbstractContainer copy(Object container);
}
