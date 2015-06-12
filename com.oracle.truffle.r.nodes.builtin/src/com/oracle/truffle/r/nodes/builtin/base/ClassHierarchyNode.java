/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.builtin.base.ClassHierarchyNodeGen.AttributeAccessNodeGen;
import com.oracle.truffle.r.nodes.builtin.base.ClassHierarchyNodeGen.VectorClassHierarchyNodeGen;
import com.oracle.truffle.r.nodes.builtin.base.S3DispatchFunctions.UseMethod;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.conn.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;

/**
 * Refactored out of {@link UseMethod} to avoid Eclipse annotation processor circularity.
 */
public abstract class ClassHierarchyNode extends UnaryNode {

    public abstract RStringVector execute(Object arg);

    @Specialization
    protected RStringVector getClassHr(@SuppressWarnings("unused") byte arg) {
        return RDataFactory.createStringVector(RType.Logical.getName());
    }

    @Specialization
    protected RStringVector getClassHr(@SuppressWarnings("unused") String arg) {
        return RDataFactory.createStringVector(RType.Character.getName());
    }

    @Specialization
    protected RStringVector getClassHr(@SuppressWarnings("unused") int arg) {
        return RDataFactory.createStringVector(RType.Integer.getName());
    }

    @Specialization
    protected RStringVector getClassHr(@SuppressWarnings("unused") double arg) {
        return RDataFactory.createStringVector(RRuntime.CLASS_DOUBLE, RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization
    protected RStringVector getClassHr(@SuppressWarnings("unused") RComplex arg) {
        return RDataFactory.createStringVector(RType.Complex.getName());
    }

    @Specialization
    protected RStringVector getClassHr(@SuppressWarnings("unused") RRaw arg) {
        return RDataFactory.createStringVector(RType.Raw.getName());
    }

    @Specialization
    protected RStringVector getClassHr(@SuppressWarnings("unused") RFunction arg) {
        return RDataFactory.createStringVector(RType.Function.getName());
    }

    @Specialization
    protected RStringVector getClassHr(@SuppressWarnings("unused") RNull arg) {
        return RDataFactory.createStringVector(RType.Null.getName());
    }

    @Specialization
    protected RStringVector getClassHr(@SuppressWarnings("unused") RSymbol arg) {
        return RDataFactory.createStringVector(RRuntime.CLASS_SYMBOL);
    }

    @Specialization
    protected RStringVector getClassHr(@SuppressWarnings("unused") RExternalPtr arg) {
        return RDataFactory.createStringVector(RType.ExternalPtr.getName());
    }

    @Specialization
    protected RStringVector getClassHr(@SuppressWarnings("unused") REnvironment arg) {
        return RDataFactory.createStringVector(RType.Environment.getName());
    }

    @Specialization
    protected RStringVector getClassHr(@SuppressWarnings("unused") RFormula arg) {
        return RDataFactory.createStringVector(RType.Formula.getName());
    }

    @Specialization
    protected RStringVector getClassHr(RConnection arg) {
        return arg.getClassHierarchy();
    }

    /**
     * Simple attribute access node that specializes on the position at which the attribute was
     * found last time.
     */
    public abstract static class AttributeAccess extends Node {

        protected final String name;

        protected AttributeAccess(String name) {
            this.name = name.intern();
        }

        public abstract Object execute(RAttributes attr);

        protected boolean nameMatches(RAttributes attr, int index) {
            /*
             * The length check is against names.length instead of size, so that the check folds
             * into the array bounds check.
             */
            return index != -1 && attr.getNames().length > index && attr.getNames()[index] == name;
        }

        @Specialization(guards = "nameMatches(attr, index)")
        protected Object accessCached(RAttributes attr, //
                        @Cached("attr.find(name)") int index) {
            return attr.getValues()[index];
        }

        @Specialization(contains = "accessCached")
        protected Object access(RAttributes attr) {
            return attr.get(name);
        }
    }

    /**
     *
     */
    public abstract static class VectorClassHierarchyNode extends Node {

        public abstract RStringVector execute(RVector vector);

        @Specialization(guards = "arg.getAttributes() == null")
        protected RStringVector getImplicitClass(RVector arg, //
                        @Cached("createClassProfile()") ValueProfile typeProfile) {
            return typeProfile.profile(arg).getImplicitClassHr();
        }

        protected AttributeAccess createAccess() {
            return AttributeAccessNodeGen.create(RRuntime.CLASS_ATTR_KEY);
        }

        @Specialization
        protected RStringVector getClass(RVector arg, //
                        @Cached("createAccess()") AttributeAccess access, //
                        @Cached("createBinaryProfile()") ConditionProfile isNullProfile, //
                        @Cached("createClassProfile()") ValueProfile typeProfile) {
            Object value = access.execute(arg.getAttributes());
            if (isNullProfile.profile(value == null)) {
                return typeProfile.profile(arg).getImplicitClassHr();
            } else {
                return (RStringVector) value;
            }
        }
    }

    public VectorClassHierarchyNode createVectorClass() {
        return VectorClassHierarchyNodeGen.create();
    }

    @Specialization
    protected RStringVector getClassHr(RVector arg, @Cached("createVectorClass()") VectorClassHierarchyNode vectorClass) {
        return vectorClass.execute(arg);
    }

    protected static boolean isRVector(Object arg) {
        return arg instanceof RVector;
    }

    @Specialization(guards = "!isRVector(arg)")
    protected RStringVector getClassHr(RAbstractContainer arg) {
        return arg.getClassHierarchy();
    }

    @Fallback
    protected RStringVector getClassHr(Object obj) {
        throw RInternalError.shouldNotReachHere("type: " + obj.getClass());
    }
}
