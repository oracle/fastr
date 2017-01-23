/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.function;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.r.nodes.unary.UnaryNode;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;

public abstract class ClassHierarchyScalarNode extends UnaryNode {

    public abstract String executeString(Object o);

    @Child private ClassHierarchyNode classHierarchyNode = ClassHierarchyNodeGen.create(true, true);

    @Specialization
    protected String getClassHr(Object arg) {
        return classHierarchyNode.execute(arg).getDataAt(0);
    }

    public static String get(Object arg) {
        CompilerAsserts.neverPartOfCompilation();

        Object v = RRuntime.asAbstractVector(arg);
        if (v instanceof RAttributable) {
            RStringVector classHierarchy = ((RAttributable) v).getClassHierarchy();
            return classHierarchy.getLength() == 0 ? "" : classHierarchy.getDataAt(0);
        } else if (arg == RNull.instance) {
            return "NULL";
        } else {
            assert arg instanceof TruffleObject;
            return "";
        }
    }
}
