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

package com.oracle.truffle.r.nodes.builtin.base;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.access.ConstantNode;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.attributes.AttributeAccess;
import com.oracle.truffle.r.nodes.attributes.AttributeAccessNodeGen;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.function.ClassHierarchyNode;
import com.oracle.truffle.r.nodes.function.ClassHierarchyNodeGen;
import com.oracle.truffle.r.nodes.function.WrapArgumentNode;
import com.oracle.truffle.r.nodes.unary.TypeofNode;
import com.oracle.truffle.r.nodes.unary.TypeofNodeGen;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.REnvironment;

@RBuiltin(name = "@", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"", ""}, nonEvalArgs = 1)
public abstract class Slot extends RBuiltinNode {

    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();
    @Child private ClassHierarchyNode classHierarchy;
    @Child private TypeofNode typeofNode;

    protected AttributeAccess createAttrAccess(String name) {
        return AttributeAccessNodeGen.create(name);
    }

    protected String getName(Object nameObj) {
        if (nameObj instanceof RPromise) {
            Object rep = ((RPromise) nameObj).getRep();
            if (rep instanceof WrapArgumentNode) {
                rep = ((WrapArgumentNode) rep).getOperand();
            }
            if (rep instanceof ConstantNode) {
                Object val = ((ConstantNode) rep).getValue();
                if (val instanceof String) {
                    return (String) val;
                }
                if (val instanceof RSymbol) {
                    return ((RSymbol) val).getName();
                }
            } else if (rep instanceof ReadVariableNode) {
                return ((ReadVariableNode) rep).getIdentifier();
            }
        }
        throw RError.error(this, RError.Message.GENERIC, "invalid type or length for slot name");
    }

    private Object getSlotS4Internal(RS4Object object, String name, Object value) {
        if (value == null) {
            if (name == RRuntime.DOT_S3_CLASS) {
                // TODO: this will not work if `@` function is called directly, as in:
                // `@`(x, ".S3Class")
                // in general, treatment of the name parameter has to be finessed to be
                // fully compatible with GNU R on direct calls to `@` function
                if (classHierarchy == null) {
                    classHierarchy = insert(ClassHierarchyNodeGen.create(true));
                }
                return classHierarchy.execute(object);
            } else if (name == RRuntime.NAMES_ATTR_KEY && object instanceof RAbstractVector) {
                assert false; // RS4Object can never be a vector?
                return RNull.instance;
            }

            RStringVector classAttr = object.getClassAttr(attrProfiles);
            if (classAttr == null) {
                if (typeofNode == null) {
                    typeofNode = insert(TypeofNodeGen.create());
                }
                throw RError.error(this, RError.Message.SLOT_CANNOT_GET, name, typeofNode.execute(object).getName());
            } else {
                throw RError.error(this, RError.Message.SLOT_NONE, name, classAttr.getLength() == 0 ? RRuntime.STRING_NA : classAttr.getDataAt(0));
            }
        }
        if (value == RRuntime.NULL_STR_VECTOR) {
            return RNull.instance;
        } else {
            return value;
        }
    }

    @Specialization(guards = "getName(nameObj) == cachedName")
    protected Object getSlotS4Cached(RS4Object object, @SuppressWarnings("unused") Object nameObj, @Cached("getName(nameObj)") String cachedName,
                    @Cached("createAttrAccess(cachedName)") AttributeAccess attrAccess) {
        Object value = attrAccess.execute(object.getAttributes());
        return getSlotS4Internal(object, cachedName, value);
    }

    @Specialization(contains = "getSlotS4Cached")
    protected Object getSlotS4(RS4Object object, Object nameObj) {
        String name = getName(nameObj);
        Object value = object.getAttr(attrProfiles, name.intern());
        return getSlotS4Internal(object, name, value);
    }

    protected RFunction getDataPart(REnvironment methodsNamespace) {
        Object f = methodsNamespace.findFunction("getDataPart");
        return (RFunction) RContext.getRRuntimeASTAccess().forcePromise(f);
    }

    protected REnvironment getMethodsNamespace() {
        return REnvironment.getRegisteredNamespace("methods");
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isS4(object)", "isDotData(getName(nameObj))"})
    protected Object getSlotNonS4(RAbstractContainer object, Object nameObj) {
        // TODO: any way to cache it?
        REnvironment methodsNamespace = REnvironment.getRegisteredNamespace("methods");
        RFunction dataPart = getDataPart(methodsNamespace);
        return RContext.getEngine().evalFunction(dataPart, methodsNamespace.getFrame(), object);
    }

    // this is really a fallback specialization but @Fallback does not work here (because of the
    // type of "object"?)
    @Specialization
    protected Object getSlot(RAbstractContainer object, Object nameObj) {
        if (!((nameObj instanceof RAbstractStringVector && ((RAbstractStringVector) nameObj).getLength() == 1) || (nameObj instanceof String))) {
            // second argument is wrong
            throw RError.error(this, RError.Message.GENERIC, "invalid type or length for slot name");
        } else {
            // first argument is wrong
            String name = nameObj instanceof String ? (String) nameObj : ((RAbstractStringVector) nameObj).getDataAt(0);

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
    }

    protected boolean isS4(Object o) {
        return o instanceof RS4Object;
    }

    protected boolean isDotData(String name) {
        // see comment on usinq object equality in getSlotS4()
        return name == RRuntime.DOT_DATA;
    }
}
