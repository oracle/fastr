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
package com.oracle.truffle.r.nodes.access.vector;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.access.vector.ExtractListElementNodeGen.UpdateStateOfListElementNodeGen;
import com.oracle.truffle.r.runtime.data.RListBase;
import com.oracle.truffle.r.runtime.data.RShareable;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

/**
 * Internal node that extracts data under given index from any RAbstractContainer. In the case of
 * RListBase, it also invokes {@link UpdateStateOfListElement} on the element before returning it.
 *
 * There are two reasons for why one accesses an element of a list: to peek at it, possibly
 * calculate some values from it, and then forget it. In such case, it is OK to access the element
 * directly through {@link RAbstractListVector#getDataAt(int)} or
 * {@link RAbstractContainer#getDataAtAsObject(int)}. However, if the object is going to be returned
 * to the user either as return value of a built-in, put inside a list, put as an attribute, or its
 * true reference count matters for some other reason, then its reference count must be put into a
 * consistent state, which is done by {@link UpdateStateOfListElement}. This node is a convenient
 * wrapper that performs the extraction as well as invocation of {@link UpdateStateOfListElement}.
 * See also the documentation of {@link RListBase}.
 */
public abstract class ExtractListElement extends Node {

    public abstract Object execute(RAbstractContainer container, int index);

    @Specialization
    protected Object doList(RListBase list, int index, @Cached("create()") UpdateStateOfListElement updateStateNode) {
        Object element = list.getDataAt(index);
        return updateStateNode.updateState(list, element);
    }

    @Specialization(guards = "isNotList(container)")
    protected Object doOthers(RAbstractContainer container, int index) {
        return container.getDataAtAsObject(index);
    }

    protected static boolean isNotList(RAbstractContainer x) {
        return !(x instanceof RAbstractListVector);
    }

    public abstract static class UpdateStateOfListElement extends Node {

        public abstract void execute(Object owner, Object item);

        /**
         * Provides more convenient interface for the {@link #execute(Object, Object)} method.
         */
        public final <T> T updateState(RAbstractContainer owner, T item) {
            execute(owner, item);
            return item;
        }

        public static UpdateStateOfListElement create() {
            return UpdateStateOfListElementNodeGen.create();
        }

        @Specialization
        protected void doShareableValues(RListBase owner, RShareable value, //
                        @Cached("createBinaryProfile()") ConditionProfile sharedValue, //
                        @Cached("createBinaryProfile()") ConditionProfile temporaryOwner) {
            if (sharedValue.profile(value.isShared())) {
                // it is already shared, not need to do anything
                return;
            }

            if (temporaryOwner.profile(owner.isTemporary())) {
                // This can happen, for example, when we immediately extract out of a temporary
                // list that was returned by a built-in, like: strsplit(...)[[1L]]. We do not need
                // to transition the element, it may stay temporary.
                return;
            }

            if (value.isTemporary()) {
                // make it at least non-shared (parent list must be also at least non-shared)
                value.incRefCount();
            }
            if (owner.isShared()) {
                // owner is shared, make the value shared too
                value.incRefCount();
            }
        }

        @Fallback
        protected void doFallback(Object owner, Object value) {
            assert !(value instanceof RShareable && owner instanceof RAbstractVector && !(owner instanceof RListBase)) : "RShareables can only live inside lists and no other vectors.";
            // nop: either value is not RShareable, or the owner is "list" like structure with
            // reference semantics (e.g. REnvironment)
        }
    }
}
