/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.ffi;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.data.RTypesFlatLayout;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

/**
 * Implements the GNU R function {@code isVectorAtomic} and checks whether length is > 0.
 */
@TypeSystemReference(RTypesFlatLayout.class)
public abstract class IsVectorAtomicNodeLG0 extends Node {
    public abstract boolean execute(Object obj);

    @Specialization
    protected boolean isVectorAtomicNode(@SuppressWarnings("unused") Byte obj) {
        return true;
    }

    @Specialization
    protected boolean isVectorAtomicNode(@SuppressWarnings("unused") String obj) {
        return true;
    }

    @Specialization
    protected boolean isVectorAtomicNode(RAbstractLogicalVector obj) {
        return obj.getLength() > 0;
    }

    @Specialization
    protected boolean isVectorAtomicNode(RAbstractIntVector obj) {
        return obj.getLength() > 0;
    }

    @Specialization
    protected boolean isVectorAtomicNode(RAbstractDoubleVector obj) {
        return obj.getLength() > 0;
    }

    @Specialization
    protected boolean isVectorAtomicNode(RAbstractStringVector obj) {
        return obj.getLength() > 0;
    }

    @Specialization
    protected boolean isVectorAtomicNode(RAbstractComplexVector obj) {
        return obj.getLength() > 0;
    }

    @Fallback
    protected boolean isVectorAtomicNode(@SuppressWarnings("unused") Object obj) {
        return false;
    }

    public static IsVectorAtomicNodeLG0 create() {
        return IsVectorAtomicNodeLG0NodeGen.create();
    }

}
