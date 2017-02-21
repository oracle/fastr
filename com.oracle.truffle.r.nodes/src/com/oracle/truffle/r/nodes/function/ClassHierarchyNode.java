/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.attributes.GetFixedAttributeNode;
import com.oracle.truffle.r.nodes.unary.CastToVectorNode;
import com.oracle.truffle.r.nodes.unary.UnaryNode;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RAttributeStorage;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RTypedValue;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

public abstract class ClassHierarchyNode extends UnaryNode {

    private static final RStringVector truffleObjectClassHeader = RDataFactory.createStringVectorFromScalar("truffle.object");

    @Child private GetFixedAttributeNode access;
    @Child private S4Class s4Class;

    private final boolean withImplicitTypes;
    private final boolean withS4;
    private final ConditionProfile noAttributesProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile nullAttributeProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile isS4Profile = ConditionProfile.createBinaryProfile();

    protected ClassHierarchyNode(boolean withImplicitTypes, boolean withS4) {
        this.withImplicitTypes = withImplicitTypes;
        this.withS4 = withS4;
    }

    public static ClassHierarchyNode create() {
        return ClassHierarchyNodeGen.create(false, false);
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
    protected RStringVector getClassHrAttributable(RAttributable arg,
                    @Cached("createBinaryProfile()") ConditionProfile attrStorageProfile,
                    @Cached("createClassProfile()") ValueProfile argProfile) {

        DynamicObject attributes;
        if (attrStorageProfile.profile(arg instanceof RAttributeStorage)) {
            // Note: the seemingly unnecessary cast is here to ensure the method can be inlined
            // Note2: the attrStorageProfile and cast is better at helping compiler to inline
            // 'getAttributes' than just the ValueProfile in else branch, which degrades when it
            // sees two different classes
            attributes = ((RAttributeStorage) arg).getAttributes();
        } else {
            attributes = argProfile.profile(arg).getAttributes();
        }
        if (noAttributesProfile.profile(attributes != null)) {
            if (access == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                access = insert(GetFixedAttributeNode.createClass());
            }
            RStringVector classHierarchy = (RStringVector) access.execute(attributes);
            if (nullAttributeProfile.profile(classHierarchy != null)) {
                if (withS4 && argProfile.profile(arg).isS4() && isS4Profile.profile(classHierarchy.getLength() > 0)) {
                    if (s4Class == null) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        s4Class = insert(S4ClassNodeGen.create());
                    }
                    return s4Class.executeRStringVector(classHierarchy.getDataAt(0));
                }
                return classHierarchy;
            }
        }
        return withImplicitTypes ? argProfile.profile(arg).getImplicitClass() : null;
    }

    protected static boolean isRTypedValue(Object obj) {
        return obj instanceof RTypedValue;
    }

    @Specialization(guards = "!isRTypedValue(object)")
    protected RStringVector getClassHrTruffleObject(@SuppressWarnings("unused") TruffleObject object) {
        return truffleObjectClassHeader;
    }

    @Specialization
    protected RStringVector getClassHrVarArgs(@SuppressWarnings("unused") RArgsValuesAndNames args) {
        // TODO: reinvestigate - this should not be necessary
        return RDataFactory.createEmptyStringVector();
    }

    @Fallback
    protected RStringVector getClassHr(Object obj) {
        throw RInternalError.shouldNotReachHere("type: " + (obj == null ? "null" : obj.getClass().getSimpleName()));
    }
}

abstract class S4Class extends RBaseNode {

    public abstract RStringVector executeRStringVector(String classAttr);

    @Child private CastToVectorNode castToVector = CastToVectorNode.create();

    @TruffleBoundary
    protected RStringVector getS4ClassInternal(String classAttr) {
        // GNU R contains an explicit check here to make sure that methods package is available but
        // we operate under this assumption already
        RStringVector s4Extends = RContext.getInstance().getS4Extends(classAttr);
        if (s4Extends == null) {
            REnvironment methodsEnv = REnvironment.getRegisteredNamespace("methods");
            RFunction sExtendsForS3Function = ReadVariableNode.lookupFunction(".extendsForS3", methodsEnv.getFrame());
            // the assumption here is that the R function can only return either a String or
            // RStringVector
            s4Extends = (RStringVector) castToVector.execute(
                            RContext.getEngine().evalFunction(sExtendsForS3Function, methodsEnv.getFrame(), RCaller.create(null, RASTUtils.getOriginalCall(this)), null, classAttr));
            RContext.getInstance().putS4Extends(classAttr, s4Extends);
        }
        return s4Extends;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "classAttr == cachedClassAttr")
    protected RStringVector getS4ClassCachedEqOp(String classAttr,
                    @Cached("classAttr") String cachedClassAttr,
                    @Cached("getS4ClassInternal(cachedClassAttr)") RStringVector s4Classes) {
        return s4Classes;
    }

    /*
     * Class names are normally string literals with == operator being sufficient for comparison,
     * but we probably cannot rely on this 100% of time and should use equals() method as backup.
     */
    @SuppressWarnings("unused")
    @Specialization(replaces = "getS4ClassCachedEqOp", guards = "classAttr.equals(cachedClassAttr)")
    protected RStringVector getS4ClassCachedEqMethod(String classAttr,
                    @Cached("classAttr") String cachedClassAttr,
                    @Cached("getS4ClassInternal(cachedClassAttr)") RStringVector s4Classes) {
        return s4Classes;
    }

    @Specialization(replaces = "getS4ClassCachedEqMethod")
    protected RStringVector getS4Class(String classAttr) {
        return getS4ClassInternal(classAttr);
    }
}
