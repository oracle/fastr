/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;

/**
 * Refactored out of {@link UseMethod} to avoid Eclipse annotation processor circularity.
 */
public abstract class ClassHierarchyNode extends UnaryNode {

    public abstract RStringVector execute(VirtualFrame frame, Object arg);

    @Specialization
    protected RStringVector getClassHr(@SuppressWarnings("unused") byte arg) {
        return RDataFactory.createStringVector(RRuntime.TYPE_LOGICAL);
    }

    @Specialization
    protected RStringVector getClassHr(@SuppressWarnings("unused") String arg) {
        return RDataFactory.createStringVector(RRuntime.TYPE_CHARACTER);
    }

    @Specialization
    protected RStringVector getClassHr(@SuppressWarnings("unused") int arg) {
        return RDataFactory.createStringVector(RRuntime.TYPE_INTEGER);
    }

    @Specialization
    protected RStringVector getClassHr(@SuppressWarnings("unused") double arg) {
        return RDataFactory.createStringVector(RRuntime.CLASS_DOUBLE, RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization
    protected RStringVector getClassHr(@SuppressWarnings("unused") RComplex arg) {
        return RDataFactory.createStringVector(RRuntime.TYPE_COMPLEX);
    }

    @Specialization
    protected RStringVector getClassHr(@SuppressWarnings("unused") RFunction arg) {
        return RDataFactory.createStringVector(RRuntime.TYPE_FUNCTION);
    }

    @Specialization
    protected RStringVector getClassHr(@SuppressWarnings("unused") RNull arg) {
        return RDataFactory.createStringVector(RRuntime.NULL);
    }

    @Specialization
    protected RStringVector getClassHr(@SuppressWarnings("unused") RSymbol arg) {
        return RDataFactory.createStringVector(RRuntime.CLASS_SYMBOL);
    }

    @Specialization
    protected RStringVector getClassHr(@SuppressWarnings("unused") REnvironment arg) {
        return RDataFactory.createStringVector(RRuntime.TYPE_ENVIRONMENT);
    }

    @Specialization
    protected RStringVector getClassHr(@SuppressWarnings("unused") RPairList arg) {
        return RDataFactory.createStringVector(RRuntime.TYPE_PAIR_LIST);
    }

    @Specialization
    protected RStringVector getClassHr(@SuppressWarnings("unused") RLanguage arg) {
        return RDataFactory.createStringVector(RRuntime.CLASS_LANGUAGE);
    }

    @Specialization
    protected RStringVector getClassHr(RAbstractContainer arg) {
        return arg.getClassHierarchy();
    }
}
