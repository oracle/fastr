/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessWriteIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqWriteIterator;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

@ExportLibrary(value = VectorDataLibrary.class, receiverType = Object[].class)
public class RListArrayDataLibrary {
    @ExportMessage
    public static NACheck getNACheck(@SuppressWarnings("unused") Object[] receiver) {
        return NACheck.getEnabled();
    }

    @ExportMessage
    public static RType getType(@SuppressWarnings("unused") Object[] receiver) {
        return RType.List;
    }

    @ExportMessage
    public static int getLength(Object[] receiver) {
        return receiver.length;
    }

    @ExportMessage
    public static Object[] materialize(Object[] receiver) {
        return receiver;
    }

    @ExportMessage
    public static boolean isWriteable(@SuppressWarnings("unused") Object[] receiver) {
        return true;
    }

    @ExportMessage
    public static class Copy {
        @Specialization(guards = "!deep")
        public static Object[] copyShallow(Object[] receiver, @SuppressWarnings("unused") boolean deep) {
            return getListDataCopy(receiver);
        }

        @Specialization(guards = {"deep", "length(receiver) <= 3"})
        public static Object[] copyWithDispatchedLibrary(Object[] receiver, @SuppressWarnings("unused") boolean deep,
                        @CachedLibrary(limit = "3") AbstractContainerLibrary containerLib) {
            Object[] result = new Object[receiver.length];
            for (int i = 0; i < receiver.length; i++) {
                result[i] = deepCopyElement(receiver[i], containerLib);
            }
            return result;
        }

        @Specialization(guards = {"deep", "length(receiver) > 3"})
        public static Object[] copyUncached(Object[] receiver, @SuppressWarnings("unused") boolean deep) {
            AbstractContainerLibrary containerLib = AbstractContainerLibrary.getFactory().getUncached();
            Object[] result = new Object[receiver.length];
            for (int i = 0; i < receiver.length; i++) {
                result[i] = deepCopyElement(receiver[i], containerLib);
            }
            return result;
        }

        private static Object deepCopyElement(Object element, AbstractContainerLibrary containerLib) {
            if (element instanceof RAbstractContainer) {
                return containerLib.duplicate(element, true);
            } else if (element instanceof RSharingAttributeStorage) {
                // Handle more general and slower case for RS4Object or RFunction.
                return ((RSharingAttributeStorage) element).deepCopy();
            } else {
                return element;
            }
        }

        protected static int length(Object[] objects) {
            return objects.length;
        }
    }

    @ExportMessage
    public static Object[] getListDataCopy(Object[] receiver) {
        Object[] result = new Object[receiver.length];
        System.arraycopy(receiver, 0, result, 0, receiver.length);
        return result;
    }

    // Read access to the elements:
    // TODO: actually use the store in the iterator, which should be just the "address" (Long) or
    // maybe add a specialized field to the iterator

    @ExportMessage
    public static SeqIterator iterator(Object[] receiver,
                    @Shared("SeqItLoopProfile") @Cached("createCountingProfile()") LoopConditionProfile loopProfile) {
        SeqIterator it = new SeqIterator(receiver, receiver.length);
        it.initLoopConditionProfile(loopProfile);
        return it;
    }

    @ExportMessage
    public static boolean nextImpl(@SuppressWarnings("unused") Object[] receiver, SeqIterator it, boolean loopCondition,
                    @Shared("SeqItLoopProfile") @Cached("createCountingProfile()") LoopConditionProfile loopProfile) {
        return it.next(loopCondition, loopProfile);
    }

    @ExportMessage
    public static void nextWithWrap(@SuppressWarnings("unused") Object[] receiver, SeqIterator it,
                    @Cached("createBinaryProfile()") ConditionProfile wrapProfile) {
        it.nextWithWrap(wrapProfile);
    }

    @ExportMessage
    public static RandomAccessIterator randomAccessIterator(Object[] receiver) {
        return new RandomAccessIterator(receiver);
    }

    @ExportMessage
    public static Object getElementAt(Object[] receiver, int index) {
        return receiver[index];
    }

    @ExportMessage
    public static Object getNextElement(Object[] receiver, SeqIterator it) {
        return receiver[it.getIndex()];
    }

    @ExportMessage
    public static Object getElement(Object[] receiver, @SuppressWarnings("unused") RandomAccessIterator it, int index) {
        return receiver[index];
    }

    // Write access to the elements:

    @ExportMessage
    public static SeqWriteIterator writeIterator(Object[] receiver) {
        return new SeqWriteIterator(receiver, receiver.length);
    }

    @ExportMessage
    public static RandomAccessWriteIterator randomAccessWriteIterator(@SuppressWarnings("unused") Object[] receiver) {
        return new RandomAccessWriteIterator(null);
    }

    @ExportMessage
    public static void setElementAt(Object[] receiver, int index, Object value) {
        receiver[index] = value;
    }

    @ExportMessage
    public static void setNextElement(Object[] receiver, SeqWriteIterator it, Object value) {
        receiver[it.getIndex()] = value;
    }

    @ExportMessage
    public static void setElement(Object[] receiver, @SuppressWarnings("unused") RandomAccessWriteIterator it, int index, Object value) {
        receiver[index] = value;
    }
}
