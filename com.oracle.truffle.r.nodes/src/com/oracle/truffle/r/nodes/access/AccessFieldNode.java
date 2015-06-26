/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.access;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.gnur.*;

/**
 * Perform a field access. This node represents the {@code $} operator in R.
 */
@NodeChildren({@NodeChild(value = "object", type = RNode.class), @NodeChild(value = "field", type = RNode.class)})
public abstract class AccessFieldNode extends RNode implements RSyntaxNode {

    public abstract Object executeAccess(VirtualFrame frame, Object o, String field);

    public abstract RNode getObject();

    public abstract RNode getField();

    @Child private AccessFieldNode accessRecursive;
    @Child private UseMethodInternalNode dcn;
    public final boolean forObjects;

    protected final ConditionProfile hasNamesProfile = ConditionProfile.createBinaryProfile();
    protected final BranchProfile inexactMatch = BranchProfile.create();
    protected final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

    public AccessFieldNode(boolean forObjects) {
        this.forObjects = forObjects;
    }

    private Object accessRecursive(VirtualFrame frame, RAbstractContainer container, String field) {
        if (accessRecursive == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            accessRecursive = insert(AccessFieldNodeGen.create(false, null, null));
        }
        return accessRecursive.executeAccess(frame, container, field);
    }

    @TruffleBoundary
    public static int getElementIndexByName(RStringVector names, String name) {
        for (int i = 0; i < names.getLength(); i++) {
            if (names.getDataAt(i).equals(name)) {
                return i;
            }
        }
        return -1;
    }

    @Specialization
    protected RNull access(@SuppressWarnings("unused") RNull object, @SuppressWarnings("unused") String field) {
        return RNull.instance;
    }

    @Specialization
    protected Object accessField(RList object, String field) {
        RStringVector names = object.getNames(attrProfiles);
        if (hasNamesProfile.profile(names != null)) {
            int index = getElementIndexByName(names, field);
            if (index == -1) {
                inexactMatch.enter();
                index = object.getElementIndexByNameInexact(attrProfiles, field);
            }
            return index == -1 ? RNull.instance : object.getDataAt(index);
        } else {
            return RNull.instance;
        }
    }

    @Specialization(guards = "isObject(container)")
    protected Object accessField(VirtualFrame frame, RAbstractContainer container, String field) {
        if (dcn == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            dcn = insert(new UseMethodInternalNode("$", ArgumentsSignature.get("", ""), true));
        }
        try {
            return dcn.execute(frame, container, new Object[]{container, field});
        } catch (S3FunctionLookupNode.NoGenericMethodException e) {
            return accessRecursive(frame, container, field);
        }
    }

    @Specialization
    protected Object accessField(REnvironment env, String field) {
        Object obj = env.get(field);
        return obj == null ? RNull.instance : obj;
    }

    @Specialization
    protected Object accessField(@SuppressWarnings("unused") RAbstractVector objec, @SuppressWarnings("unused") String field) {
        throw RError.error(RError.Message.DOLLAR_ATOMIC_VECTORS);
    }

    @Specialization
    protected Object accessFieldHasNames(RLanguage object, String field) {
        RStringVector names = object.getNames(attrProfiles);
        if (hasNamesProfile.profile(names != null)) {
            int index = getElementIndexByName(names, field);
            return index == -1 ? RNull.instance : RContext.getRRuntimeASTAccess().getDataAtAsObject(object, index);
        } else {
            return RNull.instance;
        }
    }

    @Override
    public void deparse(RDeparse.State state) {
        RSyntaxNode.cast(getObject()).deparse(state);
        state.append('$');
        RSyntaxNode.cast(getField()).deparse(state);
    }

    @Override
    public void serialize(RSerialize.State state) {
        state.setAsBuiltin("$");
        state.openPairList(SEXPTYPE.LISTSXP);
        state.serializeNodeSetCar(getObject());
        state.openPairList(SEXPTYPE.LISTSXP);
        state.serializeNodeSetCar(getField());
        state.linkPairList(2);
        state.setCdr(state.closePairList());
    }

    @Override
    public RSyntaxNode substitute(REnvironment env) {
        RNode o = RSyntaxNode.cast(getObject()).substitute(env).asRNode();
        RNode field = RSyntaxNode.cast(getField()).substitute(env).asRNode();
        return AccessFieldNodeGen.create(forObjects, o, field);
    }

    protected boolean isObject(RAbstractContainer container) {
        return container.isObject(attrProfiles) && forObjects;
    }

}
