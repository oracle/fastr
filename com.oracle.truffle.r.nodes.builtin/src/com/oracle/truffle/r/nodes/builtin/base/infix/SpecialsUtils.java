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
package com.oracle.truffle.r.nodes.builtin.base.infix;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.r.nodes.function.ClassHierarchyNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RNode;

/**
 * Helper methods for implementing special calls.
 *
 * @see com.oracle.truffle.r.runtime.builtins.RSpecialFactory
 */
class SpecialsUtils {
    private static final String valueArgName = "value".intern();

    public static boolean isCorrectUpdateSignature(ArgumentsSignature signature) {
        return signature.getLength() == 3 && signature.getName(0) == null && signature.getName(1) == null && signature.getName(2) == valueArgName;
    }

    /**
     * Common code shared between specials doing subset/subscript related operation.
     */
    abstract static class SubscriptSpecialCommon extends RNode {

        protected static boolean isValidIndex(RAbstractVector vector, int index) {
            return index >= 1 && index <= vector.getLength();
        }

        protected boolean isValidDoubleIndex(RAbstractVector vector, double index) {
            return isValidIndex(vector, toIndex(index));
        }

        /**
         * Note: conversion from double to an index differs in subscript and subset.
         */
        protected int toIndex(double index) {
            if (index == 0) {
                return 0;
            }
            int i = (int) index;
            return i == 0 ? 1 : i;
        }

        protected static int toIndexSubset(double index) {
            return index == 0 ? 0 : (int) index;
        }
    }

    /**
     * Common code shared between specials accessing/updating fields.
     */
    abstract static class ListFieldSpecialBase extends RNode {
        @CompilationFinal private String cachedField;
        @CompilationFinal private RStringVector cachedNames;
        @Child private ClassHierarchyNode hierarchyNode = ClassHierarchyNode.create();

        protected final void updateCache(RList list, String field) {
            if (cachedField == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                cachedField = field;
                cachedNames = list.getNames();
            }
        }

        protected final boolean isSimpleList(RList list) {
            return hierarchyNode.execute(list) == null;
        }

        protected final boolean isCached(RList list, String field) {
            return cachedField == null || (cachedField == field && list.getNames() == cachedNames);
        }

        protected final int getIndex(RAbstractStringVector names, String field) {
            for (int i = 0; i < names.getLength(); i++) {
                String current = names.getDataAt(i);
                if (current == field || current.equals(field)) {
                    return i;
                }
            }
            return -1;
        }
    }
}
