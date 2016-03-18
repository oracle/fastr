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

package com.oracle.truffle.r.nodes.builtin.base;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.access.AccessSlotNode;
import com.oracle.truffle.r.nodes.access.AccessSlotNodeGen;
import com.oracle.truffle.r.nodes.access.ConstantNode;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.function.WrapArgumentNode;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RBuiltinKind;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RSymbol;

@RBuiltin(name = "@", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"", ""}, nonEvalArgs = 1)
public abstract class Slot extends RBuiltinNode {

    @Child private AccessSlotNode accessSlotNode = AccessSlotNodeGen.create(null, null);

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.toAttributable(0, true, true, true);
    }

    private String getName(Object nameObj) {
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

    @Specialization
    protected Object getSlot(Object object, Object nameObj) {
        String name = getName(nameObj);
        assert name == name.intern();
        return accessSlotNode.executeAccess(object, name);
    }
}
