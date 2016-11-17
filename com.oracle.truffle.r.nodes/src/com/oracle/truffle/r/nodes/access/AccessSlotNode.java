/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1995-2014, The R Core Team
 * Copyright (c) 2002-2008, The R Foundation
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.access;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.attributes.AttributeAccess;
import com.oracle.truffle.r.nodes.attributes.AttributeAccessNodeGen;
import com.oracle.truffle.r.nodes.attributes.InitAttributesNode;
import com.oracle.truffle.r.nodes.function.ClassHierarchyNode;
import com.oracle.truffle.r.nodes.function.ClassHierarchyNodeGen;
import com.oracle.truffle.r.nodes.unary.TypeofNode;
import com.oracle.truffle.r.nodes.unary.TypeofNodeGen;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RAttributeProfiles;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.nodes.RNode;

/**
 * Perform a slot access. This node represents the {@code @} operator in R.
 */
@NodeChildren({@NodeChild(value = "object", type = RNode.class), @NodeChild(value = "name", type = RNode.class)})
public abstract class AccessSlotNode extends RNode {

    public abstract Object executeAccess(Object o, String name);

    @Child private ClassHierarchyNode classHierarchy;
    @Child private TypeofNode typeofNode;

    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();
    private final BranchProfile noSlot = BranchProfile.create();
    private final BranchProfile symbolValue = BranchProfile.create();
    private final boolean asOperator;

    public AccessSlotNode(boolean asOperator) {
        this.asOperator = asOperator;
    }

    protected AttributeAccess createAttrAccess(String name) {
        return AttributeAccessNodeGen.create(name);
    }

    private Object getSlotS4Internal(RAttributable object, String name, Object value) {
        if (value == null) {
            noSlot.enter();
            assert Utils.isInterned(name);
            if (name == RRuntime.DOT_S3_CLASS) {
                if (classHierarchy == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    classHierarchy = insert(ClassHierarchyNodeGen.create(true, false));
                }
                return classHierarchy.execute(object);
            } else if (name == RRuntime.DOT_DATA) {
                // TODO: any way to cache it or use a mechanism similar to overrides?
                REnvironment methodsNamespace = REnvironment.getRegisteredNamespace("methods");
                RFunction dataPart = getDataPartFunction(methodsNamespace);
                return RContext.getEngine().evalFunction(dataPart, methodsNamespace.getFrame(), RCaller.create(Utils.getActualCurrentFrame(), RASTUtils.getOriginalCall(this)), null, object);
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
        if (value instanceof RSymbol) {
            symbolValue.enter();
            if (((RSymbol) value).getName() == RRuntime.PSEUDO_NULL.getName()) {
                return RNull.instance;
            }
        }
        return value;
    }

    @Specialization
    protected Object getSlotS4(@SuppressWarnings("unused") RNull object, String name) {
        throw RError.error(this, RError.Message.SLOT_BASIC_CLASS, name, "NULL");
    }

    @Specialization(guards = {"slotAccessAllowed(object)", "name == cachedName"})
    protected Object getSlotS4Cached(RAttributable object, @SuppressWarnings("unused") String name, @Cached("name") String cachedName,
                    @Cached("createAttrAccess(cachedName)") AttributeAccess attrAccess, //
                    @Cached("create()") InitAttributesNode initAttrNode) {
        Object value = attrAccess.execute(initAttrNode.execute(object));
        return getSlotS4Internal(object, cachedName, value);
    }

    @Specialization(contains = "getSlotS4Cached", guards = "slotAccessAllowed(object)")
    protected Object getSlotS4(RAttributable object, String name) {
        String internedName = Utils.intern(name);
        Object value = object.getAttr(attrProfiles, internedName);
        return getSlotS4Internal(object, internedName, value);
    }

    @TruffleBoundary
    private static RFunction getDataPartFunction(REnvironment methodsNamespace) {
        String name = "getDataPart";
        Object f = methodsNamespace.findFunction(name);
        return (RFunction) RContext.getRRuntimeASTAccess().forcePromise(name, f);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!slotAccessAllowed(object)", "isDotData(name)"})
    protected Object getSlotNonS4(RAttributable object, String name) {
        // TODO: any way to cache it or use a mechanism similar to overrides?
        REnvironment methodsNamespace = REnvironment.getRegisteredNamespace("methods");
        RFunction dataPart = getDataPartFunction(methodsNamespace);
        return RContext.getEngine().evalFunction(dataPart, methodsNamespace.getFrame(), RCaller.create(Utils.getActualCurrentFrame(), RASTUtils.getOriginalCall(this)), null, object);
    }

    // this is really a fallback specialization but @Fallback does not work here (because of the
    // type of "object"?)
    @Specialization(guards = {"!slotAccessAllowed(object)", "!isDotData(name)"})
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
        assert Utils.isInterned(name);
        return name == RRuntime.DOT_DATA;
    }

    protected boolean slotAccessAllowed(RAttributable object) {
        return object.isS4() || !asOperator;
    }
}
