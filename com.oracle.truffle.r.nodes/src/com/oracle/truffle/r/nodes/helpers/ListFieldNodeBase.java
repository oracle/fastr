/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.helpers;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetNamesAttributeNode;
import com.oracle.truffle.r.runtime.data.RStringVector;

/**
 * Common code shared between nodes accessing/updating list fields by name.
 */
public abstract class ListFieldNodeBase extends Node {
    @Child protected GetNamesAttributeNode getNamesNode = GetNamesAttributeNode.create();

    protected static int getIndex(RStringVector names, String field) {
        if (names != null) {
            int fieldHash = field.hashCode();
            for (int i = 0; i < names.getLength(); i++) {
                String current = names.getDataAt(i);
                if (current == field || hashCodeEquals(current, fieldHash) && contentsEquals(current, field)) {
                    return i;
                }
            }
        }
        return -1;
    }

    @TruffleBoundary
    private static boolean contentsEquals(String current, String field) {
        return field.equals(current);
    }

    @TruffleBoundary
    private static boolean hashCodeEquals(String current, int fieldHash) {
        return current.hashCode() == fieldHash;
    }
}
