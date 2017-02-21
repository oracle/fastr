/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.function.ClassHierarchyNode;
import com.oracle.truffle.r.nodes.function.ClassHierarchyNodeGen;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RStringVector;

/**
 * Checks if given object has given R class. More specifically: whether its attribute class is a
 * vector that contains given class name as an element.
 */
public final class InheritsCheckNode extends Node {

    @Child private ClassHierarchyNode classHierarchy = ClassHierarchyNodeGen.create(false, false);
    private final ConditionProfile nullClassProfile = ConditionProfile.createBinaryProfile();
    @CompilationFinal private ConditionProfile exactMatchProfile;
    private final String checkedClazz;

    public InheritsCheckNode(String checkedClazz) {
        this.checkedClazz = checkedClazz;
        assert RType.fromMode(checkedClazz) == null : "Class '" + checkedClazz + "' cannot be checked by InheritsCheckNode";
    }

    public static InheritsCheckNode createFactor() {
        return new InheritsCheckNode(RRuntime.CLASS_FACTOR);
    }

    public boolean execute(Object value) {
        if (value instanceof RMissing) {
            return false;
        }

        RStringVector clazz = classHierarchy.execute(value);
        if (nullClassProfile.profile(clazz != null)) {
            for (int j = 0; j < clazz.getLength(); ++j) {
                if (exactMatchProfile == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    exactMatchProfile = ConditionProfile.createBinaryProfile();
                }
                if (exactMatchProfile.profile(clazz.getDataAt(j) == checkedClazz) || clazz.getDataAt(j).equals(checkedClazz)) {
                    return true;
                }
            }
        }
        return false;
    }
}
