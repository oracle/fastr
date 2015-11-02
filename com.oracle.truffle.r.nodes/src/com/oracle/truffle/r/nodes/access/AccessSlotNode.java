/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1995-2014, The R Core Team
 * Copyright (c) 2002-2008, The R Foundation
 * Copyright (c) 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.access;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.utilities.BranchProfile;
import com.oracle.truffle.api.utilities.ConditionProfile;
import com.oracle.truffle.r.nodes.attributes.AttributeAccess;
import com.oracle.truffle.r.nodes.attributes.AttributeAccessNodeGen;
import com.oracle.truffle.r.nodes.function.ClassHierarchyNode;
import com.oracle.truffle.r.nodes.function.ClassHierarchyNodeGen;
import com.oracle.truffle.r.nodes.unary.TypeofNode;
import com.oracle.truffle.r.nodes.unary.TypeofNodeGen;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.context.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.nodes.*;

/**
 * Perform a slot access. This node represents the {@code @} operator in R.
 */
@NodeChildren({@NodeChild(value = "object", type = RNode.class), @NodeChild(value = "name", type = RNode.class)})
public abstract class AccessSlotNode extends RNode {

    public abstract Object executeAccess(Object o, String name);

    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();
    @Child private ClassHierarchyNode classHierarchy;
    @Child private TypeofNode typeofNode;
    private final BranchProfile noSlot = BranchProfile.create();
    private final ConditionProfile nullSlot = ConditionProfile.createBinaryProfile();

    protected AttributeAccess createAttrAccess(String name) {
        return AttributeAccessNodeGen.create(name);
    }

    private Object getSlotS4Internal(RAttributable object, String name, Object value) {
        if (value == null) {
            noSlot.enter();
            if (name == RRuntime.DOT_S3_CLASS) {
                // TODO: this will not work if `@` function is called directly, as in:
                // `@`(x, ".S3Class")
                // in general, treatment of the name parameter has to be finessed to be
                // fully compatible with GNU R on direct calls to `@` function
                if (classHierarchy == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    classHierarchy = insert(ClassHierarchyNodeGen.create(true));
                }
                return classHierarchy.execute(object);
            } else if (name == RRuntime.DOT_DATA) {
                return getDataPart(object);
            } else if (name == RRuntime.NAMES_ATTR_KEY && object instanceof RAbstractVector) {
                assert false; // RS4Object can never be a vector?
                return RNull.instance;
            }

            RStringVector classAttr = object.getClassAttr(attrProfiles);
            if (classAttr == null) {
                if (typeofNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    typeofNode = insert(TypeofNodeGen.create());
                }
                throw RError.error(this, RError.Message.SLOT_CANNOT_GET, name, typeofNode.execute(object).getName());
            } else {
                throw RError.error(this, RError.Message.SLOT_NONE, name, classAttr.getLength() == 0 ? RRuntime.STRING_NA : classAttr.getDataAt(0));
            }
        }
        return value;
    }

    @Specialization(guards = {"object.isS4()", "name == cachedName"})
    protected Object getSlotS4Cached(RAttributable object, @SuppressWarnings("unused") String name, @Cached("name") String cachedName,
                    @Cached("createAttrAccess(cachedName)") AttributeAccess attrAccess) {
        Object value = attrAccess.execute(object.getAttributes());
        return getSlotS4Internal(object, cachedName, value);
    }

    @Specialization(contains = "getSlotS4Cached", guards = "object.isS4()")
    protected Object getSlotS4(RAttributable object, String name) {
        Object value = object.getAttr(attrProfiles, name.intern());
        return getSlotS4Internal(object, name, value);
    }

    protected RFunction getDataPartFunction(REnvironment methodsNamespace) {
        Object f = methodsNamespace.findFunction("getDataPart");
        return (RFunction) RContext.getRRuntimeASTAccess().forcePromise(f);
    }

    private Object getDataPart(RAttributable object) {
        // TODO: any way to cache it or use a mechanism similar to overrides?
        REnvironment methodsNamespace = REnvironment.getRegisteredNamespace("methods");
        RFunction dataPart = getDataPartFunction(methodsNamespace);
        return RContext.getEngine().evalFunction(dataPart, methodsNamespace.getFrame(), object);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!object.isS4()", "isDotData(name)"})
    protected Object getSlotNonS4(RAttributable object, String name) {
        return getDataPart(object);
    }

    // this is really a fallback specialization but @Fallback does not work here (because of the
    // type of "object"?)
    @Specialization(guards = {"!object.isS4()", "!isDotData(name)"})
    protected Object getSlot(RAttributable object, String name) {
        RStringVector classAttr = object.getClassAttr(attrProfiles);
        if (classAttr == null) {
            RStringVector implicitClassVec = object.getImplicitClass();
            assert implicitClassVec.getLength() > 0;
            throw RError.error(this, RError.Message.SLOT_BASIC_CLASS, name, implicitClassVec.getDataAt(0));
        } else {
            assert classAttr.getLength() > 0;
            throw RError.error(this, RError.Message.SLOT_NON_S4, name, classAttr.getDataAt(0));
        }
    }

    protected boolean isDotData(String name) {
        // see comment on usinq object equality in getSlotS4()
        return name == RRuntime.DOT_DATA;
    }
}
