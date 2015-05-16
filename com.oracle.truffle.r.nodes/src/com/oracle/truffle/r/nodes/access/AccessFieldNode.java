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

import com.oracle.truffle.api.CompilerDirectives.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.gnur.*;

/**
 * Perform a field access. This node represents the {@code $} operator in R.
 */
@NodeChild(value = "object", type = RNode.class)
@NodeField(name = "field", type = String.class)
public abstract class AccessFieldNode extends RNode implements RSyntaxNode {

    public abstract RNode getObject();

    public abstract String getField();

    protected final ConditionProfile hasNamesProfile = ConditionProfile.createBinaryProfile();
    protected final BranchProfile inexactMatch = BranchProfile.create();
    protected final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

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
    protected RNull access(@SuppressWarnings("unused") RNull object) {
        return RNull.instance;
    }

    @Specialization
    protected Object accessField(RList object) {
        RStringVector names = object.getNames(attrProfiles);
        if (hasNamesProfile.profile(names != null)) {
            int index = getElementIndexByName(names, getField());
            if (index == -1) {
                inexactMatch.enter();
                index = object.getElementIndexByNameInexact(attrProfiles, getField());
            }
            return index == -1 ? RNull.instance : object.getDataAt(index);
        } else {
            return RNull.instance;
        }
    }

    // TODO: this should ultimately be a generic function
    @Specialization
    protected Object accessField(RDataFrame object) {
        RStringVector names = object.getNames(attrProfiles);
        if (hasNamesProfile.profile(names != null)) {
            int index = getElementIndexByName(names, getField());
            if (index == -1) {
                inexactMatch.enter();
                index = object.getElementIndexByNameInexact(attrProfiles, getField());
                // TODO: add warning if index found (disabled by default using options)
            }
            return index == -1 ? RNull.instance : object.getDataAtAsObject(index);
        } else {
            return RNull.instance;
        }
    }

    @Specialization
    protected Object accessField(REnvironment env) {
        Object obj = env.get(getField());
        return obj == null ? RNull.instance : obj;
    }

    @Specialization
    protected Object accessField(@SuppressWarnings("unused") RAbstractVector object) {
        throw RError.error(RError.Message.DOLLAR_ATOMIC_VECTORS);
    }

    @Specialization
    protected Object accessFieldHasNames(RLanguage object) {
        RStringVector names = object.getNames(attrProfiles);
        if (hasNamesProfile.profile(names != null)) {
            int index = getElementIndexByName(names, getField());
            return index == -1 ? RNull.instance : RContext.getRRuntimeASTAccess().getDataAtAsObject(object, index);
        } else {
            return RNull.instance;
        }
    }

    @Override
    public void deparse(RDeparse.State state) {
        RSyntaxNode.cast(getObject()).deparse(state);
        state.append('$');
        state.append(getField());
    }

    @Override
    public void serialize(RSerialize.State state) {
        state.setAsBuiltin("$");
        state.openPairList(SEXPTYPE.LISTSXP);
        state.serializeNodeSetCar(getObject());
        state.openPairList(SEXPTYPE.LISTSXP);
        state.setCarAsSymbol(getField());
        state.linkPairList(2);
        state.setCdr(state.closePairList());
    }

    @Override
    public RSyntaxNode substitute(REnvironment env) {
        RSyntaxNode object = RSyntaxNode.cast(getObject()).substitute(env);
        String field = getField();
        RNode fieldSub = RASTUtils.substituteName(field, env);
        if (fieldSub != null) {
            field = RASTUtils.expectName(fieldSub);
        }
        return AccessFieldNodeGen.create(object.asRNode(), field);
    }
}
