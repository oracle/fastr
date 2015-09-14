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
package com.oracle.truffle.r.nodes.function;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.attributes.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

public abstract class ClassHierarchyNode extends UnaryNode {

    public static final RStringVector truffleObjectClassHeader = RDataFactory.createStringVectorFromScalar("truffle.object");

    @Child private AttributeAccess access;

    private final boolean withImplicitTypes;
    private final ConditionProfile noAttributesProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile nullAttributeProfile = ConditionProfile.createBinaryProfile();

    protected ClassHierarchyNode(boolean withImplicitTypes) {
        this.withImplicitTypes = withImplicitTypes;
    }

    @Override
    public abstract RStringVector execute(Object arg);

    @Specialization
    protected RStringVector getClassHr(@SuppressWarnings("unused") byte arg) {
        return withImplicitTypes ? RLogicalVector.implicitClassHeader : null;
    }

    @Specialization
    protected RStringVector getClassHr(@SuppressWarnings("unused") String arg) {
        return withImplicitTypes ? RStringVector.implicitClassHeader : null;
    }

    @Specialization
    protected RStringVector getClassHr(@SuppressWarnings("unused") int arg) {
        return withImplicitTypes ? RIntVector.implicitClassHeader : null;
    }

    @Specialization
    protected RStringVector getClassHr(@SuppressWarnings("unused") double arg) {
        return withImplicitTypes ? RDoubleVector.implicitClassHeader : null;
    }

    @Specialization
    protected RStringVector getClassHr(@SuppressWarnings("unused") RComplex arg) {
        return withImplicitTypes ? RComplexVector.implicitClassHeader : null;
    }

    @Specialization
    protected RStringVector getClassHr(@SuppressWarnings("unused") RNull arg) {
        return withImplicitTypes ? RNull.implicitClassHeader : null;
    }

    @Specialization
    protected RStringVector getClassHrStorage(RAttributeStorage arg, //
                    @Cached("createClassProfile()") ValueProfile argProfile) {
        return getClassHrAttributable(arg, argProfile);
    }

    @Specialization(contains = "getClassHrStorage")
    protected RStringVector getClassHrAttributable(RAttributable arg, //
                    @Cached("createClassProfile()") ValueProfile argProfile) {
        RAttributable profiledArg = argProfile.profile(arg);
        RAttributes attributes = profiledArg.getAttributes();
        if (noAttributesProfile.profile(attributes != null)) {
            if (access == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                access = insert(AttributeAccessNodeGen.create(RRuntime.CLASS_ATTR_KEY));
            }
            RStringVector classHierarcy = (RStringVector) access.execute(attributes);
            if (nullAttributeProfile.profile(classHierarcy != null)) {
                return classHierarcy;
            }
        }
        return withImplicitTypes ? profiledArg.getImplicitClass() : null;
    }

    protected static boolean isRTypedValue(Object obj) {
        return obj instanceof RTypedValue;
    }

    @Specialization(guards = "!isRTypedValue(object)")
    protected RStringVector getClassHrTruffleObject(@SuppressWarnings("unused") TruffleObject object) {
        return truffleObjectClassHeader;
    }

    @Fallback
    protected RStringVector getClassHr(Object obj) {
        throw RInternalError.shouldNotReachHere("type: " + obj.getClass());
    }
}
