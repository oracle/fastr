/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.interop;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * Creates a list from a foreign array.
 * 
 */
@ImportStatic(ConvertForeignObjectNode.class)
abstract class ForeignArrayToListNode extends RBaseNode {

    @Child protected Foreign2R foreign2R;
    @Child protected ForeignArrayToListNode recurse;

    protected abstract Object execute(Object obj, boolean recursive, RType type);

    public static ForeignArrayToListNode create() {
        return ForeignArrayToListNodeGen.create();
    }

    /**
     * Creates a list from a foreign array. In case of multi-dimensional arrays, the resulting list
     * will be composed of vectors or lists representing a particular sub-array. Whenever all
     * elements in an array are convertible to the same atomic R type (logical, double, integer,
     * character), then they will be returned in an atomic vector of the according type.
     */
    RAbstractVector toList(TruffleObject obj, boolean recursive) {
        return (RAbstractVector) execute(obj, recursive, RType.List);
    }

    @Specialization(guards = {"isForeignArray(obj, interop)"}, limit = "getInteropLibraryCacheSize()")
    protected Object toList(TruffleObject obj, boolean recursive, RType type,
                    @CachedLibrary("obj") InteropLibrary interop,
                    @CachedLibrary(limit = "getInteropLibraryCacheSize()") InteropLibrary elementInterop) {
        try {

            int size = RRuntime.getForeignArraySize(obj, interop);
            if (size == 0) {
                return RDataFactory.createList();
            }

            Object[] currentElements = new Object[size];
            ForeignTypeCheck currentTypeCheck = new ForeignTypeCheck();
            for (int i = 0; i < size; i++) {
                Object element = elementInterop.readArrayElement(obj, i);
                element = getForeign2R().convert(element);
                currentTypeCheck.check(element);
                if (recursive && (RRuntime.isForeignObject(element))) {
                    currentElements[i] = recurse(element, null);
                } else {
                    currentElements[i] = element;
                }
            }
            return ConvertForeignObjectNode.asAbstractVector(currentElements, type != null ? type : currentTypeCheck.getType());
        } catch (UnsupportedMessageException | InvalidArrayIndexException e) {
            CompilerDirectives.transferToInterpreter();
            throw error(RError.Message.GENERIC, "error while converting array: " + e.getMessage());
        }
    }

    private Object recurse(Object element, RType type) {
        if (recurse == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            recurse = insert(create());
        }
        return recurse.execute(element, true, type);
    }

    @Fallback
    public Object fallback(Object obj, @SuppressWarnings("unused") boolean recursive, @SuppressWarnings("unused") RType type) {
        return obj;
    }

    protected Foreign2R getForeign2R() {
        if (foreign2R == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            foreign2R = insert(Foreign2RNodeGen.create());
        }
        return foreign2R;
    }
}
